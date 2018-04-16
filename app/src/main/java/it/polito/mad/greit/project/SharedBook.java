package it.polito.mad.greit.project;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Serializable;

public class SharedBook implements Serializable {
  private String title;
  private String year;
  private String author;
  private String publisher;
  private String ISBN;
  private String conditions;
  private String additionalInformations;
  private String owner;
  private String addedOn;
  private String photoUri;
  
  public SharedBook() {
  
  }
  
  public String getTitle() {
    return title;
  }
  
  public void setTitle(String title) {
    this.title = title;
  }
  
  public String getYear() {
    return year;
  }
  
  public void setYear(String year) {
    this.year = year;
  }
  
  public String getAuthor() {
    return author;
  }
  
  public void setAuthor(String author) {
    this.author = author;
  }
  
  public String getPublisher() {
    return publisher;
  }
  
  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }
  
  public String getISBN() {
    return ISBN;
  }
  
  public void setISBN(String ISBN) {
    this.ISBN = ISBN;
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
  
  public String getAddedOn() {
    return addedOn;
  }
  
  public void setAddedOn(String addedOn) {
    this.addedOn = addedOn;
  }
  
  public String getPhotoUri() {
    return photoUri;
  }
  
  public void setPhotoUri(String photoUri) {
    this.photoUri = photoUri;
  }
  
  public void saveToDB(String key) {
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference dbref = db.getReference("books").child(key);
    dbref.setValue(this);
    }
}
