package it.polito.mad.greit.project

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.transaction_card.view.*

class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  fun bindReview(review: Review?) {
    with(review!!) {
      itemView.transaction_actor.text = "@lmoro"
      itemView.transaction_arrow.setImageResource(R.drawable.ic_arrow_forward_grey_800_48dp)
      itemView.transaction_start_date.text = "26/05/2018"
      itemView.transaction_end_date.text = "29/05/2018"
    }
  }
}