package it.polito.mad.greit.project;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EventListener;

public class SharedBooksByUserSplitted extends AppCompatActivity {
  
  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link FragmentPagerAdapter} derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * {@link android.support.v4.app.FragmentStatePagerAdapter}.
   */
  private SectionsPagerAdapter mSectionsPagerAdapter;
  
  /**
   * The {@link ViewPager} that will host the section contents.
   */
  private ViewPager mViewPager;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_shared_books_by_user_splitted);
    
    Toolbar toolbar = (Toolbar) findViewById(R.id.shared_books_by_user_split_toolbar);
    toolbar.setTitle(R.string.activity_shared_books);
    setSupportActionBar(toolbar);
    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
    toolbar.setNavigationOnClickListener(v -> onBackPressed());
    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
    
    // Set up the ViewPager with the sections adapter.
    mViewPager = (ViewPager) findViewById(R.id.shared_books_by_user_split_container);
    mViewPager.setAdapter(mSectionsPagerAdapter);
    
    TabLayout tabLayout = (TabLayout) findViewById(R.id.shared_books_by_user_split_tabs);
    
    mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
    tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
    
    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add_book_from_shared_books_split);
    fab.setOnClickListener(v -> {
      Intent I = new Intent(SharedBooksByUserSplitted.this, ShareNewBook.class);
      startActivity(I);
    });
    
  }
  
  /*
  public void onBackPressed() {
    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
  }
  */
  @Override
  protected void onStop() {
    super.onStop();
    //mSectionsPagerAdapter.closeConnection();
  }
  
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    //getMenuInflater().inflate(R.menu.menu_shared_books_by_user_splitted, menu);
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
  
  /**
   * A placeholder fragment containing a simple view.
   */
  public static class PlaceholderFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String DISCRMINATOR_STRING = "discriminator";
    private RecyclerView recView;
    private ArrayList<SharedBook> bookList;
    private SharedBooksAdapter adapter;
    private String discriminator;
    private RecyclerView mResultList;
    private DatabaseReference mSharedBookDb;
    private DatabaseReference dbref;
    private ChildEventListener ev;
    
    public PlaceholderFragment() {
      super();
    }
    
    public static PlaceholderFragment newInstance(String string) {
      PlaceholderFragment placeholder = new PlaceholderFragment();
      Bundle args = new Bundle();
      args.putString(DISCRMINATOR_STRING, string);
      placeholder.setArguments(args);
      return placeholder;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
      
      View rootView = inflater.inflate(R.layout.fragment_shared_books_by_user_splitted, container, false);
      mSharedBookDb = FirebaseDatabase.getInstance().getReference("SHARED_BOOKS");
      mResultList = (RecyclerView) rootView.findViewById(R.id.shared_books_by_user_split_recycler_view);
      mResultList.setItemAnimator(new DefaultItemAnimator());
      
      bookShow();
      return rootView;
    }
    
    private void bookShow() {
      mResultList.setLayoutManager(new GridLayoutManager(getActivity(), 2));
      mResultList.removeItemDecoration(mResultList.getItemDecorationAt(0));
      mResultList.addItemDecoration(new SharedBooksByUserSplitted.PlaceholderFragment.GridSpacingItemDecoration(2, dpToPx(20), true));
      
      FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
      
      discriminator = getArguments().getString(DISCRMINATOR_STRING);
      Query firebaseSearchQuery = mSharedBookDb.orderByChild(discriminator).equalTo(user.getUid());
      FirebaseRecyclerAdapter<SharedBook, SharedBooksByUserSplitted.PlaceholderFragment.SharedBookViewHolder>
          firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<SharedBook, SharedBooksByUserSplitted.PlaceholderFragment.SharedBookViewHolder>(
          SharedBook.class,
          R.layout.sharedbook_card,
          SharedBooksByUserSplitted.PlaceholderFragment.SharedBookViewHolder.class,
          firebaseSearchQuery
      ) {
        @Override
        protected void populateViewHolder(SharedBooksByUserSplitted.PlaceholderFragment.SharedBookViewHolder viewHolder, SharedBook model, int position) {
          viewHolder.setDetails(getActivity(), model, discriminator);
        }
        
        @Override
        public PlaceholderFragment.SharedBookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
          PlaceholderFragment.SharedBookViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
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
        void onItemClick(View view, SharedBook model);
      }
      
      public void setOnClickListener(SharedBookViewHolder.ClickListener clickListener) {
        mClickListener = clickListener;
      }
      
      
      public void setDetails(Context ctx, SharedBook model, String discriminator) {
        //TextView book_owner = (TextView) mView.findViewById(R.id.shared_book_card_owner);
        ImageView book_image = (ImageView) mView.findViewById(R.id.shared_book_card_thumbnail);
        ImageView iw1 = (ImageView) mView.findViewById(R.id.shared_book_card_icon1);
        ImageView iw2 = (ImageView) mView.findViewById(R.id.shared_book_card_icon2);
        TextView bookTitle = (TextView) mView.findViewById(R.id.shared_book_card_title);
        
        bookTitle.setText(model.getTitle());
        
        //book_owner.setText(model.getOwnerUid());
        //book_author.setText(model.getAuthors().keySet().iterator().next());

        book_image.setImageResource(R.drawable.ic_book_blue_grey_900_48dp);
        
        StorageReference sr = FirebaseStorage.getInstance().getReference().child("shared_books_pictures/" + model.getKey() + ".jpg");

        sr.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
          @Override
          public void onSuccess(Uri uri) {
              try {
                  Glide.with(ctx)
                          .load(uri)
                          .into(book_image);
              }catch (Exception e){
                  e.printStackTrace();
              }
            /*Picasso.get()
                    .load(uri)
                    //.fit()
                    //.centerCrop()
                    .error(R.drawable.ic_book_blue_grey_900_48dp)
                    .into(book_image);*/
          }
        });
        
        if (discriminator.equals("ownerUid")) {
          book_image.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("book", model);
            
            SharedBookDetailFragment dialogFragment = new SharedBookDetailFragment();
            dialogFragment.setArguments(bundle);
            dialogFragment.show(((FragmentActivity) ctx).getFragmentManager(), "dialog");
          });
          
          iw1.setImageResource(R.drawable.ic_delete_white_48dp);
          iw1.setOnClickListener(v -> {
            new AlertDialog.Builder(ctx)
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
                            DatabaseReference tmpDbRef  = db.getReference("BOOKS/" + model.getISBN());
                            tmpDbRef.removeValue();
                          } else {
                            DatabaseReference tmpDbRef  = db.getReference("BOOKS/" + model.getISBN());
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
          
          if (model.getShared()) {
            ImageView overlay_book_card = (ImageView) mView.findViewById(R.id.overlay_shared_book_card_thumbnail);
            overlay_book_card.setImageResource(R.drawable.ic_delete_white_48dp);
          }
          
        } else {
          iw1.setImageResource(R.drawable.ic_textsms_white_48dp);
          iw1.setOnClickListener(v -> Toast.makeText(ctx, "Contact owner", Toast.LENGTH_SHORT).show());
          
          iw2.setImageResource(R.drawable.ic_zoom_in_white_48dp);
          iw2.setOnClickListener(v -> {
            Intent I = new Intent(ctx, ShowSharedBook.class);
            I.putExtra("book", model);
            ctx.startActivity(I);
          });
          
        }
        
        
      }
    }

//    public void closeConnection() {
//      dbref.removeEventListener(ev);
//    }
    
    //TODO this must be done as standalone classes/functions
    private int dpToPx(int dp) {
      Resources r = getResources();
      return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
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
    
  }
  
  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {
    
    PlaceholderFragment shared, borrowed;
    
    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
      shared = PlaceholderFragment.newInstance("ownerUid");
      borrowed = PlaceholderFragment.newInstance("borrowToUid");
    }
    
    @Override
    public Fragment getItem(int position) {
      return (position == 0) ? shared : borrowed;
    }
    
    @Override
    public int getCount() {
      // Show 2 total pages.
      return 2;
    }

//    public void closeConnection() {
//      shared.closeConnection();
//      borrowed.closeConnection();
//    }
  }
  
}
