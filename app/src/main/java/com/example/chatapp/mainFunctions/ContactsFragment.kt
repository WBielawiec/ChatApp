package com.example.chatapp.mainFunctions

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentContactsBinding
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.contact_row.view.*

class ContactsFragment : Fragment() {

    companion object {
        var currentUser: User? = null
    }

    private lateinit var binding: FragmentContactsBinding
    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContactsBinding.inflate(inflater,container,false)

        binding.recyclerViewContactFragment.adapter = adapter

        getCurrentUser()
        getContacts()

        return binding.root
    }

    val contactsMap = HashMap<String, String>()

    private fun refreshRecyclerViewContacts(){
        adapter.clear()

        contactsMap.toList()
            .sortedBy { it.second }
            .forEach { (key,value) ->
                adapter.add(ContactItem(key))

                adapter.setOnItemClickListener { item, view ->
                    val contactItem = item as ContactItem
                    val user = contactItem.contact
                    val action = NavigationFragmentDirections.actionNavigationFragmentToChatFragment(currentUser!!,user!!)
                    findNavController().navigate(action)
                }

                adapter.setOnItemLongClickListener { item, view ->
                    popupMenu(item,view)
                    true
                }
            }
    }

    private fun getContacts() {
        val userId = FirebaseAuth.getInstance().uid
        val contactsRef = FirebaseDatabase.getInstance().getReference("contacts/$userId")

        contactsRef.addChildEventListener(object : ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val contact = snapshot.getValue(String::class.java) ?: return
                contactsMap[snapshot.key!!] = contact
                refreshRecyclerViewContacts()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val contact = snapshot.getValue(String::class.java) ?: return
                contactsMap[snapshot.key!!] = contact
                refreshRecyclerViewContacts()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                contactsMap.remove(snapshot.key!!)
                refreshRecyclerViewContacts()
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun popupMenu(item: Item<*>, view: View) {
        val contactItem = item as ContactItem
        val contact = contactItem.contact

        view.setBackgroundColor(Color.LTGRAY)

        val popupMenu = PopupMenu(activity,view)
        popupMenu.inflate(R.menu.contact_popup_menu)
        popupMenu.gravity = Gravity.RIGHT

        popupMenu.setOnDismissListener {
            view.setBackgroundColor(Color.WHITE)
        }

        popupMenu.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.showProfile -> {
                    val action = NavigationFragmentDirections.actionNavigationFragmentToUserProfileFragment(contact!!)
                    findNavController().navigate(action)
                }
                R.id.openChat -> {
                    val action = NavigationFragmentDirections.actionNavigationFragmentToChatFragment(currentUser!!,contact!!)
                    findNavController().navigate(action)
                }
                R.id.blockUser -> {
                    val blockRef = FirebaseDatabase.getInstance().getReference("blocked-users/${currentUser?.uid}")
                    blockRef.child(contact!!.uid).setValue(contact.uid)
                    val ref = FirebaseDatabase.getInstance().getReference("/contacts/${currentUser?.uid}")
                    ref.child(contact.uid).removeValue()
                }
                R.id.deleteContact -> {
                    val ref = FirebaseDatabase.getInstance().getReference("/contacts/${currentUser?.uid}")
                    ref.child(contact!!.uid).removeValue()
                }
            }
            true
        }
        popupMenu.show()
    }

    class ContactItem(val contactId: String): Item<GroupieViewHolder>(){
        var contact : User? = null

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {

            val contactRef = FirebaseDatabase.getInstance().getReference("/users/$contactId")
            Log.d("contacts", "its working")

            contactRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    contact = snapshot.getValue(User::class.java)

                    viewHolder.itemView.usernameTextView_contact_row.text = contact?.username

                    if(contact?.profileImageUrl == null){
                        viewHolder.itemView.userAvatarImageView_contact_row.setImageResource(R.drawable.avatar_icon)
                    }
                    else {
                        Picasso.get().load(contact?.profileImageUrl).into(viewHolder.itemView.userAvatarImageView_contact_row)
                    }

                    setStatus(contact?.status, viewHolder.itemView.statusImageView_contact_row)

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })

        }

        override fun getLayout(): Int {
            return R.layout.contact_row
        }

        private fun setStatus(status: String?, statusImageView: ImageView){
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

    private fun getCurrentUser(){
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}