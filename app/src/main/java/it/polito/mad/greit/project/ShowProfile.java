package it.polito.mad.greit.project;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.net.URI;


public class ShowProfile extends AppCompatActivity {
  
  Toolbar t;
  Profile profile;

  
  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);
    setContentView(R.layout.activity_show_profile);
    t = findViewById(R.id.show_toolbar);
    setSupportActionBar(t);
    Setup();
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.show_menu, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    
    if (R.id.edit == item.getItemId()) {
      Intent swap = new Intent(ShowProfile.this, EditProfile.class);
      
      swap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(swap);
      return true;
    } else return super.onOptionsItemSelected(item);
  }
  
  private void Setup() {
    
    if (ContextCompat.checkSelfPermission(ShowProfile.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(ShowProfile.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.STORAGE_PERMISSION);
    }

    profile = new Profile();
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference dbref = db.getReference("users").child(FirebaseAuth.getInstance().
        getCurrentUser().getUid());
  
    dbref.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        profile = dataSnapshot.getValue(Profile.class);
        
        TextView tv = findViewById(R.id.name);
        tv.setText(profile.getName());
        tv = findViewById(R.id.nickname);
        tv.setText(profile.getUsername());
        tv = findViewById(R.id.email);
        tv.setText(profile.getEmail());
        tv = findViewById(R.id.location);
        tv.setText(profile.getLocation());
        tv = findViewById(R.id.biography);
        tv.setText(profile.getBio());

        if(profile.getPhotoUri() != null){
            ImageView iw = findViewById(R.id.pic);
            StorageReference sr = FirebaseStorage.getInstance().getReference().child("images/profile.jpg");
            final long size = 7 * 1024 * 1024;
            sr.getBytes(size).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    // Data for "images/island.jpg" is returns, use this as needed
                    Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    iw.setImageBitmap(bm);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                    exception.printStackTrace();
                }
            });
        }



        /*
          if (profile.getPhotoUri() != null) {
              ImageView iw = findViewById(R.id.pic);
              final File localFile;

              try {
                  localFile = File.createTempFile("profile", "jpg");
                  sr.getFile(localFile)
                          .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                              @Override
                              public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                  // Successfully downloaded data to local file
                                  // ...
                                  //profile.setPhotoUri(localFile.toString());
                                  profile.setPhotoUri(localFile.toURI().toString());
                                  Log.d("UP", "onSuccess: url " + localFile.toURI().toString());
                              }
                          }).addOnFailureListener(new OnFailureListener() {
                      @Override
                      public void onFailure(@NonNull Exception exception) {
                          // Handle failed download
                          // ...
                          exception.printStackTrace();
                      }
                  });

                  Log.d("SHOWPROFILE", "onDataChange: uri" + profile.getPhotoUri());
                  iw.setImageURI(Uri.parse(profile.getPhotoUri()));
              }
              catch (Exception e ){
                  e.printStackTrace();
              }
          } else {
              ImageView iw = findViewById(R.id.pic);
              iw.setImageResource(R.mipmap.ic_launcher);
          }
          */
      }
      @Override
      public void onCancelled(DatabaseError e) {}
    });
    
    
  }
  
  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    if (requestCode == Constants.STORAGE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage("You should grant permissions to storage for the app to work properly")
          .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              finish();
            }
          });
      AlertDialog alert = builder.create();
      alert.show();
    }
  }
  
  protected void onRestart() {
    super.onRestart();
    if (ContextCompat.checkSelfPermission(ShowProfile.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(ShowProfile.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.STORAGE_PERMISSION);
    }
  }
}
