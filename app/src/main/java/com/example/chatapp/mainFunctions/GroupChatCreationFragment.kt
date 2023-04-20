package com.example.chatapp.mainFunctions

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentGroupChatCreationBinding
import com.example.chatapp.model.GroupChat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.ktx.storage
import java.util.*

class GroupChatCreationFragment : Fragment() {

    private lateinit var binding : FragmentGroupChatCreationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGroupChatCreationBinding.inflate(inflater, container, false)

        binding.createGroupButtonGroupChatCreationFragment.setOnClickListener {
            createGroupChat()
        }

        return binding.root
    }

    private fun createGroupChat() {
        val currentUserId = FirebaseAuth.getInstance().uid

        val groupChatId = UUID.randomUUID().toString()
        val groupName = binding.groupNameTextViewGroupChatCreationFragment.text.toString()
        var groupDescription = binding.groupDescriptionTextViewGroupChatCreationFragment.text.toString()
        val creatorId = FirebaseAuth.getInstance().uid
        val createDate = System.currentTimeMillis() / 1000

        if(groupName.isEmpty()) {
            Toast.makeText(activity, getString(R.string.email_password_fields_communicat), Toast.LENGTH_SHORT).show()
            return
        } else if(groupName.length < 3) {
            Toast.makeText(activity, getString(R.string.group_name_communicat), Toast.LENGTH_SHORT).show()
            return
        }

        if(groupDescription.isEmpty()) {
            groupDescription = ""
        }

        val groupChatRef = FirebaseDatabase.getInstance().getReference("group-chat/$groupChatId/information")

        val groupChat = GroupChat(groupChatId,groupName,groupDescription,creatorId!!,createDate)
        groupChatRef.setValue(groupChat).addOnSuccessListener {
            val groupChatAdministratorRef = FirebaseDatabase.getInstance().getReference("group-chat/$groupChatId/administrators")
            groupChatAdministratorRef.child(currentUserId!!).setValue(currentUserId)

            val groupChatUsersRef = FirebaseDatabase.getInstance().getReference("group-chat/$groupChatId/members")
            groupChatUsersRef.child(currentUserId).setValue(currentUserId)

            val userGroupChat = FirebaseDatabase.getInstance().getReference("user-group-chats/$currentUserId")
            userGroupChat.child(groupChatId).setValue(groupChatId)

            val action = GroupChatCreationFragmentDirections.actionGroupChatCreationFragmentToGroupChatFragment(groupChatId)
            findNavController().navigate(action)
        }.addOnFailureListener {
            Toast.makeText(activity,R.string.group_creation_fail,Toast.LENGTH_LONG).show()
        }
    }

}