package it.polito.mad.greit.project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class SearchedSharedBooks extends AppCompatActivity {
  private Toolbar t;
  TextView twTitle;
  TextView twAuthor;
  TextView twPublisher;
  TextView twISBN;
  TextView twYear;
  ImageView iwCover;
  
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
    setContentView(R.layout.activity_searched_shared_books);
    
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
    
    // Recupero posizione attuale
    if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
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
                Toast.makeText(SearchedSharedBooks.this, "Location not found, profile location set.", Toast.LENGTH_SHORT).show();
              }
              sharedBookShow(book.getISBN());
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
                Toast.makeText(SearchedSharedBooks.this, "Location not available, profile location set.", Toast.LENGTH_SHORT).show();
              }
              sharedBookShow(book.getISBN());
            }
          });
    } else {
      currentLocation = getIntent().getStringExtra("userLocation");
      Toast.makeText(SearchedSharedBooks.this, "Location not available, profile location set.", Toast.LENGTH_SHORT).show();
      sharedBookShow(book.getISBN());
    }
    return;
  }
  
  private void sharedBookShow(String ISBN) {
    mResultList = (RecyclerView) findViewById(R.id.shared_books_result_list);
    mResultList.setItemAnimator(new DefaultItemAnimator());
    mResultList.setLayoutManager(new GridLayoutManager(this, 2));
    mResultList.removeItemDecoration(mResultList.getItemDecorationAt(0));
    mResultList.addItemDecoration(new SearchedSharedBooks.GridSpacingItemDecoration(2, dpToPx(20), true));
    
    Query firebaseSearchQuery = mSharedBookDb.orderByChild("isbn").equalTo(ISBN);
    FirebaseRecyclerAdapter<SharedBook, SharedBookViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<SharedBook, SharedBookViewHolder>(
        SharedBook.class,
        R.layout.sharedbook_card,
        SharedBookViewHolder.class,
        firebaseSearchQuery
    ) {
      @Override
      protected void populateViewHolder(SharedBookViewHolder viewHolder, SharedBook model, int position) {
        viewHolder.setDetails(getApplicationContext(), model, getSupportFragmentManager());
      }
      
      @Override
      public SharedBook getItem(int position) {
        if (positions.isEmpty()) {
          DecimalFormat df = new DecimalFormat("00000.00");
          
          for (int i = 1; i <= getItemCount(); i++)
            distances.put(df.format(Utils.calcDistance(mSnapshots.getObject(i - 1).getCoordinates(), currentLocation) / 1000) + mSnapshots.getObject(i - 1).getKey(), i - 1);
          for (Map.Entry<String, Integer> entry : distances.entrySet())
            positions.add(entry.getValue());
        }
        return (SharedBook) mSnapshots.getObject(positions.get(position));
      }
      
    };
    
    mResultList.setAdapter(firebaseRecyclerAdapter);
  }
  
  public static class SharedBookViewHolder extends RecyclerView.ViewHolder {
    View mView;
    
    public SharedBookViewHolder(View itemView) {
      super(itemView);
      mView = itemView;
    }
    
    public void setDetails(Context ctx, SharedBook model, android.support.v4.app.FragmentManager fm) {
      RelativeLayout rightBar = (RelativeLayout) mView.findViewById(R.id.right_bar);
      ImageView bookImage = (ImageView) mView.findViewById(R.id.shared_book_card_thumbnail);
      ImageView contactForLoan = (ImageView) mView.findViewById(R.id.shared_book_card_icon1);
      ImageView ownerInfo = (ImageView) mView.findViewById(R.id.shared_book_card_icon2);
      ImageView distance = (ImageView) mView.findViewById(R.id.shared_book_card_icon3);
      TextView bookTitle = (TextView) mView.findViewById(R.id.shared_book_card_title);
      
      bookTitle.setText(model.getTitle());
      bookTitle.setOnClickListener(v -> {
        Bundle bundle = new Bundle();
        bundle.putSerializable("book", model);
        bundle.putSerializable("currentLocation", currentLocation);
        
        SharedBookDetailFragment dialogFragment = new SharedBookDetailFragment();
        dialogFragment.setArguments(bundle);
        dialogFragment.show(fm, "dialog");
      });
      
      bookImage.setOnClickListener(v -> {
        Bundle bundle = new Bundle();
        bundle.putSerializable("book", model);
        bundle.putSerializable("currentLocation", currentLocation);
        
        SharedBookDetailFragment dialogFragment = new SharedBookDetailFragment();
        dialogFragment.setArguments(bundle);
        dialogFragment.show(fm, "dialog");
      });
      
      if (model.getOwnerUsername().equals(ctx.getSharedPreferences("sharedpref", Context.MODE_PRIVATE).getString("username", null))) {
        // It is my book
        if (model.getShared() == true) {
          // Book is currently on loan
          rightBar.setBackgroundColor(ContextCompat.getColor(mView.getContext(), R.color.unavailable));
          contactForLoan.setImageResource(R.drawable.ic_delete_transparent_48dp);
          contactForLoan.setOnClickListener(v -> Toast.makeText(ctx, "You can't delete a book currently on loan!", Toast.LENGTH_SHORT).show());
        } else {
          contactForLoan.setImageResource(R.drawable.ic_delete_white_48dp);
          contactForLoan.setOnClickListener(v -> {
            new AlertDialog.Builder(itemView.getContext())
                .setTitle("Confirmation needed")
                .setMessage("Do you really want to delete this book?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                  
                  public void onClick(DialogInterface dialog, int whichButton) {
                    FirebaseDatabase db = FirebaseDatabase.getInstance();
                    DatabaseReference dbref = db.getReference("SHARED_BOOKS/" + model.getKey());
                    
                    dbref.removeValue();
                    
                    dbref = db.getReference("BOOKS");
                    
                    dbref.orderByKey().equalTo(model.getISBN()).addListenerForSingleValueEvent(new ValueEventListener() {
                      @Override
                      public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                          Book tmpBook = ds.getValue(Book.class);
                          if (tmpBook.getBooksOnLoan() == 1) {
                            DatabaseReference tmpDbRef = db.getReference("BOOKS/" + model.getISBN());
                            tmpDbRef.removeValue();
                          } else {
                            DatabaseReference tmpDbRef = db.getReference("BOOKS/" + model.getISBN());
                            tmpDbRef.child("booksOnLoan").setValue(Integer.valueOf(tmpBook.getBooksOnLoan()) - 1);
                            Toast.makeText(ctx, "Book removed from your collection", Toast.LENGTH_SHORT).show();
                          }
                        }
                      }
                      
                      @Override
                      public void onCancelled(DatabaseError databaseError) {
                      
                      }
                    });
                    
                    
                  }
                })
                .setNegativeButton(android.R.string.no, null).show();
          });
        }
      } else {
        // Not my book
        if (model.getShared() == true) {
          // Book is currently on loan
          rightBar.setBackgroundColor(ContextCompat.getColor(mView.getContext(), R.color.unavailable));
          contactForLoan.setImageResource(R.drawable.ic_textsms_transparent_48dp);
          contactForLoan.setOnClickListener(v -> Toast.makeText(ctx, "The book is currently on loan!", Toast.LENGTH_SHORT).show());
        } else {
          contactForLoan.setImageResource(R.drawable.ic_textsms_white_48dp);
          contactForLoan.setOnClickListener(v -> Chat.openchat(ctx, model));
        }
      }
      
      
      distanceKm = Utils.calcDistance(model.getCoordinates(), currentLocation) / 1000;
      if (distanceKm > 20)
        distance.setImageResource(R.mipmap.ic_maggiore_20);
      else if (distanceKm < 20 && distanceKm > 5)
        distance.setImageResource(R.mipmap.ic_minore_20);
      else
        distance.setImageResource(R.mipmap.ic_minore_5);
      
      ownerInfo.setImageResource(R.drawable.ic_person_white_48dp);
      ownerInfo.setOnClickListener(v ->
      
      {
        Intent I = new Intent(ctx, OtherProfile.class);
        I.putExtra("uid", model.getOwnerUid());
        ctx.startActivity(I);
      });
      
      
      StorageReference sr = FirebaseStorage.getInstance().getReference().child("shared_books_pictures/" + model.getKey() + ".jpg");
      
      bookImage.setImageResource(R.drawable.ic_book_blue_grey_900_48dp);
      
      sr.getDownloadUrl().
          
          addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
              try {
                Glide.with(ctx)
                    .load(uri)
                    .into(bookImage);
              } catch (Exception e) {
                e.printStackTrace();
              }

          /*Picasso.get()
                  .load(uri)
                  .error(R.drawable.ic_book_blue_grey_900_48dp)
                  .into(bookImage);*/
            }
          });
      
    }
  }
  
  public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
    
    private int spanCount;
    private int spacing;
    private boolean includeEdge;
    
    public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
      this.spanCount = spanCount;
      this.spacing = spacing;
      this.includeEdge = includeEdge;
    }
    
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
      int position = parent.getChildAdapterPosition(view); // item position
      int column = position % spanCount; // item column
      
      if (includeEdge) {
        outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
        outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)
        
        if (position < spanCount) { // top edge
          outRect.top = spacing;
        }
        outRect.bottom = spacing; // item bottom
      } else {
        outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
        outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
        if (position >= spanCount) {
          outRect.top = spacing; // item top
        }
      }
    }
  }
  
  private int dpToPx(int dp) {
    Resources r = getResources();
    return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
  }
  
}
