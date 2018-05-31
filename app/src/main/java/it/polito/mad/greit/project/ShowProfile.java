package it.polito.mad.greit.project;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;


public class ShowProfile extends AppCompatActivity {
  
  private Toolbar t;
  private Profile profile;

  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);
    setContentView(R.layout.activity_show_profile);
    
    t = findViewById(R.id.show_profile_toolbar);
    t.setTitle(R.string.activity_show_profile);
    setSupportActionBar(t);
    t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
    t.setNavigationOnClickListener(view -> onBackPressed());
    
    Setup();
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.show_profile_menu, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    
    if (R.id.edit == item.getItemId()) {
      Intent swap = new Intent(ShowProfile.this, EditProfile.class);
      startActivity(swap);
      return true;
    } else return super.onOptionsItemSelected(item);
  }
  
  private void Setup() {

    profile = new Profile();
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference dbref = db.getReference("USERS").child(FirebaseAuth.getInstance().
        getCurrentUser().getUid());
    
    dbref.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        profile = dataSnapshot.getValue(Profile.class);
        
        TextView tv = findViewById(R.id.name);
        tv.setText(profile.getName());
        tv = findViewById(R.id.nickname);
        tv.setText("@" + profile.getUsername());
        tv = findViewById(R.id.email);
        tv.setText(profile.getEmail());
        tv = findViewById(R.id.location);
        tv.setText(profile.getLocation());
        tv = findViewById(R.id.biography);
        tv.setText(profile.getBio());
      }
      @Override
      public void onCancelled(DatabaseError e) {
      }
    });


    File pic = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString(),"pic.jpg");
    ImageView iw = findViewById(R.id.pic);
    if(pic.exists()){
      iw.setImageURI(Uri.fromFile(pic));
    }
    else{
      FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
      StorageReference sr = FirebaseStorage.getInstance().getReference()
              .child("profile_pictures/" + user.getUid() + ".jpg");
      sr.getBytes(Constants.SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
        @Override
        public void onSuccess(byte[] bytes) {
          try{
            Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            iw.setImageBitmap(bm);
            OutputStream outs = new FileOutputStream(pic);
            bm.compress(Bitmap.CompressFormat.JPEG, 100,outs);
            outs.flush();
            outs.close();
          }catch (Exception e){
            e.printStackTrace();
            iw.setImageResource(R.mipmap.ic_launcher_round);
          }
        }
      }).addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception exception) {
          // Handle any errors
          exception.printStackTrace();
          iw.setImageResource(R.mipmap.ic_launcher_round);
        }
      });
    }
  }
/*
  @Override
  public void onBackPressed() {
    Intent intent = new Intent(ShowProfile.this, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
  }
*/
}
