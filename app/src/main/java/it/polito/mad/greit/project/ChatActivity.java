package it.polito.mad.greit.project;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
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

import static it.polito.mad.greit.project.Constants.DB_SHARED;
import static it.polito.mad.greit.project.Constants.DB_USER_CHAT;
import static it.polito.mad.greit.project.Constants.DB_USER_MESSAGES;


public class ChatActivity extends AppCompatActivity {

    /*
    Ci sono diversi problemi...
        1. Non mi piace che io possa premere se il libro é prestato ad altri
     */
    private static final String SYSTEM = "system";

    private final int STATE_FREE = 0;
    private final int STATE_BORROWTOUSER = 1;
    private final int STATE_BORROWTOOTHER = 2;
    private final int STATE_UNKNOWN = -1;

   private BookTransaction bt = null;
   private SharedBook sb = null;
   private Chat chat = null;
   private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    private int state = STATE_UNKNOWN;
    ImageButton cardBookButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Instantiate the view to owner view or receiver view
        setContentView(R.layout.activity_chat);

        //store the chat passed by inbox and send the new message if the chat is new
        chat = (Chat) getIntent().getSerializableExtra("chat");
        if(getIntent().hasExtra("new") && getIntent().getBooleanExtra("new",false)){
            sendDefaultMsg(chat);
        }

        //Toolbar setup
        toolbarSetup();

        //Book Bar setup
        bookBarSetup();

        //loads data for transaction and sharedBook
        loadSharedBook();
        loadTransaction();

        //store variable to get information from DB
        final String chatID = chat.getChatID();
        final String ownerID = chat.getUserID();

        //Message adapter instantiation
        messageAdapterSetup(chatID);

