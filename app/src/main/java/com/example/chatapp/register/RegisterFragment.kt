package com.example.chatapp.register

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentRegisterBinding
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.HashMap


class RegisterFragment : Fragment() {
    private lateinit var binding: FragmentRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)

        binding.registerButtonRegisterFragment.setOnClickListener{
            performRegister()
        }

        binding.logInTextViewRegisterFragment.setOnClickListener {
            val action = RegisterFragmentDirections
                .actionRegisterFragmentToLoginFragment()
            view?.findNavController()?.navigate(action)
        }

        return binding.root
    }

    private fun performRegister() {
        val email = binding.emailEditTextRegisterFragment.text.toString()
        val password = binding.passwordEditTextRegisterFragment.text.toString()
        val retypedPassword = binding.passwordRetypeEditTextRegisterFragment.text.toString()
        val username = binding.usernameEditTextRegisterFragment.text.toString()

        if(email.isEmpty() || password.isEmpty()) {
            Toast.makeText(activity, getString(R.string.email_password_fields_communicat), Toast.LENGTH_SHORT).show()
            return
        } else if(password.length < 6) {
            Toast.makeText(activity, getString(R.string.password_length_communicat), Toast.LENGTH_SHORT).show()
            return
        } else if(username.length < 3){
            Toast.makeText(activity, getString(R.string.username_length_communicat), Toast.LENGTH_SHORT).show()
            return
        } else if(password != retypedPassword){
            Toast.makeText(activity, getString(R.string.password_match_communicat), Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("RegisterFragment", "Email is: " + email)
        Log.d("RegisterFragment", "Password: " + password)

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful){
                    it.exception?.printStackTrace()
                    return@addOnCompleteListener
                }

                Log.d("RegisterFragment", "Successfully created user with uid: ${it.result.user?.uid}")
                saveUserToDataBase(email, username)

            }
            .addOnFailureListener {
                Log.d("RegisterFragment", "Failed to create user: ${it.message}")
                Toast.makeText(activity,getString(R.string.user_create_failure) + " " + it.message, Toast.LENGTH_SHORT).show()
            }
    }


    private fun saveUserToDataBase(email: String,username: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""

        val userRef = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val userNumberRef = FirebaseDatabase.getInstance().getReference("/available-numbers/")

        var userNumber : String

        //taking number from available-numbers column and assign it to user then delete it from column
        //numbers was generated in first app run
        //counter is telling us which number should we use

        userNumberRef.get().addOnSuccessListener {

            val numberKey = it.child("counter").value.toString()
            userNumber = it.child(numberKey).value.toString()
            val hashMap : HashMap<String, Any>
                    = HashMap<String, Any> ()
            hashMap.put("counter", numberKey.toInt() + 1)
            userNumberRef.updateChildren(hashMap)
            userNumberRef.child(numberKey).removeValue()

            Log.d("RegisterFragment","Number for $username: $userNumber")

            val user = User(uid,email, userNumber, binding.usernameEditTextRegisterFragment.text.toString())

            userRef.setValue(user)
                .addOnSuccessListener {

                    Log.d("RegisterFragment", "User registered to database.")

                    val action = RegisterFragmentDirections
                        .actionRegisterFragmentToAccountSetupFragment()
                    view?.findNavController()?.navigate(action)

                }
                .addOnFailureListener{

                    Log.d("RegisterFragment", "Failed to set value to database: ${it.message}")
                }
        }
    }

}