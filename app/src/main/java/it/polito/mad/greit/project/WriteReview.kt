package it.polito.mad.greit.project

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*


private const val ARG_UID = "reviewed_uid"
private const val ARG_USERNAME = "reviewed_username"
private const val ARG_TITLE = "shared_book_title"
private const val ARG_CHAT_ID = "chat_id"
private const val ARG_ALREADY_REVIEWED = "already_reviewed"

class WriteReview : DialogFragment() {

  private var uid: String? = null
  private var username: String? = null
  private var title: String? = null
  private var chatId: String? = null
  private var alreadyReviewed: Boolean? = null

  private var mDatabase: FirebaseDatabase? = null

  var tw: TextView? = null
  var et: EditText? = null
  var bt: Button? = null
  var rb: RatingBar? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.let {
      uid = it.getString(ARG_UID)
      username = it.getString(ARG_USERNAME)
      title = it.getString(ARG_TITLE)
      chatId = it.getString(ARG_CHAT_ID)
      alreadyReviewed = it.getBoolean(ARG_ALREADY_REVIEWED)
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    // Inflate the layout for this fragment
    val v: View = inflater.inflate(R.layout.fragment_write_review, container, false)

    tw = v.findViewById(R.id.write_review_reviewed_username)
    tw!!.text = "@" + username

    tw = v.findViewById(R.id.write_review_book_title)
    tw!!.text = "for " + title

    et = v.findViewById(R.id.write_review_comment)
    et!!.setScroller(Scroller(context))
    et!!.setVerticalScrollBarEnabled(true)

    bt = v.findViewById(R.id.write_review_confirm)
    bt!!.setOnClickListener { v -> saveReview() }

    rb = v.findViewById(R.id.write_review_rating)

    if (alreadyReviewed!!) {
      val db = FirebaseDatabase.getInstance()
      val dbref = db.getReference("USER_REVIEWS").child(chatId)

      dbref.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
          val review = dataSnapshot!!.getValue<Review>(Review::class.java)

          rb!!.rating = review!!.rating!!

          et!!.setText(review!!.comment)

        }

        override fun onCancelled(e: DatabaseError) {
        }
      })
    }



    return v

  }

  fun saveReview() {
    val comment: String? = et!!.text.toString()

    val review = Review()

    review.date = System.currentTimeMillis() / 1000L
    review.comment = comment
    review.reviewedUid = uid
    review.reviewedUsername = username
    review.rating = rb!!.rating

    val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    review.reviewerUid = user!!.uid
    review.reviewerUsername =
        this.activity.getSharedPreferences("sharedpref", Context.MODE_PRIVATE).getString("username", null)

    mDatabase = FirebaseDatabase.getInstance()

    mDatabase!!.getReference("USER_REVIEWS").child(chatId).setValue(review)

    mDatabase!!.getReference("TRANSACTIONS/" + chatId).child("alreadyReviewed").setValue(true)

    if (alreadyReviewed!!) {
      dismiss()
    } else {
      val db = FirebaseDatabase.getInstance()
      val dbref = db.getReference("USERS").child(uid)

      dbref.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
          val profile = dataSnapshot!!.getValue<Profile>(Profile::class.java)
          mDatabase = FirebaseDatabase.getInstance()

          mDatabase!!.getReference("USERS/" + uid).child("totReviewsReceived").setValue(Integer.valueOf(profile!!.totReviewsReceived + 1))
          mDatabase!!.getReference("USERS/" + uid).child("totScoringReviews").setValue(profile!!.totScoringReviews + rb!!.rating)
          dismiss()
        }

        override fun onCancelled(e: DatabaseError) {
          FirebaseAuth.getInstance().signOut()
          val intent = Intent(activity, SignInActivity::class.java)
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
          startActivity(intent)
          dismiss()
        }
      })
    }
  }
}
