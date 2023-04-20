package com.example.chatapp.mainFunctions

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentAddContactBinding
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
import kotlinx.android.synthetic.main.add_contact_row.view.*
import java.util.regex.Pattern

private lateinit var binding: FragmentAddContactBinding

class AddContactFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddContactBinding.inflate(inflater, container, false)


        binding.backImageViewToolbarAddContact.setOnClickListener {
            findNavController().popBackStack()
        }

        searchUsers()

        return binding.root
    }

    private fun searchUsers(){
        binding.searchEditTextToolbarAddContact.addTextChangedListener(object: TextWatcher {
            val pattern = Pattern.compile("^[0-9]")

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if(p0.toString() == ""){
                    val adapter = GroupAdapter<GroupieViewHolder>()
                    view?.findViewById<RecyclerView>(R.id.recyclerView_add_contact_fragment)?.adapter = adapter
                } else if (/*pattern.matcher(p0.toString()).find()*/ p0.toString().matches(Regex("[0-9]+"))){
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
                    //Log.d("NewMessage", it.toString())
                    val user = it.getValue(User::class.java)
                    if (user != null) {
                        val blockedUserRef = FirebaseDatabase.getInstance()
                            .getReference("/blocked-users/${user?.uid}")
                        blockedUserRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (!snapshot.hasChild(currentUserId.toString())) {
                                    adapter.add(UserItem(user))
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }
                        })
                    }
                }

                adapter.setOnItemClickListener { item, view ->

                    val userItem = item as UserItem
                    val user = userItem.user

                    val action = AddContactFragmentDirections.actionAddContactFragmentToUserProfileFragment(user)
                    findNavController().navigate(action)
                }

                view?.findViewById<RecyclerView>(R.id.recyclerView_add_contact_fragment)?.adapter = adapter
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
}