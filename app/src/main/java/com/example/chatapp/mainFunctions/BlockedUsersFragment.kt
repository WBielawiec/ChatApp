package com.example.chatapp.mainFunctions

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.navigation.fragment.findNavController
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentBlockedUsersBinding
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.contact_row.view.*

class BlockedUsersFragment : Fragment() {

    private lateinit var binding : FragmentBlockedUsersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =  FragmentBlockedUsersBinding.inflate(inflater, container, false)

        getBlockedUsers()

        return binding.root
    }

    private fun getBlockedUsers() {
        val userId = FirebaseAuth.getInstance().uid
        val blockedRef = FirebaseDatabase.getInstance().getReference("blocked-users/$userId")
        val userRef = FirebaseDatabase.getInstance().getReference("users")


        blockedRef.addValueEventListener(object: ValueEventListener {
            val adapter = GroupAdapter<GroupieViewHolder>()
            override fun onDataChange(snapshot: DataSnapshot) {
                adapter.clear()

                snapshot.children.forEach(){
                    userRef.child(it.value.toString()).addValueEventListener(object: ValueEventListener {

                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(User::class.java)
                            if (user != null) {
                                adapter.add(BlockedUserItem(user))
                            }

                            adapter.setOnItemClickListener { item, view ->
                                val blockedUserItem = item as BlockedUserItem
                                val user = blockedUserItem.blockedUser
                                val action = BlockedUsersFragmentDirections.actionBlockedUsersFragmentToUserProfileFragment(user)
                                findNavController().navigate(action)
                            }

                            adapter.setOnItemLongClickListener { item, view ->
                                popupMenu(item,view)
                                true
                            }
                            binding.recyclerViewBlockedUsersFragment.adapter = adapter
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

    private fun popupMenu(item: Item<*>,view: View) {
        val userId = FirebaseAuth.getInstance().uid
        val blockedUserItem = item as BlockedUserItem
        val blockedUser = blockedUserItem.blockedUser

        val popupMenu = PopupMenu(activity,view)
        popupMenu.inflate(R.menu.blocked_users_menu)
        popupMenu.gravity = Gravity.RIGHT
        view.setBackgroundColor(Color.LTGRAY)

        popupMenu.setOnDismissListener {
            view.setBackgroundColor(Color.WHITE)
        }

        popupMenu.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.unblockUser -> {
                    val ref = FirebaseDatabase.getInstance().getReference("blocked-users/$userId/${blockedUser.uid}")
                    ref.removeValue()
                }
            }
            true
        }
        popupMenu.show()
    }

    class BlockedUserItem(val blockedUser: User) : Item<GroupieViewHolder>() {

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.usernameTextView_contact_row.text = blockedUser.username

            if(blockedUser.profileImageUrl == null){
                viewHolder.itemView.userAvatarImageView_contact_row.setImageResource(R.drawable.avatar_icon)
            }
            else {
                Picasso.get().load(blockedUser.profileImageUrl).into(viewHolder.itemView.userAvatarImageView_contact_row)
            }
        }

        override fun getLayout(): Int {
            return R.layout.contact_row
        }
    }

}