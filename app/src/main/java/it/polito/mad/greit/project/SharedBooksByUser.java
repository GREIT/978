package it.polito.mad.greit.project;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class SharedBooksByUser extends AppCompatActivity {
  private RecyclerView recyclerView;
  private SharedBooksAdapter adapter;
  private List<SharedBook> bookList;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_shared_books_by_user);
  
    Toolbar toolbar = (Toolbar) findViewById(R.id.shared_books_by_user_toolbar);
    toolbar.setTitle("Your shared books");
    setSupportActionBar(toolbar);
    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
    toolbar.setNavigationOnClickListener(v -> finish());
  
//    initCollapsingToolbar();
  
    recyclerView = (RecyclerView) findViewById(R.id.shared_books_by_user_recycler_view);
    
    bookList = new ArrayList<>();
    adapter = new SharedBooksAdapter(this, bookList);
  
    RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
    recyclerView.setLayoutManager(mLayoutManager);
    recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(10), true));
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.setAdapter(adapter);
  
    prepareBooks();
  }
  
//  private void initCollapsingToolbar() {
//    final CollapsingToolbarLayout collapsingToolbar =
//        (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
//
//    collapsingToolbar.setTitle(" ");
//    AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.shared_books_by_user_appbar);
//    appBarLayout.setExpanded(true);
//
//    // hiding & showing the title when toolbar expanded & collapsed
//    appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
//      boolean isShow = false;
//      int scrollRange = -1;
//
//      @Override
//      public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
//        if (scrollRange == -1) {
//          scrollRange = appBarLayout.getTotalScrollRange();
//        }
//        if (scrollRange + verticalOffset == 0) {
//          collapsingToolbar.setTitle(getString(R.string.app_name));
//          isShow = true;
//        } else if (isShow) {
//          collapsingToolbar.setTitle(" ");
//          isShow = false;
//        }
//      }
//    });
//  }
  
  private void prepareBooks() {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
  
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference dbref = db.getReference("books");
    
    dbref.orderByChild("owner").equalTo(user.getUid()).addChildEventListener(new ChildEventListener() {
      @Override
      public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        SharedBook tmp = new SharedBook();
        tmp = dataSnapshot.getValue(SharedBook.class);
        bookList.add(tmp);
        adapter.notifyDataSetChanged();
      }
  
      @Override
      public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        SharedBook tmp = new SharedBook();
        tmp = dataSnapshot.getValue(SharedBook.class);
        bookList.add(tmp);
        adapter.notifyDataSetChanged();
      }
  
      @Override
      public void onChildRemoved(DataSnapshot dataSnapshot) {
    
      }
  
      @Override
      public void onChildMoved(DataSnapshot dataSnapshot, String s) {
    
      }
  
      @Override
      public void onCancelled(DatabaseError databaseError) {
    
      }
    });
    
    
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
