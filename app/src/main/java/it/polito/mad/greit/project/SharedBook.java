package it.polito.mad.greit.project;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedBook extends Book implements Serializable {
  private String key;
  private String conditions;
  private String additionalInformations;
  private String owner;
  private String borrowTo;
  private String addedOn;
  private String position;
  private String coordinates;
  private Boolean shared;
  private String username;
  
  public SharedBook() {
  }
  
  public SharedBook(Book b) {
    this.setISBN(b.getISBN());
    this.setYear(b.getYear());
    this.setAuthors(b.getAuthors());
    this.setTitle(b.getTitle());
    this.setPublisher(b.getPublisher());
    this.setCover(b.getCover());
  }
  
  public String getKey() {
    return key;
  }
  
  public void setKey(String key) {
    this.key = key;
  }
  
  public String getConditions() {
    return conditions;
  }
  
  public void setConditions(String conditions) {
    this.conditions = conditions;
  }
  
  public String getAdditionalInformations() {
    return additionalInformations;
  }
  
  public void setAdditionalInformations(String additionalInformations) {
    this.additionalInformations = additionalInformations;
  }
  
  public String getOwner() {
    return owner;
  }
  
  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getBorrowTo() {return borrowTo; }

  public void setBorrowTo(String borrowTo) {this.borrowTo = borrowTo; }

  public String getAddedOn() {
    return addedOn;
  }
  
  public void setAddedOn(String addedOn) {
    this.addedOn = addedOn;
  }
  
  public String getPosition() {
    return position;
  }
  
  public void setPosition(String position) {
    this.position = position;
  }
  
  public Boolean getShared() {
    return shared;
  }
  
  public void setShared(Boolean shared) {
    this.shared = shared;
  }

  public String getUsername(){ return username;}

  public void setUsername(String user) { this.username = user; }

  public String getCoordinates(){return coordinates;}

  public void setCoordinates(String coordinates) { this.coordinates = coordinates; }

}
