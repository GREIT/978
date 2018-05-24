package it.polito.mad.greit.project;

import android.content.Context;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ChatActivity extends AppCompatActivity {

    Chat chat = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chat = (Chat) getIntent().getSerializableExtra("chat");
        if(getIntent().hasExtra("new") && getIntent().getBooleanExtra("new",false)){
            sendDefaultMsg(chat);
        }
        final String chatID = chat.getChatID();
        final String ownerID = chat.getUserID();

        Toolbar t;
        t = findViewById(R.id.chat_toolbar);
        t.setTitle(chat.getUsername());
        setSupportActionBar(t);
        t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        t.setNavigationOnClickListener(view -> onBackPressed());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference fbd = FirebaseDatabase.getInstance().getReference("USER_MESSAGES").child(chatID);

        ArrayList<Message> messages = new ArrayList<>();
        RecyclerView rv = findViewById(R.id.chat_message_list);
        MessageListAdapter mla = new MessageListAdapter(ChatActivity.this,messages);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        rv.setLayoutManager(linearLayoutManager);
        rv.setAdapter(mla);

        fbd.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                messages.clear();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    messages.add(ds.getValue(Message.class));
                    mla.notifyDataSetChanged();
                    rv.smoothScrollToPosition(mla.getItemCount() - 1);
                    zerounread(user,chatID);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        ImageButton ib = findViewById(R.id.chat_send_button);
        EditText et = findViewById(R.id.chat_input);
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = et.getText().toString();
                et.setText("");
                long time = System.currentTimeMillis()/1000L;
                if (msg.trim().isEmpty()) {
                    return;
                }
                Message tosend = new Message(time,
                        FirebaseAuth.getInstance().getCurrentUser().getUid(),
                        getSharedPreferences("sharedpref", Context.MODE_PRIVATE).getString("username",null)
                        ,msg);

                DatabaseReference fbd = FirebaseDatabase.getInstance().getReference("USER_MESSAGES").child(chatID);
                String key = fbd.push().getKey();
                fbd.child(key).setValue(tosend);

                DatabaseReference sender_chat = FirebaseDatabase.getInstance()
                        .getReference("USER_CHATS").child(user.getUid()).child(chatID);

                sender_chat.runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        Chat c = mutableData.getValue(Chat.class);
                        //c.setIsnew(false);
                        c.setLastMsg(msg);
                        c.setTimestamp(time);
                        mutableData.setValue(c);
                        Chat.sendnotification(getSharedPreferences("sharedpref",
                                Context.MODE_PRIVATE).getString("username",null),c,false);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                    }
                });

                DatabaseReference receiver_chat = FirebaseDatabase.getInstance()
                        .getReference("USER_CHATS").child(ownerID).child(chatID);

                receiver_chat.runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        Chat c = mutableData.getValue(Chat.class);
                        c.setLastMsg(msg);
                        c.setTimestamp(time);
                        c.setUnreadCount(c.getUnreadCount()+1);
                        mutableData.setValue(c);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                    }
                });

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (R.id.toolbar_chat_borrow_button == item.getItemId()) {
            //TODO add logic here
            BookTransaction.startTransaction(chat,getSharedPreferences("sharedpref",
                    Context.MODE_PRIVATE).getString("username",null));
            Toast.makeText( this , "Borrow Chat Pressed", Toast.LENGTH_SHORT).show();
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ChatActivity.this,InboxActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void zerounread(FirebaseUser user,String chatID){
        //put to 0 unread count
        DatabaseReference user_chat = FirebaseDatabase.getInstance()
                .getReference("USER_CHATS").child(user.getUid()).child(chatID);

        user_chat.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Chat c = mutableData.getValue(Chat.class);
                c.setUnreadCount(0);
                mutableData.setValue(c);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });
    }

    private void sendDefaultMsg(Chat chat){

        String msg = getResources().getString(R.string.default_msg,chat.getUsername(),chat.getBookTitle());

        Message tosend = new Message(chat.getTimestamp(),
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                getSharedPreferences("sharedpref", Context.MODE_PRIVATE).getString("username",null)
                ,msg);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference fbd = FirebaseDatabase.getInstance().getReference("USER_MESSAGES").child(chat.getChatID());
        String key = fbd.push().getKey();
        fbd.child(key).setValue(tosend);

        DatabaseReference sender_chat = FirebaseDatabase.getInstance()
                .getReference("USER_CHATS").child(user.getUid()).child(chat.getChatID());

        sender_chat.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Chat c = mutableData.getValue(Chat.class);
                c.setLastMsg(msg);
                c.setTimestamp(System.currentTimeMillis()/1000L);
                mutableData.setValue(c);
                Chat.sendnotification(getSharedPreferences("sharedpref",
                        Context.MODE_PRIVATE).getString("username",null),c,true);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });

        DatabaseReference receiver_chat = FirebaseDatabase.getInstance()
                .getReference("USER_CHATS").child(chat.getUserID()).child(chat.getChatID());

        receiver_chat.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Chat c = mutableData.getValue(Chat.class);
                c.setLastMsg(msg);
                c.setTimestamp(System.currentTimeMillis()/1000L);
                c.setUnreadCount(c.getUnreadCount()+1);
                mutableData.setValue(c);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });

    }
}