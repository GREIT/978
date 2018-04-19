package it.polito.mad.greit.project;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class MainEmptyActivity extends AppCompatActivity {
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    FirebaseDatabase.getInstance().setPersistenceEnabled(true);
  
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    Intent intent;
  
    if (user == null) {
      intent = new Intent(this, SignInActivity.class);
    } else {
      intent = new Intent(this, MainActivity.class);
    }

    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }
}
