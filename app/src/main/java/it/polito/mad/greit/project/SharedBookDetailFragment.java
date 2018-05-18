package it.polito.mad.greit.project;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.function.IntConsumer;

public class SharedBookDetailFragment extends android.support.v4.app.DialogFragment {
  
  private static final String ARG_PARAM1 = "book";
  
  private SharedBook sb;
  
  private OnFragmentInteractionListener mListener;
  
  public SharedBookDetailFragment() {
  }
  
  public static SharedBookDetailFragment newInstance(SharedBook sb) {
    SharedBookDetailFragment fragment = new SharedBookDetailFragment();
    Bundle args = new Bundle();
    args.putSerializable(ARG_PARAM1, sb);
    fragment.setArguments(args);
    return fragment;
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      sb = (SharedBook) getArguments().getSerializable(ARG_PARAM1);
    }
    
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View v = inflater.inflate(R.layout.fragment_shared_book_detail, container, false);
    
    TextView tv;
    RatingBar book_ratings = (RatingBar) v.findViewById(R.id.shared_book_detail_conditions);
    ImageView thumbnail = v.findViewById(R.id.shared_book_detail_thumbnail);
    ImageView contactForLoan = (ImageView) v.findViewById(R.id.shared_book_detail_icon1);
    ImageView zoomOut = (ImageView) v.findViewById(R.id.shared_book_detail_icon2);
    ImageView distance = (ImageView) v.findViewById(R.id.shared_book_detail_icon3);
    String date = sb.getAddedOn();
    
    tv = (TextView) v.findViewById(R.id.shared_book_detail_owner);
    tv.setText("Added on " + date + "\nby @" + sb.getOwner());
    
    tv = (TextView) v.findViewById(R.id.shared_book_detail_text);
    tv.setText("\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent dictum " +
        "ac dui eget sodales. Cras eget mauris vitae nunc dictum massa nunc." + "\"");
    
    book_ratings.setRating(Float.valueOf(sb.getConditions()));
    
    contactForLoan.setImageResource(R.drawable.ic_textsms_white_48dp);
    contactForLoan.setOnClickListener(view -> Toast.makeText(getActivity().getApplicationContext(), "Start chat", Toast.LENGTH_SHORT).show());
    contactForLoan.setOnClickListener(view -> openchat(this.getContext(), sb));
/*
        zoomOut.setImageResource(R.drawable.ic_zoom_out_white_48dp);
        zoomOut.setOnClickListener(view -> getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit());
*/
    distance.setImageResource(R.mipmap.ic_minore_5);
    
    StorageReference sr = FirebaseStorage.getInstance().getReference().child("shared_books_pictures/" + sb.getKey() + ".jpg");
    
    sr.getBytes(5 * Constants.SIZE).addOnSuccessListener(bytes -> {
      Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      bm.compress(Bitmap.CompressFormat.JPEG, 85, stream);
      Glide.with(this)
          .asBitmap()
          .load(stream.toByteArray())
          
          .apply(new RequestOptions()
              .placeholder(R.drawable.ic_book_blue_grey_900_48dp)
              .fitCenter())
          .into(thumbnail);
    }).addOnFailureListener(e -> {
          if (getContext() != null) {
            Glide.with(this)
                .load("")
                .apply(new RequestOptions()
                    .error(R.drawable.ic_book_blue_grey_900_48dp)
                    .fitCenter())
                .into(thumbnail);
          }
        }
    );
    
    
    return v;
  }
  
  // TODO: Rename method, update argument and hook method into UI event
  public void onButtonPressed(Uri uri) {
    if (mListener != null) {
      mListener.onFragmentInteraction(uri);
    }
  }
  
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    Activity a;
//    if (context instanceof OnFragmentInteractionListener) {
//      mListener = (OnFragmentInteractionListener) context;
//    } else {
//      throw new RuntimeException(context.toString()
//          + " must implement OnFragmentInteractionListener");
//    }
  }
  
  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }
  
  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnFragmentInteractionListener {
    // TODO: Update argument type and name
    void onFragmentInteraction(Uri uri);
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
