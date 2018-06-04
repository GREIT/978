package it.polito.mad.greit.project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class BookTransaction implements Serializable, Comparable {

  private String chatId;
  private String ownerUid;
  private Map<String, String> actors;
  private String ownerUsername;
  private Boolean alreadyReviewedByOwner;
  private Boolean alreadyReviewedByBorrower;
  private String receiverUid;
  private String receiverUsername;
  private String bookId;
  private String bookTitle;
  private long dateStart;
  private long dateEnd;
  //if the receiver has still the book, this is false, true otherwise
  private Boolean free;

  public String getOwnerUid() {
    return ownerUid;
  }

  public void setOwnerUid(String ownerUid) {
    this.ownerUid = ownerUid;
  }

  public String getOwnerUsername() {
    return ownerUsername;
  }

  public void setOwnerUsername(String ownerUsername) {
    this.ownerUsername = ownerUsername;
  }

  public String getReceiverUid() {
    return receiverUid;
  }

  public void setReceiverUid(String receiverUid) {
    this.receiverUid = receiverUid;
  }

  public String getReceiverUsername() {
    return receiverUsername;
  }

  public void setReceiverUsername(String receiverUsername) {
    this.receiverUsername = receiverUsername;
  }

  public Boolean getAlreadyReviewedByOwner() {
    return alreadyReviewedByOwner;
  }

  public void setAlreadyReviewedByOwner(Boolean alreadyReviewedByOwner) {
    this.alreadyReviewedByOwner = alreadyReviewedByOwner;
  }

  public Boolean getAlreadyReviewedByBorrower() {
    return alreadyReviewedByBorrower;
  }

  public void setAlreadyReviewedByBorrower(Boolean alreadyReviewedByBorrower) {
    this.alreadyReviewedByBorrower = alreadyReviewedByBorrower;
  }

  public Map<String, String> getActors() {
    return actors;
  }

  public void setActors(Map<String, String> actors) {
    this.actors = actors;
  }

  public String getBookId() {
    return bookId;
  }

  public void setBookId(String bookId) {
    this.bookId = bookId;
  }

  public String getBookTitle() {
    return bookTitle;
  }

  public void setBookTitle(String bookTitle) {
    this.bookTitle = bookTitle;
  }

  public long getDateStart() {
    return dateStart;
  }

  public void setDateStart(long dateStart) {
    this.dateStart = dateStart;
  }

  public long getDateEnd() {
    return dateEnd;
  }

  public void setDateEnd(long dateEnd) {
    this.dateEnd = dateEnd;
  }

  public String getChatId() {
    return chatId;
  }

  public void setChatId(String chatId) {
    this.chatId = chatId;
  }

  public boolean isFree() {
    return free;
  }

  public void setFree(boolean free) {
    this.free = free;
  }

  public void lock_book() {
    DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TRANSACTIONS").child(this.chatId);
    BookTransaction current = this;

    dbref.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {

        if(dataSnapshot.getValue() == null){
          current.setFree(false);
          current.setAlreadyReviewedByBorrower(false);
          current.setAlreadyReviewedByOwner(false);
          current.setDateStart(System.currentTimeMillis() / 1000L);
          Map<String, String> actors = new HashMap<>();
          actors.put(current.getOwnerUid(), current.getOwnerUid());
          actors.put(current.getReceiverUid(), current.getReceiverUid());
          current.setActors(actors);
          Log.d("DEBUGTRANSACTION", "startTransaction: set mutable data in lock" + current.free);

          dbref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
              mutableData.setValue(current);
              return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
              if (b) {
                Log.d("DEBUGTRANSACTION", "entered on complete lock");
                updateSharedBook(true);
              }
            }
          });

        } else {
          BookTransaction bt = dataSnapshot.getValue(BookTransaction.class);
          bt.setFree(false);
          bt.setAlreadyReviewedByBorrower(false);
          bt.setAlreadyReviewedByOwner(false);
          bt.setDateStart(System.currentTimeMillis() / 1000L);
          bt.setDateEnd(0);
          Map<String, String> actors = new HashMap<>();
          actors.put(bt.getOwnerUid(), bt.getOwnerUid());
          actors.put(bt.getReceiverUid(), bt.getReceiverUid());
          bt.setActors(actors);
          Log.d("DEBUGTRANSACTION", "startTransaction: data already present in lock" + current.free);

          dbref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
              mutableData.setValue(bt);
              return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
              if (b) {
                Log.d("DEBUGTRANSACTION", "entered on complete lock");
                updateSharedBook(true);
              }
            }
          });
        }
      }

      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });

  }

  public void unlock_book() {
    DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TRANSACTIONS").child(this.chatId);
    BookTransaction bt = this;
    dbref.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        if(dataSnapshot.getValue() != null){
          BookTransaction bt = dataSnapshot.getValue(BookTransaction.class);
          bt.setFree(true);
          bt.setDateEnd(System.currentTimeMillis() / 1000L);

          dbref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
              mutableData.setValue(bt);
              return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
              if (b) {
                Chat.sendnotification(bt.ownerUsername,bt.chatId,bt.receiverUid,"review");
                Chat.sendnotification(bt.receiverUsername,bt.chatId,bt.ownerUid,"review");
                updateSharedBook(false);
              }
            }
          });
        }
      }

      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });
  }

  private void updateSharedBook(Boolean op) {
    //again, if op is true book is locked, unlocked otherwise
    DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("SHARED_BOOKS").child(this.bookId);
    BookTransaction bt = this;
    dbref.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        SharedBook sb = dataSnapshot.getValue(SharedBook.class);

        if (op) {
          Log.d("DEBUGTRANSACTION", "startTransaction: updating lock");
          sb.setBorrowToUid(bt.getReceiverUid());
          sb.setBorrowToUsername(bt.getReceiverUsername());
          sb.setShared(true);
        } else {
          Log.d("DEBUGTRANSACTION", "startTransaction: updating unlock");
          sb.setBorrowToUid("");
          sb.setBorrowToUsername("");
          sb.setShared(false);
        }

        dbref.runTransaction(new Transaction.Handler() {
          @Override
          public Transaction.Result doTransaction(MutableData mutableData) {
            mutableData.setValue(sb);
            return Transaction.success(mutableData);
          }

          @Override
          public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
            if(b){
              updateBooks(op,sb.getISBN());
            }
          }
        });

      }

      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });
  }

  private void updateBooks(boolean op,String ISBN){
    FirebaseDatabase dbB = FirebaseDatabase.getInstance();
    DatabaseReference dbrefB = dbB.getReference("BOOKS").child(ISBN);

    dbrefB.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        Book B = dataSnapshot.getValue(Book.class);

        if (op) {
          B.setLentBooks(B.getLentBooks()+1);
        } else {
          B.setLentBooks(B.getLentBooks()-1);
        }


        dbrefB.runTransaction(new Transaction.Handler() {
          @Override
          public Transaction.Result doTransaction(MutableData mutableData) {
            mutableData.setValue(B);
            return Transaction.success(mutableData);
          }

          @Override
          public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

          }
        });
      }

      @Override
      public void onCancelled(DatabaseError e) {
      }
    });
  }

  @Override
  public int compareTo(@NonNull Object o) {
    return Long.compare(this.getDateStart(), ((BookTransaction) o).getDateStart());
  }
}
