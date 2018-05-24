package it.polito.mad.greit.project

import android.support.v7.view.menu.ActionMenuItemView
import android.support.v7.widget.RecyclerView
import android.view.View

import kotlinx.android.synthetic.main.review_card.*
import kotlinx.android.synthetic.main.review_card.view.*

class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  fun bindReview(review: Review?) {
    with(review!!) {
      itemView.review_card_date.text = review.date
      itemView.review_card_comment.text = review.comment
      itemView.review_card_reviewer.text = review.reviewerUsername
      itemView.review_card_rating.numStars = review.rating!!.toInt()
    }
  }
}