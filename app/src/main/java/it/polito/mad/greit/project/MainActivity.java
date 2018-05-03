package it.polito.mad.greit.project;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.TypedValue;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
  private static String TAG = "Main Activity";
  private Profile profile;
  private ImageView iw_user;
  private TextView tw_username;
  private TextView tw_name;
  private TextView tw_searchText;
  
  // Search variables
  //private EditText mSearchField;
  private RecyclerView mResultList;
  private DatabaseReference mBookDb, mSharedBookDb;
  
  
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
        if (profile == null) {
          Intent intent = new Intent(MainActivity.this, SignInActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
          startActivity(intent);
        } else {
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
                  File pic = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString(), "pic.jpg");
                  Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                  OutputStream outs = new FileOutputStream(pic);
                  bm.compress(Bitmap.CompressFormat.JPEG, 85, outs);
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
    
  }
  
  private void setupSearchBox(String field) {
    String fixedString = "Search books by ";
    
    SpannableString ss = new SpannableString(fixedString + field.toUpperCase());
    ClickableSpan clickableSpan = new ClickableSpan() {
      @Override
      public void onClick(View textView) {
        chooseSearchField();
      }
    };
    ss.setSpan(clickableSpan, fixedString.length(), fixedString.length() + field.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    ss.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)),
        fixedString.length(), fixedString.length() + field.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    
    tw_searchText = (TextView) findViewById(R.id.search_text_main);
    tw_searchText.setText(ss);
    tw_searchText.setMovementMethod(LinkMovementMethod.getInstance());
    
    //Create a new ArrayAdapter with your context and the simple layout for the dropdown menu provided by Android
    final ArrayAdapter<String> autoComplete = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    
    DatabaseReference database = FirebaseDatabase.getInstance().getReference();
    database.child("BOOKS").addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        autoComplete.clear();
        //Basically, this says "For each DataSnapshot *Data* in dataSnapshot, do what's inside the method.
        for (DataSnapshot suggestionSnapshot : dataSnapshot.getChildren()) {
          //Get the suggestion by childing the key of the string you want to get.
          if (field.equals("authors")) {
            for (DataSnapshot A : suggestionSnapshot.child(field).getChildren()) {
              autoComplete.add(A.getKey());
            }
          } else {
            String suggestion = suggestionSnapshot.child(field).getValue(String.class);
            autoComplete.add(suggestion);
          }
        }
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
        hideKeyboard(MainActivity.this);
        bookExpand(field, autoComplete.getItem(position));
      }
    });
  }
  
  public void chooseSearchField() {
    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
    builder.setTitle("Choose search field:");
    
    //list of items
    String[] items = {"Title", "Author", "ISBN", "Year", "TAGs"};
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
            
            setupSearchBox(finalField);
          }
        });
    
    String negativeText = getString(android.R.string.cancel);
    builder.setNegativeButton(negativeText,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            // negative button logic
          }
        });
    
    AlertDialog dialog = builder.create();
    // display dialog
    dialog.show();
  }
  
  public static class BookViewHolder extends RecyclerView.ViewHolder {
    View mView;
    private BookViewHolder.ClickListener mClickListener;
    
    public BookViewHolder(View itemView) {
      super(itemView);
      mView = itemView;
    }
    
    public interface ClickListener {
      void onItemClick(View view, String ISBN);
    }
    
    public void setOnClickListener(BookViewHolder.ClickListener clickListener) {
      mClickListener = clickListener;
    }
    
    public void setDetails(Context ctx, String bookTitle, String bookAuthor, String ISBN) {
      TextView book_title = (TextView) mView.findViewById(R.id.book_card_title);
      TextView book_author = (TextView) mView.findViewById(R.id.book_card_author);
      
      book_title.setText(bookTitle);
      book_author.setText(bookAuthor);
      
      itemView.setOnClickListener(v -> {
        mClickListener.onItemClick(v, ISBN);
      });
    }
  }
  
  private void bookExpand(String field, String value) {
    mResultList.setLayoutManager(new GridLayoutManager(this, 2));
    mResultList.addItemDecoration(new MainActivity.GridSpacingItemDecoration(2, dpToPx(10), true));
  
    Query firebaseSearchQuery;
    
    if (field.equals("authors")) {
      firebaseSearchQuery = mSharedBookDb.orderByChild(field + "/" + value).equalTo(value);
    } else {
      firebaseSearchQuery = mSharedBookDb.orderByChild(field).equalTo(value);
    }
    FirebaseRecyclerAdapter<SharedBook, SharedBookViewHolder> firebaseRecyclerAdapter =
        new FirebaseRecyclerAdapter<SharedBook, SharedBookViewHolder>(
        SharedBook.class,
        R.layout.book_card,
        SharedBookViewHolder.class,
        firebaseSearchQuery
    ) {
      @Override
      protected void populateViewHolder(SharedBookViewHolder viewHolder, SharedBook model, int position) {
        viewHolder.setDetails(getApplicationContext(), model.getTitle(), model.getAuthors().get(0), model.getKey());
      }
      
      @Override
      public SharedBookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SharedBookViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
        viewHolder.setOnClickListener(new SharedBookViewHolder.ClickListener() {
          @Override
          public void onItemClick(View view) {
            Toast.makeText(MainActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
          }
        });
        return viewHolder;
      }
    };
    
    mResultList.setAdapter(firebaseRecyclerAdapter);
  }
  
  public static class SharedBookViewHolder extends RecyclerView.ViewHolder {
    View mView;
    private SharedBookViewHolder.ClickListener mClickListener;
    
    public SharedBookViewHolder(View itemView) {
      super(itemView);
      mView = itemView;
    }
    
    public interface ClickListener {
      void onItemClick(View view);
    }
    
    public void setOnClickListener(SharedBookViewHolder.ClickListener clickListener) {
      mClickListener = clickListener;
    }
    
    public void setDetails(Context ctx, String bookTitle, String bookAuthor, String bookKey) {
      TextView book_title = (TextView) mView.findViewById(R.id.book_card_title);
      TextView book_author = (TextView) mView.findViewById(R.id.book_card_author);
      ImageView book_image = (ImageView) mView.findViewById(R.id.book_card_thumbnail);
      
      book_title.setText(bookTitle);
      book_author.setText(bookAuthor);
      
      StorageReference sr = FirebaseStorage.getInstance().getReference().child("shared_books_pictures/" + bookKey + ".jpg");
      sr.getBytes(5 * Constants.SIZE).addOnSuccessListener(bytes -> {
        Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        Glide.with(ctx)
            .asBitmap()
            .load(stream.toByteArray())
            .into(book_image);
      }).addOnFailureListener(e ->
          Glide.with(ctx)
              .asBitmap()
              .load(R.drawable.ic_book_blue_grey_900_48dp)
              .into(book_image)
      );
      
      itemView.setOnClickListener(v -> {
        mClickListener.onItemClick(v);
      });
    }
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
}
