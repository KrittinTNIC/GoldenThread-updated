package com.example.finalproject.ui.thread

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalproject.DatabaseHelper
import com.example.finalproject.R
import com.example.finalproject.adapter.DramaLocationAdapter
import com.example.finalproject.databinding.FragmentThreadBinding
import com.example.finalproject.ui.thread.data.DL
import com.example.finalproject.ui.thread.data.Drama
import com.example.finalproject.ui.thread.data.LocationData
import com.example.finalproject.ui.thread.data.LocationDramaItem
import com.example.finalproject.util.FavoriteManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior

class ThreadFragment : Fragment(), OnMapReadyCallback,
    DramaLocationAdapter.OnItemButtonClickListener {

    private var _binding: FragmentThreadBinding? = null
    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null
    private val markers = mutableListOf<Marker>()
    private var iconBitmap: Bitmap? = null

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private lateinit var recyclerAdapter: DramaLocationAdapter
    private lateinit var dbHelper: DatabaseHelper

    // Data holders
    private val dramas = mutableListOf<Drama>()
    private val dlList = mutableListOf<DL>()
    private val locations = mutableListOf<LocationData>()

    private val TAG = "ThreadFragment"

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // Arguments for navigation
    private var targetLocationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetLocationId = arguments?.getString("locationId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentThreadBinding.inflate(inflater, container, false)

        dbHelper = DatabaseHelper(requireContext())

        recyclerAdapter = DramaLocationAdapter(this)
        binding.locationRecyclerview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recyclerAdapter
        }

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior?.apply {
            isHideable = true
            state = BottomSheetBehavior.STATE_HIDDEN
            peekHeight = 0
        }

        setupBackButton()

        try {
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            mapFragment?.getMapAsync(this) ?: Log.e(TAG, "MapFragment not found")
        } catch (t: Throwable) {
            Log.e(TAG, "Error getting map fragment: ${t.message}", t)
        }

        return binding.root
    }

    private fun setupBackButton() {
        binding.btnBack?.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        try {
            iconBitmap = BitmapFactory.decodeResource(resources, R.drawable.goldenthread_icon)
        } catch (t: Throwable) {
            Log.e(TAG, "Icon decode failed: ${t.message}", t)
        }

        loadDataFromDatabase()
        addMarkersFromLocations()

        targetLocationId?.let { locationId ->
            zoomToLocation(locationId)
        }

        mMap?.setOnCameraIdleListener {
            val zoom = mMap?.cameraPosition?.zoom ?: return@setOnCameraIdleListener
            iconBitmap?.let { bmp ->
                val scaled = getScaledMarkerIcon(bmp, zoom)
                for (marker in markers) marker.setIcon(scaled)
            }
        }

        mMap?.setOnMarkerClickListener { marker ->
            val tag = marker.tag as? List<*>
            val items = tag?.filterIsInstance<LocationDramaItem>() ?: emptyList()

            if (items.isNotEmpty()) {
                recyclerAdapter.submitList(items)
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))
            }
            true
        }

        mMap?.setOnMapClickListener {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        }

        checkLocationPermission()
    }

    private fun loadDataFromDatabase() {
        try {
            locations.clear()
            val db = dbHelper.readableDatabase

            val locationCursor = db.rawQuery(
                "SELECT location_id, name_en, name_th, address, latitude, longitude FROM locations",
                null
            )

            while (locationCursor.moveToNext()) {
                locations.add(
                    LocationData(
                        id = locationCursor.getString(0),
                        nameEn = locationCursor.getString(1) ?: "",
                        nameTh = locationCursor.getString(2) ?: "",
                        address = locationCursor.getString(3) ?: "",
                        latitude = locationCursor.getDouble(4),
                        longitude = locationCursor.getDouble(5)
                    )
                )
            }
            locationCursor.close()
            Log.d(TAG, "Loaded ${locations.size} locations from database")

            dramas.clear()
            val dramaCursor = db.rawQuery(
                "SELECT drama_id, title_en, title_th, release_year FROM dramas",
                null
            )

            while (dramaCursor.moveToNext()) {
                dramas.add(
                    Drama(
                        dramaId = dramaCursor.getString(0),
                        titleEn = dramaCursor.getString(1) ?: "",
                        titleTh = dramaCursor.getString(2) ?: "",
                        releaseYear = dramaCursor.getString(3) ?: ""
                    )
                )
            }
            dramaCursor.close()

            dlList.clear()
            val dlCursor = db.rawQuery(
                "SELECT drama_id, location_id, scene_notes, order_in_trip, car_travel_min FROM drama_locations",
                null
            )

            while (dlCursor.moveToNext()) {
                dlList.add(
                    DL(
                        dramaId = dlCursor.getString(0),
                        locationId = dlCursor.getString(1),
                        sceneNotes = dlCursor.getString(2) ?: "",
                        orderInTrip = dlCursor.getInt(3),
                        carTravelMin = dlCursor.getInt(4)
                    )
                )
            }
            dlCursor.close()

        } catch (e: Exception) {
            Log.e(TAG, "Error loading data: ${e.message}", e)
        }
    }

    private fun addMarkersFromLocations() {
        mMap ?: run { Log.e(TAG, "Map not ready"); return }
        if (locations.isEmpty()) { Log.w(TAG, "No locations"); return }

        markers.clear()
        val boundsBuilder = LatLngBounds.Builder()

        for (loc in locations) {
            if (loc.latitude == 0.0 && loc.longitude == 0.0) continue

            val position = LatLng(loc.latitude, loc.longitude)
            val scaledIcon = iconBitmap?.let { getScaledMarkerIcon(it, 10f) }
            val markerOptions = MarkerOptions()
                .position(position)
                .title(loc.nameEn)
                .snippet(loc.address)

            scaledIcon?.let { markerOptions.icon(it) }

            val marker = mMap!!.addMarker(markerOptions)
            if (marker != null) {
                val relatedDL = dlList.filter { it.locationId == loc.id }
                val items = relatedDL.mapNotNull { dl ->
                    val drama = dramas.find { it.dramaId == dl.dramaId }
                    LocationDramaItem(
                        nameEn = loc.nameEn,
                        nameTh = loc.nameTh,
                        address = loc.address,
                        titleEn = drama?.titleEn ?: "Unknown Drama",
                        titleTh = drama?.titleTh ?: "",
                        releaseYear = drama?.releaseYear ?: "",
                        sceneNotes = dl.sceneNotes,
                        orderInTrip = dl.orderInTrip,
                        carTravelMin = dl.carTravelMin,
                        latitude = loc.latitude,
                        longitude = loc.longitude
                    )
                }

                marker.tag = items.ifEmpty {
                    listOf(
                        LocationDramaItem(
                            nameEn = loc.nameEn,
                            nameTh = loc.nameTh,
                            address = loc.address,
                            titleEn = "No associated drama",
                            titleTh = "",
                            releaseYear = "",
                            sceneNotes = "",
                            orderInTrip = 0,
                            carTravelMin = 0,
                            latitude = loc.latitude,
                            longitude = loc.longitude
                        )
                    )
                }

                markers.add(marker)
                boundsBuilder.include(position)
            }
        }

        if (markers.isNotEmpty() && targetLocationId == null) {
            try {
                val bounds = boundsBuilder.build()
                mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (t: Throwable) {
                Log.e(TAG, "Error moving camera: ${t.message}", t)
            }
        }
    }

    private fun zoomToLocation(locationId: String) {
        val targetMarker = markers.find { marker ->
            val tag = marker.tag as? List<*>
            val items = tag?.filterIsInstance<LocationDramaItem>() ?: emptyList()
            items.any { it.nameEn == locations.find { it.id == locationId }?.nameEn }
        }

        targetMarker?.let { marker ->
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))
            val tag = marker.tag as? List<*>
            val items = tag?.filterIsInstance<LocationDramaItem>() ?: emptyList()
            if (items.isNotEmpty()) {
                recyclerAdapter.submitList(items)
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun getScaledMarkerIcon(original: Bitmap, zoom: Float): BitmapDescriptor {
        val size = (64 * (zoom / 7f)).toInt().coerceIn(32, 84)
        val scaled = Bitmap.createScaledBitmap(original, size, size, false)
        return BitmapDescriptorFactory.fromBitmap(scaled)
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            enableMyLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            }
        }
    }

    private fun enableMyLocation() {
        try {
            mMap?.isMyLocationEnabled = true
            mMap?.uiSettings?.isMyLocationButtonEnabled = true
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onGoToDrama(item: LocationDramaItem) {
        val drama = dramas.find { it.titleEn == item.titleEn }
        drama?.let {
            findNavController().navigate(
                R.id.action_thread_to_tourDetails,
                bundleOf("tourId" to drama.dramaId)
            )
        } ?: run {
            Log.e(TAG, "Could not find drama ID for: ${item.titleEn}")
        }
    }

    override fun onNextPoint(item: LocationDramaItem) {
        val nextMarker = markers.find { m ->
            val tag = m.tag as? List<*>
            tag?.any {
                it is LocationDramaItem &&
                        it.orderInTrip == item.orderInTrip + 1 &&
                        it.titleEn == item.titleEn
            } == true
        }

        nextMarker?.let {
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(it.position, 15f))
            val tag = nextMarker.tag as? List<*>
            val nextItems = tag?.filterIsInstance<LocationDramaItem>()?.filter {
                it.titleEn == item.titleEn
            } ?: emptyList()

            if (nextItems.isNotEmpty()) {
                recyclerAdapter.submitList(nextItems)
            }
        }
    }

    // ✅ Added favorite handling here
    override fun onFavorite(item: LocationDramaItem, isFavorite: Boolean) {
        if (isFavorite) {
            FavoriteManager.addThreadFavorite(item)
            Toast.makeText(requireContext(), "Added to favorites!", Toast.LENGTH_SHORT).show()
        } else {
            FavoriteManager.removeThreadFavorite(item)
            Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show()
        }

        val index = recyclerAdapter.currentList.indexOf(item)
        if (index != -1) {
            recyclerAdapter.notifyItemChanged(index)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            mMap?.setOnCameraIdleListener(null)
            mMap?.setOnMarkerClickListener(null)
            mMap?.setOnMapClickListener(null)
            mMap?.clear()
        } catch (t: Throwable) {
            Log.w(TAG, "Error clearing map: ${t.message}")
        }
        _binding = null
    }

    companion object {
        fun newInstance(locationId: String?): ThreadFragment {
            return ThreadFragment().apply {
                arguments = Bundle().apply {
                    locationId?.let { putString("locationId", it) }
                }
            }
        }
    }
}
