package com.example.chatapp.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentLoginBinding
import com.example.chatapp.databinding.FragmentPasswordResetBinding
import com.google.firebase.auth.FirebaseAuth

class PasswordResetFragment : Fragment() {

    private lateinit var binding : FragmentPasswordResetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPasswordResetBinding.inflate(inflater, container, false)

        binding.passwordResetButtonPasswordResetFragment.setOnClickListener {
            val email = binding.emailEditTextPasswordResetFragment.text.toString()

            if(email.isEmpty()) {
                Toast.makeText(activity, getString(R.string.empty_email_field_communicat), Toast.LENGTH_SHORT).show()
            } else {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener {
                    if(it.isSuccessful){
                        Toast.makeText(context, R.string.password_reset_complete, Toast.LENGTH_LONG)
                        val action = PasswordResetFragmentDirections.actionPasswordResetFragmentToLoginFragment()
                        view?.findNavController()?.navigate(action)
                    } else {
                        Toast.makeText(context, it.exception?.message, Toast.LENGTH_LONG)
                    }
                }
            }
        }

        return binding.root
    }

}