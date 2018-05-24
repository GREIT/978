package it.polito.mad.greit.project

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toolbar
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.firebase.ui.database.ChangeEventListener
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_other_profile.*

class OtherProfile : AppCompatActivity() {
  private val TAG = "Other Profile"

  private var mReviewsList: RecyclerView? = null
  private var mDatabase: DatabaseReference? = null
  private var mAdapter: FirebaseRecyclerAdapter<Review, ReviewViewHolder>? = null
  private var tw: TextView? = null
  private var rb: RatingBar? = null
  private var iw: ImageView? = null
  private var tb: android.support.v7.widget.Toolbar? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_other_profile)

    tb = findViewById(R.id.other_profile_toolbar)
    setSupportActionBar(tb)
    tb!!.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
    tb!!.setNavigationOnClickListener{view -> onBackPressed()}



    mReviewsList = findViewById(R.id.other_profile_reviews)

    val uid: String = intent.getStringExtra("uid")

    mDatabase = FirebaseDatabase.getInstance().getReference("USERS").child(uid)

    val sr: StorageReference? = FirebaseStorage.getInstance().getReference().child("profile_pictures/" + uid + ".jpg")

    mDatabase!!.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(dataSnapshot: DataSnapshot) {
        val user = dataSnapshot.getValue(Profile::class.java)

        tw = findViewById(R.id.other_profile_username)
        tw!!.setText("@" + user!!.username)

        tw = findViewById(R.id.other_profile_bio)
        tw!!.setText(user!!.bio)

        rb = findViewById(R.id.other_profile_mean_rating)
        rb!!.numStars = 5

        iw = findViewById(R.id.other_profile_pic)
        sr!!.downloadUrl.addOnSuccessListener { uri ->
          Glide.with(this@OtherProfile)
              .load(uri)
              .into(iw)
        }.addOnFailureListener { e ->
          Glide.with(this@OtherProfile)
              .load("")
              .apply(RequestOptions()
                  .error(R.mipmap.ic_launcher_round)
                  .fitCenter())
              .into(iw)
        }

      }

      override fun onCancelled(error: DatabaseError) {
        // Failed to read value
      }
    })

    mDatabase = FirebaseDatabase.getInstance().getReference("USER_REVIEWS")
    setupReviewsList(uid)
  }

  fun setupReviewsList (uid : String) {
    val layoutManager = LinearLayoutManager(this)
    layoutManager.reverseLayout = false
    mReviewsList!!.setHasFixedSize(true)
    mReviewsList!!.layoutManager = layoutManager

    val query = mDatabase!!.orderByChild("uid").equalTo(uid)

    mAdapter = object : FirebaseRecyclerAdapter<Review, ReviewViewHolder>(
        Review::class.java, R.layout.review_card, ReviewViewHolder::class.java, query) {

      override fun populateViewHolder(viewHolder: ReviewViewHolder?, model: Review?, position: Int) {
        viewHolder!!.bindReview(model)
      }

      override fun onChildChanged(type: ChangeEventListener.EventType, snapshot: DataSnapshot?, index: Int, oldIndex: Int) {
        super.onChildChanged(type, snapshot, index, oldIndex)

        mReviewsList!!.scrollToPosition(index)
      }
    }

    mReviewsList!!.adapter = mAdapter
  }

  override fun onDestroy() {
    super.onDestroy()

    mAdapter!!.cleanup()
  }
}
