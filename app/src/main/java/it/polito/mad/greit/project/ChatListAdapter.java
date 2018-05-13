package it.polito.mad.greit.project;


import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter {
    private Context mContext;
    private SortedList<Chat> chatList;
    private FirebaseUser user;

    public ChatListAdapter(Context context) {
        mContext = context;
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
                return oldItem.getChatID().equals(newItem.getChatID());
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
        for (int i = 0; i < chats.size(); i++) {
            chatList.add(chats.get(i));
        }
        chatList.endBatchedUpdates();
    }

    //conversation helpers
    public void add(Chat chat) {
        chatList.add(chat);

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

        ((ChatItemHolder) holder).bind(chat);
    }


    @Override
    public int getItemCount() {
        return chatList.size();
    }

    private class ChatItemHolder extends RecyclerView.ViewHolder {
        TextView usernameText, titleText, lastText, dateText;
        Button unreadBut;

        ChatItemHolder(View itemView) {
            super(itemView);
            usernameText = (TextView) itemView.findViewById(R.id.inbox_chat_user);
            titleText = (TextView) itemView.findViewById(R.id.inbox_chat_title);
            lastText = (TextView) itemView.findViewById(R.id.inbox_chat_last);
            unreadBut = (Button) itemView.findViewById(R.id.inbox_chat_unread);
            dateText = (TextView) itemView.findViewById(R.id.inbox_chat_date);


        }

        void bind(Chat chat) {
            usernameText.setText(chat.getUsername());
            titleText.setText(chat.getBookTitle());
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