package it.polito.mad.greit.project

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
class Review {
  var reviewedUid: String? = null
  var reviewedUsername: String? = null
  var reviewerUid: String? = null
  var reviewerUsername: String? = null
  var rating: String? = null
  var comment: String? = null
  var bookTitle: String? = null
  var date: String? = null

  constructor() {
  }

  constructor(reviewedUid: String?, reviewedUsername: String?, reviewerUid: String?,
              reviewerUsername: String?, rating: String?, comment: String?, bookTitle: String?,
              date: String?) {
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