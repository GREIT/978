package it.polito.mad.greit.project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.sax.StartElementListener;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
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
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.net.URI;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
  private static String TAG = "Main Activity";
  private Profile profile;
  private ImageView iw_user;
  private TextView tw_username;
  private TextView tw_name;
  private StorageReference sr;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    
    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add_book);
    fab.setOnClickListener(v -> {
      Intent I = new Intent(MainActivity.this, ShareNewBook.class);
      startActivity(I);
    });
  
    profile = new Profile();
  
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
  
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference dbref = db.getReference("USERS").child(user.getUid());
    
    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.addDrawerListener(toggle);
    toggle.syncState();
    
    NavigationView navigationView = findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);
    View headerView = navigationView.getHeaderView(0);
    
    tw_username = headerView.findViewById(R.id.drawer_username);
    tw_name = headerView.findViewById(R.id.drawer_name);
    iw_user = headerView.findViewById(R.id.drawer_image);

    iw_user.setOnClickListener(v -> {
      Intent intent = new Intent(MainActivity.this, ShowProfile.class);
      startActivity(intent);
    });
  
    dbref.addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        profile = dataSnapshot.getValue(Profile.class);
        if(profile == null){
          Intent intent = new Intent(MainActivity.this, SignInActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
          startActivity(intent);
        }
        else{
          tw_username.setText("@" + profile.getUsername());
          tw_name.setText(profile.getName());
                StorageReference sr = FirebaseStorage.getInstance().getReference()
                    .child("profile_pictures/" + user.getUid() + ".jpg");
                sr.getBytes(Constants.SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        // Data for "images/island.jpg" is returns, use this as needed
                        Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        iw_user.setImageBitmap(bm);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                        exception.printStackTrace();
                        iw_user.setImageResource(R.mipmap.ic_launcher_round);
                    }
                });

          Log.d("onDataChange", profile.getUsername());
        }
      }
      @Override
      public void onCancelled(DatabaseError e) {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(MainActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
      }
    });
  }
  
  @Override
  public void onBackPressed() {
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      Log.d(TAG, "Back button pressed");
    }
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    
    return super.onOptionsItemSelected(item);
  }
  
  @SuppressWarnings("StatementWithEmptyBody")
  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();
    
    if (id == R.id.nav_shared_books) {
      Intent intent = new Intent(MainActivity.this, SharedBooksByUserSplitted.class);
      startActivity(intent);
    } else if (id == R.id.nav_sign_out) {
      FirebaseAuth.getInstance().signOut();
      Intent intent = new Intent(MainActivity.this, SignInActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(intent);
      finish();
    }
    
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }
  
}
