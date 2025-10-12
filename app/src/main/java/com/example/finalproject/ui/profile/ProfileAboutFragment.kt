package com.example.finalproject.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finalproject.databinding.FragmentProfileAboutBinding

class ProfileAboutFragment : Fragment() {

    private var _binding: FragmentProfileAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    // ============================== CONTENT ==============================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // =========== Buttons ===========
        binding.btnBack.setOnClickListener {        // To ProfileMainFragment
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
