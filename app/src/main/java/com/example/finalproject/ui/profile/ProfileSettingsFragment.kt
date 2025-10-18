package com.example.finalproject.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finalproject.R
import com.example.finalproject.databinding.FragmentProfileSettingsBinding
import com.example.finalproject.ui.home.HomeFragment

class ProfileSettingsFragment : Fragment() {

    private var _binding: FragmentProfileSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // ============================== CONTENT ==============================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // =========== Buttons ===========
        binding.btnBack.setOnClickListener {        // To ProfileMainFragment
            findNavController().navigateUp()
        }

        binding.switchPush.setOnCheckedChangeListener { switchView, isChecked ->            // To notify 'push' is toggled
            val bundle = Bundle().apply { putBoolean("push_notifications", isChecked) }
            parentFragmentManager.setFragmentResult("settings_key", bundle)
        }

        binding.switchEmail.setOnCheckedChangeListener { switchView, isChecked ->           // To notify 'email' is toggled
            val bundle = Bundle().apply { putBoolean("email_notifications", isChecked) }
            parentFragmentManager.setFragmentResult("settings_key", bundle)
        }

        binding.btnInterests.setOnClickListener {    // To ProfileInterestsFragment
            val bundle = bundleOf("isFirstTimeSetup" to false)
            Log.d("ProfileSettings", "Interests clicked")
            findNavController().navigate(R.id.action_profileSettings_to_interests, bundle)
        }

        binding.btnLogout.setOnClickListener {       // **To Log in page** (gotta find code for actual logout)
            // Just a placeholder to go back to ProfileMainFragment
            parentFragmentManager.popBackStack(
                null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.main, HomeFragment())
                .commit()
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
