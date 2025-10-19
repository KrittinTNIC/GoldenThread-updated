package com.example.finalproject.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finalproject.databinding.FragmentProfileEditBinding
import com.bumptech.glide.Glide
import com.example.finalproject.R
import com.example.finalproject.util.PreferencesManager
import com.example.finalproject.util.UserDatabaseHelper

class ProfileEditFragment : Fragment() {

    private var _binding: FragmentProfileEditBinding? = null
    private val binding get() = _binding!!

    private lateinit var userDatabaseHelper: UserDatabaseHelper
    private lateinit var preferencesManager: PreferencesManager
    private var selectedPhotoUriString: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    // ============================== CONTENT ==============================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialise database helper and preferences manager
        userDatabaseHelper = UserDatabaseHelper(requireContext())
        preferencesManager = PreferencesManager(requireContext())

        val userEmail = preferencesManager.getLoggedInUserEmail()
        val user = userDatabaseHelper.getUserByEmail(userEmail)

        // =========== Update data ===========
        user?.let { it ->
            // Profile pic (Set to default pic if null)
            val img = binding.imgProfile
            val defaultImg = R.drawable.profile_pic_default

            if(!it.profileImageUri.isNullOrEmpty()) {
                Glide.with(this)
                    .load(user.profileImageUri)
                    .placeholder(R.drawable.profile_pic_default)
                    .error(R.drawable.profile_pic_default)
                    .circleCrop()
                    .into(img)
                selectedPhotoUriString = it.profileImageUri  // Store as String
            } else {
                img.setImageResource(defaultImg)
            }


            // Name hint
            val firstName = binding.inputFirstName
            val lastName = binding.inputLastName
            val email = binding.inputEmail

            firstName.hint = it.firstName
            lastName.hint = it.lastName
            email.hint = it.email

        }

        // =========== Edit data ===========
        // Image selection
        val newImg = binding.imgProfile
        val choosePhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                newImg.setImageURI(it)
                selectedPhotoUriString = it.toString()
            }
        }

        // Name and email
        val newFirst = binding.inputFirstName
        val newLast = binding.inputLastName
        val newEmail = binding.inputEmail

        // =========== Buttons ===========
        binding.btnBack.setOnClickListener {            // To ProfileMainFragment
            findNavController().navigateUp()
        }

        binding.btnPhoto1.setOnClickListener {          // To image selection
            choosePhoto.launch("image/*")
        }
        binding.btnPhoto2.setOnClickListener {          // To image selection
            choosePhoto.launch("image/*")
        }

        binding.btnSubmit.setOnClickListener {          // **To update data**
            val userEmail = preferencesManager.getLoggedInUserEmail()
            val currentUser = userDatabaseHelper.getUserByEmail(userEmail)

            currentUser?.let { user ->
                val newFirstName = newFirst.text.toString().trim().ifEmpty { user.firstName }
                val newLastName = newLast.text.toString().trim().ifEmpty { user.lastName }
                val newEmailText = newEmail.text.toString().trim().ifEmpty { user.email }

                var success = true

                // Update name if changed
                if (newFirstName != user.firstName || newLastName != user.lastName) {
                    success = userDatabaseHelper.updateUser(userEmail, newFirstName, newLastName) && success
                }

                // Update email if changed
                if (newEmailText != user.email) {
                    // Check if new email is already taken
                    if (userDatabaseHelper.isEmailTaken(newEmailText) && newEmailText != user.email) {
                        Toast.makeText(requireContext(), "Email is already taken", Toast.LENGTH_SHORT).show()
                        success = false
                    } else {
                        if (newEmailText != user.email) {
                            Toast.makeText(requireContext(), "Email update feature coming soon", Toast.LENGTH_SHORT).show()
                            success = false
                        }
                    }
                }

                // Update profile image if changed
                if (!selectedPhotoUriString.isNullOrEmpty() && selectedPhotoUriString != user.profileImageUri) {
                    val imageUpdated = userDatabaseHelper.updateProfileImage(userEmail, selectedPhotoUriString!!)
                    success = imageUpdated && success
                }

                if (success) {
                    Toast.makeText(requireContext(), "Your data has been updated", Toast.LENGTH_SHORT).show()

                    // Notify ProfileMainFragment to update data
                    findNavController().previousBackStackEntry?.savedStateHandle?.set("userUpdated", true)

                    findNavController().navigateUp() // Go back to profile
                } else {
                    Toast.makeText(requireContext(), "Failed to update some information", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
