package it.polito.mad.greit.project;

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
    private String user1Id;
    private String user1Username;
    private String user2Uid;
    private String user2Username;
    private String bookId;
    private String bookTitle;
    private Boolean user1Checked;
    private Boolean user2Checked;
    private long dateStart;
    private long dateEnd;

    public String getUser1Id() {
        return user1Id;
    }

    public void setUser1Id(String user1Id) {
        this.user1Id = user1Id;
    }

    public String getUser1Username() {
        return user1Username;
    }

    public void setUser1Username(String user1Username) {
        this.user1Username = user1Username;
    }

    public String getUser2Uid() {
        return user2Uid;
    }

    public void setUser2Uid(String user2Uid) {
        this.user2Uid = user2Uid;
    }

    public String getUser2Username() {
        return user2Username;
    }

    public void setUser2Username(String user2Username) {
        this.user2Username = user2Username;
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

    public Boolean getUser1Checked() {
        return user1Checked;
    }

    public void setUser1Checked(Boolean user1Checked) {
        this.user1Checked = user1Checked;
    }

    public Boolean getUser2Checked() {
        return user2Checked;
    }

    public void setUser2Checked(Boolean user2Checked) {
        this.user2Checked = user2Checked;
    }

    public static void startTransaction(Chat c,String username){
        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TRANSACTIONS").child(c.getChatID());
        BookTransaction bt = new BookTransaction();
        dbref.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() == null){
                    bt.bookId = c.getBookID();
                    bt.bookTitle = c.getBookTitle();
                    bt.chatId = c.getChatID();
                    bt.user1Id = fbu.getUid();
                    bt.user1Username = username;
                    bt.user2Uid = c.getUserID();
                    bt.user2Username = c.getUsername();
                    bt.dateStart = System.currentTimeMillis()/1000L;
                    bt.dateEnd = 0;
                    bt.user1Checked = true;
                    bt.user2Checked = false;
                    mutableData.setValue(bt);
                    return Transaction.success(mutableData);
                }
                else
                    return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if(b){
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
                    if(fbu.getUid().equals(bt.getUser1Id())){
                        bt.user1Checked = !bt.user1Checked;
                    }
                    else{
                        bt.user2Checked = !bt.user2Checked;
                    }
                    mutableData.setValue(bt);
                    return Transaction.success(mutableData);
                }
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if(b){
                    BookTransaction bt = dataSnapshot.getValue(BookTransaction.class);
                    if(bt.user1Checked && bt.user2Checked){
                        closeTransaction(System.currentTimeMillis()/1000L,chatId);
                    }
                }
            }
        });
    }

    public static void closeTransaction(long dateEnd,String chatId){
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TRANSACTIONS").child(chatId);
        dbref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() == null){
                    return Transaction.abort();
                }
                else{
                    BookTransaction bt = mutableData.getValue(BookTransaction.class);
                    bt.setDateEnd(dateEnd);
                    //maybe reset all to false for returning?
                    mutableData.setValue(bt);
                    return Transaction.success(mutableData);
                }
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                //send notification
            }
        });
    }

}
