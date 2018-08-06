package it.polito.mad.greit.project;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Book implements Serializable, Comparable {
  private String ISBN;
  private String title;
  private Map<String, String> authors;
  private String publisher;
  private String year;
  private String cover;
  private int booksOnLoan;
  private int lentBooks;
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
  
  public int getBooksOnLoan() {
    return booksOnLoan;
  }
  
  public void setBooksOnLoan(int booksOnLoan) {
    this.booksOnLoan = booksOnLoan;
  }
  
  public int getLentBooks() {
    return lentBooks;
  }
  
  public void setLentBooks(int lentBooks) {
    this.lentBooks = lentBooks;
  }
  
  public Map<String, String> getTags() {
    return tags;
  }

  public void setTags(Map<String, String> tags) {
    this.tags = tags;
  }
  
  @Override
  public int compareTo(@NonNull Object o) {
    return this.getBooksOnLoan() - ((Book) o).getBooksOnLoan();
  }
}
