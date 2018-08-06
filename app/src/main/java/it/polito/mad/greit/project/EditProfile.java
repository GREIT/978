package it.polito.mad.greit.project;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompletePredictionBufferResponse;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;


public class EditProfile extends AppCompatActivity {

  private Toolbar t;
  private Profile profile;
  private CircleImageView ciw;
  private File pic = null;
  private File temp = null;
  private Uri imageUri = null;
  private boolean changed = false;
  private String coordinates=null;
  private String location=null;
  private ImageButton explanation = null;
 // boolean def = false;


  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);
    setContentView(R.layout.activity_edit_profile);

    t = findViewById(R.id.edit_profile_toolbar);
    t.setTitle(R.string.activity_edit_profile);
    setSupportActionBar(t);
    t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
    t.setNavigationOnClickListener(v -> onBackPressed());

    pic = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString(),"pic.jpg");
    temp = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString(),"temp.jpg");

    Setup(b);

    ciw = findViewById(R.id.edit_pic);
    ciw.setImageResource(R.mipmap.ic_launcher_round);
    ciw.setOnClickListener(v -> pic_action());
    
    explanation = findViewById(R.id.explanationLocationProfile);
    explanation.setOnClickListener(v -> {
      android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(EditProfile.this);
      builder.setMessage(getResources().getString(R.string.explanation_location))
          .setCancelable(false)
          .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              //do things
            }
          });
      android.support.v7.app.AlertDialog alert = builder.create();
      alert.show();
    });

    PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
            getFragmentManager().findFragmentById(R.id.edit_location);

    autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
      @Override
      public void onPlaceSelected(Place place) {
        location = place.getAddress().toString();
        coordinates = place.getLatLng().latitude + ";" + place.getLatLng().longitude;
      }

      @Override
      public void onError(Status status) {

      }
    });

  }

  public static void hideKeyboard(Activity activity) {
    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
    //Find the currently focused view, so we can grab the correct window token from it.
    View view = activity.getCurrentFocus();
    //If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
      view = new View(activity);
    }
    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
  }

  private void pic_action() {
    if(!EditProfile.this.isFinishing()){
      String upload = getResources().getString(R.string.upload_pic);
      String snap = getResources().getString(R.string.snap_pic);
      String select = getResources().getString(R.string.select_image_action);
      final CharSequence[] items = {upload, snap};
      AlertDialog.Builder builder = new AlertDialog.Builder(EditProfile.this);
      builder.setTitle(select)
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
    } else
      return super.onOptionsItemSelected(item);
  }

  void Setup(Bundle b) {
    profile = new Profile();

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference dbref = db.getReference("USERS").child(user.getUid());

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
      if(b.containsKey("bio")){
        //tv = findViewById(R.id.edit_location);
        //tv.setText(b.getString("location"));
        location = b.getString("location");
        coordinates = b.getString("coords");
        tv = findViewById(R.id.edit_biography);
        tv.setText(b.getString("bio"));
      }
    }
    else{
      //tv = findViewById(R.id.edit_location);
      //tv.setText(profile.getLocation());
      location = profile.getLocation();
      coordinates = profile.getCoordinates();
      tv = findViewById(R.id.edit_biography);
      tv.setText(profile.getBio());
    }

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    if(pic.exists()){
        //createpic(BitmapFactory.decodeFile(pic.toString()));
      Bitmap bm = BitmapFactory.decodeFile(pic.getPath());
      ciw.setImageBitmap(bm);
    }
    else {
      StorageReference sr = FirebaseStorage.getInstance().getReference()
              .child("profile_pictures/" + user.getUid() + ".jpg");
      sr.getBytes(Constants.SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
        @Override
        public void onSuccess(byte[] bytes) {
          try {
            Bitmap bm = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            ciw.setImageBitmap(bm);
            OutputStream outs = new FileOutputStream(pic);
            bm.compress(Bitmap.CompressFormat.JPEG, 100,outs);
            outs.flush();
            outs.close();
          }catch (Exception e){
            e.printStackTrace();
            ciw.setImageResource(R.mipmap.ic_launcher_round);
          }
        }
      });
    }
  }

  void SaveInfo() {
    profile.setCoordinates(coordinates);
    profile.setLocation(location);
    TextView tv = findViewById(R.id.edit_name);
    profile.setName(tv.getText().toString());
    tv = findViewById(R.id.edit_nickname);
    profile.setUsername(tv.getText().toString());
    tv = findViewById(R.id.edit_email);
    profile.setEmail(tv.getText().toString());
    tv = findViewById(R.id.edit_biography);
    profile.setBio(tv.getText().toString());

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference dbref = db.getReference("USERS").child(user.getUid());
    dbref.setValue(profile);

    updateLocation(coordinates, location);


    if(changed){
      StorageReference sr = FirebaseStorage.getInstance().getReference().child("profile_pictures/" + user.getUid() + ".jpg");
      ProgressDialog dialog = ProgressDialog.show(EditProfile.this, "",  getResources().getString(R.string.loading), true);
      sr.putFile(FileProvider.getUriForFile(this, "it.polito.mad.greit.project", temp))
              .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
        @Override
        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
          dialog.dismiss();
          pic.delete();
          temp.renameTo(new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString(), "pic.jpg"));
          Intent swap = new Intent(EditProfile.this, MainActivity.class);
          swap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
          startActivity(swap);
        }
      });
    }
    else{
      Intent swap = new Intent(EditProfile.this, MainActivity.class);
      swap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(swap);
    }
  }

  private void UploadPic() {

    Intent gallery = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    gallery.setType("image/*");
    //gallery.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    gallery.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivityForResult(gallery, Constants.REQUEST_GALLERY);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == Constants.REQUEST_GALLERY && resultCode == RESULT_OK) {
      try {
        Uri result = data.getData();
        CropImage.activity(result)
                .setCropShape(CropImageView.CropShape.OVAL)
                //.setOutputUri(FileProvider.getUriForFile(this, "it.polito.mad.greit.project", pic))
                .setOutputUri(FileProvider.getUriForFile(this, "it.polito.mad.greit.project", temp))
                .setAspectRatio(1, 1)
                .setRequestedSize(300, 300, CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                .start(this);
      }catch (Exception e){
        e.printStackTrace();
      }
    }
    else if (requestCode == Constants.REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      try {
        CropImage.activity(imageUri)
                .setCropShape(CropImageView.CropShape.OVAL)
                //.setOutputUri(FileProvider.getUriForFile(this, "it.polito.mad.greit.project", pic))
                .setOutputUri(FileProvider.getUriForFile(this, "it.polito.mad.greit.project", temp))
                .setAspectRatio(1, 1)
                .setRequestedSize(300, 300,CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                .start(this);
      }catch (Exception e){
        e.printStackTrace();
      }
    }
    else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
      try {
        changed = true;
        Bitmap bm = BitmapFactory.decodeFile(temp.getPath());
        ciw.setImageBitmap(bm);
      }catch (Exception e){
        e.printStackTrace();
      }
    }

  }

  void Camera() {
    if (ContextCompat.checkSelfPermission(EditProfile.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(EditProfile.this, new String[]{Manifest.permission.CAMERA}, Constants.CAMERA_PERMISSION);
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
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    if (requestCode == Constants.CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Camera();
    }
  }
/*
  @Override
  public void onBackPressed() {
    Intent intent = new Intent(EditProfile.this, ShowProfile.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
  }
*/

  /*private void createpic(Bitmap bm) {

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
  }*/

  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    TextView tv;
    //tv = findViewById(R.id.edit_location);
    //b.putString("location",tv.getText().toString());
    b.putString("coords",coordinates);
    b.putString("location",location);
    tv = findViewById(R.id.edit_biography);
    b.putString("bio",tv.getText().toString());

  }

  private void updateLocation(String coordinates, String position) {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseDatabase db = FirebaseDatabase.getInstance();

    DatabaseReference mSharedBookDb = db.getReference("SHARED_BOOKS");

    mSharedBookDb.orderByChild("ownerUid").equalTo(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        for (DataSnapshot ds : dataSnapshot.getChildren()) {
          SharedBook tmpBook = ds.getValue(SharedBook.class);
          tmpBook.setCoordinates(coordinates);
          tmpBook.setPosition(location);
          db.getReference("SHARED_BOOKS").child(tmpBook.getKey()).setValue(tmpBook);
        }
      }

      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });
  }

  public void onDestroy(){
    try {
      File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString());
      for (File f : dir.listFiles()) {
        if (!f.getName().equals("pic.jpg")) {
          f.delete();
        }
      }
    }catch (Exception e){
      e.printStackTrace();
    }
    super.onDestroy();
  }

}