package it.polito.mad.greit.project;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;

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
    ImageView thumbnail = v.findViewById(R.id.shared_book_detail_thumbnail);
    ImageView contactForLoan = (ImageView) v.findViewById(R.id.shared_book_detail_icon1);
    ImageView zoomOut = (ImageView) v.findViewById(R.id.shared_book_detail_icon2);
    ImageView distance = (ImageView) v.findViewById(R.id.shared_book_detail_icon3);
    String date = sb.getAddedOn().subSequence(4, 10)+sb.getAddedOn().substring(29, 34);
    
    tv = (TextView) v.findViewById(R.id.shared_book_detail_owner);
    String dateAndOwnerInfo = "Added on " + date + "\nby @" + sb.getOwnerUsername();
  
    Spannable spannable = new SpannableString(dateAndOwnerInfo);
    spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 24,
        dateAndOwnerInfo.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD_ITALIC),
        25, dateAndOwnerInfo.length(), 0);
    
    tv.setText(spannable, TextView.BufferType.SPANNABLE);
    
    tv = (TextView) v.findViewById(R.id.shared_book_detail_text);
    tv.setText("\"" + sb.getAdditionalInformations() + "\"");
    
    contactForLoan.setImageResource(R.drawable.ic_textsms_white_48dp);
    contactForLoan.setOnClickListener(view -> Toast.makeText(getActivity().getApplicationContext(), "Start chat", Toast.LENGTH_SHORT).show());
    contactForLoan.setOnClickListener(view -> Chat.openchat(this.getContext(), sb));
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
      if (getContext() != null) {
        Glide.with(this)
            .asBitmap()
            .load(stream.toByteArray())
            
            .apply(new RequestOptions()
                .placeholder(R.drawable.ic_book_blue_grey_900_48dp)
                .fitCenter())
            .into(thumbnail);
      }
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
 
}
