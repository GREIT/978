package it.polito.mad.greit.project;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
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

import org.json.JSONArray;
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
    private int unreadCount;
    private long timestamp;
    private String bookAuthor;
    private boolean mine; //to detect if book is mine or not
    private boolean deleted;

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

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

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public void setBookAuthor(String bookAuthor) {
        this.bookAuthor = bookAuthor;
    }

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

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
        res.setBookAuthor(c.getBookAuthor());
        res.setMine(c.isMine());
        return res;
    }

    public static void openchat(Context context, SharedBook sb){
        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference dbref = db.getReference("USER_CHATS").child(fbu.getUid());
        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Boolean chat_exists = false;
                try {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        Chat c = ds.getValue(Chat.class);
                        if(c.getBookID().equals(sb.getKey()) && c.getUserID().equals(sb.getOwnerUid())){
                            //chat already present
                            Intent intent = new Intent(context,ChatActivity.class);
                            intent.putExtra("chat",c);
                            context.startActivity(intent);
                            chat_exists = true;
                        }
                    }

                    if(!chat_exists){
                        Chat c = new Chat();
                        c.setUsername(sb.getOwnerUsername());
                        c.setBookID(sb.getKey());
                        c.setUserID(sb.getOwnerUid());
                        c.setBookTitle(sb.getTitle());
                        c.setBookAuthor(sb.getAuthors().values().iterator().next());
                        c.setMine(false);

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
                        intent.putExtra("new",true);

                        DatabaseReference ref_second_user = db.getReference("USER_CHATS").child(sb.getOwnerUid());
                        c.setUserID(fbu.getUid());
                        c.setMine(true);
                        c.setUsername(Profile.getCurrentUsername(context));
                        ref_second_user.child(chatid).setValue(c);
                        context.startActivity(intent);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static void sendnotification(String sender_username,String chatId,String receiver_Uid,String type){
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference dbref = db.getReference("TOKENS").child(receiver_Uid);
        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String token = dataSnapshot.getValue(String.class);
                new PostNotify().execute(token,sender_username,chatId,type);
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

                JSONObject data = new JSONObject();
                data.put("username", strings[1]);
                data.put("chatID", strings[2]);
                data.put("type", strings[3]);

                jsonObject.put("data",data);

                String tosend = jsonObject.toString();
                Log.d("NOTIFICATION", "doInBackground: " + tosend);

                conn.setFixedLengthStreamingMode(tosend.getBytes().length);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(tosend);
                writer.flush();
                writer.close();
                os.close();

                Log.d("NOTIFICATION", "doInBackground: sent " + conn.getResponseCode());
                conn.disconnect();

            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }

}
