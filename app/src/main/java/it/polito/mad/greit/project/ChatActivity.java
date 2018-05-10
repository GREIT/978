package it.polito.mad.greit.project;

import android.content.Intent;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        final String chatID = getIntent().getStringExtra("chatid");
        final String ownerID = getIntent().getStringExtra("ownerid");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference fbd = FirebaseDatabase.getInstance().getReference("USER_MESSAGES").child(chatID);

        ArrayList<Message> messages = new ArrayList<>();
        RecyclerView rv = findViewById(R.id.chat_message_list);
        MessageListAdapter mla = new MessageListAdapter(ChatActivity.this,messages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(mla);

        fbd.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                messages.clear();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    messages.add(ds.getValue(Message.class));
                    mla.notifyDataSetChanged();
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
                Message tosend = new Message(System.currentTimeMillis()/1000L,
                        FirebaseAuth.getInstance().getCurrentUser().getUid(),
                        FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),msg);

                DatabaseReference fbd = FirebaseDatabase.getInstance().getReference("USER_MESSAGES").child(chatID);
                String key = fbd.push().getKey();
                fbd.child(key).setValue(tosend);

                DatabaseReference sender_chat = FirebaseDatabase.getInstance()
                        .getReference("USER_CHATS").child(user.getUid()).child(chatID);
                sender_chat.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        try {
                            Chat c = dataSnapshot.getValue(Chat.class);
                            c.setLastMsg(msg);
                            sender_chat.setValue(c);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                DatabaseReference receiver_chat = FirebaseDatabase.getInstance()
                        .getReference("USER_CHATS").child(ownerID).child(chatID);
                receiver_chat.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        try {
                            Chat c = dataSnapshot.getValue(Chat.class);
                            c.setLastMsg(msg);
                            c.setUnreadCount(c.getUnreadCount()+1);
                            receiver_chat.setValue(c);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

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

}



/*
@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //String chatID = getIntent().getStringExtra("chatID");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference fbd = FirebaseDatabase.getInstance().getReference("USERS_MESSAGES").child("tempID");
        ArrayList<Message> messages = new ArrayList<>();
        RecyclerView rv = findViewById(R.id.chat_message_list);
        MessageListAdapter mla = new MessageListAdapter(ChatActivity.this,messages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(mla);

        fbd.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    messages.add(ds.getValue(Message.class));
                    Log.d("TAGTAGTAG", "onDataChange: " + ds.getValue().toString());
                    mla.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        ImageButton ib = findViewById(R.id.chat_send_button);
        EditText et = findViewById(R.id.chat_input);
        ib.setOnClickListener(view -> send(et.getText().toString()));

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ChatActivity.this,MainActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void send(String msg){
        Message tosend = new Message(System.currentTimeMillis()/1000L,
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),msg);

        DatabaseReference fbd = FirebaseDatabase.getInstance().getReference("USERS_MESSAGES").child("tempID");
        String key = fbd.push().getKey();
        fbd.child(key).setValue(tosend);


    }
 */