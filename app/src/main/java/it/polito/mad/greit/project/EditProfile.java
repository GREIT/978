package it.polito.mad.greit.project;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Array;

import de.hdodenhof.circleimageview.CircleImageView;


public class EditProfile extends AppCompatActivity {

  private Toolbar t;
  private Profile profile;
  private CircleImageView ciw;
  private byte[] photo = null;


  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);
    setContentView(R.layout.activity_edit_profile);

    t = findViewById(R.id.edit_profile_toolbar);
    t.setTitle(R.string.activity_edit_profile);
    setSupportActionBar(t);
    t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
    t.setNavigationOnClickListener(v -> RevertInfo());


    Setup(b);

    ciw = findViewById(R.id.edit_pic);
    ciw.setOnClickListener(v -> pic_action());
  }

  private void pic_action() {
    if(!EditProfile.this.isFinishing()){
      final CharSequence[] items = {"Upload Pic", "Snap Pic"};
      AlertDialog.Builder builder = new AlertDialog.Builder(EditProfile.this);
      builder.setTitle("Select source for image")
              .setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                  if(which == 0){
                    UploadPic();
                  }
                  else if(which == 1){
                    Camera();
                  }
                  else{
                    dialog.dismiss();
                  }
                }
              });

      builder.show();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.edit_profile_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    if (R.id.confirm == item.getItemId()) {
      SaveInfo();
      return true;
    } else return super.onOptionsItemSelected(item);
  }


  void Setup(Bundle b) {
    profile = new Profile();

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference dbref = db.getReference("users").child(user.getUid());

    dbref.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        profile = dataSnapshot.getValue(Profile.class);
        Fill(b);
      }

      @Override
      public void onCancelled(DatabaseError e) {
      }
    });

  }

  void Fill(Bundle b) {
    TextView tv = findViewById(R.id.edit_name);
    tv.setText(profile.getName());
    tv = findViewById(R.id.edit_nickname);
    tv.setText(profile.getUsername());
    tv = findViewById(R.id.edit_email);
    tv.setText(profile.getEmail());
    if(b!=null){
      if(b.containsKey("location")){
        tv = findViewById(R.id.edit_location);
        tv.setText(b.getString("location"));
        tv = findViewById(R.id.edit_biography);
        tv.setText(b.getString("bio"));
      }
    }
    else{
      tv = findViewById(R.id.edit_location);
      tv.setText(profile.getLocation());
      tv = findViewById(R.id.edit_biography);
      tv.setText(profile.getBio());
    }

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    StorageReference sr = FirebaseStorage.getInstance().getReference().child("profile_pictures/" + user.getUid() + ".jpg");
    sr.getBytes(Constants.SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
      @Override
      public void onSuccess(byte[] bytes) {
        Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        createpic(bm);
      }
    }).addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception exception) {
        // Handle any errors
        exception.printStackTrace();
        ciw.setImageResource(R.mipmap.ic_launcher_round);
      }
    });
  }

  void SaveInfo() {
    TextView tv = findViewById(R.id.edit_name);
    profile.setName(tv.getText().toString());
    tv = findViewById(R.id.edit_nickname);
    profile.setUsername(tv.getText().toString());
    tv = findViewById(R.id.edit_email);
    profile.setEmail(tv.getText().toString());
    tv = findViewById(R.id.edit_location);
    profile.setLocation(tv.getText().toString());
    tv = findViewById(R.id.edit_biography);
    profile.setBio(tv.getText().toString());
    //profile.setPhotoUri(photo.toString());

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    profile.saveToDB(FirebaseAuth.getInstance().getCurrentUser().getUid());

    if (this.photo != null) {
      try {
        StorageReference sr = FirebaseStorage.getInstance().getReference().child("profile_pictures/" + user.getUid() + ".jpg");
        ProgressDialog dialog = ProgressDialog.show(EditProfile.this, "", "Uploading, please wait...", true);
        sr.putBytes(this.photo).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
          @Override
          public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
            Log.d("UP", "onSuccess: url " + taskSnapshot.getDownloadUrl().toString());
            dialog.dismiss();
            Intent swap = new Intent(EditProfile.this, ShowProfile.class);
            swap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(swap);
          }
        }).addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception exception) {
            exception.printStackTrace();
            dialog.dismiss();
            Intent swap = new Intent(EditProfile.this, ShowProfile.class);
            swap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(swap);
          }
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    else{
      Intent swap = new Intent(EditProfile.this, ShowProfile.class);
      swap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(swap);
    }
  }

  private void RevertInfo() {
    Intent swap = new Intent(EditProfile.this, ShowProfile.class);
    swap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(swap);
  }


  private void UploadPic() {
//    if (ContextCompat.checkSelfPermission(EditProfile.this,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//      ActivityCompat.requestPermissions(EditProfile.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//              Constants.STORAGE_PERMISSION);
//    }

    Intent gallery = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    gallery.setType("image/*");
    //gallery.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    gallery.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivityForResult(gallery, Constants.REQUEST_GALLERY);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Bitmap bm = null;

    if (requestCode == Constants.REQUEST_GALLERY && resultCode == RESULT_OK) {
      Uri uri = data.getData();
      try {
        bm = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else if (requestCode == Constants.REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      bm = (Bitmap) data.getExtras().get("data");
    }

    if (bm != null) {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      bm.compress(Bitmap.CompressFormat.JPEG, 85, stream);
      this.photo = stream.toByteArray();
      this.createpic(bm);
      bm.recycle();
    }


  }

  void Camera() {

    if (ContextCompat.checkSelfPermission(EditProfile.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(EditProfile.this, new String[]{Manifest.permission.CAMERA}, Constants.CAMERA_PERMISSION);
    } else {
      Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      startActivityForResult(takePictureIntent, Constants.REQUEST_IMAGE_CAPTURE);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    if (requestCode == Constants.CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Camera();
    }
  }

  @Override
  public void onBackPressed() {
    Intent intent = new Intent(EditProfile.this, ShowProfile.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
  }


  private void createpic(Bitmap bm) {

    //Bitmap imageBitmap = Bitmap.createBitmap(ciw.getWidth(), ciw.getHeight(), Bitmap.Config.ARGB_8888);
    //Bitmap imageBitmap = bm.copy(Bitmap.Config.ARGB_8888,true);
    int width = ciw.getWidth();
    int height = ciw.getHeight();
    Bitmap imageBitmap = Bitmap.createScaledBitmap(bm, width, height, false);
    Canvas canvas = new Canvas(imageBitmap);

    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    p.setColor(Color.WHITE);
    p.setStyle(Paint.Style.FILL);

    canvas.drawRect((float)(width/2 - (0.03*width)),(float)(0.7*height),(float)(width/2 + (0.03*width)),(float)(0.3*height),p);
    canvas.drawRect((float)(0.7*width),(float)(height/2 - (0.03*height)),(float)(0.3*width),(float)(height/2 + (0.03*height)),p);
    ciw.setImageBitmap(imageBitmap);
  }

  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    TextView tv;
    tv = findViewById(R.id.edit_location);
    b.putString("location",tv.getText().toString());
    tv = findViewById(R.id.edit_biography);
    b.putString("bio",tv.getText().toString());

  }


}