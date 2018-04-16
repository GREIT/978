package it.polito.mad.greit.project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Calendar;

public class CompleteBookRegistration extends AppCompatActivity {
  SharedBook book;
  
  private Button bb;
  private Uri photo;
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
    
    bb = (Button) findViewById(R.id.complete_book_snap_pic);
    bb.setOnClickListener(v -> camera());
  }
  
  void camera() {
    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, Constants.CAMERA_PERMISSION);
    }
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    try {
      File img = File.createTempFile("photoBook", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
      if (img != null) {
        photo = FileProvider.getUriForFile(this, "it.polito.mad.greit.project", img);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photo);
        startActivityForResult(takePictureIntent, Constants.REQUEST_IMAGE_CAPTURE);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    
    if (requestCode == Constants.REQUEST_GALLERY && resultCode == RESULT_OK) {
      this.photo = data.getData();
    }
    //else if (requestCode == Constants.REQUEST_IMAGE_CAPTURE && resultCode == RESULT_CANCELED) {}
    
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
      FirebaseDatabase db = FirebaseDatabase.getInstance();
      String key = db.getReference("books").push().getKey();
      
      book.setConditions(String.valueOf(rb_conditions.getRating()));
      
      book.setAdditionalInformations(et_additionalInfo.getText().toString());
      
      book.setAddedOn(Calendar.getInstance().getTime().toString());
      
      
      try {
        StorageReference sr = FirebaseStorage.getInstance().getReference().child("shared_books_pictures/" + key + ".jpg");
        sr.putFile(this.photo).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
          @Override
          public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
            // Get a URL to the uploaded content
            //Uri downloadUrl = taskSnapshot.getDownloadUrl();
            book.setPhotoUri(taskSnapshot.getDownloadUrl().toString());
            book.saveToDB(key);
            Log.d("UP", "onSuccess: url " + taskSnapshot.getDownloadUrl().toString());
          }
        })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                // ...
                exception.printStackTrace();
              }
            });
        
       
      } catch (Exception e) {
        e.printStackTrace();
      }
      
      Intent I = new Intent(this, MainActivity.class);
      I.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(I);
      
      return true;
    } else return super.onOptionsItemSelected(item);
  }
}
