package it.polito.mad.greit.project;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;

public class SharedBooksByUser extends AppCompatActivity {
  
  private RecyclerView mResultList;
  private DatabaseReference mSharedBookDb;
  
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
        viewHolder.setDetails(getApplicationContext(), model);
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
 
    
    public void setDetails(Context ctx, SharedBook model) {
      //TextView book_owner = (TextView) mView.findViewById(R.id.shared_book_card_owner);
      ImageView book_image = (ImageView) mView.findViewById(R.id.shared_book_card_thumbnail);
      ImageView editSharedBook = (ImageView) mView.findViewById(R.id.shared_book_card_contactForLoan);
      ImageView deleteSharedBook = (ImageView) mView.findViewById(R.id.shared_book_card_moreInfo);
      
      //book_owner.setText(model.getOwnerUid());
      //book_author.setText(model.getAuthors().keySet().iterator().next());
      
      editSharedBook.setImageResource(R.drawable.ic_mode_edit_white_48dp);
      editSharedBook.setOnClickListener(v -> Toast.makeText(ctx, "Edit book", Toast.LENGTH_SHORT).show());
      
      deleteSharedBook.setImageResource(R.drawable.ic_delete_white_48dp);
      deleteSharedBook.setOnClickListener(v -> Toast.makeText(ctx, "Delete book", Toast.LENGTH_SHORT).show());
      
      
      StorageReference sr = FirebaseStorage.getInstance().getReference().child("shared_books_pictures/" + model.getKey() + ".jpg");
      
      sr.getBytes(5 * Constants.SIZE).addOnSuccessListener(bytes -> {
        Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        Glide.with(ctx)
            .asBitmap()
            .load(stream.toByteArray())
            .apply(new RequestOptions()
                .placeholder(R.drawable.ic_book_blue_grey_900_48dp)
                .fitCenter())
            .into(book_image);
      }).addOnFailureListener(e ->
          Glide.with(ctx)
              .load("")
              .apply(new RequestOptions()
                  .error(R.drawable.ic_book_blue_grey_900_48dp)
                  .fitCenter())
              .into(book_image)
      );
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
