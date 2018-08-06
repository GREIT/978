package it.polito.mad.greit.project

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.support.v7.widget.RecyclerView
import android.view.View

import kotlinx.android.synthetic.main.review_card.view.*
import java.text.DateFormat
import java.util.*
import android.widget.TextView



class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  fun bindReview(review: Review?, ctx: Context?) {
    with(review!!) {
      itemView.review_card_date.text = DateFormat.getDateInstance().format( Date(review.date!! * 1000))
      itemView.review_card_comment.text = review.comment
      itemView.review_card_reviewer.text = "@" + review.reviewerUsername
      itemView.review_card_rating.rating = review.rating!!.toFloat()
      itemView.review_card_reviewer.setOnClickListener{view ->
        val I = Intent(ctx!!, OtherProfile::class.java)
        I.setFlags(FLAG_ACTIVITY_NEW_TASK)
        I.putExtra("uid", review.reviewerUid)
        ctx!!.startActivity(I)
      }
    }
  }
}