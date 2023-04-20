package com.example.chatapp.mainFunctions

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentNavigationBinding
import com.example.chatapp.model.User
import com.google.android.material.badge.BadgeDrawable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_navigation.*

class NavigationFragment : Fragment() {

    companion object {
        var currentUser: User? = null
    }

    private lateinit var binding: FragmentNavigationBinding
    private val contactsFragment = ContactsFragment()
    private val latestMessagesFragment = LatestMessagesFragment()
    private val notificationsFragment = NotificationsFragment()

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
        binding =  FragmentNavigationBinding.inflate(inflater,container,false)
        getCurrentUser()
        replaceFragment(latestMessagesFragment)

        binding.statusNavigationViewNavigationFragment.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.onlineStatusDrawer -> changeStatus("online")
                R.id.idleStatusDrawer -> changeStatus("idle")
                R.id.doNotDisturbDrawer -> changeStatus("doNotDisturb")
                R.id.offlineDrawer -> changeStatus("offline")
                R.id.settingsDrawer -> {
                    val action = NavigationFragmentDirections.actionNavigationFragmentToSettingsFragment()
                    findNavController().navigate(action)
                }
                R.id.logOutDrawer -> logout()
            }
            true
        }

        binding.bottomNavigationViewNavigationFragment.setOnItemSelectedListener {
            when(it.itemId){
                R.id.latestMessage -> {
                    binding.toolbarTextViewNavigationFragment.text = getString(R.string.latest_message_toolbar_title)
                    binding.toolbarNavigationFragment.menu.clear()
                    replaceFragment(latestMessagesFragment)
                }
                R.id.contacts -> {
                    binding.toolbarTextViewNavigationFragment.text = getString(R.string.contacts_toolbar_title)
                    binding.toolbarNavigationFragment.menu.clear()
                    binding.toolbarNavigationFragment.inflateMenu(R.menu.contacts_toolbar_menu)
                    binding.toolbarNavigationFragment.setOnMenuItemClickListener {
                        when(it.itemId){
                            R.id.add_contact_toolbar_button -> {
                                val action = NavigationFragmentDirections.actionNavigationFragmentToAddContactFragment()
                                findNavController().navigate(action)
                            }
                            R.id.create_group_chat -> {
                                val action = NavigationFragmentDirections.actionNavigationFragmentToGroupChatCreationFragment()
                                findNavController().navigate(action)
                            }
                            R.id.show_group_chats -> {
                                val action = NavigationFragmentDirections.actionNavigationFragmentToGroupChatListFragment()
                                findNavController().navigate(action)
                            }
                        }
                        true
                    }
                    replaceFragment(contactsFragment)
                }
                R.id.notifications -> {
                    binding.toolbarTextViewNavigationFragment.text = getString(R.string.notifications_toolbar_title)
                    binding.toolbarNavigationFragment.menu.clear()
                    replaceFragment(notificationsFragment)
                }
            }
            true
        }



        binding.toolbarImageViewNavigationFragment.setOnClickListener {
            binding.drawerLayout.openDrawer(Gravity.LEFT)
        }

        val badge = binding.bottomNavigationViewNavigationFragment.getOrCreateBadge(R.id.notifications)
        listenForNotification(badge)

        return binding.root
    }

    private fun getCurrentUser() {
        val userId = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("users/$userId")

        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser =  snapshot.getValue(User::class.java)
                setDrawerData()
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun setDrawerData(){
        val header = binding.statusNavigationViewNavigationFragment.getHeaderView(0);
        val usernameTextView = header.findViewById<TextView>(R.id.usernameTextView_drawer_header)
        //val numberTextView = header.findViewById<TextView>(R.id.numberTextView_drawer_header)
        val descriptionTextView =  header.findViewById<TextView>(R.id.descriptionTextView_drawer_header)
        val userAvatarImageView = header.findViewById<ImageView>(R.id.userAvatarImageView_drawer_header)

        userAvatarImageView.setOnClickListener {
            val action = NavigationFragmentDirections.actionNavigationFragmentToUserProfileFragment(currentUser!!)
            findNavController().navigate(action)
        }

        usernameTextView.text = currentUser?.username
        //numberTextView.text = currentUser?.number.toString()
        descriptionTextView.text = currentUser?.description
        if(currentUser?.profileImageUrl != null) {
            Picasso.get().load(currentUser?.profileImageUrl).into(userAvatarImageView)
            Picasso.get().load(currentUser?.profileImageUrl).into(binding.toolbarImageViewNavigationFragment)
        }
        setStatus(currentUser?.status.toString())

    }

    //function to change user status in database
    private fun changeStatus(status: String) {
        val userId = currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("users/$userId")
        val hashMap : HashMap<String, Any>
                = HashMap<String, Any>()
        hashMap.put("status", status)
        ref.updateChildren(hashMap)
        setStatus(status)
    }

    //function to change status imageView
    private fun setStatus(status: String){
        val header = binding.statusNavigationViewNavigationFragment.getHeaderView(0);
        val statusImageView = header.findViewById<ImageView>(R.id.statusImageView_drawer_header)
        when(status) {
            "online" -> {statusImageView.setImageResource(R.drawable.online_icon)
                binding.statusNavigationViewNavigationFragment.setCheckedItem(R.id.onlineStatusDrawer)}
            "idle" -> {statusImageView.setImageResource(R.drawable.idle_icon)
                binding.statusNavigationViewNavigationFragment.setCheckedItem(R.id.idleStatusDrawer)}
            "doNotDisturb" -> {statusImageView.setImageResource(R.drawable.do_not_disturb_icon)
                binding.statusNavigationViewNavigationFragment.setCheckedItem(R.id.doNotDisturbDrawer)}
            "offline" -> {statusImageView.setImageResource(R.drawable.offline_icon)
                binding.statusNavigationViewNavigationFragment.setCheckedItem(R.id.offlineDrawer)}
        }
    }

    private fun logout() {
        val userId = FirebaseAuth.getInstance().uid
        val userTokenRef = FirebaseDatabase.getInstance().getReference("users/$userId/token")
        val userStatusRef = FirebaseDatabase.getInstance().getReference("users/$userId/status")
        userTokenRef.removeValue()
        userStatusRef.setValue("offline")
        Firebase.auth.signOut()
        findNavController().popBackStack(R.id.startScreenFragment,false)
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(binding.frameLayoutNavigationFragment.id, fragment)
        transaction?.commit()
    }

    private fun listenForNotification(badge: BadgeDrawable){
        val userId = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("notifications/$userId/invitations")

        val query = ref.orderByChild("read").equalTo(false)

        query.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notificationCounter = snapshot.childrenCount.toInt()
                if( notificationCounter == 0 ) {
                    badge.isVisible = false
                } else {
                    badge.isVisible = true
                    badge.number = snapshot.childrenCount.toInt()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }
}