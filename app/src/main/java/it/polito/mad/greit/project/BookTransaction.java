package it.polito.mad.greit.project;

import android.content.SharedPreferences;
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

public class BookTransaction implements Serializable{

    private String chatId;
    private String ownerUid;
    private String ownerUsername;
    private String receiverUid;
    private String receiverUsername;
    private String bookId;
    private String bookTitle;
    private long dateStart;
    private long dateEnd;
    //if the receiver has still the book, this is false, true otherwise
    private Boolean isFree;

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

    public Boolean getFree() {
        return isFree;
    }

    public void setFree(Boolean free) {
        isFree = free;
    }

    public void lock_book(){
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TRANSACTIONS").child(this.chatId);
        BookTransaction current = this;

        dbref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() == null){
                    current.setFree(false);
                    current.setDateStart(System.currentTimeMillis()/1000L);
                    mutableData.setValue(current);
                    Log.d("DEBUGTRANSACTION", "startTransaction: set mutable data in lock" + current.isFree);
                    return Transaction.success(mutableData);

                }
                else {
                    BookTransaction bt = mutableData.getValue(BookTransaction.class);
                    bt.setFree(false);
                    bt.setDateStart(System.currentTimeMillis()/1000L);
                    mutableData.setValue(bt);
                    Log.d("DEBUGTRANSACTION", "startTransaction: data already present in lock" + current.isFree);
                    return Transaction.success(mutableData);
                }
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if(b) {
                    Log.d("DEBUGTRANSACTION", "entered on complete lock");
                    updateSharedBook(true);
                }
            }
        });

    }

    public void unlock_book(){
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TRANSACTIONS").child(this.chatId);

        dbref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() != null){
                    BookTransaction bt = mutableData.getValue(BookTransaction.class);
                    bt.setFree(true);
                    bt.setDateEnd(System.currentTimeMillis()/1000L);
                    mutableData.setValue(bt);
                    return Transaction.success(mutableData);
                }
                else {
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if(b) {
                    updateSharedBook(false);
                }
            }
        });
    }

    private void updateSharedBook(Boolean op){
        //again, if op is true book is locked, unlocked otherwise
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("SHARED_BOOKS").child(this.bookId);
        BookTransaction bt = this;
        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                SharedBook sb = dataSnapshot.getValue(SharedBook.class);
                if(op){
                    Log.d("DEBUGTRANSACTION", "startTransaction: updating lock");
                    sb.setBorrowToUid(bt.getReceiverUid());
                    sb.setBorrowToUsername(bt.getReceiverUsername());
                    sb.setShared(true);
                }else{
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

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
