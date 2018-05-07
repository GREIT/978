package it.polito.mad.greit.project;

import java.io.Serializable;

public class Message  implements Serializable {
    private long timestamp;
    private String userID;
    private String userName;
    private String message;

    public Message(){

    }

    public Message(long timestamp, String userID, String userName, String message) {
        this.timestamp = timestamp;
        this.userID = userID;
        this.userName = userName;
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
