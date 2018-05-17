package it.polito.mad.greit.project;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.Image;
import android.os.Build;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
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
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.dynamic.SupportFragmentWrapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;

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
        mResultList = (RecyclerView) findViewById(R.id.shared_books_result_list);
        mResultList.setItemAnimator(new DefaultItemAnimator());

        sharedBookShow(book.getISBN());
    }

    private void sharedBookShow(String ISBN) {
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
            public SharedBookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                SharedBookViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
                viewHolder.setOnClickListener(new SharedBookViewHolder.ClickListener() {
                    @Override
                    public void onItemClick(View view, SharedBook model) {
//            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
//            if (prev != null) {
//              ft.remove(prev);
//            }
//            ft.addToBackStack(null);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("book", model);

                        SharedBookDetailFragment dialogFragment = new SharedBookDetailFragment();
                        dialogFragment.setArguments(bundle);
                        dialogFragment.show(getSupportFragmentManager(), "dialog");

//            ImageView iv = (ImageView) findViewById(R.id.shared_book_detail_thumbnail);
//            Intent intent = new Intent(SearchedSharedBooks.this, SharedBookDetail.class);
//
//            try {
//              intent.putExtra("book", model);
//
//              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                ActivityOptionsCompat options = ActivityOptionsCompat.
//                    makeSceneTransitionAnimation(SearchedSharedBooks.this, iv, "thumbnailTransition");
//                startActivity(intent, options.toBundle());
//              } else {
//                startActivity(intent);
//              }
//            } catch (Exception e) {
//              intent.putExtra("book", "");
//              startActivity(intent);
//            }
//

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
            void onItemClick(View view, SharedBook model);
        }

        public void setOnClickListener(SharedBookViewHolder.ClickListener clickListener) {
            mClickListener = clickListener;
        }

        public void setDetails(Context ctx, SharedBook model, android.support.v4.app.FragmentManager fm) {
            //TextView book_owner = (TextView) mView.findViewById(R.id.shared_book_card_owner);
            RatingBar book_ratings = (RatingBar) mView.findViewById(R.id.shared_book_card_conditions);
            ImageView book_image = (ImageView) mView.findViewById(R.id.shared_book_card_thumbnail);
            ImageView showMoreInfo = (ImageView) mView.findViewById(R.id.shared_book_card_moreInfo);
            ImageView contactForLoan = (ImageView) mView.findViewById(R.id.shared_book_card_contactForLoan);
            ImageView distance = (ImageView) mView.findViewById(R.id.shared_book_card_distance);

            //book_owner.setText(model.getOwner());
            book_ratings.setRating(Float.valueOf(model.getConditions()));
            //book_author.setText(model.getAuthors().keySet().iterator().next());

            showMoreInfo.setImageResource(R.drawable.ic_zoom_in_white_48dp);

            showMoreInfo.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", model);

                SharedBookDetailFragment dialogFragment = new SharedBookDetailFragment();
                dialogFragment.setArguments(bundle);
                dialogFragment.show(fm, "dialog");
            });

            contactForLoan.setImageResource(R.drawable.ic_textsms_white_48dp);
            //contactForLoan.setOnClickListener(v -> Toast.makeText(ctx, "Start chat", Toast.LENGTH_SHORT).show());
            contactForLoan.setOnClickListener(v -> openchat(ctx, model));

            distance.setImageResource(R.mipmap.ic_minore_5);


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

            itemView.setOnClickListener(v -> {
                mClickListener.onItemClick(v, model);
            });
        }

        private void openchat(Context ctx, SharedBook sb) {
            FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseDatabase db = FirebaseDatabase.getInstance();
            DatabaseReference dbref = db.getReference("USER_CHATS").child(fbu.getUid());
            dbref.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    Boolean chat_exists = false;
                    try {
                        for (MutableData ds : mutableData.getChildren()) {
                            Chat c = ds.getValue(Chat.class);
                            if (c.getBookID().equals(sb.getKey()) && c.getUserID().equals(sb.getOwner())) {
                                //chat already present
                                Intent intent = new Intent(ctx, ChatActivity.class);
                                intent.putExtra("chat", c);
                                ctx.startActivity(intent);
                                chat_exists = true;
                            }
                        }

                        if (!chat_exists) {
                            Chat c = new Chat();
                            //getUsername(db,sb.getOwner(),c);
                            db.getReference("USERS").child(sb.getOwner()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    Profile p = dataSnapshot.getValue(Profile.class);
                                    c.setUsername(p.getUsername());
                                    c.setBookID(sb.getKey());
                                    c.setUserID(sb.getOwner());
                                    c.setLastMsg("");
                                    c.setUnreadCount(0);
                                    c.setBookTitle(sb.getTitle());
                                    DatabaseReference user_mess = db.getReference("USERS_MESSAGES");
                                    String chatid = user_mess.push().getKey();
                                    c.setChatID(chatid);
                                    dbref.child(chatid).setValue(c);
                                    Intent intent = new Intent(ctx, ChatActivity.class);
                                    intent.putExtra("chat", Chat.copy(c));
                                    //second user
                                    db.getReference("USERS").child(fbu.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Profile p = dataSnapshot.getValue(Profile.class);
                                            DatabaseReference ref_second_user = db.getReference("USER_CHATS").child(sb.getOwner());
                                            c.setUserID(fbu.getUid());
                                            c.setUsername(p.getUsername());
                                            //c.setUsername(fbu.getDisplayName());
                                            //c.setUsername(getUsername(db,fbu.getUid(),c));
                                            ref_second_user.child(chatid).setValue(c);
                                            ctx.startActivity(intent);
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

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
