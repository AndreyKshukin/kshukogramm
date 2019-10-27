package com.company.kshukogramm.activities

import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.company.kshukogramm.R
import com.company.kshukogramm.models.User
import com.company.kshukogramm.utils.FirebaseHelper
import com.company.kshukogramm.utils.ValueEventListenerAdapter
import kotlinx.android.synthetic.main.activity_add_friends.*
import kotlinx.android.synthetic.main.add_friends_item.view.*

class AddFriendsActivity : AppCompatActivity(), FriendsAdapter.Listener {
    private lateinit var mFirebase: FirebaseHelper
    private lateinit var mUser: User
    private lateinit var mUsers: List<User>
    private lateinit var mAdapter: FriendsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friends)

        mFirebase = FirebaseHelper(this)
        mAdapter = FriendsAdapter(this)

        val uid = mFirebase.auth.currentUser!!.uid

        add_friends_recycler.adapter = mAdapter
        add_friends_recycler.layoutManager = LinearLayoutManager(this)

        mFirebase.database.child("users").addValueEventListener(ValueEventListenerAdapter{
            val allUsers = it.children.map {it.getValue(User::class.java)!!.copy(uid = it.key)}
            val (userList, otherUsersList) = allUsers.partition { it.uid == uid }
            mUser = userList.first()
            mUsers = otherUsersList

            mAdapter.update(mUsers, mUser.follows)
        })
    }

    override fun follow(uid: String) {
        setFollow(uid, true){ mAdapter.followed(uid) }
    }

    override fun unfollow(uid: String) {
        setFollow(uid, false){ mAdapter.unfollowed(uid) }
    }

    private fun setFollow(uid: String, follow: Boolean, onSuccess: () -> Unit){
        val followTask = mFirebase.database.child("users").child(mUser.uid!!).child(uid)
            .child("follows")
        val setFollow = if(follow) followTask.setValue(true) else followTask.removeValue()

        val followersTask = mFirebase.database.child("users").child(uid)
            .child("followers").child(mUser.uid!!)
        val setFollower = if(follow) followersTask.setValue(true) else followersTask.removeValue()

        setFollow.continueWithTask{setFollower}.addOnCompleteListener{
            if(it.isSuccessful){
                onSuccess()
            } else {
                showToast(it.exception!!.message!!)
            }
        }
    }
}


class FriendsAdapter(private val listener: Listener)
    : RecyclerView.Adapter<FriendsAdapter.ViewHolder> (){

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    interface Listener{
        fun follow(uid: String)
        fun unfollow(uid: String)
    }

    private var mUsers = listOf<User>()
    private var mPosition = mapOf<String, Int>()
    private var mFollows =  mapOf<String, Boolean>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.add_friends_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            val user = mUsers[position]
            view.photo_image.loadImage(mUsers[position].photo)
            view.username_text.text = user.username
            view.name_text.text = user.name
            view.follow_btn.setOnClickListener{ listener.follow(user.uid!!)}
            view.unfollow_btn.setOnClickListener{ listener.unfollow(user.uid!!)}

            val follows = mFollows[user.uid] ?: false
            if(follows){
                view.follow_btn.visibility = View.GONE
                view.unfollow_btn.visibility = View.VISIBLE
            } else {
                view.follow_btn.visibility = View.VISIBLE
                view.unfollow_btn.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = mUsers.size

    fun update(users: List<User>, follows: Map<String, Boolean>) {
        mUsers = users
        mPosition = users.withIndex().map{(idx, user) -> user.uid!! to idx}.toMap()
        mFollows = follows
        notifyDataSetChanged()
    }

    fun followed(uid: String) {
        mFollows += (uid to true)
        notifyItemChanged(mPosition[uid]!!)
    }

    fun unfollowed(uid: String) {
        mFollows -= uid
        notifyItemChanged(mPosition[uid]!!)
    }
}