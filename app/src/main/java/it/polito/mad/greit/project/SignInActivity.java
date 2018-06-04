package it.polito.mad.greit.project;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class SignInActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{
  private static final String TAG = "SignIn Activity";
  private static final int RC_SIGN_IN = 9001;
  
  private FirebaseAuth mAuth;
  SignInButton signInButton;
  GoogleApiClient mGoogleApiClient;
  
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.activity_login);
    
    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(getString(R.string.AUTH_ID))
        .requestEmail()
        .build();
    
    mGoogleApiClient = new GoogleApiClient.Builder(this)
        .enableAutoManage(this, this)
        .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
        .build();
    
    signInButton = (SignInButton) findViewById(R.id.sign_in_button);
    signInButton.setSize(SignInButton.SIZE_WIDE);
    signInButton.setOnClickListener(v -> signIn());
  
    mAuth = FirebaseAuth.getInstance();
  
  }
  
  private void signIn() {
    Log.d(TAG, "Sign in w/ Google");

    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
    startActivityForResult(signInIntent, RC_SIGN_IN);
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    
    if (requestCode == RC_SIGN_IN) {
      Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
      try {
        GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
        handleSignInResult(result);
      } catch (Exception e) {
        // Google Sign In failed, update UI appropriately
        Log.w(TAG, "Google sign in failed", e);
        // ...
      }
  
    }
  }
  
  private void handleSignInResult(GoogleSignInResult result) {
    Log.d(TAG, "handleSignInResult:" + result.isSuccess());
    if (result.isSuccess()) {
      GoogleSignInAccount acct = result.getSignInAccount();
      firebaseAuthWithGoogle(acct);
    } else {
      Toast.makeText(SignInActivity.this, "Authentication failed.",
          Toast.LENGTH_SHORT).show();
    }
  }
  
  private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
    Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
    
    AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
    
    mAuth.signInWithCredential(credential)
        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
          @Override
          public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
              // Sign in success, update UI with the signed-in user's information
              Log.d(TAG, "signInWithCredential:success");
              
              FirebaseUser U = mAuth.getCurrentUser();
              
              if (task.getResult().getAdditionalUserInfo().isNewUser()) {
                Intent intent = new Intent(SignInActivity.this, CompleteRegistration.class);
                startActivity(intent);
              } else {
                DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("TOKENS").child(U.getUid());
                String token = FirebaseInstanceId.getInstance().getToken();
                dbref.setValue(token);
                Log.d("MyFirebaseIIDService", "onCreate: sent token " + token);
                Intent I = new Intent(SignInActivity.this, MainActivity.class);
                I.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(I);
              }
              
            } else {
              // If sign in fails, display a message to the user.
              Log.w(TAG, "signInWithCredential:failure", task.getException());
              //Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
            }
          }
        });
  }
  
  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.d(TAG, "onConnectionFailed:" + connectionResult);
  }
  
  @Override
  public void onBackPressed() {
    Log.d(TAG, "Back button pressed");
  }
}
