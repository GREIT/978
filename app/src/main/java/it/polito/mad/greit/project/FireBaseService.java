package it.polito.mad.greit.project;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaExtractor;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class FireBaseService extends Service{

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private NotificationCompat.Builder builder;
    private HashMap<String,String> messages;
    private NotificationManagerCompat notificationManagerCompat;

    private final class ServiceHandler extends Handler {
        boolean started = false;
        public ServiceHandler(Looper looper) { super(looper); }
        @Override
        public void handleMessage(Message msg) {
            while (true) {
                synchronized (this) {
                    try {
                        if(!started){
                            startlistening();
                            started = true;
                        }
                        Log.d("FIREBASESERVICE", "Still Alive");
                        Thread.sleep(30000);
                    } catch (Exception e) { stopSelf(msg.arg1); }
                }
            }
        }
    }

    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        // code to execute when the service is first created

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Project";
            String description = "Channel for notifications from Project";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("project", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        messages = new HashMap<>();

        notificationManagerCompat = NotificationManagerCompat.from(FireBaseService.this);

        builder = new NotificationCompat.Builder(FireBaseService.this, "project")
                .setSmallIcon(R.drawable.ic_book_blue_700_48dp)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentTitle(getResources().getString(R.string.new_messages))
                .setContentText("");

        Log.d("FIREBASESERVICE", "onCreate: Created service");

        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Takes the HandlerThread Looper and pass it to ServiceHandler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startid)
    {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startid;
        mServiceHandler.sendMessage(msg);

        Log.d("FIREBASESERVICE", "onStartCommand: Start Command");

        return START_STICKY;
    }

    public void onDestroy() {
        Log.d("FIREBASESERVICE", "onDestroy: service closed");
    }

    public void startlistening(){
        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference dbref = db.getReference("USER_CHATS").child(fbu.getUid());
        dbref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Chat c = dataSnapshot.getValue(Chat.class);

                //String msg = getResources().getString(R.string.new_request,c.getUsername(),c.getBookTitle());
                String msg = getResources().getString(R.string.new_request,c.getUsername(),c.getBookTitle());
                String key = c.getUsername() + "-" + c.getBookTitle();

                if(c.getIsnew()){

                    messages.remove(key);
                    messages.put(key,msg);

                    PendingIntent pendingIntent=null;
                    if(messages.size() == 1){
                        Intent intent = new Intent(FireBaseService.this, ChatActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("chat", c);
                        pendingIntent = PendingIntent.getActivity(FireBaseService.this, 0, intent, 0);
                    }
                    else if(messages.size() >= 2){
                        Intent intent = new Intent(FireBaseService.this, InboxActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        pendingIntent = PendingIntent.getActivity(FireBaseService.this, 0, intent, 0);
                    }

                    builder.setContentText(setupString())
                            .setContentIntent(pendingIntent)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(setupString()));
                    notificationManagerCompat.notify(0,builder.build());
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Chat c = dataSnapshot.getValue(Chat.class);

                //String msg = c.getBookTitle() + "--" + c.getUsername();
                String msg = getResources().getString(R.string.incoming,c.getUnreadCount(),c.getUsername(),c.getBookTitle());
                String key = c.getUsername() + "-" + c.getBookTitle();

                if(c.getUnreadCount() != 0){

                    messages.remove(key);
                    messages.put(key,msg);

                    PendingIntent pendingIntent=null;
                    if(messages.size() == 1){
                        Intent intent = new Intent(FireBaseService.this, ChatActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("chat", c);
                        pendingIntent = PendingIntent.getActivity(FireBaseService.this, 0, intent, 0);
                    }
                    else if(messages.size() >= 2){
                        Intent intent = new Intent(FireBaseService.this, InboxActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        pendingIntent = PendingIntent.getActivity(FireBaseService.this, 0, intent, 0);
                    }

                    builder.setContentText(setupString())
                            .setContentIntent(pendingIntent)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(setupString()));
                    notificationManagerCompat.notify(0,builder.build());

                }
                else{
                    messages.remove(key);
                    builder.setContentText(setupString())
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(setupString()));
                    if(messages.size() == 0){
                        notificationManagerCompat.cancel(0);
                    }
                    else{
                        notificationManagerCompat.notify(0,builder.build());
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private static String formatDateTime(long time){
        Date date = new Date(time*1000L);
        SimpleDateFormat dt1 = new SimpleDateFormat("dd-MM hh:mm");
        return dt1.format(date);
    }

    private String setupString(){
        String res="";
        for(String msg : messages.values()){
            res = msg + "\n" + res;
        }
        return res;
    }
}
