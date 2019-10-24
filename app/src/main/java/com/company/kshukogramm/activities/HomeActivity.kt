package com.company.kshukogramm.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.company.kshukogramm.R
import com.company.kshukogramm.utils.FirebaseHelper
import com.company.kshukogramm.utils.ValueEventListenerAdapter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : BaseActivity(0) {
    private val TAG = "HomeActivity"
    private lateinit var mFirebase: FirebaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setupBottomNavigation()
        Log.d(TAG, "onCreate")

        mFirebase = FirebaseHelper(this)

        sign_out_text.setOnClickListener{
            mFirebase.auth.signOut()
        }
        mFirebase.auth.addAuthStateListener {
            if (it.currentUser == null){
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
        mFirebase.database.child("feed-posts").child(mFirebase.auth.currentUser!!.uid)
            .addValueEventListener(ValueEventListenerAdapter{
                val posts = it.children.map { it.getValue(FeedPost::class.java)!! }
                Log.d(TAG,"feedPosts: ${posts.first().timestampDate()}")
            })
    }

    override fun onStart() {
        super.onStart()
        if(mFirebase.auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
