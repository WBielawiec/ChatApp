package com.example.chatapp.login

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentLoginBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import java.util.HashMap

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentLoginBinding.inflate(inflater, container, false)


        binding.loginButtonLoginFragment.setOnClickListener {
            performLogin()
        }

        binding.passwordResetTextViewLoginFragment.setOnClickListener {
            val action = LoginFragmentDirections
                .actionLoginFragmentToPasswordResetFragment()
            view?.findNavController()?.navigate(action)
        }

        binding.registerTextViewLoginFragment.setOnClickListener {
            val action = LoginFragmentDirections
                .actionLoginFragmentToRegisterFragment()
            view?.findNavController()?.navigate(action)
        }

        return binding.root
    }

    private fun performLogin() {
        val email = binding.emailEditTextLoginFragment.text.toString()
        val password = binding.passwordEditTextLoginFragment.text.toString()

        Log.d("Login", "Attempt login with email/pw: $email/***")

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful){
                    return@addOnCompleteListener
                }

                Log.d("Login", "Successfully login user with uid: ${it.result.user?.uid}")
                generateNotificationToken()
                view?.findNavController()?.navigate(R.id.frame_navigation_graph)
            }
            .addOnFailureListener {
                Log.d("Login", "Failed to login: ${it.message}")
                Toast.makeText(activity, R.string.login_error, Toast.LENGTH_LONG).show()
            }
    }

    private fun generateNotificationToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            val token = task.result
            val userRef = FirebaseDatabase.getInstance()
                .getReference("users/${FirebaseAuth.getInstance().uid}/token")

            userRef.setValue(token)
        })
    }
}