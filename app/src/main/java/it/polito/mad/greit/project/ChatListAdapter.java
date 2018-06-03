package it.polito.mad.greit.project;


import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static android.support.v7.util.SortedList.INVALID_POSITION;

public class ChatListAdapter extends RecyclerView.Adapter {
    private Context mContext;
    private SortedList<Chat> chatList;
    private HashMap<String, Chat> chatMap;
    private FirebaseUser user;

    public ChatListAdapter(Context context) {
        mContext = context;
        this.chatMap = new HashMap<>();
        this.chatList = new SortedList<>(Chat.class, new SortedList.Callback<Chat>(){

            @Override
            public void onInserted(int position, int count) {
                notifyItemRangeInserted(position, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                notifyItemRangeRemoved(position, count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                notifyItemMoved(fromPosition, toPosition);
            }

            @Override
            public int compare(Chat o1, Chat o2) {
                return - o1.compareTo(o2);
            }

            @Override
            public void onChanged(int position, int count) {
                notifyItemRangeChanged(position, count);
            }

            @Override
            public boolean areContentsTheSame(Chat oldItem, Chat newItem) {
                if(oldItem.getChatID()==newItem.getChatID())
                    return oldItem.getTimestamp() == newItem.getTimestamp();
                else return false;
            }

            @Override
            public boolean areItemsTheSame(Chat item1, Chat item2) {
                return item1.getChatID().equals(item2.getChatID());
            }
        });
        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    //conversation helpers
    public void addAll(List<Chat> chats) {
        chatList.beginBatchedUpdates();
        Chat old; int indexOld;
        for (int i = 0; i < chats.size(); i++) {
            if(chatMap.containsKey(chats.get(i).getChatID())){
                old = chatMap.get(chats.get(i).getChatID());
                indexOld = chatList.indexOf(old);
                chatList.updateItemAt(indexOld, chats.get(i));
                chatMap.put(chats.get(i).getChatID(), chats.get(i));
            }
            else {
                chatList.add(chats.get(i));
                chatMap.put(chats.get(i).getChatID(), chats.get(i));
            }
        }
        chatList.endBatchedUpdates();
    }

    public void removeAt(int position){
        chatList.removeItemAt(position);
    }

    public Chat get(int position) {
        return chatList.get(position);
    }

    public void clear() {
        chatList.beginBatchedUpdates();
        //remove items at end, to avoid unnecessary array shifting
        while (chatList.size() > 0) {
            chatList.removeItemAt(chatList.size() - 1);
        }
        chatList.endBatchedUpdates();
    }

    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatItemHolder(view);

    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        //to invert order of showning
        int iPos = position;

        Chat chat = (Chat) chatList.get(iPos);

        ((ChatItemHolder) holder).bind(chat, mContext);
    }


    @Override
    public int getItemCount() {
        return chatList.size();
    }

    private class ChatItemHolder extends RecyclerView.ViewHolder {
        StorageReference sr;
        ImageView imageProfile;
        TextView usernameText, titleText, lastText, dateText;
        Button unreadBut;

        ChatItemHolder(View itemView) {
            super(itemView);
            imageProfile = (ImageView) itemView.findViewById(R.id.inbox_chat_profile_pic);
            usernameText = (TextView) itemView.findViewById(R.id.inbox_chat_user);
            titleText = (TextView) itemView.findViewById(R.id.inbox_chat_title);
            lastText = (TextView) itemView.findViewById(R.id.inbox_chat_last);
            unreadBut = (Button) itemView.findViewById(R.id.inbox_chat_unread);
            dateText = (TextView) itemView.findViewById(R.id.inbox_chat_date);
            

        }

        void bind(Chat chat, Context ctx) {
            sr = FirebaseStorage.getInstance().getReference().child("profile_pictures/" + chat.getUserID() + ".jpg");
    
            sr.getDownloadUrl().addOnSuccessListener(uri -> {
                if (ctx != null) {
                    Glide.with(ctx.getApplicationContext())
                        .load(uri)
                        .into(imageProfile);
                }
                   
            }).addOnFailureListener( e -> {
                if (ctx != null) {
                    Glide.with(ctx.getApplicationContext())
                        .load("")
                        .apply(new RequestOptions()
                            .error(R.mipmap.ic_launcher_round)
                            .fitCenter())
                        .into(imageProfile);
                }
            });
            
            usernameText.setText(chat.getUsername());
            titleText.setText(chat.getBookTitle() + " - " + chat.getBookAuthor() );
            // Format the stored timestamp into a readable String using method.
            lastText.setText(chat.getLastMsg());
            dateText.setText(ChatListAdapter.formatDateTime(chat.getTimestamp()));
            
            

            if(chat.getUnreadCount() == 0){
                unreadBut.setVisibility(View.INVISIBLE);
            }
            else {
                unreadBut.setVisibility(View.VISIBLE);
                unreadBut.setText(Long.toString(chat.getUnreadCount()));
            }

        }
    }

    private static String formatDateTime(long time){
        Date date = new Date(time*1000L);
        SimpleDateFormat dt1 = new SimpleDateFormat("dd-MM hh:mm");

        return dt1.format(date);
    }
}