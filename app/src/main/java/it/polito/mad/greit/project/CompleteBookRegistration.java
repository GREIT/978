package it.polito.mad.greit.project;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Calendar;

public class CompleteBookRegistration extends AppCompatActivity {
  private final String TAG = "Book Registration";
  
  private Book book;
  
  private Profile profile;
  private Button bb;
  //private byte[] photo = null;
  private TextView tw_ISBN;
  private TextView tw_author;
  private TextView tw_year;
  private TextView tw_title;
  private TextView tw_publisher;
  private TextView tw_conditions;
  private TagEditText tags;
  private Uri imageUri;
  Toolbar t;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_complete_book_registration);
    
    t = findViewById(R.id.complete_book_toolbar);
    t.setTitle(R.string.actvity_complete_book_info);
    setSupportActionBar(t);
    t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
    t.setNavigationOnClickListener(v -> finish());

    book = (Book) getIntent().getSerializableExtra("book");
    
    tw_ISBN = (TextView) findViewById(R.id.complete_book_ISBN);
    tw_ISBN.setText(book.getISBN());
    
    tw_author = (TextView) findViewById(R.id.complete_book_author);
    String AS = android.text.TextUtils.join(", ", book.getAuthors().keySet());
    tw_author.setText(AS);
    
    tw_year = (TextView) findViewById(R.id.complete_book_year);
    tw_year.setText(book.getYear());
    
    tw_publisher = (TextView) findViewById(R.id.complete_book_publisher);
    tw_publisher.setText(book.getPublisher());
    
    tw_title = (TextView) findViewById(R.id.complete_book_title);
    tw_title.setText(book.getTitle());
    
    tw_conditions = (TextView) findViewById(R.id.complete_book_conditions);
    
    tags = (TagEditText) findViewById(R.id.complete_book_tags);
    
    bb = (Button) findViewById(R.id.complete_book_snap_pic);
    bb.setOnClickListener(v -> camera());
  }
  
  void camera() {
    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, Constants.CAMERA_PERMISSION);
    } else {
      Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      try {
        File img = File.createTempFile("temp", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        imageUri = FileProvider.getUriForFile(this, "it.polito.mad.greit.project", img);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(takePictureIntent, Constants.REQUEST_IMAGE_CAPTURE);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == Constants.REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK ) {
      //Log.d("DEBUGDEBUG", "onActivityResult: " + uri.getPath().toString());
      try {
        CropImage.activity(imageUri)
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setAspectRatio(1, 1)
                .setRequestedSize(500, 500,CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                .start(this);
      }catch (Exception e){
        e.printStackTrace();
      }
    }
    else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
      try {
        CropImage.ActivityResult result = CropImage.getActivityResult(data);
        imageUri = result.getUri();
      }catch (Exception e){
        e.printStackTrace();
      }
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
    
    if (TextUtils.isEmpty(tw_conditions.getText().toString())) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage("You must describe your book's conditions!")
          .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              Log.d(TAG, "Dialog clicked");
            }
          });
      AlertDialog alert = builder.create();
      alert.show();
      return true;
    }
    
    if (R.id.complete_book_registration_confirm == item.getItemId()) {
      FirebaseDatabase db = FirebaseDatabase.getInstance();
      String key = db.getReference("SHARED_BOOKS").push().getKey();
      
      SharedBook sb = new SharedBook(book);
      
      sb.setAdditionalInformations(tw_conditions.getText().toString());
      
      String[] tagString = tags.getText().toString().toLowerCase().replaceAll("[\\/\\#\\.\\/\\$\\[]", "").split(",");
      
      sb.setAddedOn(System.currentTimeMillis() / 1000L);
      
      sb.setOwnerUid(FirebaseAuth.getInstance().getCurrentUser().getUid());
      
      sb.setShared(false);
      
      sb.setKey(key);
  
      FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
  
      FirebaseDatabase dbU = FirebaseDatabase.getInstance();
      DatabaseReference dbrefU = dbU.getReference("USERS").child(user.getUid());
  
      dbrefU.addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
          profile = dataSnapshot.getValue(Profile.class);
          if (profile == null) {
            Intent intent = new Intent(CompleteBookRegistration.this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
          } else {
            sb.setPosition(profile.getLocation());
            sb.setCoordinates(profile.getCoordinates());
            sb.setOwnerUsername(profile.getUsername());
            DatabaseReference dbref = db.getReference("SHARED_BOOKS").child(key);
            dbref.setValue(sb);
          }
        }
    
        @Override
        public void onCancelled(DatabaseError e) {
        }
      });
  
      DatabaseReference dbref = db.getReference("BOOKS/" + book.getISBN());
      
      dbref.child("booksOnLoan").setValue(Integer.valueOf(book.getBooksOnLoan()) + 1);
      
      for (String x : tagString) {
        if(!x.isEmpty())
          dbref.child("tags").child(x).setValue(x);
      }

      if (imageUri == null) {
        Intent intent = new Intent(CompleteBookRegistration.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
      } else {
        try {
          StorageReference sr = FirebaseStorage.getInstance().getReference().child("shared_books_pictures/" + sb.getKey() + ".jpg");
          ProgressDialog dialog = ProgressDialog.show(CompleteBookRegistration.this, "", "Uploading, please wait...", true);
          sr.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
              // Get a URL to the uploaded content
              //Uri downloadUrl = taskSnapshot.getDownloadUrl();
              
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
