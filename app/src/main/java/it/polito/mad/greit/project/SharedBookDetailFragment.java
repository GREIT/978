package it.polito.mad.greit.project;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.util.Date;

public class SharedBookDetailFragment extends android.support.v4.app.DialogFragment {
  
  private static final String ARG_PARAM1 = "book";
  private static final String ARG_PARAM2 = "currentLocation";
  
  private SharedBook sb;
  private String currentLocation;
  private double distanceKm;
  
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
    String dateAndOwnerInfo = "Added on " + date + "\nby @" + sb.getOwnerUsername();

    Spannable spannable = new SpannableString(dateAndOwnerInfo);
    spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 24,
            dateAndOwnerInfo.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD_ITALIC),
            25, dateAndOwnerInfo.length(), 0);

    tv.setText(spannable, TextView.BufferType.SPANNABLE);

    tv = (TextView) v.findViewById(R.id.shared_book_detail_text);
    tv.setText("\"" + sb.getAdditionalInformations() + "\"");

    if (sb.getOwnerUsername().equals(this.getContext().getSharedPreferences("sharedpref", Context.MODE_PRIVATE).getString("username", null))) {
      contactForLoan.setImageResource(R.drawable.ic_textsms_transparent_48dp);
    } else if(sb.getShared() == true) {
      contactForLoan.setImageResource(R.drawable.ic_textsms_transparent_48dp);
      rightBar.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.colorGrey));
    } else {
      contactForLoan.setImageResource(R.drawable.ic_textsms_white_48dp);
      contactForLoan.setOnClickListener(view -> Chat.openchat(this.getContext(), sb));
    }

    if (currentLocation != null) {
      distanceKm = Utils.calcDistance(sb.getCoordinates(), currentLocation) / 1000;
      if (distanceKm > 20)
        distance.setImageResource(R.mipmap.ic_maggiore_20);
      else if (distanceKm < 20 && distanceKm > 5)
        distance.setImageResource(R.mipmap.ic_minore_20);
      else
        distance.setImageResource(R.mipmap.ic_minore_5);
    }

    ownerInfo.setImageResource(R.drawable.ic_person_white_48dp);
    ownerInfo.setOnClickListener(view -> {
      Intent I = new Intent(this.getContext(), OtherProfile.class);
      I.putExtra("uid", sb.getOwnerUid());
      this.getContext().startActivity(I);
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
