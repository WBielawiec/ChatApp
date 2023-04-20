package com.example.chatapp.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.snap
import androidx.navigation.fragment.findNavController
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentAvatarChangeBinding
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso


class AvatarChangeFragment : Fragment() {

    private lateinit var binding : FragmentAvatarChangeBinding
    private var selectedPhotoUri: Uri? = null
    private lateinit var userId : String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAvatarChangeBinding.inflate(inflater, container, false)
        userId = FirebaseAuth.getInstance().uid!!

        loadAvatar()

        binding.changeAvatarButtonAvatarChangeFragment.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"

            startActivityForResult(intent,0)
        }

        binding.removeAvatarButtonAvatarChangeFragment.setOnClickListener {
            removeAvatar()
        }

        binding.backImageViewAvatarChangeFragment.setOnClickListener {
            findNavController().popBackStack()
        }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 0 && resultCode == Activity.RESULT_OK && data != null){
            Log.d("RegisterActivity", "Photo was selected")

            selectedPhotoUri = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(activity?.contentResolver, selectedPhotoUri)

            binding.avatarChangeImageViewAvatarChangeFragment.setImageBitmap(bitmap)

            changeAvatar()

        }
    }

    private fun loadAvatar() {
        val ref = FirebaseDatabase.getInstance().getReference("users/$userId")

        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if(user?.profileImageUrl != null){
                    Picasso.get().load(user.profileImageUrl).into(binding.avatarChangeImageViewAvatarChangeFragment)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }


    private fun changeAvatar() {
        if(selectedPhotoUri == null) return

        val storageRef = FirebaseStorage.getInstance().getReference("/images/avatars/$userId")

        storageRef.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.d("AvatarChangeFragment", "Successfully changed image: ${it.metadata?.path}")

                storageRef.downloadUrl.addOnSuccessListener {
                    Log.d("RegisterActivity", "File Location: $it")

                    val userRef = FirebaseDatabase.getInstance().getReference("users/$userId")
                    val hashMap : HashMap<String, Any>
                            = HashMap<String, Any> ()
                    hashMap.put("profileImageUrl", it.toString())

                    userRef.updateChildren(hashMap)
                }
            }
            .addOnFailureListener{
                Log.d("RegisterActivity", "Failed to upload image")
            }
    }

    private fun removeAvatar() {
        val storageRef = FirebaseStorage.getInstance().getReference("/images/avatars/$userId")
        storageRef.delete().addOnSuccessListener {
            Log.d("AvatarChangeFragment", "Avatar deleted successfully ")
        }

        val userRef = FirebaseDatabase.getInstance().getReference("users/$userId")
        userRef.child("profileImageUrl").removeValue()

        binding.avatarChangeImageViewAvatarChangeFragment.setImageResource(R.drawable.person_icon)
    }

}