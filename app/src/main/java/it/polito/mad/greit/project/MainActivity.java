package it.polito.mad.greit.project;

import android.content.Intent;
import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener {
  private static String TAG = "Main Activity";
  private Profile P;
  private ImageView iw_user;
  private TextView tw_username;
  private TextView tw_name;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    
    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add_book);
    fab.setOnClickListener(v -> Toast.makeText(MainActivity.this, "Share a book",
        Toast.LENGTH_SHORT).show());
  
    P = new Profile();
  
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
  
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference dbref = db.getReference("users").child(user.getUid());
    
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.addDrawerListener(toggle);
    toggle.syncState();
    
    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);
    View headerView = navigationView.getHeaderView(0);
    
    tw_username = (TextView) headerView.findViewById(R.id.drawer_username);
    tw_name = (TextView) headerView.findViewById(R.id.drawer_name);
    iw_user = (ImageView) headerView.findViewById(R.id.drawer_image);
    
    iw_user.setOnClickListener(v -> {
      Intent I = new Intent(MainActivity.this, ShowProfile.class);
      startActivity(I);
    });
  
    dbref.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        P = dataSnapshot.getValue(Profile.class);
        tw_username.setText("@" + P.getUsername());
        tw_name.setText(P.getName());
        // Set also the profilePic
        Log.d("onDataChange", P.getUsername());
      }
      @Override
      public void onCancelled(DatabaseError e) {}
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
      Toast.makeText(MainActivity.this, "View your shared books",
          Toast.LENGTH_SHORT).show();
    } else if (id == R.id.nav_sign_out) {
      FirebaseAuth.getInstance().signOut();
      Intent intent = new Intent(MainActivity.this, SignInActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(intent);
    }
    
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }
}
