package it.polito.mad.greit.project;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

    public void onBackPressed(){
        Intent intent = new Intent(this,MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    protected void onStop(){
        super.onStop();
        mSectionsPagerAdapter.closeConnection();
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
        private DatabaseReference dbref;
        private ChildEventListener ev;

        public PlaceholderFragment(){
            super();
        }

        public static PlaceholderFragment newInstance(String string){
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
            recView = (RecyclerView) rootView.findViewById(R.id.shared_books_by_user_split_recycler_view);
            bookList = new ArrayList<>();
            adapter = new SharedBooksAdapter(this.getContext(), bookList);

            RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this.getContext(), 2);
            recView.setLayoutManager(mLayoutManager);
            recView.addItemDecoration(new PlaceholderFragment.GridSpacingItemDecoration(2, dpToPx(10), true));
            recView.setItemAnimator(new DefaultItemAnimator());
            recView.setAdapter(adapter);

            ItemClickSupport.addTo(recView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                @Override
                public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                    SharedBook current = bookList.get(position);
                    Intent intent = new Intent(getActivity(),ShowBookActivity.class);

                    try {
                        intent.putExtra("book", current);
                    }catch (Exception e){
                        intent.putExtra("book","");
                    }
                    try{
                        ImageView iv = v.findViewById(R.id.book_card_thumbnail);
                        Bitmap bitmap = ((BitmapDrawable)iv.getDrawable()).getBitmap();
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        byte[] bitmapdata = stream.toByteArray();
                        intent.putExtra("pic",bitmapdata);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    startActivity(intent);
                }
            });

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseDatabase db = FirebaseDatabase.getInstance();
            dbref = db.getReference("SHARED_BOOKS");
            discriminator =  getArguments().getString(DISCRMINATOR_STRING);
            Log.d("TabbedInfo", "onCreateView: called on Tab " + discriminator);
            ev = dbref.orderByChild(discriminator).equalTo(user.getUid()).addChildEventListener(
                    new ChildEventListener() {
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
                    }
                );
            return rootView;
        }

        public void closeConnection(){
            dbref.removeEventListener(ev);
        }

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

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
            shared = PlaceholderFragment.newInstance("owner");
            borrowed = PlaceholderFragment.newInstance("borrowTo");
        }

        @Override
        public Fragment getItem(int position) {
            return (position==0) ? shared : borrowed ;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        public void closeConnection(){
            shared.closeConnection();
            borrowed.closeConnection();
        }
    }

}
