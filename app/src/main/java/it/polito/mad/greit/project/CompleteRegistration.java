package it.polito.mad.greit.project;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompletePredictionBufferResponse;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CompleteRegistration extends AppCompatActivity {
  Button bb;
  FirebaseUser U;
  private static final String TAG = "CompleteRegistration";
  String coordinates=null;
  String location = null;
  //Boolean def = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_complete_registration);

    U = FirebaseAuth.getInstance().getCurrentUser();

    bb = findViewById(R.id.complete_registration);
    bb.setOnClickListener(v -> registrationCompleted());

    //setuplocation();
    PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
            getFragmentManager().findFragmentById(R.id.complete_location);

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

  public void setuplocation() {
    double radiusDegrees = 30;
    LatLng northEast,southWest;
    northEast = new LatLng(41.5 + radiusDegrees, 12.3 + radiusDegrees);
    southWest = new LatLng(41.5 - radiusDegrees, 12.3 - radiusDegrees);
    LatLngBounds bounds = LatLngBounds.builder().include(northEast).include(southWest).build();
    final ArrayAdapter<String> autoComplete = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    AutoCompleteTextView ACTV = findViewById(R.id.complete_location);
    ACTV.setAdapter(autoComplete);
    HashMap<String,String> place_ids = new HashMap<>();
    ACTV.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        Task<AutocompletePredictionBufferResponse> results =
                Places.getGeoDataClient(CompleteRegistration.this)
                        .getAutocompletePredictions(charSequence.toString(), bounds, null);
        results.addOnSuccessListener(new OnSuccessListener<AutocompletePredictionBufferResponse>() {
          @Override
          public void onSuccess(AutocompletePredictionBufferResponse autocompletePredictions) {
            try {
              autoComplete.clear();
              for (int i = 0; i < results.getResult().getCount() && i < 10; i++) {
                autoComplete.add(results.getResult().get(i).getFullText(null).toString());
                place_ids.put(results.getResult().get(i).getFullText(null).toString()
                        ,results.getResult().get(i).getPlaceId());
              }
              results.getResult().release();
              Log.d("DEBUGDEBUGDEBUG", "onSuccess: ENTERED,autocomplete has " + autoComplete.getCount() + "elements");
            } catch (Exception e) {
              results.getResult().release();
              autoComplete.clear();
            }

          }
        }).addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            results.getResult().release();
            autoComplete.clear();
            place_ids.clear();
          }
        });
      }

      @Override
      public void afterTextChanged(Editable editable) {
      }

    });

    ACTV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        hideKeyboard(CompleteRegistration.this);
        String place_id = place_ids.get(autoComplete.getItem(position));
        Log.d("DEBUGDEBUGDEBUG", "onItemClick: " + place_ids.get(autoComplete.getItem(position)));
        Places.getGeoDataClient(CompleteRegistration.this).getPlaceById(place_id).addOnSuccessListener(new OnSuccessListener<PlaceBufferResponse>() {
          @Override
          public void onSuccess(PlaceBufferResponse places) {
            LatLng coords = places.get(0).getLatLng();
            coordinates = coords.latitude + ";" + coords.longitude;
          }
        });
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

  private void registrationCompleted() {
    EditText edit_nickname = findViewById(R.id.complete_nickname);
    //AutoCompleteTextView edit_location = findViewById(R.id.complete_location);

    if (TextUtils.isEmpty(edit_nickname.getText().toString().replaceAll(" ", ""))
            //|| TextUtils.isEmpty(edit_location.getText().toString().replaceAll(" ", ""))) {
            || location == null || coordinates == null) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage(R.string.fill_fields)
              .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  dialog.dismiss();
                }
              });
      AlertDialog alert = builder.create();
      alert.show();
    } else {
      FirebaseDatabase db = FirebaseDatabase.getInstance();
      String userNameAdded = edit_nickname.getText().toString().toLowerCase();
      //Log.d("USERNAME", "ADDED=" + userNameAdded);
      db.getReference("USERNAMES").child(userNameAdded).addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
          String username = dataSnapshot.getValue(String.class);
          //Log.d("USERNAME", "USERNAME=" + username);
          if (username!=null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CompleteRegistration.this);
            builder.setMessage(R.string.username_taken)
              .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  dialog.dismiss();
                }
              });
            AlertDialog alert = builder.create();
            alert.show();
          } else {
            allowregistration();
          }

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
      });
    }
  }

  public void allowregistration() {

    EditText edit_nickname = findViewById(R.id.complete_nickname);
    //AutoCompleteTextView edit_location = findViewById(R.id.complete_location);
    EditText edit_bio = findViewById(R.id.complete_biography);

    Profile P = new Profile();
    P.setName(U.getDisplayName());
    P.setEmail(U.getEmail());
    P.setBio(edit_bio.getText().toString());
    P.setUsername(edit_nickname.getText().toString());
    P.setLocation(location);
    P.setCoordinates(coordinates);
    P.setTotReviewsReceived(0);
    P.setTotScoringReviews(0f);
    //P.setToken(FirebaseInstanceId.getInstance().getToken());

    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference dbref = db.getReference("TOKENS").child(U.getUid());
    dbref.setValue(FirebaseInstanceId.getInstance().getToken());

    String username = P.getUsername().toLowerCase();
    dbref = db.getReference("USERNAMES").child(username);
    dbref.setValue(username);

    dbref = db.getReference("USERS").child(U.getUid());
    dbref.setValue(P).addOnSuccessListener(new OnSuccessListener<Void>() {
      @Override
      public void onSuccess(Void aVoid) {
        Intent intent = new Intent(CompleteRegistration.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
      }
    });

  }

  @Override
  public void onBackPressed() {
    U.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
      @Override
      public void onComplete(@NonNull Task<Void> task) {
        if (task.isSuccessful()) {
          Log.d(TAG, "User account deleted.");
        }
      }
    });
    super.onBackPressed();
  }

 /* @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    if(requestCode == Constants.FINE_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
      setuplocation();
    }
    else if(requestCode == Constants.COARSE_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
      setuplocation();
    }
    else if(requestCode == Constants.COARSE_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED){
      def=true;
      setuplocation();
    }
    else if(requestCode == Constants.FINE_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED){
      def=true;
      setuplocation();
    }
  }*/

}