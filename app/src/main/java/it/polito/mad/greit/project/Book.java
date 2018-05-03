package it.polito.mad.greit.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Book implements Serializable {
  private String ISBN;
  private String title;
  private Map<String, String> authors;
  private String publisher;
  private String year;
  private String cover;
  private Map<String, String> tags;

  public Book() {
    this.authors = new HashMap<>();
    this.tags = new HashMap<>();
  }
  
  public String getISBN() {
    return ISBN;
  }
  
  public void setISBN(String ISBN) {
    this.ISBN = ISBN;
  }
  
  public String getTitle() {
    return title;
  }
  
  public void setTitle(String title) {
    this.title = title;
  }
  
  public Map<String, String> getAuthors() {
    return authors;
  }
  
  public void setAuthors(Map<String, String> authors) {
    this.authors = authors;
  }
  
  public String getPublisher() {
    return publisher;
  }
  
  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }
  
  public String getYear() {
    return year;
  }
  
  public void setYear(String year) {
    this.year = year;
  }
  
  public String getCover() {
    return cover;
  }
  
  public void setCover(String cover) {
    this.cover = cover;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public void setTags(Map<String, String> tags) {
    this.tags = tags;
  }
}
