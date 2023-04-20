package com.example.chatapp.mainFunctions

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.chat.GroupChatFragmentArgs
import com.example.chatapp.chat.GroupChatFragmentDirections
import com.example.chatapp.databinding.FragmentAddUserToGroupChatBinding
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.add_contact_row.view.*
import java.util.regex.Pattern


class AddUserToGroupChatFragment : Fragment() {

    private lateinit var binding: FragmentAddUserToGroupChatBinding
    private val args: AddUserToGroupChatFragmentArgs by navArgs()
    private lateinit var groupChatId : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupChatId = args.groupChatId

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            //Toast.makeText(context, "back pressed", Toast.LENGTH_LONG).show()
            val action = AddUserToGroupChatFragmentDirections.actionAddUserToGroupChatFragmentToGroupChatFragment(groupChatId)
            findNavController().navigate(action)
        }
        callback.isEnabled
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddUserToGroupChatBinding.inflate(inflater, container, false)

        binding.backImageViewToolbarAddUserToGroupChatFragment.setOnClickListener {
            val action = AddUserToGroupChatFragmentDirections.actionAddUserToGroupChatFragmentToGroupChatFragment(groupChatId)
            findNavController().navigate(action)
        }

        searchUsers()

        return binding.root
    }

    private fun searchUsers(){
        binding.searchEditTextToolbarAddUserToGroupChatFragment.addTextChangedListener(object: TextWatcher {

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if(p0.toString() == ""){
                    val adapter = GroupAdapter<GroupieViewHolder>()
                    binding.recyclerViewAddUserToGroupChatFragment.adapter = adapter
                } else if (p0.toString().matches(Regex("[0-9]+"))){
                    showUsers("number",p0.toString())
                }
                else {
                    showUsers("username",p0.toString())
                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })
    }


    private fun showUsers(path: String, text: String) {
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        val currentUserId = FirebaseAuth.getInstance().uid

        val query = ref.orderByChild(path)
            .startAt(text)
            .endAt(text + "\uf8ff")

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                val adapter = GroupAdapter<GroupieViewHolder>()

                p0.children.forEach {
                    val user = it.getValue(User::class.java)
                    if (user != null) {
                        adapter.add(UserItem(user))
                    }
                }

                adapter.setOnItemClickListener { item, view ->
                    val userItem = item as UserItem
                    val user = userItem.user

                    showDialog(user)
                }

                binding.recyclerViewAddUserToGroupChatFragment.adapter = adapter
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }

    class UserItem(val user: User) : Item<GroupieViewHolder>() {
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.usernameTextView_add_contact_row.text = user.username
            viewHolder.itemView.numberTextView_add_contact_row.text = user.number

            if(user.profileImageUrl == null){
                viewHolder.itemView.userAvatarImageView_add_contact_row.setImageResource(R.drawable.avatar_icon)
            }
            else {
                Picasso.get().load(user.profileImageUrl).into(viewHolder.itemView.userAvatarImageView_add_contact_row)
            }
        }

        override fun getLayout(): Int {
            return R.layout.add_contact_row
        }
    }

    private fun showDialog(user : User) {
        val alertDialog: AlertDialog? = activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setPositiveButton(R.string.add_group_user,
                    DialogInterface.OnClickListener { dialog, id ->
                        val groupUserRef = FirebaseDatabase.getInstance().getReference("user-group-chats/${user.uid}")
                        groupUserRef.child(groupChatId).setValue(groupChatId)
                        val userGroupRef = FirebaseDatabase.getInstance().getReference("group-chat/$groupChatId/members")
                        userGroupRef.child(user.uid).setValue(user.uid)



                        dialog.cancel()
                        Toast.makeText(context, R.string.user_added, Toast.LENGTH_LONG).show()
                    })
                setNegativeButton(R.string.cancel,
                    DialogInterface.OnClickListener { dialog, id ->
                        dialog.cancel()
                    })
            }
            builder.setTitle(R.string.add_user_dialog_title)
            builder.setMessage(getString(R.string.add_user_dialog_message, user.username))
            builder.create()
        }
        alertDialog?.show()
    }

}