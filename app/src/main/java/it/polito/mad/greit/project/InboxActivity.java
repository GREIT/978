package it.polito.mad.greit.project;

import android.content.Intent;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class InboxActivity extends AppCompatActivity {

    ArrayList<Chat> chats;
    ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference dbref = db.getReference("USER_CHATS").child(fbu.getUid());
        chats = new ArrayList<>();
        lv = findViewById(R.id.list_inbox);
        ArrayAdapter<Chat> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, chats);
        lv.setAdapter(adapter);

        dbref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                chats.clear();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    chats.add(ds.getValue(Chat.class));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Chat c = chats.get(i);
                openChat(c);
            }
        });

        /*Button bb = findViewById(R.id.button);
        bb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //start new chat
                String chatID = db.getReference("USER_MESSAGES").push().getKey();
                Chat chat = new Chat();
                chat.setBookID("1");
                chat.setBookTitle("Book");
                chat.setUserID("5Mj8qcWvKWOjBE1QHhOtdCyNm3S2");
                chat.setUsername("fcdl2");
                chat.setLastMsg("");
                chat.setUnreadCount(0);
                chat.setChatID(chatID);

                DatabaseReference dr = db.getReference("USER_CHATS").child(fbu.getUid());

                String key = dr.push().getKey();
                dr.child(key).setValue(chat);

                dr = db.getReference("USER_CHATS").child(chat.getUserID());
                chat.setUserID(fbu.getUid());
                chat.setUsername(fbu.getDisplayName());

                key = dr.push().getKey();
                dr.child(key).setValue(chat);

                openChat(c);
            }
        });*/


    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(InboxActivity.this,MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void openChat(Chat chat){
        Intent intent = new Intent(InboxActivity.this,ChatActivity.class);
        intent.putExtra("chatid",chat.getChatID());
        intent.putExtra("ownerid",chat.getUserID());
        startActivity(intent);
    }
}
