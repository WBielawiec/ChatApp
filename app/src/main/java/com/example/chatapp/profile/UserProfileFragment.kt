package com.example.chatapp.profile

import android.content.DialogInterface
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentUserProfileBinding
import com.example.chatapp.mainFunctions.ContactsFragment
import com.example.chatapp.mainFunctions.LatestMessagesFragment
import com.example.chatapp.model.ContactRequest
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso

private lateinit var binding: FragmentUserProfileBinding

class UserProfileFragment : Fragment() {

    private val args: UserProfileFragmentArgs by navArgs()
    private lateinit var user : User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        user = args.user

        fetchUser()
        addContactHandle()
        val currentUserId = FirebaseAuth.getInstance().uid

        if(user.uid == currentUserId){
            binding.chatButtonUserProfileFragment.visibility = View.GONE
            binding.addContactButtonUserProfileFragment.visibility = View.GONE
        }

        if(user.uid != currentUserId){
            val blockRef = FirebaseDatabase.getInstance().getReference("blocked-users/$currentUserId")

            blockRef.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.toolbarUserProfileFragment.inflateMenu(R.menu.user_profile_menu)
                    if(snapshot.value == null){
                        binding.toolbarUserProfileFragment.menu.findItem(R.id.unblockUser).isVisible = false
                    }
                    else{
                        binding.toolbarUserProfileFragment.menu.findItem(R.id.blockUser).isVisible = false
                        binding.chatButtonUserProfileFragment.visibility = View.GONE
                        binding.addContactButtonUserProfileFragment.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

            binding.toolbarUserProfileFragment.setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.blockUser -> {
                        val blockRef = FirebaseDatabase.getInstance().getReference("blocked-users/$currentUserId")
                        blockRef.child(user.uid).setValue(user.uid)
                        val ref = FirebaseDatabase.getInstance().getReference("/contacts/$currentUserId")
                        ref.child(user.uid).removeValue()

                        binding.toolbarUserProfileFragment.menu.findItem(R.id.unblockUser).isVisible = true
                        binding.toolbarUserProfileFragment.menu.findItem(R.id.blockUser).isVisible = false
                        binding.chatButtonUserProfileFragment.visibility = View.GONE
                        binding.addContactButtonUserProfileFragment.visibility = View.GONE
                    }
                    R.id.unblockUser -> {
                        val ref = FirebaseDatabase.getInstance().getReference("blocked-users/$currentUserId/${user.uid}")
                        ref.removeValue()

                        binding.toolbarUserProfileFragment.menu.findItem(R.id.unblockUser).isVisible = false
                        binding.toolbarUserProfileFragment.menu.findItem(R.id.blockUser).isVisible = true
                        binding.chatButtonUserProfileFragment.visibility = View.VISIBLE
                        binding.addContactButtonUserProfileFragment.visibility = View.VISIBLE
                    }
                }
                true
            }
        }

        binding.chatButtonUserProfileFragment.setOnClickListener {
            startChat()
        }

        return binding.root
    }

    private fun fetchUser() {
        if(user.profileImageUrl == null){
            binding.avatarImageViewUserProfileFragment.setImageResource(R.drawable.avatar_icon)
        }
        else {
            Picasso.get().load(user.profileImageUrl).into(binding.avatarImageViewUserProfileFragment)
        }
        binding.usernameTextViewUserProfileFragment.text = user.username
        binding.userNumberTextViewUserProfileFragment.text = user.number
        binding.descriptionTextViewUserProfileFragment.text = user.description
    }

    private fun addContactHandle() {
        val userId = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("contacts/$userId")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(user.uid)) {
                    binding.addContactButtonUserProfileFragment.visibility = View.GONE
                } else {
                    //add button onClick
                    binding.addContactButtonUserProfileFragment.setOnClickListener {
                        showDialog()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun showDialog() {
        val alertDialog: AlertDialog? = activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setPositiveButton(R.string.send_with_notification,
                    DialogInterface.OnClickListener { dialog, id ->
                        sendInvitation(user)
                        addContact(user)
                        binding.addContactButtonUserProfileFragment.visibility = View.GONE
                        dialog.cancel()
                    })
                setNegativeButton(R.string.send_without_notification,
                    DialogInterface.OnClickListener { dialog, id ->
                        addContact(user)
                        binding.addContactButtonUserProfileFragment.visibility = View.GONE
                        dialog.cancel()
                    })
            }
            builder.setTitle(R.string.send_invitation)
            builder.create()
        }
        alertDialog?.show()
    }

    private fun addContact(user: User) {
        val userId = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("contacts/$userId/${user.uid}")
        ref.setValue(user.username)
    }

    private fun sendInvitation(user: User) {
        val senderId = FirebaseAuth.getInstance().uid ?: ""
        val receiverId = user.uid

        val contactRef = FirebaseDatabase.getInstance().getReference("contacts/$receiverId")
        val query = contactRef.orderByChild(senderId)

        contactRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(!snapshot.hasChild(senderId))
                {
                    val notificationRef = FirebaseDatabase.getInstance().getReference("/notifications/$receiverId/invitations/$senderId")
                    val contactRequest = ContactRequest(
                        senderId,
                        receiverId,
                        System.currentTimeMillis() / 1000,
                        false
                    )

                    val blockedUserRef = FirebaseDatabase.getInstance()
                        .getReference("/blocked-users/$receiverId")
                    blockedUserRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.hasChild(senderId)) {
                                notificationRef.setValue(contactRequest)
                                    .addOnSuccessListener {
                                        Log.d(
                                            "AddContactFragment",
                                            "Successfully send Invitation to $receiverId"
                                        )
                                    }
                                    .addOnFailureListener {
                                        Log.d("AddContactFragment", "Can't send invitation to $receiverId")
                                    }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun fetchCurrentUser() {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                LatestMessagesFragment.currentUser = snapshot.getValue(User::class.java)
                Log.d("LatestMessages", "Current user ${LatestMessagesFragment.currentUser?.profileImageUrl}")
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun startChat() {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentUser = snapshot.getValue(User::class.java)
                val action = UserProfileFragmentDirections.actionUserProfileFragmentToChatFragment(currentUser!!,user)
                findNavController().navigate(action)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

}