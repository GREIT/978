package it.polito.mad.greit.project

import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import java.util.*


private const val ARG_UID = "reviewed_uid"
private const val ARG_USERNAME = "reviewed_username"
private const val ARG_TITLE = "shared_book_title"

class WriteReview : DialogFragment() {

  private var uid: String? = null
  private var username: String? = null
  private var title: String? = null

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
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    // Inflate the layout for this fragment
    val v: View = inflater.inflate(R.layout.fragment_write_review, container, false)

    tw = v.findViewById(R.id.write_review_reviewed_username)
    tw!!.text = "@" +username

    tw = v.findViewById(R.id.write_review_book_title)
    tw!!.text = "for " + title

    et = v.findViewById(R.id.write_review_comment)
    et!!.setScroller(Scroller(context))
    et!!.setVerticalScrollBarEnabled(true)

    bt = v.findViewById(R.id.write_review_confirm)
    // TODO set click listener

    rb = v.findViewById(R.id.write_review_rating)

    return v

  }

  fun saveReview() {
    val comment: String? = et!!.text.toString()
    var key: String? = null

    mDatabase = FirebaseDatabase.getInstance()
    key = mDatabase!!.getReference("USER_REVIEWS").push().key

    val review = Review()

    review.date = Calendar.getInstance().getTime().toString()
    review.comment = comment
    review.reviewedUid = uid
    review.reviewedUsername = username
    review.rating = rb!!.numStars.toString()

    val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    review.reviewerUid = user!!.uid
    review.reviewerUsername =
        this.activity.getSharedPreferences("sharedpref", Context.MODE_PRIVATE).getString("username", null)

    mDatabase!!.getReference("USER_REVIEWS").child(key).setValue(review)

     dismiss()
  }
}
