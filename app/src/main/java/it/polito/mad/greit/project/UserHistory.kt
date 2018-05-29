package it.polito.mad.greit.project

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.firebase.ui.database.ChangeEventListener
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Transaction

import kotlinx.android.synthetic.main.activity_user_history.*
import java.util.*

class UserHistory : AppCompatActivity() {
  private val TAG = "Transaction History"

  private var mTransactionList: RecyclerView? = null
  private var mDatabaseTransactions: DatabaseReference? = null
  private var mAdapter: FirebaseRecyclerAdapter<BookTransaction, TransactionViewHolder>? = null
  private var tw: TextView? = null
  private var rb: RatingBar? = null
  private var iw: ImageView? = null
  private var tb: android.support.v7.widget.Toolbar? = null

  private var dates: SortedMap<Long, Int>? = null
  private var positions: ArrayList<Int>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_user_history)

    mDatabaseTransactions = FirebaseDatabase.getInstance().getReference("TRANSACTIONS");
  }

  override fun onStart() {
    super.onStart()

    tb = findViewById(R.id.user_history_toolbar)
    setSupportActionBar(tb!!)
    tb!!.title = "History"
    tb!!.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
    tb!!.setNavigationOnClickListener{view -> onBackPressed()}

    setupTransactionsList(intent.getStringExtra("uid"))
  }

  fun setupTransactionsList (uid : String) {
    val layoutManager = LinearLayoutManager(this)
    layoutManager.reverseLayout = true
    layoutManager.stackFromEnd = true
    user_history_transactions.setHasFixedSize(true)
    user_history_transactions.layoutManager = layoutManager

    val query = mDatabaseTransactions!!.orderByChild("actors/" + uid).equalTo(uid);

    mAdapter = object : FirebaseRecyclerAdapter<BookTransaction, TransactionViewHolder>(
        BookTransaction::class.java, R.layout.transaction_card, TransactionViewHolder::class.java, query) {

      override fun populateViewHolder(viewHolder: TransactionViewHolder?, model: BookTransaction?, position: Int) {
        viewHolder!!.bindReview(model, getSupportFragmentManager(), applicationContext)
      }

      override fun onChildChanged(type: ChangeEventListener.EventType, snapshot: DataSnapshot?, index: Int, oldIndex: Int) {
        super.onChildChanged(type, snapshot, index, oldIndex)

        user_history_transactions.scrollToPosition(index)
      }

      override fun onDataChanged() {
        super.onDataChanged()

        dates = TreeMap<Long, Int>()
        positions = ArrayList()

        for (i in 1..itemCount)
          dates!!.put(mSnapshots.getObject(i - 1).dateStart, i - 1)
        for (entry in dates!!.entries)
          positions!!.add(entry.value)

        Collections.reverse(positions!!)
      }

      override fun getItem(position: Int): BookTransaction {
        return mSnapshots.getObject(positions!!.get(position)) as BookTransaction
      }
    }

    user_history_transactions.adapter = mAdapter
  }

  override fun onDestroy() {
    super.onDestroy()

    mAdapter!!.cleanup()
  }

}
