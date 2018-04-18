package it.polito.mad.greit.project;

import android.Manifest;
import android.app.ProgressDialog;
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
  private EditText et_ISBN;
  private EditText et_author;
  private EditText et_year;
  private EditText et_title;
  private EditText et_publisher;
  private RatingBar rb_conditions;
  private EditText et_additionalInfo;
  Toolbar t;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_complete_book_registration);
    
    t = findViewById(R.id.complete_book_toolbar);
    t.setTitle("Complete book informations");
    setSupportActionBar(t);
    t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
    t.setNavigationOnClickListener(v -> finish());
    
    book = (SharedBook) getIntent().getSerializableExtra("book");
    
    et_ISBN = (EditText) findViewById(R.id.complete_book_ISBN);
    et_ISBN.setText(book.getISBN());
    
    et_author = (EditText) findViewById(R.id.complete_book_author);
    et_author.setText(book.getAuthor());
    
    et_year = (EditText) findViewById(R.id.complete_book_year);
    et_year.setText(book.getYear());
    
    et_publisher = (EditText) findViewById(R.id.complete_book_publisher);
    et_publisher.setText(book.getPublisher());
    
    et_title = (EditText) findViewById(R.id.complete_book_title);
    et_title.setText(book.getTitle());
    
    rb_conditions = (RatingBar) findViewById(R.id.complete_book_conditions);
    
    et_additionalInfo = (EditText) findViewById(R.id.complete_book_additionalInfo);
    
    bb = (Button) findViewById(R.id.complete_book_snap_pic);
    bb.setOnClickListener(v -> camera());
  }
  
  void camera() {
    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, Constants.CAMERA_PERMISSION);
    } else {
      Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      try {
        File img = File.createTempFile("photoBook", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        photo = FileProvider.getUriForFile(this, "it.polito.mad.greit.project", img);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photo);
        startActivityForResult(takePictureIntent, Constants.REQUEST_IMAGE_CAPTURE);
      } catch (Exception e) {
        e.printStackTrace();
      }
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
      
      book.setTitle(et_title.getText().toString());
      
      book.setAuthor(et_author.getText().toString());
      
      book.setISBN(et_ISBN.getText().toString());
      
      book.setPublisher(et_publisher.getText().toString());
      
      book.setYear(et_year.getText().toString());
      
      book.setConditions(String.valueOf(rb_conditions.getRating()));
      
      book.setAdditionalInformations(et_additionalInfo.getText().toString());
      
      book.setAddedOn(Calendar.getInstance().getTime().toString());
      
      book.setKey(key);
      
      if (photo == null) {
        book.saveToDB(key);
        Intent intent = new Intent(CompleteBookRegistration.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
      } else {
        try {
          StorageReference sr = FirebaseStorage.getInstance().getReference().child("shared_books_pictures/" + book.getKey() + ".jpg");
          ProgressDialog dialog = ProgressDialog.show(CompleteBookRegistration.this, "", "Uploading, please wait...", true);
          sr.putFile(this.photo).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
              // Get a URL to the uploaded content
              //Uri downloadUrl = taskSnapshot.getDownloadUrl();
              book.saveToDB(key);
              Log.d("UP", "onSuccess: url " + taskSnapshot.getDownloadUrl().toString());
              dialog.dismiss();
              Intent intent = new Intent(CompleteBookRegistration.this, MainActivity.class);
              intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
              startActivity(intent);
            }
          })
              .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                  // Handle unsuccessful uploads
                  // ...
                  exception.printStackTrace();
                  dialog.dismiss();
                  Intent intent = new Intent(CompleteBookRegistration.this, MainActivity.class);
                  intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                  startActivity(intent);
                }
              });
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      
      return true;
    } else return super.onOptionsItemSelected(item);
  }
}
