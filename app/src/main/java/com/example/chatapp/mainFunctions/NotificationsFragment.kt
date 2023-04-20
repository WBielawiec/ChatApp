package com.example.chatapp.mainFunctions

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentNotificationsBinding
import com.example.chatapp.model.ContactRequest
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.notification_add_contatct_row.view.*
import java.text.SimpleDateFormat
import java.util.*

private lateinit var binding: FragmentNotificationsBinding
private lateinit var currentUserId : String

class NotificationsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentNotificationsBinding.inflate(inflater,container,false)

        displayNotifications()

        return binding.root
    }

    private fun displayNotifications(){
        currentUserId = FirebaseAuth.getInstance().uid.toString()
        val adapter = GroupAdapter<GroupieViewHolder>()
        binding.notificationRecyclerViewNotificationFragment.adapter = adapter
        val ref = FirebaseDatabase.getInstance().getReference("/notifications/$currentUserId/invitations")

        ref.addChildEventListener(object: ChildEventListener{

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val contactRequest = snapshot.getValue(ContactRequest::class.java)
                val communicat = " invited you to contact"

                if(contactRequest != null){
                    adapter.add(ContactRequestItem(contactRequest, communicat, adapter))
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
        })
    }

    class ContactRequestItem(val contactRequest: ContactRequest, val communicat: String, val adapter: GroupAdapter<GroupieViewHolder>): Item<GroupieViewHolder>(){
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {

            val userRef = FirebaseDatabase.getInstance().getReference("users/${contactRequest.fromUserId}")

            userRef.addListenerForSingleValueEvent(object: ValueEventListener{

                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    val simpleDateFormat = SimpleDateFormat("dd/MM")
                    val sendDate = Date(contactRequest.timestamp * 1000)
                    viewHolder.itemView.usernameTextView_notification_add_contact_row.text = user?.username + communicat
                    viewHolder.itemView.dateTextView_contact_row.text = simpleDateFormat.format(sendDate)

                    if(user?.profileImageUrl == null){
                        viewHolder.itemView.userAvatarImageView_notification_add_contact_row.setImageResource(R.drawable.avatar_icon)
                    }
                    else {
                        Picasso.get().load(user?.profileImageUrl).into(viewHolder.itemView.userAvatarImageView_notification_add_contact_row)
                    }



                    viewHolder.itemView.acceptImageView_notification_add_contact_row.setOnClickListener {
                        acceptHandle()
                    }

                    viewHolder.itemView.cancelImageView_notification_add_contact_row.setOnClickListener {
                        deleteNotification()
                    }

                    viewHolder.itemView.userAvatarImageView_notification_add_contact_row.setOnClickListener {
                        val action = NavigationFragmentDirections.actionNavigationFragmentToUserProfileFragment(user!!)
                        it.findNavController().navigate(action)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

                fun acceptHandle() {
                    val contactsRef = FirebaseDatabase.getInstance().getReference("contacts/$currentUserId/${contactRequest.fromUserId}")
                    contactsRef.setValue(contactRequest.fromUserId)
                        .addOnSuccessListener {
                            deleteNotification()
                        }
                }

                fun deleteNotification(){
                    val ref = FirebaseDatabase.getInstance().getReference("/notifications/$currentUserId/invitations")
                    ref.child(contactRequest.fromUserId).removeValue()

                    adapter.removeGroupAtAdapterPosition(position)
                    adapter.notifyDataSetChanged()
                }
            })
        }

        override fun getLayout(): Int {
            return R.layout.notification_add_contatct_row
        }

    }
}