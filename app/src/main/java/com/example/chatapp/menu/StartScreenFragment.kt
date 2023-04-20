package com.example.chatapp.menu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentStartScreenBinding
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


private lateinit var binding : FragmentStartScreenBinding

class StartScreenFragment : Fragment() {

    companion object {
        var currentUser: User? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        verifyUserIsLoggedIn(container)
        binding = FragmentStartScreenBinding.inflate(inflater,container,false)

        binding.loginButtonStartScreenFragment.setOnClickListener {
            val action = StartScreenFragmentDirections
                .actionStartScreenFragmentToLoginFragment()
            view?.findNavController()?.navigate(action)
        }

        binding.registerButtonStartScreenFragment.setOnClickListener {
            val action = StartScreenFragmentDirections
                .actionStartScreenFragmentToRegisterFragment()
            view?.findNavController()?.navigate(action)
        }

        return binding.root
    }

    private fun verifyUserIsLoggedIn(container: ViewGroup?) {
        val uid = FirebaseAuth.getInstance().uid
        if(uid != null) {
            Log.d("StartScreenFragment","Logged user id: $uid")
            val action = StartScreenFragmentDirections
                .actionStartScreenFragmentToFrameNavigationGraph()
            container?.findNavController()?.navigate(action)
        }
    }
}