package com.example.chatapp.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentInformationChangeBinding
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class InformationChangeFragment : Fragment() {
    private lateinit var binding : FragmentInformationChangeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInformationChangeBinding.inflate(inflater, container, false)

        setUserInformation()

        binding.saveChangeButtonInformationChangeFragment.setOnClickListener{
            changeInformation()
        }

        binding.backImageViewAvatarChangeFragment.setOnClickListener {
            findNavController().popBackStack()
        }

        return binding.root
    }

    private fun setUserInformation() {
        val userId = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("users/$userId")

        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)

                binding.usernameChangeEditTextInformationChangeFragment.setText(user?.username)
                binding.descriptionChangeEditTextInformationChangeFragment.setText(user?.description)
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun changeInformation() {
        val userId = FirebaseAuth.getInstance().uid

        val username = binding.usernameChangeEditTextInformationChangeFragment.text.toString()
        val description = binding.descriptionChangeEditTextInformationChangeFragment.text.toString()

        val ref = FirebaseDatabase.getInstance().getReference("users/$userId")
        val hashMap : HashMap<String, Any>
                = HashMap<String, Any> ()
        hashMap.put("username", username)
        hashMap.put("description", description)

        ref.updateChildren(hashMap).addOnSuccessListener {
            Toast.makeText(activity,R.string.information_update_success,Toast.LENGTH_LONG).show()
        }
    }
}