package it.polito.mad.greit.project;

import java.io.Serializable;

public class Chat implements Serializable, Comparable<Chat>{
    private String chatID;
    private String userID;
    private String username;
    private String bookID;
    private String bookTitle;
    private String lastMsg;
    private long unreadCount;
    private long timestamp;

    public String getChatID() {
        return chatID;
    }

    public void setChatID(String chatID) {
        this.chatID = chatID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBookID() {
        return bookID;
    }

    public void setBookID(String bookID) {
        this.bookID = bookID;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getLastMsg() {
        return lastMsg;
    }

    public void setLastMsg(String lastMsg) {
        this.lastMsg = lastMsg;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(Chat o){
        return (int) (this.timestamp - o.timestamp);
    }

    @Override
    public boolean equals(Object o){
        if(o.getClass().equals(Chat.class)){
            return this.chatID.equals( ((Chat) o).chatID);
        }
        return false;
    }
}
