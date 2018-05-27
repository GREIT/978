package it.polito.mad.greit.project;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
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
import android.widget.CompoundButton;
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

    /*
    Ci sono diversi problemi...
        1. Non mi piace che io possa premere se il libro é prestato ad altri
        2. Non mi piace non sapere chi é il proprietario del libro
        3. Non mi piace che io non sappia in che stato é la transazione
        4. Non mi piace che io non sappia cosa ha fatto l'altro utente
     */

    Chat chat = null;
    ImageView cardBookIcon;
    boolean isClickable, haveLoaded;
    //boolean to signal the alert output
    private boolean switchAlertConfirmed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isClickable = false;
        haveLoaded = false;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chat = (Chat) getIntent().getSerializableExtra("chat");
        if(getIntent().hasExtra("new") && getIntent().getBooleanExtra("new",false)){
            sendDefaultMsg(chat);
        }
        final String chatID = chat.getChatID();
        final String ownerID = chat.getUserID();

        //Toolbar
        Toolbar t;
        t = findViewById(R.id.chat_toolbar);
        t.setTitle("@"+ chat.getUsername());
        setSupportActionBar(t);
        t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        t.setNavigationOnClickListener(view -> onBackPressed());

        //Book Bar
        TextView cardBookTitle = findViewById(R.id.chat_card_book_title);
        TextView cardBookAuthor = findViewById(R.id.chat_card_book_author);
        Switch cardBookSwitch = findViewById(R.id.chat_card_book_switch);
        cardBookIcon = findViewById(R.id.chat_card_book_icon);

        cardBookTitle.setText(chat.getBookTitle());
        cardBookAuthor.setText(chat.getBookAuthor());

        cardBookSwitch.setOnClickListener( (v) ->  createAlert( (Switch) v));

        //Do the query to see if the book is borrowed or not.
        //At the end of the query set haveLoaded to True and isClickable to false if borrowed, otherwise not.
        //DatabaseReference dbForBorrowed = FirebaseDatabase.getInstance().getReference("USER_MESSAGES").child(chatID);


        //Messages
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
                                Context.MODE_PRIVATE).getString("username",null),
                                c.getChatID(),c.getUserID(),"message");
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

    private void sendDefaultMsg(Chat chat){

        String msg = getResources().getString(R.string.default_msg,chat.getUsername(),chat.getBookTitle());
        Context ctx = this;
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
                Chat.sendnotification(Profile.getCurrentUsername(ctx) ,c.getChatID(),c.getUserID(),"newRequest");
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


    private void createAlert(Switch s){
        //I want the state as the one where I want to go
            //ST state true when is locking, false when is opening
        final boolean state = s.isChecked();
        cardBookIcon.setActivated(state);

        String title = (state)
                ?getResources().getString(R.string.alert_title_chat_lock)
                :getResources().getString(R.string.alert_title_chat_unlock);

        String message = (state)
                ?getResources().getString(R.string.alert_message_chat_lock)
                :getResources().getString(R.string.alert_message_chat_unlock);

        int icon = (state) ? R.drawable.ic_lock_outline_white_24dp
                : R.drawable.ic_lock_open_white_24dp;

        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (d,w) ->
                        BookTransaction.startTransaction(chat, Profile.getCurrentUsername(this)))
                .setNegativeButton(android.R.string.cancel, (d,w) -> {
                        s.setChecked(!state);
                        cardBookIcon.setActivated(!state);
                    }
                )
                .setIcon(icon)
                .show();

    }


}