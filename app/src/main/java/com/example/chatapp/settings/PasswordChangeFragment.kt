package com.example.chatapp.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentPasswordChangeBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth


class PasswordChangeFragment : Fragment() {

    private lateinit var binding : FragmentPasswordChangeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPasswordChangeBinding.inflate(inflater, container, false)
        binding.saveButtonPasswordChangeFragment.setOnClickListener {
            changePassword()
        }

        return binding.root
    }

    private fun changePassword() {
        val actualPasswordCheck = binding.actualPasswordEditTextPasswordChangeFragment.text.toString()
        val newPassword = binding.newPasswordEditTextPasswordChangeFragment.text.toString()
        val retypedPassword = binding.retypeNewPasswordEditTextPasswordChangeFragment.text.toString()

        val user = FirebaseAuth.getInstance().currentUser

        if(actualPasswordCheck.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(activity, getString(R.string.password_fields_communicat), Toast.LENGTH_SHORT).show()
            return
        }
        else if (newPassword.length < 6){
            Toast.makeText(activity, getString(R.string.password_length_communicat), Toast.LENGTH_SHORT).show()
            return
        }

        val userCredential = EmailAuthProvider.getCredential(user?.email!!, actualPasswordCheck)

        user.reauthenticate(userCredential).addOnSuccessListener {
            if(newPassword == retypedPassword){
                user.updatePassword(newPassword).addOnSuccessListener {
                    Toast.makeText(activity,getString(R.string.password_update_success),Toast.LENGTH_LONG).show()
                }.addOnFailureListener{
                    Toast.makeText(activity,getString(R.string.password_update_failure),Toast.LENGTH_LONG).show()
                }
            }
            else {
                Toast.makeText(activity,getString(R.string.password_match_communicat),Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener{
            Toast.makeText(activity,getString(R.string.incorrect_password_communicat),Toast.LENGTH_LONG).show()
        }
    }


}