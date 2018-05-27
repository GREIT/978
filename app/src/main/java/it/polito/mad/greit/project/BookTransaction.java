package it.polito.mad.greit.project;

import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.io.Serializable;

public class BookTransaction implements Serializable{

    private String chatId;
    private String ownerUid;
    private String ownerUsername;
    private String receiverUid;
    private String receiverUsername;
    private String bookId;
    private String bookTitle;
    private Boolean ownerChecked;
    private Boolean receiverChecked;
    private long dateStart;
    private long dateEnd;
    //if type is false, transaction is in borrow phase, if true book is about to be returned back
    private Boolean type;

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

    public Boolean getOwnerChecked() {
        return ownerChecked;
    }

    public void setOwnerChecked(Boolean ownerChecked) {
        this.ownerChecked = ownerChecked;
    }

    public Boolean getReceiverChecked() {
        return receiverChecked;
    }

    public void setReceiverChecked(Boolean receiverChecked) {
        this.receiverChecked = receiverChecked;
    }

    public Boolean getType() {
        return type;
    }

    public void setType(Boolean type) {
        this.type = type;
    }

    public static void startTransaction(Chat c,String username){
        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TRANSACTIONS").child(c.getChatID());
        BookTransaction bt = new BookTransaction();
        dbref.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            boolean isNew = false;
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() == null){
                    bt.bookId = c.getBookID();
                    bt.bookTitle = c.getBookTitle();
                    bt.chatId = c.getChatID();
                    if(c.isMine()) { //I'm the owner
                        bt.ownerUid = fbu.getUid();
                        bt.ownerUsername = username;
                        bt.receiverUid = c.getUserID();
                        bt.receiverUsername = c.getUsername();
                        bt.ownerChecked = true;
                        bt.receiverChecked = false;
                    }
                    else { //I'm not the owner
                        bt.receiverUid = fbu.getUid();
                        bt.receiverUsername = username;
                        bt.ownerUid = c.getUserID();
                        bt.ownerUsername = c.getUsername();
                        bt.ownerChecked = false;
                        bt.receiverChecked = true;
                    }
                    bt.dateStart = System.currentTimeMillis()/1000L;
                    bt.dateEnd = 0;
                    bt.type = false; //Means borrow phase
                    mutableData.setValue(bt);
                    isNew = true;
                    return Transaction.success(mutableData);
                }
                else {
                    isNew = false;
                    return Transaction.success(mutableData);
                }
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if(b && !isNew){
                    isNew = false;
                    updateTransaction(c.getChatID());
                }
            }
        });
    }

    public static void updateTransaction(String chatId){
        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TRANSACTIONS").child(chatId);
        dbref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() == null){
                    return Transaction.abort();
                }
                else{
                    BookTransaction bt = mutableData.getValue(BookTransaction.class);
                    if(fbu.getUid().equals(bt.getOwnerUid())){
                        bt.ownerChecked = !bt.ownerChecked;
                    }
                    else{
                        bt.receiverChecked = !bt.receiverChecked;
                    }
                    mutableData.setValue(bt);
                    return Transaction.success(mutableData);
                }
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if(b){
                    BookTransaction bt = dataSnapshot.getValue(BookTransaction.class);
                    if(bt.ownerChecked && bt.receiverChecked){
                        closeTransaction(System.currentTimeMillis()/1000L,chatId);
                    }
                }
            }
        });
    }

    public static void closeTransaction(long dateEnd,String chatId){
        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TRANSACTIONS").child(chatId);
        dbref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() == null){
                    return Transaction.abort();
                }
                else{
                    BookTransaction bt = mutableData.getValue(BookTransaction.class);
                    bt.type = !bt.type;
                    bt.ownerChecked = false;
                    bt.receiverChecked = false;
                    if(!bt.type){
                        bt.dateEnd = dateEnd;
                    }
                    //maybe reset all to false for returning?
                    mutableData.setValue(bt);
                    return Transaction.success(mutableData);
                }
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                //send notification
                if(b){
                    BookTransaction bt = dataSnapshot.getValue(BookTransaction.class);
                    if(fbu.getUid().equals(bt.getOwnerUid())){
                        Chat.sendnotification(bt.ownerUsername,chatId,bt.receiverUid,"transaction");
                    }
                    else{
                        Chat.sendnotification(bt.receiverUsername,chatId,bt.ownerUid,"transaction");
                    }
                    updateSharedBook(bt);
                }
            }
        });
    }

    public static void updateSharedBook(BookTransaction bt){
        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("SHARED_BOOKS").child(bt.bookId);
        dbref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                SharedBook sb = mutableData.getValue(SharedBook.class);
                if(!bt.type){
                    sb.setShared(false);
                    sb.setBorrowToUid("");
                    sb.setBorrowToUsername("");
                }else {
                    if (fbu.getUid().equals(bt.getOwnerUid())) {
                        if (bt.getOwnerUid().equals(sb.getOwnerUid())) {
                            sb.setBorrowToUid(bt.receiverUid);
                            sb.setBorrowToUsername(bt.receiverUsername);
                            sb.setShared(true);
                        } else {
                            sb.setBorrowToUid(bt.ownerUid);
                            sb.setBorrowToUsername(bt.ownerUsername);
                            sb.setShared(true);
                        }
                    } else {
                        if (bt.getReceiverUid().equals(sb.getOwnerUid())) {
                            sb.setBorrowToUid(bt.ownerUid);
                            sb.setBorrowToUsername(bt.ownerUsername);
                            sb.setShared(true);
                        } else {
                            sb.setBorrowToUid(bt.receiverUid);
                            sb.setBorrowToUsername(bt.receiverUsername);
                            sb.setShared(true);
                        }

                    }
                }
                mutableData.setValue(sb);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });
    }

}
