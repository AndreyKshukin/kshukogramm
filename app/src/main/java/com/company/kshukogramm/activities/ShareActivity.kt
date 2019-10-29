package com.company.kshukogramm.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.company.kshukogramm.R
import com.company.kshukogramm.models.FeedPost
import com.company.kshukogramm.models.User
import com.company.kshukogramm.utils.CameraHelper
import com.company.kshukogramm.utils.FirebaseHelper
import com.company.kshukogramm.utils.GlideApp
import com.company.kshukogramm.utils.ValueEventListenerAdapter
import kotlinx.android.synthetic.main.activity_share.*

class ShareActivity : BaseActivity(2) {
    private val TAG = "ShareActivity"
    private lateinit var mCamera: CameraHelper
    private lateinit var mFirebase: FirebaseHelper
    private lateinit var mUser: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)
        Log.d(TAG, "onCreate")

        mFirebase = FirebaseHelper(this)
        mCamera = CameraHelper(this)
        mCamera.takeCameraPicture()

        back_image.setOnClickListener{ finish()}
        share_text.setOnClickListener{ share()}

        mFirebase.currentUserReference().addValueEventListener(ValueEventListenerAdapter{
            mUser = it.asUser()!!
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == mCamera.REQUES_CODE ){
            if(resultCode == Activity.RESULT_OK){
                GlideApp.with(this).load(mCamera.imageUri).centerCrop().into(post_image)
            } else {
                finish()
            }
        }
    }

    private fun share() {
        val imageUri = mCamera.imageUri
        if(mCamera.imageUri != null){
            val uid = mFirebase.currentUid()!!
            mFirebase.storage.child("users").child(uid).child("images")
                .child(imageUri!!.lastPathSegment!!).putFile((imageUri)).addOnCompleteListener{
                    if(it.isSuccessful){
                        val downloadTask = it.result!!.metadata!!.reference!!.downloadUrl
                        downloadTask.addOnSuccessListener{url ->
                            mFirebase.database.child("images").child(uid).push()
                                .setValue(url.toString()).addOnCompleteListener{
                                    if(it.isSuccessful){
                                        mFirebase.database.child("feed-posts").child(uid)
                                            .push().setValue(mkFeedPost(uid, url))
                                            .addOnCompleteListener{
                                                if(it.isSuccessful){
                                                    startActivity(Intent(this,
                                                        ProfileActivity::class.java))
                                                    finish()
                                                }
                                            }

                                    }else {
                                        showToast(it.exception!!.message!!)
                                    }
                                }
                        }
                    } else {
                        showToast(it.exception!!.message!!)
                    }
                }
        }
    }

    private fun mkFeedPost(
        uid: String,
        url: Uri
    ): FeedPost {
        return FeedPost(
            uid = uid,
            username = mUser.username,
            image = url.toString(),
            caption = caption_input.text.toString(),
            photo = mUser.photo
        )
    }
}

