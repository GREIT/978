package it.polito.mad.greit.project;

import java.io.Serializable;

public class Message  implements Serializable {
    private long timestamp;
    private String senderID;
    private String sender;
    private String message;

    public Message(){

    }

    public Message(long timestamp, String senderID, String sender, String message) {
        this.timestamp = timestamp;
        this.senderID = senderID;
        this.sender = sender;
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderID() {
        return senderID;
    }

    public void setSenderID(String senderID) {
        this.senderID = senderID;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
