package it.polito.mad.greit.project;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.util.Date;

public class SharedBookDetailFragment extends DialogFragment {
  
  private static final String ARG_PARAM1 = "book";
  private static final String ARG_PARAM2 = "currentLocation";
  
  private SharedBook sb;
  private String currentLocation;
  private double distanceKm;
  private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

  public SharedBookDetailFragment() {
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      sb = (SharedBook) getArguments().getSerializable(ARG_PARAM1);
      currentLocation = (String) getArguments().getSerializable(ARG_PARAM2);
    }
    
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View v = inflater.inflate(R.layout.fragment_shared_book_detail, container, false);
    
    TextView tv;
    RelativeLayout rightBar = v.findViewById(R.id.right_bar_detail);
    ImageView thumbnail = v.findViewById(R.id.shared_book_detail_thumbnail);
    ImageView contactForLoan = (ImageView) v.findViewById(R.id.shared_book_detail_icon1);
    ImageView ownerInfo = (ImageView) v.findViewById(R.id.shared_book_detail_icon2);
    ImageView distance = (ImageView) v.findViewById(R.id.shared_book_detail_icon3);
    String date = DateFormat.getDateInstance().format(new Date(sb.getAddedOn() * 1000));
    
    tv = (TextView) v.findViewById(R.id.shared_book_detail_owner);
    String dateAndOwnerInfo = getActivity().getResources().getString(R.string.book_detail, date, sb.getOwnerUsername());

    Spannable spannable = new SpannableString(dateAndOwnerInfo);
    spannable.setSpan(new ClickableSpan() {
        @Override
        public void onClick(View view) {
            Intent I = new Intent(getActivity(), OtherProfile.class);
            I.putExtra("uid", sb.getOwnerUid());
            getActivity().startActivity(I);
        }
    }, getActivity().getResources().getString(R.string.book_detail, date, sb.getOwnerUsername()).length() - sb.getOwnerUsername().length() - 1, dateAndOwnerInfo.length(), 0);
    spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.accent)),
        getActivity().getResources().getString(R.string.book_detail, date, sb.getOwnerUsername()).length() - sb.getOwnerUsername().length() - 1, dateAndOwnerInfo.length(), 0);
    spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
        getActivity().getResources().getString(R.string.book_detail, date, sb.getOwnerUsername()).length() - sb.getOwnerUsername().length() - 1, dateAndOwnerInfo.length(), 0);

    tv.setText(spannable, TextView.BufferType.SPANNABLE);
    tv.setClickable(true);
    tv.setMovementMethod(LinkMovementMethod.getInstance());

    tv = (TextView) v.findViewById(R.id.shared_book_detail_text);
    tv.setText("\"" + sb.getAdditionalInformations() + "\"");

    if (sb.getOwnerUid().equals(user.getUid())) {
      // It is my book
      if (sb.getShared()) {
        // Book is currently on loan
        rightBar.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.unavailable));
        contactForLoan.setImageResource(R.drawable.ic_delete_transparent_48dp);
        contactForLoan.setOnClickListener(view -> Toast.makeText(getActivity().getApplicationContext(), R.string.cannot_delete, Toast.LENGTH_SHORT).show());
      } else {
        contactForLoan.setImageResource(R.drawable.ic_delete_white_48dp);
        contactForLoan.setOnClickListener(view -> {
          new AlertDialog.Builder(getActivity())
              .setTitle(R.string.confirmation_needed)
              .setMessage(R.string.delete_shared_book)
              .setIcon(android.R.drawable.ic_dialog_alert)
              .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                  FirebaseDatabase db = FirebaseDatabase.getInstance();
                  DatabaseReference dbref = db.getReference("SHARED_BOOKS/" + sb.getKey());

                  dbref.removeValue();

                  dbref = db.getReference("BOOKS");

                  dbref.orderByKey().equalTo(sb.getISBN()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                      for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        Book tmpBook = ds.getValue(Book.class);
                        if (tmpBook.getBooksOnLoan() == 1) {
                          DatabaseReference tmpDbRef  = db.getReference("BOOKS/" + sb.getISBN());
                          tmpDbRef.removeValue();
                        } else {
                          DatabaseReference tmpDbRef  = db.getReference("BOOKS/" + sb.getISBN());
                          tmpDbRef.child("booksOnLoan").setValue(Integer.valueOf(tmpBook.getBooksOnLoan()) - 1);
                          Toast.makeText(getActivity().getApplicationContext(), R.string.book_removed, Toast.LENGTH_SHORT).show();
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
      }
    } else {
      // Not my book
      if (sb.getShared()) {
        // Book is currently on loan
        rightBar.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.unavailable));
        contactForLoan.setImageResource(R.drawable.ic_textsms_transparent_48dp);
        contactForLoan.setOnClickListener(view -> Toast.makeText(getActivity().getApplicationContext(), R.string.book_on_loan, Toast.LENGTH_SHORT).show());
      } else {
        contactForLoan.setImageResource(R.drawable.ic_textsms_white_48dp);
        contactForLoan.setOnClickListener(view -> Chat.openchat(getActivity().getApplicationContext(), sb));
      }
    }

    if (currentLocation != null) {
      distanceKm = Utils.calcDistance(sb.getCoordinates(), currentLocation) / 1000;
      if (distanceKm > 20)
        distance.setImageResource(R.mipmap.ic_maggiore_20);
      else if (distanceKm < 20 && distanceKm > 5)
        distance.setImageResource(R.mipmap.ic_minore_20);
      else
        distance.setImageResource(R.mipmap.ic_minore_5);
    } else distance.setImageResource(R.mipmap.ic_minore_5);

    ownerInfo.setImageResource(R.drawable.ic_person_white_48dp);
    ownerInfo.setOnClickListener(view -> {
      Intent I = new Intent(getActivity().getApplicationContext(), OtherProfile.class);
      I.putExtra("uid", sb.getOwnerUid());
      I.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getActivity().getApplicationContext().startActivity(I);
    });

    thumbnail.setImageResource(R.drawable.ic_book_blue_grey_900_48dp);
    StorageReference sr = FirebaseStorage.getInstance().getReference().child("shared_books_pictures/" + sb.getKey() + ".jpg");
    sr.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
      @Override
      public void onSuccess(Uri uri) {
        try {
          Glide.with(SharedBookDetailFragment.this)
              .load(uri)
              .into(thumbnail);
        }catch (Exception e){
          e.printStackTrace();
        }
        /*Picasso.get()
                .load(uri)
                .error(R.drawable.ic_book_blue_grey_900_48dp)
                .into(thumbnail);*/
      }
    });
    
    return v;
  }
 
}
