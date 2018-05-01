package it.polito.mad.greit.project;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static String TAG = "Main Activity";
    private Profile profile;
    private ImageView iw_user;
    private TextView tw_username;
    private TextView tw_name;
    private StorageReference sr;

    // Search variables
    private EditText mSearchField;
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
                if(profile == null){
                    Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                else{
                    tw_username.setText("@" + profile.getUsername());
                    tw_name.setText(profile.getName());
                    File pic = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString(),"pic.jpg");
                    if(pic.exists()){
                        iw_user.setImageURI(Uri.fromFile(pic));
                    }
                    else {
                        StorageReference sr = FirebaseStorage.getInstance().getReference()
                                .child("profile_pictures/" + user.getUid() + ".jpg");
                        sr.getBytes(Constants.SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                try{
                                    File pic = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString(),"pic.jpg");
                                    Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    OutputStream outs = new FileOutputStream(pic);
                                    bm.compress(Bitmap.CompressFormat.JPEG, 85,outs);
                                    iw_user.setImageBitmap(bm);
                                    outs.close();
                                }catch (Exception e){
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
        mResultList.setHasFixedSize(true);
        mResultList.setLayoutManager(new LinearLayoutManager(this));

        mSearchField = (EditText) findViewById(R.id.search_field);
        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                bookSearch(editable.toString());
            }
        });
    }

    private void bookSearch(String searchText) {
        if (searchText.isEmpty()) {
            mResultList.setAdapter(null);
            return;
        }

        Query firebaseSearchQuery = mBookDb.orderByChild("title").startAt(searchText).endAt(searchText + "\uf8ff");
        FirebaseRecyclerAdapter<Book, BookViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Book, BookViewHolder>(
                Book.class,
                R.layout.book_item,
                BookViewHolder.class,
                firebaseSearchQuery
        ) {
            @Override
            protected void populateViewHolder(BookViewHolder viewHolder, Book model, int position) {
                viewHolder.setDetails(getApplicationContext(), model.getTitle(), model.getAuthors().get(0), model.getISBN());
            }

            @Override
            public BookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                BookViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
                viewHolder.setOnClickListener(new BookViewHolder.ClickListener() {
                    @Override
                    public void onItemClick(View view, String ISBN) {
                        hideKeyboard(MainActivity.this);

                        mSearchField.setText("");
                        mResultList.requestFocus();
                        mResultList.setAdapter(null);

                        bookExpand(ISBN);
                    }
                });
                return viewHolder;
            }
        };

        mResultList.setAdapter(firebaseRecyclerAdapter);
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        View mView;
        private BookViewHolder.ClickListener mClickListener;

        public BookViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public interface ClickListener{
            void onItemClick(View view, String ISBN);
        }

        public void setOnClickListener(BookViewHolder.ClickListener clickListener){
            mClickListener = clickListener;
        }

        public void setDetails(Context ctx, String bookTitle, String bookAuthor, String ISBN){
            TextView book_title = (TextView) mView.findViewById(R.id.book_card_title);
            TextView book_author = (TextView) mView.findViewById(R.id.book_card_author);

            book_title.setText(bookTitle);
            book_author.setText(bookAuthor);

            itemView.setOnClickListener(v -> {
                mClickListener.onItemClick(v, ISBN);
            });
        }
    }

    private void bookExpand(String ISBN) {
        Log.d("bookExpand", ISBN);

        Query firebaseSearchQuery = mSharedBookDb.orderByChild("ISBN").equalTo(ISBN);
        FirebaseRecyclerAdapter<SharedBook, SharedBookViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<SharedBook, SharedBookViewHolder>(
                SharedBook.class,
                R.layout.book_search_item,
                SharedBookViewHolder.class,
                firebaseSearchQuery
        ) {
            @Override
            protected void populateViewHolder(SharedBookViewHolder viewHolder, SharedBook model, int position) {
                viewHolder.setDetails(getApplicationContext(), model.getTitle(), model.getAuthors().get(0), "0000");
            }

            @Override
            public SharedBookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                SharedBookViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
                viewHolder.setOnClickListener(new SharedBookViewHolder.ClickListener() {
                    @Override
                    public void onItemClick(View view, String ISBN) {
                        Toast.makeText(MainActivity.this, "Item: " + ISBN, Toast.LENGTH_SHORT).show();
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

        public interface ClickListener{
            void onItemClick(View view, String title);
        }

        public void setOnClickListener(SharedBookViewHolder.ClickListener clickListener){
            mClickListener = clickListener;
        }

        public void setDetails(Context ctx, String bookTitle, String bookAuthor, String ISBN){
            TextView book_title = (TextView) mView.findViewById(R.id.book_card_title);
            TextView book_author = (TextView) mView.findViewById(R.id.book_card_author);

            book_title.setText(bookTitle);
            book_author.setText(bookAuthor);

            itemView.setOnClickListener(v -> {
                mClickListener.onItemClick(v, ISBN);
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
}
