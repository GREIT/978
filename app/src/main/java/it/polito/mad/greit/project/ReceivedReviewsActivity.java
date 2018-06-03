package it.polito.mad.greit.project;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ReceivedReviewsActivity extends AppCompatActivity {

    private String USER_REVIEWS = "USER_REVIEWS";
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private ValueEventListener evListener;
    private DatabaseReference profileDbRef;
    private FirebaseRecyclerAdapter reviewListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_received_reviews);

        setupView();

        setupReviewList();

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        profileDbRef.removeEventListener(evListener);
        reviewListAdapter.cleanup();
    }

    private void setupView(){
        Toolbar t;
        t = findViewById(R.id.reviews_toolbar);
        t.setTitle(R.string.nav_my_reviews);
        setSupportActionBar(t);
        t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        t.setNavigationOnClickListener(view -> onBackPressed());
//reviews_mean_value
        TextView name = findViewById(R.id.reviews_username);
        name.setText("@" + Profile.getCurrentUsername(this));
        TextView mean_value = findViewById(R.id.reviews_mean_value);

        RatingBar rating = findViewById(R.id.reviews_mean_rating);

        profileDbRef = FirebaseDatabase.getInstance().getReference("USERS").child(user.getUid());

        evListener = profileDbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Profile profile = dataSnapshot.getValue(Profile.class);
                if(profile!= null) {
                    Float score = (profile.getTotReviewsReceived() != 0) ? profile.getTotScoringReviews() / profile.getTotReviewsReceived() : 0;

                    String sScore = score.toString();
                    sScore = sScore.substring(0, (sScore.length() > 4) ? 4 : sScore.length());

                    rating.setRating(score);
                    mean_value.setText(sScore);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setupReviewList(){
        Query query = FirebaseDatabase.getInstance().getReference(USER_REVIEWS)
                .orderByChild("reviewedUid").equalTo(user.getUid());

        RecyclerView rv = findViewById(R.id.reviews_list);
        reviewListAdapter = new FirebaseRecyclerAdapter<Review, ReviewViewHolder>
                (Review.class, R.layout.review_card, ReviewViewHolder.class, query) {

            @Override
            protected void populateViewHolder(ReviewViewHolder viewHolder, Review model , int position) {
                viewHolder.bindReview(model, getApplicationContext());
            }

            @Override
            public ReviewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                ReviewViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
                return viewHolder;
            }

        };

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(linearLayoutManager);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        rv.setAdapter(reviewListAdapter);
    }

}
