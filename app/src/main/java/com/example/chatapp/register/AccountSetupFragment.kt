package com.example.chatapp.register

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
import android.widget.Toast
import androidx.activity.addCallback
import androidx.navigation.findNavController
import com.example.chatapp.databinding.FragmentAccountSetupBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import kotlin.collections.HashMap

class AccountSetupFragment : Fragment() {

    private lateinit var binding: FragmentAccountSetupBinding
    private var selectedPhotoUri: Uri? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {

        }
        callback.isEnabled
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountSetupBinding.inflate(inflater, container, false)

        binding.avatarChooseImageViewRegisterFragment.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"

            startActivityForResult(intent,0)
        }

        binding.saveButtonImageRegisterFragment.setOnClickListener{
            uploadImageToFirebaseStorage()
            //generateNotificationToken()
//            val action = AccountSetupFragmentDirections
//                .actionAccountSetupFragmentToFrameNavigationGraph()
//            view?.findNavController()?.navigate(action)
        }

        binding.skipButtonImageRegisterFragment.setOnClickListener{
            generateNotificationToken()
//            val action = AccountSetupFragmentDirections
//                .actionAccountSetupFragmentToFrameNavigationGraph()
//            view?.findNavController()?.navigate(action)
        }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 0 && resultCode == Activity.RESULT_OK && data != null){
            Log.d("RegisterActivity", "Photo was selected")

            selectedPhotoUri = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(activity?.contentResolver, selectedPhotoUri)

            binding.avatarChooseImageViewRegisterFragment.setImageBitmap(bitmap)
            //binding.avatarChooseImageViewRegisterActivity.alpha = 0f

/*            val bitmapDrawable = BitmapDrawable(bitmap)
            selectphoto_button_register.setBackgroundDrawable(bitmapDrawable)*/

        }
    }

    private fun uploadImageToFirebaseStorage() {
        if(selectedPhotoUri == null) return

        val filename = FirebaseAuth.getInstance().uid
        val ref = FirebaseStorage.getInstance().getReference("/images/avatars/$filename")

        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.d("RegisterActivity", "Successfully uploaded image: ${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener {
                    Log.d("RegisterActivity", "File Location: $it")

                    val userDescription = binding.userDescriptionTextViewAccountSetupFragment.text.toString()

                    val userId = FirebaseAuth.getInstance().uid
                    val userRef = FirebaseDatabase.getInstance().getReference("users/$userId")
                    val hashMap : HashMap<String, Any>
                            = HashMap<String, Any> ()
                    hashMap.put("profileImageUrl", it.toString())
                    hashMap.put("description", userDescription)

                    userRef.updateChildren(hashMap).addOnSuccessListener {
                        generateNotificationToken()
                    }

                }
            }
            .addOnFailureListener{
                Log.d("RegisterActivity", "Failed to upload image")
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

            userRef.setValue(token).addOnSuccessListener {
                val action = AccountSetupFragmentDirections
                    .actionAccountSetupFragmentToFrameNavigationGraph()
                view?.findNavController()?.navigate(action)
            }
        })
    }

}