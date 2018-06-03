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


private const val ARG_UID = "reviewed_uid"
private const val ARG_USERNAME = "reviewed_username"
private const val ARG_TITLE = "shared_book_title"
private const val ARG_CHAT_ID = "chat_id"
private const val ARG_ALREADY_REVIEWED_BY_OWNER = "already_reviewed_by_owner"
private const val ARG_AM_I_OWNER = "owner"
private const val ARG_ALREADY_REVIEWED_BY_BORROWER = "already_reviewed_by_borrower"

class WriteReview : DialogFragment() {

  private var reviewedUid: String? = null
  private var reviewedUsername: String? = null
  private var reviewerUid: String? = null
  private var title: String? = null
  private var chatId: String? = null
  private var alreadyReviewedbyOwner: Boolean? = null
  private var alreadyReviewedbyBorrower: Boolean? = null
  private var amIOwner: Boolean? = null
  private var oldRating: Float? = null

  private var mDatabase: FirebaseDatabase? = null

  var tw: TextView? = null
  var et: EditText? = null
  var bt: Button? = null
  var rb: RatingBar? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    reviewerUid = FirebaseAuth.getInstance().currentUser!!.uid

    arguments?.let {
      reviewedUid = it.getString(ARG_UID)
      reviewedUsername = it.getString(ARG_USERNAME)
      title = it.getString(ARG_TITLE)
      chatId = it.getString(ARG_CHAT_ID)
      alreadyReviewedbyOwner = it.getBoolean(ARG_ALREADY_REVIEWED_BY_OWNER)
      alreadyReviewedbyBorrower = it.getBoolean(ARG_ALREADY_REVIEWED_BY_BORROWER)
      amIOwner = it.getBoolean(ARG_AM_I_OWNER)
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    // Inflate the layout for this fragment
    val v: View = inflater.inflate(R.layout.fragment_write_review, container, false)

    tw = v.findViewById(R.id.write_review_reviewed_username)
    tw!!.text = "@" + reviewedUsername

    tw = v.findViewById(R.id.write_review_book_title)
    tw!!.text = "for " + title

    et = v.findViewById(R.id.write_review_comment)
    et!!.setScroller(Scroller(context))
    et!!.setVerticalScrollBarEnabled(true)

    bt = v.findViewById(R.id.write_review_confirm)
    bt!!.setOnClickListener { v -> saveReview() }

    rb = v.findViewById(R.id.write_review_rating)

    if ((amIOwner!! && alreadyReviewedbyOwner!!) || (!amIOwner!! && alreadyReviewedbyBorrower!!)) {
      val db = FirebaseDatabase.getInstance()
      val dbref = db.getReference("USER_REVIEWS").child(chatId + reviewerUid)

      dbref.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
          val review = dataSnapshot!!.getValue<Review>(Review::class.java)

          rb!!.rating = review!!.rating!!

          oldRating = review!!.rating!!

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
    review.reviewedUid = reviewedUid
    review.reviewedUsername = reviewedUsername
    review.rating = rb!!.rating

    review.reviewerUid = reviewerUid
    review.reviewerUsername =
        this.activity.getSharedPreferences("sharedpref", Context.MODE_PRIVATE).getString("username", null)

    mDatabase = FirebaseDatabase.getInstance()

    mDatabase!!.getReference("USER_REVIEWS").child(chatId + reviewerUid).setValue(review)

    if (amIOwner!!) {
      mDatabase!!.getReference("TRANSACTIONS/" + chatId).child("alreadyReviewedByOwner").setValue(true)
    } else {
      mDatabase!!.getReference("TRANSACTIONS/" + chatId).child("alreadyReviewedByBorrower").setValue(true)
    }


    val db = FirebaseDatabase.getInstance()
    val dbref = db.getReference("USERS").child(reviewedUid)

    dbref.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(dataSnapshot: DataSnapshot) {
        val profile = dataSnapshot.getValue<Profile>(Profile::class.java)?: return
        mDatabase = FirebaseDatabase.getInstance()

        if ((amIOwner!! && alreadyReviewedbyOwner!!) || (!amIOwner!! && alreadyReviewedbyBorrower!!)) {
          println(oldRating!!)
          val newRating: Float = (profile!!.totScoringReviews - oldRating!!) + rb!!.rating
          mDatabase!!.getReference("USERS/" + reviewedUid).child("totScoringReviews").setValue(newRating)

        } else {
          mDatabase!!.getReference("USERS/" + reviewedUid).child("totReviewsReceived").setValue(Integer.valueOf(profile!!.totReviewsReceived + 1))
          mDatabase!!.getReference("USERS/" + reviewedUid).child("totScoringReviews").setValue(profile!!.totScoringReviews + rb!!.rating)
        }

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
