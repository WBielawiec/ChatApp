package com.example.chatapp.chat

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentGroupChatBinding
import com.example.chatapp.model.ChatMessage
import com.example.chatapp.model.GroupChat
import com.example.chatapp.model.User
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import kotlinx.android.synthetic.main.image_from_row.view.*
import kotlinx.android.synthetic.main.image_to_row.view.*
import java.text.SimpleDateFormat
import java.util.*

private lateinit var binding: FragmentGroupChatBinding

class GroupChatFragment : Fragment() {

    private val args: GroupChatFragmentArgs by navArgs()
    private lateinit var groupChatId : String
    private  val adapter = GroupAdapter<GroupieViewHolder>()
    private var selectedAttachmentUri: Uri? = null;
    private lateinit var groupChatMessageRef : DatabaseReference
    private lateinit var childListener : ChildEventListener
    private lateinit var groupChatMembersRef : DatabaseReference
    private lateinit var informationRef : DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            //Toast.makeText(context, "back pressed", Toast.LENGTH_LONG).show()
            val action = GroupChatFragmentDirections.actionGroupChatFragmentToNavigationFragment()
            findNavController().navigate(action)
        }
        callback.isEnabled

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        groupChatId = args.groupChatId
        informationRef = FirebaseDatabase.getInstance().getReference("group-chat/$groupChatId/information")
        groupChatMembersRef = FirebaseDatabase.getInstance().getReference("group-chat/$groupChatId/members")

        binding.toolbarGroupChatFragment.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.addGroupUser -> {
                    val action = GroupChatFragmentDirections.actionGroupChatFragmentToAddUserToGroupChatFragment(groupChatId)
                    findNavController().navigate(action)
                }
                R.id.leaveGroup -> {
                    showLeaveDialog()
                }
            }
            true
        }

        binding.recyclerViewGroupChatFragment.adapter = adapter

        loadGroupChatData()
        //getChatMembers()

        binding.backImageViewToolbarGroupChatFragment.setOnClickListener {
            val action = GroupChatFragmentDirections.actionGroupChatFragmentToNavigationFragment()
            findNavController().navigate(action)
        }

        listenForMessages()

        //getMessages()

        binding.sendButtonGroupChatFragment.setOnClickListener {
            Log.d("Group Chat Fragment", "Attemp to send messgae....")
            performSendMessage()
        }

        binding.attachFileButtonGroupChatFragment.setOnClickListener {
            Log.d("Group Chat Fragment", "Attemp to send attachment....")
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"

            startActivityForResult(intent, 1)
        }

        return binding.root
    }

    private fun loadGroupChatData() {
        informationRef.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupChat = snapshot.getValue(GroupChat::class.java)
                if(groupChat != null) {
                    binding.toolbarGroupChatNameTextViewToolbarChatFragment.text = groupChat.groupName
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }


    private fun listenForMessages(){
        adapter.clear()
        groupChatMessageRef = FirebaseDatabase.getInstance().getReference("group-chat/$groupChatId/messages")

        childListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val groupChatMessage = snapshot.getValue(ChatMessage::class.java)

                if (groupChatMessage != null) {
                    Log.d("Group Chat Fragment", groupChatMessage.text)

                    val fromUserRef = FirebaseDatabase.getInstance()
                        .getReference("users/${groupChatMessage.fromId}")

                    fromUserRef.get()
                        .addOnSuccessListener {
                            val fromUser = it.getValue(User::class.java)
                            if (fromUser != null) {
                                if (groupChatMessage.fromId == FirebaseAuth.getInstance().uid) {
                                    if (groupChatMessage.attachment) {
                                        adapter.add(
                                            GroupImageToItem(
                                                groupChatMessage.text,
                                                fromUser
                                            )
                                        )
                                        adapter.setOnItemLongClickListener { item, view ->
                                            val imageItem = item as GroupImageToItem
                                            val imagePath = Uri.parse(imageItem.imagePath)
                                            popupMenu(imagePath, view)
                                            true
                                        }
                                    } else {
                                        adapter.add(
                                            GroupChatToItem(
                                                groupChatMessage,
                                                fromUser
                                            )
                                        )
                                    }
                                    binding.recyclerViewGroupChatFragment.scrollToPosition(
                                        adapter.itemCount - 1
                                    )
                                } else {
                                    if (groupChatMessage.attachment) {
                                        adapter.add(
                                            GroupImageFromItem(
                                                groupChatMessage.text,
                                                fromUser
                                            )
                                        )
                                        adapter.setOnItemLongClickListener { item, view ->
                                            val imageItem = item as GroupImageFromItem
                                            val imagePath = Uri.parse(imageItem.imagePath)
                                            popupMenu(imagePath, view)
                                            true
                                        }
                                    } else {
                                        adapter.add(
                                            GroupChatFromItem(
                                                groupChatMessage,
                                                fromUser
                                            )
                                        )
                                    }
                                    binding.recyclerViewGroupChatFragment.scrollToPosition(
                                        adapter.itemCount - 1
                                    )
                                }
                            }
                        }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }

        groupChatMessageRef.addChildEventListener(childListener)
    }

    private fun performSendMessage() {
        val text = binding.messageEditTextGroupChatFragment.text.toString()
        if (text.isNotBlank()) {
            val fromId = FirebaseAuth.getInstance().uid

            val messageRef = FirebaseDatabase.getInstance().getReference("group-chat/$groupChatId/messages").push()

            val groupChatMessage = ChatMessage(messageRef.key!!, text.replace(" +".toRegex().replace("\n"," "), " "), fromId!!, groupChatId, System.currentTimeMillis() / 1000, false, true)

            messageRef.setValue(groupChatMessage)
                .addOnSuccessListener {
                    Log.d("Group Chat Fragment", "Saved our chat message: ${messageRef.key}")
                    binding.messageEditTextGroupChatFragment.text?.clear()
                    binding.recyclerViewGroupChatFragment.scrollToPosition(adapter.itemCount - 1)
                }

            informationRef.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val secondImage = snapshot.child("firstImage").getValue()

                    val userRef = FirebaseDatabase.getInstance().getReference("users/$fromId")
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val firstImage = snapshot.child("profileImageUrl").getValue()
                            if(secondImage != firstImage) {
                                informationRef.child("secondImage").setValue(secondImage)
                            }
                            informationRef.child("firstImage").setValue(firstImage)
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })

            groupChatMembersRef.addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach(){
                        val latestMessagesRef = FirebaseDatabase.getInstance().getReference("latest-messages/${it.value.toString()}/$groupChatId")
                        latestMessagesRef.setValue(groupChatMessage)
                        val userStatusRef = FirebaseDatabase.getInstance().getReference("users/${it.value.toString()}")
                        val userStatus = userStatusRef.get().addOnSuccessListener {
                            val user = it.getValue(User::class.java)
                            if((user?.status == "online" || user?.status == "idle") && user.token != ""){
                                sendNotificationToUser(user.token, "Group", groupChatMessage.text)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
        }
    }

    private fun leaveChat(){
        val userID = FirebaseAuth.getInstance().uid

        val groupUsersRef = FirebaseDatabase.getInstance().getReference("group-chat/${groupChatId}/members")
        groupUsersRef.child(userID!!).removeValue()
        FirebaseDatabase.getInstance().getReference("user-group-chats/${userID}/$groupChatId").removeValue()
        FirebaseDatabase.getInstance().getReference("latest-messages/$userID/$groupChatId").removeValue()

    }

    private fun showLeaveDialog() {
        val alertDialog: AlertDialog? = activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setPositiveButton(R.string.group_chat_leave,
                    DialogInterface.OnClickListener { dialog, id ->
                        leaveChat()
                        dialog.cancel()
                        findNavController().popBackStack()
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

    private fun popupMenu(imagePath : Uri, view: View) {

        val popupMenu = PopupMenu(activity,view)
        popupMenu.inflate(R.menu.attachment_popup_menu)
        popupMenu.gravity = Gravity.RIGHT

        popupMenu.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.downloadAttachment -> {
                    val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val request = DownloadManager.Request(imagePath)
                        .setTitle("image")
                        .setDescription("Downloading...")
                        .setDestinationInExternalFilesDir(context ,
                            Environment.DIRECTORY_DOWNLOADS, "imaged.jpg")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setAllowedOverMetered(true)

                    downloadManager.enqueue(request)
                }
            }
            true
        }
        popupMenu.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 1 && resultCode == Activity.RESULT_OK && data != null){
            selectedAttachmentUri = data.data

            performSendAttachment()
        }
    }

    private fun performSendAttachment() {
        if (selectedAttachmentUri != null) {

            val filename = UUID.randomUUID().toString()
            val attachmentRef = FirebaseStorage.getInstance().getReference("attachments/$filename")

            attachmentRef.putFile(selectedAttachmentUri!!)
                .addOnSuccessListener {
                    attachmentRef.downloadUrl.addOnSuccessListener {

                        val fromId = FirebaseAuth.getInstance().uid
                        //val toId = toUser?.uid

                        val reference = FirebaseDatabase.getInstance()
                            .getReference("/group-chat/$groupChatId/messages").push()

                        val groupChatMessage = ChatMessage(
                            reference.key!!,
                            it.toString(),
                            fromId!!,
                            groupChatId,
                            System.currentTimeMillis() / 1000,
                            true,
                            true
                        )

                        reference.setValue(groupChatMessage)
                            .addOnSuccessListener {
                                Log.d("Group Chat Fragment", "Saved our chat message: ${reference.key}")
                                binding.messageEditTextGroupChatFragment.text?.clear()
                                binding.recyclerViewGroupChatFragment.scrollToPosition(adapter.itemCount - 1)
                            }

                        groupChatMembersRef.addListenerForSingleValueEvent(object: ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                snapshot.children.forEach(){
                                    val latestMessagesRef = FirebaseDatabase.getInstance().getReference("latest-messages/${it.value.toString()}/$groupChatId")
                                    latestMessagesRef.setValue(groupChatMessage)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }

                        })
                    }
                }
        }
    }

    private fun sendNotificationToUser(token: String, username : String, message : String){
        val notificationReference = FirebaseDatabase.getInstance().getReference("notificationRequests")

        val notification = HashMap<String, String>()
        notification.put("token", token)
        notification.put("username", username)
        notification.put("message", message)

        notificationReference.push().setValue(notification)
    }
}

