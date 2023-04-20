package com.example.chatapp.settings

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentDeleteUserBinding
import com.example.chatapp.mainFunctions.NavigationFragmentDirections
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

private lateinit var binding : FragmentDeleteUserBinding
private lateinit var user : FirebaseUser

class DeleteUserFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDeleteUserBinding.inflate(inflater, container, false)
        user = FirebaseAuth.getInstance().currentUser!!

        binding.deleteButtonDeleteUserFragment.setOnClickListener {
            deleteProfile()
        }

        return binding.root
    }

    private fun deleteProfile() {
        val password = binding.passwordEditTextDeleteUserFragment.text.toString()

        if(password.isEmpty()) {
            Toast.makeText(activity, getString(R.string.password_fields_communicat), Toast.LENGTH_SHORT).show()
            return
        }

        val userCredential = EmailAuthProvider.getCredential(user.email!!, password)

        user.reauthenticate(userCredential).addOnSuccessListener {
            showDialog()

        }.addOnFailureListener{
            Toast.makeText(activity,getString(R.string.incorrect_password_communicat),Toast.LENGTH_LONG).show()
        }
    }

    private fun showDialog() {
        val alertDialog: AlertDialog? = activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setPositiveButton(R.string.delete,
                    DialogInterface.OnClickListener { dialog, id ->
                        removeUserFromFirebase()
                        dialog.cancel()
                        findNavController().popBackStack(R.id.startScreenFragment,false)
                    })
                setNegativeButton(R.string.cancel,
                    DialogInterface.OnClickListener { dialog, id ->
                        dialog.cancel()
                    })
            }
            builder.setTitle(R.string.remove_dialog_title)
            builder.setMessage(R.string.remove_dialog_message)
            builder.create()
        }
        alertDialog?.show()
    }

    private fun removeUserFromFirebase() {
        val currentUserId = FirebaseAuth.getInstance().uid

        val databaseRef = FirebaseDatabase.getInstance().getReference("users/$currentUserId")
        databaseRef.removeValue().addOnSuccessListener {
            Log.d("DeleteUserFragment","User deleted")
        }

        val storageRef = FirebaseStorage.getInstance().getReference("images/avatars/$currentUserId")
        storageRef.delete().addOnSuccessListener {
            Log.d("DeleteUserFragment","User avatar deleted")
        }

        user.delete().addOnSuccessListener {
            Log.d("DeleteUserFragment","User auth deleted")
        }

        Firebase.auth.signOut()
        findNavController().popBackStack(R.id.startScreenFragment,false)
    }

}