        //Edit Text for Message Setup
        editTextMessageSetup(chatID, ownerID);

    }

    @Override
    public void onBackPressed() {
        if(getIntent().hasExtra("notification") && getIntent().getBooleanExtra("notification", true)) {
            Intent intent = new Intent(ChatActivity.this, MainActivity.class);
            startActivity(intent);
        }
        super.onBackPressed();
    }

    public void sendSystemMessage(String msg){
        Message toSend = new Message(chat.getTimestamp(),
                SYSTEM, SYSTEM ,msg);
        DatabaseReference fbd = FirebaseDatabase.getInstance().getReference("USER_MESSAGES").child(chat.getChatID());
        String key = fbd.push().getKey();
        fbd.child(key).setValue(toSend);
    }

    private void zeroUnread(FirebaseUser user,String chatID){
        //put to 0 unread count
        DatabaseReference user_chat = FirebaseDatabase.getInstance()
                .getReference(DB_USER_CHAT).child(user.getUid()).child(chatID);

        user_chat.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Chat c = mutableData.getValue(Chat.class);
                if(c!=null) c.setUnreadCount(0);
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
        Message toSend = new Message(chat.getTimestamp(),
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                Profile.getCurrentUsername(this)
                ,msg);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference fbd = FirebaseDatabase.getInstance().getReference("USER_MESSAGES").child(chat.getChatID());
        String key = fbd.push().getKey();
        fbd.child(key).setValue(toSend);

        DatabaseReference sender_chat = FirebaseDatabase.getInstance()
                .getReference(DB_USER_CHAT).child(user.getUid()).child(chat.getChatID());

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
                .getReference(DB_USER_CHAT).child(chat.getUserID()).child(chat.getChatID());

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

    private void createAlertBorrow(ImageButton s){
        //I want the state as the one where I want to go
            //ST state true when is locking, false when is opening
        boolean toBorrow = (state==STATE_FREE) ;

        int newState = (toBorrow)?STATE_BORROWTOUSER:STATE_FREE;

        String title = (toBorrow)
                ?getResources().getString(R.string.alert_title_chat_lock)
                :getResources().getString(R.string.alert_title_chat_unlock);

        String message = (toBorrow)
                ?getResources().getString(R.string.alert_message_chat_lock)
                :getResources().getString(R.string.alert_message_chat_unlock);

        int icon = (toBorrow) ? R.drawable.ic_lock_outline_white_24dp
                : R.drawable.ic_lock_open_white_24dp;

        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (d,w) -> {
                       startTransaction(toBorrow);
                       setState(newState);
                })
                .setNegativeButton(android.R.string.cancel, (d,w) ->
                    d.dismiss()
                )
                .setIcon(icon)
                .show();

    }

    private void createDialogMessage(){
        String title;
        String message;
        Drawable icon  = getResources().getDrawable(R.drawable.ic_announcement_white_24dp);
        int color = 0;
        if( state == STATE_FREE) {
            title = getResources().getString(R.string.free);
            message = getResources().getString(R.string.dialog_free_message);
            color = getResources().getColor(R.color.colorGreen);
        } else if (state == STATE_BORROWTOUSER){
            title = getResources().getString(R.string.borrowed_to_you);
            message = getResources().getString(R.string.dialog_borrowed_to_you);
            color = getResources().getColor(R.color.colorGold);
        } else {
            title = getResources().getString(R.string.borrowed_to_other);
            message = getResources().getString(R.string.dialog_borrowed_to_other);
            color = getResources().getColor(R.color.colorRed);
        }
        icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (d,w) -> {
                    d.dismiss();
                })
                .setIcon(icon)
                .show();
    }

    private void createDialogOther(String borrowTo){
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(R.string.book_already_borrowed_title)
                .setMessage(getResources().getString(R.string.book_already_borrowed_message, borrowTo))
                .setPositiveButton(android.R.string.yes, (d,w) -> {
                    d.dismiss();
                })
                .setIcon(R.drawable.ic_announcement_white_24dp)
                .show();
    }

    private void loadTransaction(){
        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TRANSACTIONS").child(chat.getChatID());
        dbref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() == null){
                    bt = new BookTransaction();
                    bt.setBookId(chat.getBookID());
                    bt.setBookTitle(chat.getBookTitle());
                    bt.setDateEnd(0);
                    if(chat.isMine()) {
                        bt.setOwnerUid(fbu.getUid());
                        bt.setOwnerUsername(Profile.getCurrentUsername(ChatActivity.this));
                        bt.setReceiverUid(chat.getUserID());
                        bt.setReceiverUsername(chat.getUsername());
                    }
                    else {
                        bt.setOwnerUid(chat.getUserID());
                        bt.setOwnerUsername(chat.getUsername());
                        bt.setReceiverUid(fbu.getUid());
                        bt.setReceiverUsername(Profile.getCurrentUsername(ChatActivity.this));
                    }
                    bt.setDateStart(0);
                    bt.setChatId(chat.getChatID());
                    bt.setFree(true);
                }
                else{
                    bt = dataSnapshot.getValue(BookTransaction.class);
                }
                //call it here too to be sure that at least one of the two (see loadShared) works
                buttonSetup();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void loadSharedBook(){
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference(DB_SHARED)
                .child(chat.getBookID());

        dbref.addValueEventListener(new ValueEventListener() { //listener for the shared book
            //it's automatically updated when it changes
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                sb = dataSnapshot.getValue(SharedBook.class);
                //call it here too to be sure that at least one of the two (see loadTrans) works
                buttonSetup();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void startTransaction(Boolean op){
        //op is true if book have to be locked, false otherwise
        Log.d("DEBUGTRANSACTION", "startTransaction: " +  bt.getBookId());
        if(op){
            Log.d("DEBUGTRANSACTION", "startTransaction: enter lock");
            bt.lock_book();
            sendSystemMessage( getResources().getString(R.string.system_message_accepted,
                    Profile.getCurrentUsername(this)));
        }
        else{
            Log.d("DEBUGTRANSACTION", "startTransaction: enter unlock");
            bt.unlock_book();
            sendSystemMessage( getResources().getString(R.string.system_message_closed,
                    Profile.getCurrentUsername(this)));
        }
    }

    private boolean isOwner(){
        return chat.isMine();
    }

    private void toolbarSetup(){
        Toolbar t;
        t = findViewById(R.id.chat_toolbar);
        t.setTitle("@"+ chat.getUsername());
        setSupportActionBar(t);
        t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        t.setNavigationOnClickListener(view -> onBackPressed());
    }

    private void bookBarSetup(){
        CardView cardView = findViewById(R.id.chat_card_book_layout);

        if(isOwner())
            cardView.addView(getLayoutInflater().inflate(R.layout.item_book_card_owner, null));
        else
            cardView.addView(getLayoutInflater().inflate(R.layout.item_book_card_receiver, null));

        TextView cardBookTitle = findViewById(R.id.chat_card_book_title);
        TextView cardBookAuthor = findViewById(R.id.chat_card_book_author);
        cardBookButton = findViewById(R.id.chat_card_book_button);

        cardBookTitle.setText(chat.getBookTitle());
        cardBookAuthor.setText(chat.getBookAuthor());

        cardBookButton.setEnabled(false);

    }

    private void buttonSetup(){
        int newState = -1;
        if(bt!= null && sb!=null) {
            //we must be sure to have initialized all
            if (isOwner()) {
                //we have the button to set enabled and show the right state
                if(!sb.getShared()){
                    cardBookButton.setOnClickListener((v) -> createAlertBorrow((ImageButton) v));
                    newState = STATE_FREE;
                }
                else if(sb.getBorrowToUid().equals(bt.getReceiverUid())) {
                    cardBookButton.setOnClickListener((v) -> createAlertBorrow((ImageButton) v));
                    newState = STATE_BORROWTOUSER;
                }
                else {
                    cardBookButton.setOnClickListener((v) -> createDialogOther(sb.getBorrowToUsername()));
                    newState  = STATE_BORROWTOOTHER;
                }
            } else {
                cardBookButton.setOnClickListener((v)-> createDialogMessage());
                //we have to show the right color in the circular button
                if(!sb.getShared()){
                    newState = STATE_FREE;
                }
                else if(sb.getBorrowToUid().equals(bt.getReceiverUid())) {
                    newState = STATE_BORROWTOUSER;
                }
                else {
                    newState  = STATE_BORROWTOOTHER;
                }
            }
            setState(newState);
        }
    }

    private void setState(int newState) {
        this.state = newState;
        setButtonState();
    }

    private void setButtonState(){
        ImageButton b = cardBookButton;
        b.setEnabled(true);
        int color;

        switch (state){
            case STATE_FREE:
                color = getResources().getColor(R.color.colorGreen);
                break;
            case STATE_BORROWTOUSER:
                color = getResources().getColor(R.color.colorGold);
                break;
            case STATE_BORROWTOOTHER:
                color = getResources().getColor(R.color.colorRed);
                break;
            default:
                color = getResources().getColor(R.color.colorGrey);
        }
        if(isOwner()){
            b.setBackgroundColor(color);
        }
        else {
            Drawable roundDrawable = getResources().getDrawable(R.drawable.circular_button);
            roundDrawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

            b.setBackground(roundDrawable);
        }
    }

    //region Messaging Functions
    private void messageAdapterSetup(String chatID){
        DatabaseReference fbd = FirebaseDatabase.getInstance().getReference(DB_USER_MESSAGES).child(chatID);

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
                    zeroUnread(user,chatID);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void editTextMessageSetup(String chatID, String ownerID) {
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
                        Profile.getCurrentUsername(ChatActivity.this),msg);

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
                        Chat.sendnotification(Profile.getCurrentUsername(ChatActivity.this),
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
    //endregion



}