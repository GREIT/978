package it.polito.mad.greit.project;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // ...

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d("FirebaseMessagingServic", "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d("FirebaseMessagingServic", "Message data payload: " + remoteMessage.getData());
            String from = remoteMessage.getData().get("username");
            String chatID = remoteMessage.getData().get("chatID");
            String type = remoteMessage.getData().get("type");
            FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseDatabase db = FirebaseDatabase.getInstance();
            DatabaseReference dbref = db.getReference("USER_CHATS").child(fbu.getUid()).child(chatID);
            dbref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Chat c = dataSnapshot.getValue(Chat.class);
                    //sendNotification("Received " + c.getUnreadCount() + " new messages from " + from
                     //       + " for the book " + c.getBookTitle(), );
                    sendNotification(from,c,type);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d("FirebaseMessagingServic", "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    private void sendNotification(String from,Chat c,String type) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("chat",c);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, from.hashCode(), intent,
                PendingIntent.FLAG_ONE_SHOT);

        //String messageBody = "Received " + c.getUnreadCount() + " new messages from " + from
          //     + " for the book " + c.getBookTitle();
        String messageBody = null;
        if(type.equals("message")){
            messageBody = getResources().getString(R.string.incoming,from,c.getBookTitle());
        }
        else if(type.equals("transaction")){
            messageBody = getResources().getString(R.string.lock_event,from,c.getBookTitle());
        }
        else{
            messageBody = getResources().getString(R.string.new_request,from,c.getBookTitle());
        }

        String channelId = "project";
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_book_blue_700_48dp)
                        .setContentTitle("Project")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(from.hashCode(), notificationBuilder.build());
        Log.d("FirebaseMessagingServic", "sendNotification: ");
    }
}
