package it.polito.mad.greit.project;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
  private static String TAG = "Main Activity";
  private static Profile profile;
  private ImageView iw_user;
  private TextView tw_username;
  private TextView tw_name;
  private TextView tw_searchText;
  private TextView tw_searchMain;
  private FirebaseUser user;
  private ImageButton searchButton;
  
  // Search variables
  private RecyclerView mResultList;
  private FirebaseRecyclerAdapter<Book, BookViewHolder> firebaseRecyclerAdapter;
  private DatabaseReference mBookDb, mSharedBookDb;
  private Button mSearchButton;
  
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    
    profile = new Profile();
    
    user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference dbref = db.getReference("USERS").child(user.getUid());
    
    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.addDrawerListener(toggle);
    toggle.syncState();
    
    tw_searchMain = (TextView) findViewById(R.id.main_title_search);
    
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
        if (profile == null) {
          Intent intent = new Intent(MainActivity.this, SignInActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
          startActivity(intent);
        } else {
          saveUsername(profile.getUsername());
          tw_username.setText("@" + profile.getUsername());
          tw_name.setText(profile.getName());
          File pic = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString(), "pic.jpg");
          if (pic.exists()) {
            iw_user.setImageURI(Uri.fromFile(pic));
          } else {
            StorageReference sr = FirebaseStorage.getInstance().getReference()
                .child("profile_pictures/" + user.getUid() + ".jpg");
            sr.getBytes(Constants.SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
              @Override
              public void onSuccess(byte[] bytes) {
                try {
                  Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                  OutputStream outs = new FileOutputStream(pic);
                  bm.compress(Bitmap.CompressFormat.JPEG, 100, outs);
                  iw_user.setImageBitmap(bm);
                  outs.close();
                } catch (Exception e) {
                  e.printStackTrace();
                  pic.delete();
                  iw_user.setImageResource(R.mipmap.ic_launcher_round);
                }
              }
            }).addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                exception.printStackTrace();
                iw_user.setImageResource(R.mipmap.ic_launcher_round);
              }
            });
          }
          
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
    
    mBookDb = FirebaseDatabase.getInstance().getReference("BOOKS");
    mSharedBookDb = FirebaseDatabase.getInstance().getReference("SHARED_BOOKS");
    mResultList = (RecyclerView) findViewById(R.id.result_list);
    mResultList.setItemAnimator(new DefaultItemAnimator());
    
    setupSearchBox("title");
    startupRecycleView();
  }
  
  
  private void setupSearchBox(String field) {
    mSearchButton = (Button) findViewById(R.id.search_button);
    mSearchButton.setText(field.toUpperCase() + "▼");
    mSearchButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        chooseSearchField();
      }
    });
    
    //Create a new ArrayAdapter with your context and the simple layout for the dropdown menu provided by Android
    final ArrayAdapter<String> autoComplete = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    List<String> tmpAutoComplete = new LinkedList<>();
    
    DatabaseReference database = FirebaseDatabase.getInstance().getReference();
    database.child("BOOKS").addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        autoComplete.clear();
        
        for (DataSnapshot suggestionSnapshot : dataSnapshot.getChildren()) {
          if (field.equals("authors") || field.equals("tags")) {
            for (DataSnapshot A : suggestionSnapshot.child(field).getChildren()) {
              if (!tmpAutoComplete.contains(A.getKey())) {
                tmpAutoComplete.add(A.getKey());
              }
            }
          } else {
            String suggestion = suggestionSnapshot.child(field).getValue(String.class);
            if (!tmpAutoComplete.contains(suggestion)) {
              tmpAutoComplete.add(suggestion);
            }
          }
        }
        autoComplete.addAll(tmpAutoComplete);
      }
      
      @Override
      public void onCancelled(DatabaseError databaseError) {
      }
    });
    
    AutoCompleteTextView ACTV = (AutoCompleteTextView) findViewById(R.id.search_field);
    ACTV.setAdapter(autoComplete);
    ACTV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //hideKeyboard(MainActivity.this);
        mResultList.requestFocus();
        ACTV.setText(autoComplete.getItem(position));
      }
    });
    ACTV.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        
        if ((actionId == EditorInfo.IME_ACTION_DONE)) {
          hideKeyboard(MainActivity.this);
          String textToSearch = ACTV.getText().toString();
          ACTV.setText("");
          bookSearch(field, textToSearch);
          return true;
        }
        return false;
      }
    });
    
    searchButton = findViewById(R.id.search_button_main);
    searchButton.setOnClickListener(v -> {
          String textToSearch = ACTV.getText().toString();
          ACTV.setText("");
          bookSearch(field, textToSearch);
        }
    );
  }
  
  public void chooseSearchField() {
    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
    builder.setTitle("Choose search field:");
    
    String title = getResources().getString(R.string.item_title);
    String author = getResources().getString(R.string.item_author);
    String isbn = getResources().getString(R.string.item_isbn);
    String year = getResources().getString(R.string.item_year);
    String tag = getResources().getString(R.string.item_tag);
    
    //list of items
    String[] items = {title, author, isbn, year, tag};
    final int[] choice = new int[1];
    
    builder.setSingleChoiceItems(items, 0,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            choice[0] = which;
          }
        });
    
    String positiveText = getString(android.R.string.ok);
    builder.setPositiveButton(positiveText,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            String finalField = "title";
            switch (choice[0]) {
              case 0:
                finalField = "title";
                break;
              
              case 1:
                finalField = "authors";
                break;
              
              case 2:
                finalField = "isbn";
                break;
              
              case 3:
                finalField = "year";
                break;
              
              case 4:
                finalField = "tags";
                break;
            }
            
            AutoCompleteTextView ACTV = (AutoCompleteTextView) findViewById(R.id.search_field);
            ACTV.setText("");
            setupSearchBox(finalField);
          }
        });
    
    String negativeText = getString(android.R.string.cancel);
    builder.setNegativeButton(negativeText,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
          }
        });
    
    AlertDialog dialog = builder.create();
    dialog.show();
  }
  
  
  private void startupRecycleView() {
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setReverseLayout(true);
    layoutManager.setStackFromEnd(true);
    mResultList.setLayoutManager(layoutManager);
    mResultList.removeItemDecoration(mResultList.getItemDecorationAt(0));
    mResultList.addItemDecoration(new MainActivity.GridSpacingItemDecoration(1, dpToPx(10), true));
    
    tw_searchMain.setText(R.string.main_title_search_1);
    
    Query firebaseSearchQuery = mBookDb.orderByChild("lentBooks").limitToLast(8);
    firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Book, BookViewHolder>(
        Book.class,
        R.layout.book_card,
        BookViewHolder.class,
        firebaseSearchQuery
    ) {
      @Override
      protected void populateViewHolder(BookViewHolder viewHolder, Book model, int position) {
        viewHolder.setDetails(getApplicationContext(), model);
      }
      
      @Override
      public BookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BookViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
        return viewHolder;
      }
      
    };
    
    
    mResultList.setAdapter(firebaseRecyclerAdapter);
  }
  
  private void bookSearch(String field, String value) {
    mResultList.setLayoutManager(new GridLayoutManager(this, 1));
    mResultList.removeItemDecoration(mResultList.getItemDecorationAt(0));
    mResultList.addItemDecoration(new MainActivity.GridSpacingItemDecoration(1, dpToPx(10), true));
    
    tw_searchMain.setText(R.string.main_title_search_2);
    
    if (value.isEmpty()) {
      mResultList.setAdapter(null);
      return;
    }
    
    Query firebaseSearchQuery;
    if (field.equals("authors") || field.equals("tags"))
      firebaseSearchQuery = mBookDb.orderByChild(field + "/" + value).equalTo(value);
    else
      firebaseSearchQuery = mBookDb.orderByChild(field).equalTo(value);
    
    firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Book, BookViewHolder>(
        Book.class,
        R.layout.book_card,
        BookViewHolder.class,
        firebaseSearchQuery
    ) {
      @Override
      protected void populateViewHolder(BookViewHolder viewHolder, Book model, int position) {
        viewHolder.setDetails(getApplicationContext(), model);
      }
      
      @Override
      public BookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BookViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
        
        return viewHolder;
      }
  
      @Override
      public void onDataChanged() {
        super.onDataChanged();
        if (getItemCount() == 0) {
          AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
          builder.setMessage("No book found 😔")
              .setCancelable(false)
              .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  //do things
                }
              });
          AlertDialog alert = builder.create();
          alert.show();
        }
      }
    };
    
    mResultList.setAdapter(firebaseRecyclerAdapter);
  }
  
  public static class BookViewHolder extends RecyclerView.ViewHolder {
    View mView;
    
    public BookViewHolder(View itemView) {
      super(itemView);
      mView = itemView;
    }
    
    public void setDetails(Context ctx, Book model) {
      TextView twTitle = (TextView) mView.findViewById(R.id.bookCardTitle);
      TextView twAuthor = (TextView) mView.findViewById(R.id.bookCardAuthor);
      TextView twPublisher = (TextView) mView.findViewById(R.id.bookCardPublisher);
      TextView twISBN = (TextView) mView.findViewById(R.id.bookCardISBN);
      TextView twYear = (TextView) mView.findViewById(R.id.bookCardYear);
      ImageView iwCover = (ImageView) mView.findViewById(R.id.bookCardCover);
      TextView twCopies = (TextView) mView.findViewById(R.id.bookCardCopies);
      
      itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Intent I = new Intent(itemView.getContext(),
              ShowSharedBooks.class);
          I.putExtra("book", model);
          I.putExtra("userLocation", profile.getCoordinates());
          itemView.getContext().startActivity(I);
        }
      });
      
      String AS = android.text.TextUtils.join(", ", model.getAuthors().keySet());
      twAuthor.setText(AS);
      twTitle.setText(model.getTitle());
      if (!model.getPublisher().isEmpty())
        twPublisher.setVisibility(View.VISIBLE);
      twPublisher.setText(model.getPublisher());
      twYear.setText(model.getYear());
      Glide.with(ctx)
          .load(model.getCover())
          .into(iwCover);
      
      if (model.getBooksOnLoan() == 1) twCopies.setText(model.getBooksOnLoan() + " COPY AVAILABLE");
      else twCopies.setText(model.getBooksOnLoan() + " COPIES AVAILABLE");
    }
  }
  
  @Override
  public void onBackPressed() {
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      Log.d(TAG, "Back button pressed");
      super.onBackPressed();
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
    
    if (id == R.id.nav_my_books) {
      Intent intent = new Intent(MainActivity.this, SharedBooksByUser.class);
      startActivity(intent);
    } else if (id == R.id.nav_add_book) {
      Intent intent = new Intent(MainActivity.this, ShareNewBook.class);
      startActivity(intent);
    } else if (id == R.id.nav_chat) {
      Intent intent = new Intent(MainActivity.this, InboxActivity.class);
      startActivity(intent);
    } else if (id == R.id.nav_my_history) {
      Intent intent = new Intent(MainActivity.this, UserHistory.class);
      intent.putExtra("uid", user.getUid());
      startActivity(intent);
    } else if (id == R.id.nav_my_reviews) {
      Intent intent = new Intent(MainActivity.this, ReceivedReviewsActivity.class);
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
  
  private void saveUsername(String s) {
    SharedPreferences sharedref = getApplicationContext().getSharedPreferences("sharedpref", MODE_PRIVATE);
    if (sharedref.getString("username", null) == null) {
      SharedPreferences.Editor editor = sharedref.edit();
      editor.putString("username", s);
      editor.commit();
    }
  }
  
  @Override
  protected void onDestroy() {
    super.onDestroy();
    firebaseRecyclerAdapter.cleanup();
  }
}