class GroupChatFromItem(val chatMessage: ChatMessage, val user: User): Item<GroupieViewHolder>() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        val simpleDateFormat = SimpleDateFormat("dd/MM HH:mm")
        val sendDate = Date(chatMessage.timestamp * 1000)
        viewHolder.itemView.messageSendDate_chat_from_row.text = simpleDateFormat.format(sendDate)

        viewHolder.itemView.messageTextView_chat_from_row.text = chatMessage.text

        val uri = user.profileImageUrl
        val userAvatarImageView = viewHolder.itemView.userAvatarImageView_chat_from_row
        Picasso.get().load(uri).into(userAvatarImageView)
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }
}

class GroupChatToItem(val chatMessage: ChatMessage, val user: User): Item<GroupieViewHolder>() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        val simpleDateFormat = SimpleDateFormat("dd/MM HH:mm")
        val sendDate = Date(chatMessage.timestamp * 1000)
        viewHolder.itemView.messageSendDate_chat_to_row.text = simpleDateFormat.format(sendDate)

        viewHolder.itemView.messageTextView_chat_to_row.text = chatMessage.text

        val uri = user.profileImageUrl
        val userAvatarImageView = viewHolder.itemView.userAvatarImageView_chat_to_row
        Picasso.get().load(uri).into(userAvatarImageView)

    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }
}

class GroupImageFromItem(val imagePath: String, val user: User): Item<GroupieViewHolder>() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val messageImageView = viewHolder.itemView.messageImageView_image_from_row
        Picasso.get().load(imagePath).into(messageImageView)

        val uri = user.profileImageUrl
        val userAvatarImageView = viewHolder.itemView.userAvatarImageView_image_from_row
        Picasso.get().load(uri).into(userAvatarImageView)
    }

    override fun getLayout(): Int {
        return R.layout.image_from_row
    }
}

class GroupImageToItem(val imagePath: String, val user: User): Item<GroupieViewHolder>() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val messageImageView = viewHolder.itemView.messageImageView_image_to_row
        Picasso.get().load(imagePath).into(messageImageView)

        val uri = user.profileImageUrl
        val userAvatarImageView = viewHolder.itemView.userAvatarImageView_image_to_row
        Picasso.get().load(uri).into(userAvatarImageView)
    }

    override fun getLayout(): Int {
        return R.layout.image_to_row
    }
}
