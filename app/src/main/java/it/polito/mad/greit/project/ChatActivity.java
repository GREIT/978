package it.polito.mad.greit.project;

import android.content.Intent;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        final Chat chat = (Chat) getIntent().getSerializableExtra("chat");
        final String chatID = chat.getChatID();
        final String ownerID = chat.getUserID();

        if(getIntent().getBooleanExtra("new", false)){
            //we must send default message
            sendFirstMessage((Message)getIntent().getSerializableExtra("msg"), chat);
        }

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
                        FirebaseAuth.getInstance().getCurrentUser().getDisplayName()
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
                        c.setLastMsg(msg);
                        c.setTimestamp(time);
                        mutableData.setValue(c);
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

    private void sendFirstMessage(Message msg, Chat chat){
        DatabaseReference fbd = FirebaseDatabase.getInstance().getReference("USER_MESSAGES").child(chat.getChatID());
        String key = fbd.push().getKey();
        fbd.child(key).setValue(msg);
    }
}