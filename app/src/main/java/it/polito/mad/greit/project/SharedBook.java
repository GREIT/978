package it.polito.mad.greit.project;


import java.io.Serializable;

public class SharedBook extends Book implements Serializable {
  private String key;
  private String additionalInformations;
  private String ownerUid;
  private String ownerUsername;
  private String borrowToUid;
  private String borrowToUsername;
  private String addedOn;
  private String position;
  private String coordinates;
  private Boolean shared;
  
  
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
  
  public String getAdditionalInformations() {
    return additionalInformations;
  }
  
  public void setAdditionalInformations(String additionalInformations) {
    this.additionalInformations = additionalInformations;
  }
  
  public String getOwnerUid() {
    return ownerUid;
  }
  
  public void setOwnerUid(String ownerUid) {
    this.ownerUid = ownerUid;
  }
  
  public String getBorrowToUid() {
    return borrowToUid;
  }
  
  public void setBorrowToUid(String borrowToUid) {
    this.borrowToUid = borrowToUid;
  }
  
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
  
  public String getOwnerUsername() {
    return ownerUsername;
  }
  
  public void setOwnerUsername(String user) {
    this.ownerUsername = user;
  }
  
  public String getCoordinates() {
    return coordinates;
  }
  
  public void setCoordinates(String coordinates) {
    this.coordinates = coordinates;
  }
  
  public String getBorrowToUsername() {
    return this.borrowToUsername;
  }
  
  public void setBorrowToUsername(String btu) {
    this.borrowToUsername = btu;
  }
  
}
