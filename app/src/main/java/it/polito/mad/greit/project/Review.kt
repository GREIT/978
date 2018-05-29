package it.polito.mad.greit.project

import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
class Review {
  var reviewedUid: String? = null
  var reviewedUsername: String? = null
  var reviewerUid: String? = null
  var reviewerUsername: String? = null
  var rating: Float? = null
  var comment: String? = null
  var bookTitle: String? = null
  var date: Long? = null

  constructor() {
  }

  constructor(reviewedUid: String?, reviewedUsername: String?, reviewerUid: String?,
              reviewerUsername: String?, rating: Float?, comment: String?, bookTitle: String?,
              date: Long?) {
    this.reviewedUid = reviewedUid
    this.reviewedUsername = reviewedUsername
    this.reviewerUid = reviewerUid
    this.reviewerUsername = reviewerUsername
    this.rating = rating
    this.comment = comment
    this.bookTitle = bookTitle
    this.date = date
  }

}