package it.polito.mad.greit.project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class ShowSharedBooks extends AppCompatActivity {
  private Toolbar t;
  TextView twTitle;
  TextView twAuthor;
  TextView twPublisher;
  TextView twISBN;
  TextView twYear;
  ImageView iwCover;
  Fragment fr;
  ImageButton ib_map;
  ImageButton ib_list;
  TextView tw_map;
  TextView tw_list;
  
  Boolean onList;
  
  private Book book;
  
  private RecyclerView mResultList;
  private DatabaseReference mSharedBookDb;
  
  private FusedLocationProviderClient mFusedLocationClient;
  private static String currentLocation;
  private static double distanceKm;
  
  private SortedMap<String, Integer> distances;
  private ArrayList<Integer> positions;
  
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_show_shared_books);
    
    onList = true;
    
    t = findViewById(R.id.searched_sharedBooks_toolbar);
    t.setTitle("Books near you");
    setSupportActionBar(t);
    t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
    t.setNavigationOnClickListener(view -> onBackPressed());
    
    book = (Book) getIntent().getSerializableExtra("book");
    
    twTitle = (TextView) findViewById(R.id.bookCardTitle);
    twTitle.setText(book.getTitle());
    
    twAuthor = (TextView) findViewById(R.id.bookCardAuthor);
    String AS = android.text.TextUtils.join(", ", book.getAuthors().keySet());
    twAuthor.setText(AS);
    
    twPublisher = (TextView) findViewById(R.id.bookCardPublisher);
    if (!book.getPublisher().isEmpty())
      twPublisher.setVisibility(View.VISIBLE);
    twPublisher.setText(book.getPublisher());
    
    twISBN = (TextView) findViewById(R.id.bookCardISBN);
    twISBN.setText("ISBN: " + book.getISBN());
    
    
    twYear = (TextView) findViewById(R.id.bookCardYear);
    twYear.setText(book.getYear());
    
    iwCover = (ImageView) findViewById(R.id.bookCardCover);
    Glide.with(this)
        .load(book.getCover())
        .into(iwCover);
    
    mSharedBookDb = FirebaseDatabase.getInstance().getReference("SHARED_BOOKS");
    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    
    distances = new TreeMap<>();
    positions = new ArrayList<>();
    
    tw_list = (TextView) findViewById(R.id.list_text);
    tw_list.setOnClickListener(v -> {
      if (!onList) {
        onList = true;
        switchToList();
      }
    });
    
    tw_map = (TextView) findViewById(R.id.map_text);
    tw_map.setOnClickListener(v -> {
      if (onList) {
        onList = false;
        switchToMap();
      }
    });
    
    ib_list = (ImageButton) findViewById(R.id.list_icon);
    ib_list.setOnClickListener(v -> {
      if (!onList) {
        onList = true;
        switchToList();
      }
    });
    ib_map = (ImageButton) findViewById(R.id.map_icon);
    ib_map.setOnClickListener(v -> {
      if (onList) {
        onList = false;
        switchToMap();
      }
    });
    
    // Recupero posizione attuale
    if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Constants.FINE_LOCATION_PERMISSION);
    else
      mFusedLocationClient.getLastLocation()
          .addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
              // Got last known location. In some rare situations this can be null.
              if (location != null) {
                currentLocation = location.getLatitude() + ";" + location.getLongitude();
              } else {
                currentLocation = getIntent().getStringExtra("userLocation");
                Toast.makeText(ShowSharedBooks.this, "Location not found, profile location set.", Toast.LENGTH_SHORT).show();
              }
              switchToList();
            }
          });
  }
  
  @SuppressLint("MissingPermission")
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      mFusedLocationClient.getLastLocation()
          .addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
              // Got last known location. In some rare situations this can be null.
              if (location != null) {
                currentLocation = location.getLatitude() + ";" + location.getLongitude();
              } else {
                currentLocation = getIntent().getStringExtra("userLocation");
                Toast.makeText(ShowSharedBooks.this, "Location not available, profile location set.", Toast.LENGTH_SHORT).show();
              }
              switchToList();
            }
          });
    } else {
      currentLocation = getIntent().getStringExtra("userLocation");
      Toast.makeText(ShowSharedBooks.this, "Location not available, profile location set.", Toast.LENGTH_SHORT).show();
      switchToList();
    }
    return;
  }
  
  private void switchToList() {
    Bundle bundle = new Bundle();
    bundle.putString("isbn", book.getISBN());
    bundle.putString("location", currentLocation);
  
    fr = new SharedBooksOnList();
    fr.setArguments(bundle);
    FragmentManager fm = getFragmentManager();
    FragmentTransaction fragmentTransaction = fm.beginTransaction();
    fragmentTransaction.replace(R.id.fragment_container, fr);
    fragmentTransaction.commit();
  }
  
  private void switchToMap() {
    Bundle bundle = new Bundle();
    bundle.putString("isbn", book.getISBN());
    bundle.putString("location", currentLocation);
    
    fr = new SharedBooksOnMap();
    fr.setArguments(bundle);
    FragmentManager fm = getFragmentManager();
    FragmentTransaction fragmentTransaction = fm.beginTransaction();
    fragmentTransaction.replace(R.id.fragment_container, fr);
    fragmentTransaction.commit();
  }
  
}
