package it.polito.mad.greit.project;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CompleteRegistration extends AppCompatActivity {
  Button bb;
  FirebaseUser U;
  private static final String TAG = "CompleteRegistration";
  
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_complete_registration);
    
    U = FirebaseAuth.getInstance().getCurrentUser();
    
    bb = findViewById(R.id.complete_registration);
    bb.setOnClickListener(v -> registrationCompleted());
  }
  
  private void registrationCompleted() {
    EditText edit_nickname = findViewById(R.id.complete_nickname);
    EditText edit_location = findViewById(R.id.complete_location);
    EditText edit_bio = findViewById(R.id.complete_biography);

//    if (TextUtils.isEmpty(edit_nickname.getText()) || TextUtils.isEmpty(edit_location.getText())
//        || TextUtils.isEmpty(edit_bio.getText())) {
    if (TextUtils.isEmpty(edit_nickname.getText())) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage(R.string.fill_fields)
          .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              Log.d(TAG, "Dialog clicked");
            }
          });
      AlertDialog alert = builder.create();
      alert.show();
    } else {
      Profile P = new Profile();
      P.setName(U.getDisplayName());
      P.setEmail(U.getEmail());
      P.setBio(edit_bio.getText().toString());
      P.setUsername(edit_nickname.getText().toString());
      P.setLocation(edit_location.getText().toString());
      
      FirebaseDatabase db = FirebaseDatabase.getInstance();
      DatabaseReference dbref = db.getReference("USERS").child(U.getUid());
      dbref.setValue(P);
      
      Intent intent = new Intent(this, MainActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(intent);
    }
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
}
