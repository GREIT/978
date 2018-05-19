package it.polito.mad.greit.project;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Debug;
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

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

public class Chat implements Serializable, Comparable<Chat>{
    private String chatID;
    private String userID;
    private String username;
    private String bookID;
    private String bookTitle;
    private String lastMsg;
    private long unreadCount;
    private long timestamp;
    //private boolean isnew;

    public String getChatID() {
        return chatID;
    }

    public void setChatID(String chatID) {
        this.chatID = chatID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBookID() {
        return bookID;
    }

    public void setBookID(String bookID) {
        this.bookID = bookID;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getLastMsg() {
        return lastMsg;
    }

    public void setLastMsg(String lastMsg) {
        this.lastMsg = lastMsg;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /*public void setIsnew(Boolean b){
        this.isnew = b;
    }

    public Boolean getIsnew(){
        return this.isnew;
    }*/

    @Override
    public int compareTo(Chat o){
        return (int) (this.timestamp - o.timestamp);
    }

    @Override
    public boolean equals(Object o){
        if(o.getClass().equals(Chat.class)){
            return this.chatID.equals( ((Chat) o).chatID);
        }
        return false;
    }

    public static Chat copy(Chat c){
        Chat res = new Chat();
        res.setUsername(c.getUsername());
        res.setUnreadCount(c.getUnreadCount());
        res.setTimestamp(c.getTimestamp());
        res.setLastMsg(c.getLastMsg());
        res.setUserID(c.getUserID());
        res.setBookTitle(c.getBookTitle());
        res.setChatID(c.getChatID());
        res.setBookID(c.getBookID());
        //res.setIsnew(c.getIsnew());
        return res;
    }

    /*public static void openchat(Context context, SharedBook sb){
        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference dbref = db.getReference("USER_CHATS").child(fbu.getUid());
        dbref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Boolean chat_exists = false;
                try {
                    for (MutableData ds : mutableData.getChildren()) {
                        Chat c = ds.getValue(Chat.class);
                        if(c.getBookID().equals(sb.getKey()) && c.getUserID().equals(sb.getOwner())){
                            //chat already present
                            Intent intent = new Intent(context,ChatActivity.class);
                            intent.putExtra("chat",c);
                            context.startActivity(intent);
                            chat_exists = true;
                        }
                    }

                    if(!chat_exists){
                        Chat c = new Chat();
                        //getUsername(db,sb.getOwner(),c);
                        db.getReference("USERS").child(sb.getOwner()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Profile p = dataSnapshot.getValue(Profile.class);
                                //c.setIsnew(true);
                                c.setUsername(p.getUsername());
                                c.setBookID(sb.getKey());
                                c.setUserID(sb.getOwner());
                                c.setBookTitle(sb.getTitle());

                                DatabaseReference user_mess = db.getReference("USER_MESSAGES");
                                String chatid = user_mess.push().getKey();

                                //SEND pre formatted message at startup
                                //get msg parameters
                                long time = System.currentTimeMillis()/1000L;
                                String msg = context.getResources().getString(R.string.hello) + " " + p.getUsername()
                                        +"!. " + context.getResources().getString(R.string.can_i_have) + " " + sb.getTitle() + "?";
                                //send message parameters and put it into the chat newly created
                                Message tosend = new Message(time,
                                        FirebaseAuth.getInstance().getCurrentUser().getUid(),
                                        FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),msg);

                                c.setTimestamp(time);
                                c.setUnreadCount(1);
                                c.setLastMsg(msg);

                                //save the chatID and deploy the user_chat entry
                                c.setChatID(chatid);
                                //set the chat for the owner
                                dbref.child(chatid).setValue(c);

                                //prepare for making the other UserChat and create it
                                Intent intent = new Intent(context,ChatActivity.class);

                                intent.putExtra("chat",Chat.copy(c));
                                intent.putExtra("new", true);
                                intent.putExtra("msg", tosend);
                                //second user
                                db.getReference("USERS").child(fbu.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        Profile p = dataSnapshot.getValue(Profile.class);
                                        DatabaseReference ref_second_user = db.getReference("USER_CHATS").child(sb.getOwner());
                                        c.setUserID(fbu.getUid());
                                        c.setUsername(p.getUsername());
                                        //c.setIsnew(false);
                                        //c.setUsername(fbu.getDisplayName());
                                        //c.setUsername(getUsername(db,fbu.getUid(),c));
                                        ref_second_user.child(chatid).setValue(c);
                                        context.startActivity(intent);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });
    }*/

    public static void openchat(Context context, SharedBook sb){
        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference dbref = db.getReference("USER_CHATS").child(fbu.getUid());
        dbref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Boolean chat_exists = false;
                try {
                    for (MutableData ds : mutableData.getChildren()) {
                        Chat c = ds.getValue(Chat.class);
                        if(c.getBookID().equals(sb.getKey()) && c.getUserID().equals(sb.getOwner())){
                            //chat already present
                            Intent intent = new Intent(context,ChatActivity.class);
                            intent.putExtra("chat",c);
                            context.startActivity(intent);
                            chat_exists = true;
                        }
                    }

                    if(!chat_exists){
                        Chat c = new Chat();
                        c.setUsername(sb.getUsername());
                        c.setBookID(sb.getKey());
                        c.setUserID(sb.getOwner());
                        c.setBookTitle(sb.getTitle());

                        //unique id for chat
                        DatabaseReference user_mess = db.getReference("USER_MESSAGES");
                        String chatid = user_mess.push().getKey();

                        long time = System.currentTimeMillis()/1000L;
                        c.setTimestamp(time);
                        c.setUnreadCount(0);
                        c.setLastMsg("");
                        c.setChatID(chatid);

                        //set the chat for the current user
                        dbref.child(chatid).setValue(c);

                        //prepare for making the other UserChat and create it
                        Intent intent = new Intent(context,ChatActivity.class);
                        intent.putExtra("chat",Chat.copy(c));

                        DatabaseReference ref_second_user = db.getReference("USER_CHATS").child(sb.getOwner());
                        c.setUserID(fbu.getUid());
                        c.setUsername(context.getSharedPreferences("sharedpref",Context.MODE_PRIVATE).getString("username",null));
                        ref_second_user.child(chatid).setValue(c);
                        context.startActivity(intent);
                        }
                    }catch (Exception e){
                    e.printStackTrace();
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });
    }

    public void sendnotification(String sender_username){
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference dbref = db.getReference("USERS").child(this.getUserID());
        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Profile p = dataSnapshot.getValue(Profile.class);
                String token = p.getToken();
                new PostNotify().execute(token,sender_username,lastMsg,String.valueOf(unreadCount),bookTitle);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static class PostNotify extends AsyncTask<String,String,Void> {

        @Override
        public Void doInBackground(String... strings){
            try {
                URL url = new URL("https://fcm.googleapis.com/fcm/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type","application/json");
                conn.setRequestProperty("Authorization","key=" + Constants.SERVER_KEY);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("to",strings[0]);
                jsonObject.put("username",strings[1]);
                jsonObject.put("lastMsg",strings[2]);
                jsonObject.put("unreadCount",strings[3]);
                jsonObject.put("bookTitle",strings[4]);

                String tosend = jsonObject.toString();

                conn.setFixedLengthStreamingMode(tosend.getBytes().length);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(tosend);
                writer.flush();
                writer.close();
                os.close();

            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }

}
