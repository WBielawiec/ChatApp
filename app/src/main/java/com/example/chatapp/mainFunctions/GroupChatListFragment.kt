package com.example.chatapp.mainFunctions

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.snapshotFlow
import androidx.navigation.fragment.findNavController
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentGroupChatListBinding
import com.example.chatapp.model.GroupChat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.group_chat_row.view.*


class GroupChatListFragment : Fragment() {

    private lateinit var binding: FragmentGroupChatListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGroupChatListBinding.inflate(inflater, container, false)

        showGroupChats()
        binding.backImageViewGroupChatList.setOnClickListener {
            findNavController().popBackStack()
        }

        return binding.root
    }

    private fun showGroupChats() {
        val adapter = GroupAdapter<GroupieViewHolder>()

        val currentUserId = FirebaseAuth.getInstance().uid
        val groupChatsUsersRef = FirebaseDatabase.getInstance().getReference("user-group-chats/$currentUserId")

        groupChatsUsersRef.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach{
                    val groupChatRef = FirebaseDatabase.getInstance().getReference("group-chat/${it.value}/information")

                    groupChatRef.addListenerForSingleValueEvent(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val groupChat = snapshot.getValue(GroupChat::class.java)

                            if(groupChat != null){
                                adapter.add(GroupChatItem(groupChat))

                                adapter.setOnItemClickListener { item, view ->
                                    val toChat = item as GroupChatItem
                                    val action = GroupChatListFragmentDirections.actionGroupChatListFragmentToGroupChatFragment(toChat.groupChat.groupId)
                                    findNavController().navigate(action)
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })
                }
                binding.recyclerViewGroupChatList.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    class GroupChatItem(val groupChat: GroupChat) : Item<GroupieViewHolder>() {
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.groupNameTextView_group_chat_row.text = groupChat.groupName

            if(groupChat.firstImage != ""){
                Picasso.get().load(groupChat.firstImage).into(viewHolder.itemView.firstUserAvatarImageView_group_chat_row)
            }

            if(groupChat.secondImage != ""){
                Picasso.get().load(groupChat.secondImage).into(viewHolder.itemView.secondUserAvatarImageView_group_chat_row)
            }
        }

        override fun getLayout(): Int {
            return R.layout.group_chat_row
        }
    }
}