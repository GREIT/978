package it.polito.mad.greit.project;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import java.util.Calendar;

public class CompleteBookRegistration extends AppCompatActivity {
  SharedBook book;
  
  private TextView tw_ISBN;
  private TextView tw_author;
  private TextView tw_year;
  private TextView tw_title;
  private TextView tw_publisher;
  private RatingBar rb_conditions;
  private EditText et_additionalInfo;
  Toolbar t;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_complete_book_registration);
    t = findViewById(R.id.complete_book_toolbar);
    setSupportActionBar(t);
    
    book = (SharedBook) getIntent().getSerializableExtra("book");
    
    tw_ISBN = (TextView) findViewById(R.id.complete_book_ISBN);
    tw_ISBN.setText(book.getISBN());
    
    tw_author = (TextView) findViewById(R.id.complete_book_author);
    tw_author.setText(book.getAuthor());
    
    tw_year = (TextView) findViewById(R.id.complete_book_year);
    tw_year.setText(book.getYear());
    
    tw_publisher = (TextView) findViewById(R.id.complete_book_publisher);
    tw_publisher.setText(book.getPublisher());
    
    tw_title = (TextView) findViewById(R.id.complete_book_title);
    tw_title.setText(book.getTitle());
    
    rb_conditions = (RatingBar) findViewById(R.id.complete_book_conditions);
    
    et_additionalInfo = (EditText) findViewById(R.id.complete_book_additionalInfo);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.completebookregistration_menu, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    
    if (R.id.complete_book_registration_confirm == item.getItemId()) {
      book.setConditions(String.valueOf(rb_conditions.getRating()));
      
      book.setAdditionalInformations(et_additionalInfo.getText().toString());
      
      book.setAddedOn(Calendar.getInstance().getTime().toString());
      
      book.saveToDB();
  
      Intent I = new Intent(this, MainActivity.class);
      I.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(I);
      
      return true;
    } else return super.onOptionsItemSelected(item);
  }
}
