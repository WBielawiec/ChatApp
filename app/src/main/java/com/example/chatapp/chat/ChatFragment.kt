package com.example.chatapp.chat

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment.*
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentChatBinding
import com.example.chatapp.model.ChatMessage
import com.example.chatapp.model.User
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
import kotlinx.android.synthetic.main.latest_message_row.view.*
import java.text.SimpleDateFormat
import java.util.*

private lateinit var binding : FragmentChatBinding
private var selectedAttachmentUri: Uri? = null;

class ChatFragment : Fragment() {

    private val args: ChatFragmentArgs by navArgs()
    private val adapter = GroupAdapter<GroupieViewHolder>()
    private var toUser : User? = null
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(inflater, container, false)

        currentUser = args.currentUser
        toUser = args.toUser

        binding.recyclerViewChatFragment.adapter = adapter
        binding.usernameTextViewToolbarChatFragment.text = toUser?.username
        Picasso.get().load(toUser?.profileImageUrl).into(binding.toolbarImageViewChatFragment)

        binding.toolbarChatFragment.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.clearChat -> {
                    showDialog()
                }
            }
            true
        }

        binding.toolbarImageViewChatFragment.setOnClickListener {
            val action = ChatFragmentDirections.actionChatFragmentToUserProfileFragment(toUser!!)
            findNavController().navigate(action)
        }

        binding.backImageViewToolbarChatFragment.setOnClickListener {
            findNavController().popBackStack()
        }

        changeUserStatusListener()
        listenForMessages()

        binding.sendButtonChatFragment.setOnClickListener {
            Log.d("Chat Fragment", "Attemp to send messgae....")
            performSendMessage()
        }

        binding.attachFileButtonChatFragment.setOnClickListener {
            Log.d("Chat Fragment", "Attemp to send attachment....")
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"

            startActivityForResult(intent, 1)
        }

        return binding.root
    }

    private fun showDialog() {
        val alertDialog: AlertDialog? = activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setPositiveButton(R.string.clear_chat,
                    DialogInterface.OnClickListener { dialog, id ->
                        val chatReference = FirebaseDatabase.getInstance().getReference("user-messages/${currentUser?.uid}/${toUser?.uid}")
                        chatReference.removeValue()
                        val latestMessageReference = FirebaseDatabase.getInstance().getReference("latest-messages/${currentUser?.uid}/${toUser?.uid}")
                        latestMessageReference.removeValue()
                        dialog.cancel()
                    })
                setNegativeButton(R.string.cancel,
                    DialogInterface.OnClickListener { dialog, id ->
                        dialog.cancel()
                    })
            }
            builder.setTitle(R.string.remove_dialog_title)
            builder.setMessage(R.string.clear_chat_confirmation)
            builder.create()
        }
        alertDialog?.show()
    }

    private fun listenForMessages() {
        val fromId = FirebaseAuth.  getInstance().uid
        val toId = toUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")

        ref.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)

                if(chatMessage != null) {
                    Log.d("Chat Fragment", chatMessage.text)

                    if(chatMessage.fromId == FirebaseAuth.getInstance().uid){
                        if(chatMessage.attachment){
                            adapter.add(ImageToItem(chatMessage.text, currentUser!!))

                            adapter.setOnItemLongClickListener { item, view ->
                                val imageItem = item as ImageToItem
                                val imagePath = Uri.parse(imageItem.imagePath)
                                popupMenu(imagePath,view)
                                true
                            }

                        } else {
                            adapter.add(ChatToItem(chatMessage, currentUser!!))
                        }
                        binding.recyclerViewChatFragment.scrollToPosition(adapter.itemCount - 1)
                    } else {
                        if(chatMessage.attachment){
                            adapter.add(ImageFromItem(chatMessage.text, toUser!!))

                            adapter.setOnItemLongClickListener { item, view ->
                                val imageItem = item as ImageFromItem
                                val imagePath = Uri.parse(imageItem.imagePath)
                                popupMenu(imagePath,view)
                                true
                            }

                        } else {
                            adapter.add(ChatFromItem(chatMessage, toUser!!))
                        }
                        binding.recyclerViewChatFragment.scrollToPosition(adapter.itemCount - 1)
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                adapter.clear()
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun performSendMessage() {
        val text = binding.messageEditTextChatFragment.text.toString()
        if (text.isNotBlank()) {
            val fromId = FirebaseAuth.getInstance().uid

            if(fromId == null || toUser == null) return
            val toId = toUser?.uid

            val fromReference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
            val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

            val chatMessage = ChatMessage(fromReference.key!!, text.replace(" +".toRegex().replace("\n"," "), " "), fromId, toId!!, System.currentTimeMillis() / 1000)

            fromReference.setValue(chatMessage)
                .addOnSuccessListener {
                    Log.d("Chat Fragment", "Saved our chat message: ${fromReference.key}")
                    binding.messageEditTextChatFragment.text?.clear()
                    binding.recyclerViewChatFragment.scrollToPosition(adapter.itemCount - 1)
                }

            val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
            latestMessageRef.setValue(chatMessage)

            val blockRef = FirebaseDatabase.getInstance().getReference("blocked-users/$toId")

            blockRef.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.value == null){
                        toReference.setValue(chatMessage)
                        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
                        latestMessageToRef.setValue(chatMessage)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

            if(toUser!!.status == "online" || toUser!!.status == "idle"){
                sendNotificationToUser(toUser!!.token, currentUser!!.username, chatMessage.text)
            }
        }
    }

    private fun changeUserStatusListener(){
        val userReference = FirebaseDatabase.getInstance().getReference("users/${toUser?.uid}")

        userReference.addChildEventListener(object: ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                toUser?.status = snapshot.getValue().toString()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })


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
                        val toId = toUser?.uid

                        val reference = FirebaseDatabase.getInstance()
                            .getReference("/user-messages/$fromId/$toId").push()
                        val toReference = FirebaseDatabase.getInstance()
                            .getReference("/user-messages/$toId/$fromId").push()

                        val chatMessage = ChatMessage(
                            reference.key!!,
                            it.toString(),
                            fromId!!,
                            toId!!,
                            System.currentTimeMillis() / 1000,
                            true
                        )

                        reference.setValue(chatMessage)
                            .addOnSuccessListener {
                                Log.d("Chat Fragment", "Saved our chat message: ${reference.key}")
                                binding.messageEditTextChatFragment.text?.clear()
                                binding.recyclerViewChatFragment.scrollToPosition(adapter.itemCount - 1)
                            }

                        val latestMessageRef = FirebaseDatabase.getInstance()
                            .getReference("/latest-messages/$fromId/$toId")
                        latestMessageRef.setValue(chatMessage)

                        val blockRef = FirebaseDatabase.getInstance().getReference("blocked-users/$toId")

                        blockRef.addListenerForSingleValueEvent(object: ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if(snapshot.value == null){
                                    toReference.setValue(chatMessage)
                                    val latestMessageToRef = FirebaseDatabase.getInstance()
                                        .getReference("/latest-messages/$toId/$fromId")
                                    latestMessageToRef.setValue(chatMessage)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }
                        })

                        if(toUser!!.status != "offline" || toUser!!.status != "doNotDisturb"){
                            sendNotificationToUser(toUser!!.token, toUser!!.username, getString(R.string.notification_attachement))
                        }
                    }
                }
        }
    }

    private fun popupMenu(imagePath : Uri, view: View) {

        val popupMenu = PopupMenu(activity,view)
        popupMenu.inflate(R.menu.attachment_popup_menu)
        popupMenu.gravity = Gravity.RIGHT

        popupMenu.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.downloadAttachment -> {
                    if(ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                        val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val request = DownloadManager.Request(imagePath)
                            .setTitle("image")
                            .setDescription("Downloading...")
                            .setDestinationInExternalPublicDir(DIRECTORY_DOWNLOADS, "image.jpg")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setAllowedOverMetered(true)

                        downloadManager.enqueue(request)
                    }
                    else {
                        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
                    }

                }
            }
            true
        }
        popupMenu.show()
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

class ChatFromItem(val chatMessage: ChatMessage, val user: User): Item<GroupieViewHolder>() {
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

class ChatToItem(val chatMessage: ChatMessage, val user: User): Item<GroupieViewHolder>() {
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

class ImageFromItem(val imagePath: String, val user: User): Item<GroupieViewHolder>() {
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

class ImageToItem(val imagePath: String, val user: User): Item<GroupieViewHolder>() {
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

