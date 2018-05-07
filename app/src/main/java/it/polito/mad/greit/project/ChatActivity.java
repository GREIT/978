package it.polito.mad.greit.project;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

        //String chatID = getIntent().getStringExtra("chatID");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference fbd = FirebaseDatabase.getInstance().getReference("USERS_MESSAGES").child("tempID");
        ArrayList<Message> messages = new ArrayList<>();
        MessageListAdapter mla = new MessageListAdapter(ChatActivity.this,messages);

        fbd.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
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
        fbd.setValue(tosend);


    }

}