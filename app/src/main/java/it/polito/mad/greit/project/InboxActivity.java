package it.polito.mad.greit.project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.renderscript.Sampler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.util.SortedList;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class InboxActivity extends AppCompatActivity {

    RecyclerView rv;
    ChatListAdapter adapter;
    ValueEventListener velInbox;
    DatabaseReference dbref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        Toolbar t;
        t = findViewById(R.id.inbox_toolbar);
        t.setTitle(R.string.inbox);
        setSupportActionBar(t);
        t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        t.setNavigationOnClickListener(view -> onBackPressed());

        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        dbref = db.getReference("USER_CHATS").child(fbu.getUid());

        rv = findViewById(R.id.list_inbox);

        adapter = new ChatListAdapter(this);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(linearLayoutManager);

        rv.setAdapter(adapter);


        SwipeToDeleteCallback swipeHandler = new SwipeToDeleteCallback(this) {
            @Override
            public void onSwiped(RecyclerView.ViewHolder vh, int direction) {
                Chat c = adapter.get(vh.getAdapterPosition());
                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference(Constants.DB_TRANSACTIONS).child(c.getChatID());
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        BookTransaction transaction = dataSnapshot.getValue(BookTransaction.class);
                        if(transaction!=null && !transaction.isFree()){
                            Toast.makeText(InboxActivity.this,
                                    R.string.cannot_delete, Toast.LENGTH_SHORT).show();
                            adapter.notifyDataSetChanged();
                        }
                        else {
                            adapter.removeAt(vh.getAdapterPosition());
                            c.setDeleted(true);
                            deleteChat(c, fbu.getUid());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHandler);
        itemTouchHelper.attachToRecyclerView(rv);


        velInbox = dbref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<Chat> chats = new ArrayList<>();
                chats.clear();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    if(!ds.getValue(Chat.class).isDeleted())
                        chats.add(ds.getValue(Chat.class));
                }
                //adapter.clear();
                adapter.addAll(chats);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        ItemClickSupport.addTo(rv).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                Chat c = adapter.get(position);
                openChat(c);
            }
        });

    }

    @Override
    public void onBackPressed() {
        dbref.removeEventListener(velInbox);
        super.onBackPressed();
    }

    public void openChat(Chat chat){
        Intent intent = new Intent(InboxActivity.this,ChatActivity.class);
        intent.putExtra("chat",chat);
        startActivity(intent);
    }

    public void deleteChat(Chat c,String user){
        //user is the current session FireBaseUser
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        //db.getReference("USER_CHATS").child(user).child(c.getChatID()).removeValue();



        DatabaseReference dbref = db.getReference(Constants.DB_USER_CHAT).child(c.getUserID()).child(c.getChatID());
        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Chat other = (dataSnapshot!=null)?dataSnapshot.getValue(Chat.class):null;
                if(other == null){
                    //the other chat doesn't exist anymore, delete my chat
                    db.getReference(Constants.DB_USER_MESSAGES).child(c.getChatID()).removeValue();
                }
                else if(other.isDeleted()){
                    //other already tried to delete his chat, delete both
                    db.getReference(Constants.DB_USER_MESSAGES).child(c.getChatID()).removeValue(); //remove messages
                    db.getReference(Constants.DB_USER_CHAT).child(user).child(c.getChatID()).removeValue(); //remove my chat
                    db.getReference(Constants.DB_USER_CHAT).child(c.getUserID()).child(c.getChatID()).removeValue(); //remove his chat
                }
                else {
                    //chat exist but not deleted. Add my delete information and return
                    db.getReference(Constants.DB_USER_CHAT).child(user).child(c.getChatID()).child("deleted").setValue(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
