package it.polito.mad.greit.project;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
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

public class SharedBooksByUser extends AppCompatActivity {
  
  private RecyclerView mResultList;
  private DatabaseReference mSharedBookDb;
  private FloatingActionButton fab;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_shared_books_by_user);
    
    Toolbar toolbar = (Toolbar) findViewById(R.id.shared_books_by_user_toolbar);
    toolbar.setTitle(R.string.activity_shared_books);
    setSupportActionBar(toolbar);
    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
    toolbar.setNavigationOnClickListener(v -> onBackPressed());

//    initCollapsingToolbar();
    fab = findViewById(R.id.fab_add_book_from_shared_books_by_user);
    fab.setOnClickListener(v -> {
      Intent i = new Intent(this, ShareNewBook.class);
      startActivity(i);
    });
    
    mSharedBookDb = FirebaseDatabase.getInstance().getReference("SHARED_BOOKS");
    mResultList = (RecyclerView) findViewById(R.id.shared_books_by_user_result_list);
    mResultList.setItemAnimator(new DefaultItemAnimator());
    
    sharedBookShow();
  }
  
  private void sharedBookShow() {
    mResultList.setLayoutManager(new GridLayoutManager(this, 2));
    mResultList.removeItemDecoration(mResultList.getItemDecorationAt(0));
    mResultList.addItemDecoration(new SharedBooksByUser.GridSpacingItemDecoration(2, dpToPx(20), true));
    
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    
    Query firebaseSearchQuery = mSharedBookDb.orderByChild("ownerUid").equalTo(user.getUid());
    FirebaseRecyclerAdapter<SharedBook, SharedBooksByUser.SharedBookViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<SharedBook, SharedBooksByUser.SharedBookViewHolder>(
        SharedBook.class,
        R.layout.sharedbook_card,
        SharedBooksByUser.SharedBookViewHolder.class,
        firebaseSearchQuery
    ) {
      @Override
      protected void populateViewHolder(SharedBooksByUser.SharedBookViewHolder viewHolder, SharedBook model, int position) {
        viewHolder.setDetails(getApplicationContext(), model, getSupportFragmentManager());
      }
      
      @Override
      public SharedBooksByUser.SharedBookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SharedBooksByUser.SharedBookViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
        
        return viewHolder;
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
        bundle.putSerializable("currentLocation", null);
        
        SharedBookDetailFragment dialogFragment = new SharedBookDetailFragment();
        dialogFragment.setArguments(bundle);
        dialogFragment.show(fm, "dialog");
      });
      
      bookImage.setOnClickListener(v -> {
        Bundle bundle = new Bundle();
        bundle.putSerializable("book", model);
        bundle.putSerializable("currentLocation", null);
        
        SharedBookDetailFragment dialogFragment = new SharedBookDetailFragment();
        dialogFragment.setArguments(bundle);
        dialogFragment.show(fm, "dialog");
      });
      
      if (model.getShared() == true) {
        // Book is currently on loan
        rightBar.setBackgroundColor(ContextCompat.getColor(mView.getContext(), R.color.colorGrey));
        contactForLoan.setImageResource(R.drawable.ic_delete_transparent_48dp);
        contactForLoan.setOnClickListener(v -> Toast.makeText(ctx, "You can't delete a book currently on loan!", Toast.LENGTH_SHORT).show());
      } else {
        contactForLoan.setImageResource(R.drawable.ic_delete_white_48dp);
        contactForLoan.setOnClickListener(v -> {
          
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
        });
      }
      
      
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
