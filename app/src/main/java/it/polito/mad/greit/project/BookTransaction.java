package it.polito.mad.greit.project;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.io.Serializable;

public class BookTransaction implements Serializable{

    private String chatId;
    private String ownerId;
    private String ownerUsername;
    private String receiverId;
    private String receiverUsername;
    private String bookId;
    private String bookUsername;
    private int counter;
    private long dateStart;
    private long dateEnd;

    public BookTransaction(){}

    public BookTransaction(String ownerId,String ownerUsername,String receiverId
            ,String receiverUsername,String bookId,String chatId,
                       String bookUsername){
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.receiverId = receiverId;
        this.receiverUsername = receiverUsername;
        this.bookId = bookId;
        this.bookUsername = bookUsername;
        this.dateStart = 0;
        this.dateEnd = 0;
        this.counter = 0;
        this.chatId = chatId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
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

    public String getBookUsername() {
        return bookUsername;
    }

    public void setBookUsername(String bookUsername) {
        this.bookUsername = bookUsername;
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

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void startTransaction(){
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TRANSACTIONS").child(this.chatId);
        BookTransaction bt = this;
        dbref.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() == null){
                    bt.dateStart = System.currentTimeMillis()/1000L;
                    bt.dateEnd = 0;
                    bt.counter++;
                    mutableData.setValue(bt);
                    return Transaction.success(mutableData);
                }
                else
                    bt.updateTransaction(+1);
                    return Transaction.abort();
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
            }
        });
    }

    public void updateTransaction(int toggle){
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TRANSACTIONS").child(this.chatId);
        BookTransaction localbt = this;
        dbref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() == null){
                    return Transaction.abort();
                }
                else{
                    BookTransaction bt = mutableData.getValue(BookTransaction.class);
                    bt.setCounter(bt.counter + toggle);
                    mutableData.setValue(bt);
                    localbt.counter++;
                    if(bt.counter == 2){
                        closeTransaction(System.currentTimeMillis()/1000L);
                    }
                    return Transaction.success(mutableData);
                }
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
            }
        });
    }

    public void closeTransaction(long dateEnd){
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TRANSACTIONS").child(this.chatId);
        BookTransaction localbt = this;
        this.setDateEnd(dateEnd);
        dbref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() == null){
                    return Transaction.abort();
                }
                else{
                    BookTransaction bt = mutableData.getValue(BookTransaction.class);
                    bt.setDateEnd(dateEnd);
                    mutableData.setValue(bt);
                    localbt.dateEnd = dateEnd;
                    return Transaction.success(mutableData);
                }
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });
    }

}
