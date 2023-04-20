package com.example.chatapp.mainFunctions

import android.annotation.SuppressLint
import android.os.Bundle
import android.renderscript.Sampler
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.core.snap
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentLatestMessagesBinding
import com.example.chatapp.model.ChatMessage
import com.example.chatapp.model.GroupChat
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import com.xwray.groupie.OnItemLongClickListener
import kotlinx.android.synthetic.main.contact_row.view.*
import kotlinx.android.synthetic.main.group_chat_row.view.*
import kotlinx.android.synthetic.main.group_latest_message_row.view.*
import kotlinx.android.synthetic.main.latest_message_row.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class LatestMessagesFragment : Fragment() {

    companion object {
        var currentUser: User? = null
    }

    private lateinit var binding : FragmentLatestMessagesBinding
    private val adapter = GroupAdapter<GroupieViewHolder>()
    lateinit var yourMessage : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
        ): View? {
        binding = FragmentLatestMessagesBinding.inflate(inflater, container, false)

        binding.recyclerViewLatestMessagesFragment.adapter = adapter
        binding.recyclerViewLatestMessagesFragment.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))

        yourMessage = getString(R.string.you_latest_message)

        getCurrentUser()
        listenForLatestMessages()

        return binding.root
    }

   private fun getCurrentUser() {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)
                Log.d("LatestMessages", "Current user ${currentUser?.profileImageUrl}")
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
   }

    val latestMessagesMap = HashMap<String, ChatMessage>()

    private fun refreshRecyclerViewMessages() {
        adapter.clear()

        latestMessagesMap.toList()
            .sortedByDescending { it.second.timestamp }
            .forEach { (key, value) ->
                adapter.add(LatestMessageRow(value,yourMessage))

                adapter.setOnItemClickListener { item, view ->

                    val latestMessageRow = item as LatestMessageRow

                    if(latestMessageRow.chatMessage.group){
                        val groupId = latestMessageRow.chatMessage.toId
                        val action = NavigationFragmentDirections.actionNavigationFragmentToGroupChatFragment(groupId)
                        findNavController().navigate(action)
                    } else {
                        val user = latestMessageRow.chatPartnerUser
                        val action = NavigationFragmentDirections.actionNavigationFragmentToChatFragment(currentUser!!,user!!)
                        findNavController().navigate(action)
                    }
                }

                adapter.setOnItemLongClickListener { item, view ->
                    //view.setBackgroundColor(R.color.purple_200)

                    popupMenu(item,view)
                    true
                }
            }
    }

    private fun listenForLatestMessages() {
        val fromId = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId")

        ref.addChildEventListener(object: ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = chatMessage
                refreshRecyclerViewMessages()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = chatMessage
                refreshRecyclerViewMessages()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                latestMessagesMap.remove(snapshot.key)
                refreshRecyclerViewMessages()
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun popupMenu(item: Item<*>,view: View) {
        val popupMenu = PopupMenu(activity,view)
        popupMenu.inflate(R.menu.latest_message_popuop_menu)
        popupMenu.gravity = Gravity.RIGHT
        view.setBackgroundColor(android.graphics.Color.LTGRAY)

        popupMenu.setOnDismissListener {
            view.setBackgroundColor(android.graphics.Color.WHITE)
        }

        popupMenu.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.openChat-> {
                    val latestMessageRow = item as LatestMessageRow
                    val user = latestMessageRow.chatPartnerUser
/*                    val action = NavigationFragmentDirections.actionNavigationFragmentToChatFragment(
                        currentUser!!,user!!)
                    findNavController().navigate(action)*/
                }
                R.id.DeleteChat -> {
                    val userId = FirebaseAuth.getInstance().uid
                    val latestMessageRow = item as LatestMessageRow
                    val user = latestMessageRow.chatPartnerUser
                    val ref = FirebaseDatabase.getInstance().getReference("/latest-messages/$userId/${user?.uid}")
                    ref.removeValue()
                }
            }
            true
        }
        popupMenu.show()
    }

    class LatestMessageRow(val chatMessage: ChatMessage, val yourMessage: String): Item<GroupieViewHolder>() {
        var chatPartnerUser : User? = null

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {

            if(!chatMessage.group) {

                if (chatMessage.fromId == currentUser?.uid) {
                    viewHolder.itemView.latestMessageTextView_latest_message_row.text =
                        yourMessage + " " + chatMessage.text
                } else {
                    viewHolder.itemView.latestMessageTextView_latest_message_row.text =
                        chatMessage.text
                }

                val simpleDateFormat = SimpleDateFormat("dd/MM")
                val sendDate = Date(chatMessage.timestamp * 1000)
                viewHolder.itemView.dateTextView_latest_message_row.text =
                    simpleDateFormat.format(sendDate)

                val chatPartnerId: String
                if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                    chatPartnerId = chatMessage.toId
                } else {
                    chatPartnerId = chatMessage.fromId
                }

                val ref = FirebaseDatabase.getInstance().getReference("/users/$chatPartnerId")

                ref.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        chatPartnerUser = snapshot.getValue(User::class.java)
                        viewHolder.itemView.usernameTextView_latest_message_row.text =
                            chatPartnerUser?.username

                        val targetImageView = viewHolder.itemView.userAvatarImageView_latest_message_row

                        if(chatPartnerUser?.profileImageUrl == null){
                            targetImageView.setImageResource(R.drawable.avatar_icon)
                        }
                        else {
                            Picasso.get().load(chatPartnerUser?.profileImageUrl).into(targetImageView)
                        }

                        setStatus(
                            chatPartnerUser?.status!!,
                            viewHolder.itemView.statusImageView_latest_message_row
                        )
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
            }
            else {
                viewHolder.itemView.latestMessageTextView_group_latest_message_row.text = chatMessage.text

                if (chatMessage.fromId == currentUser?.uid) {
                    viewHolder.itemView.latestMessageTextView_group_latest_message_row.text =
                        yourMessage + " " + chatMessage.text
                } else {
                    viewHolder.itemView.latestMessageTextView_group_latest_message_row.text =
                        chatMessage.text
                }

                val simpleDateFormat = SimpleDateFormat("dd/MM")
                val sendDate = Date(chatMessage.timestamp * 1000)
                viewHolder.itemView.dateTextView_group_latest_message_row.text = simpleDateFormat.format(sendDate)

                val ref = FirebaseDatabase.getInstance().getReference("/group-chat/${chatMessage.toId}/information")
                var userRef : DatabaseReference

                ref.addValueEventListener(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val informations = snapshot.getValue(GroupChat::class.java)
                        viewHolder.itemView.groupNameTextView_group_latest_message_row.text = informations?.groupName
                        if(informations?.firstImage != ""){
                            Picasso.get().load(informations?.firstImage).into(viewHolder.itemView.firstUserAvatarImageView_group_latest_message_row)

                        }
                        if(informations?.secondImage != ""){
                            Picasso.get().load(informations?.secondImage).into(viewHolder.itemView.secondUserAvatarImageView_group_latest_message_row)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
            }
        }

        override fun getLayout(): Int {
            if(!chatMessage.group) {
                return R.layout.latest_message_row
            }

            return R.layout.group_latest_message_row
        }

        private fun setStatus(status: String, statusImageView: ImageView){
            when(status) {
                "online" -> {
                    statusImageView.setImageResource(R.drawable.online_icon)
                }
                "idle" -> {
                    statusImageView.setImageResource(R.drawable.idle_icon)
                }
                "doNotDisturb" -> {
                    statusImageView.setImageResource(R.drawable.do_not_disturb_icon)
                }
                "offline" -> {
                    statusImageView.setImageResource(R.drawable.offline_icon)
                }
            }
        }
    }
}