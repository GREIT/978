package it.polito.mad.greit.project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class SharedBooksAdapter extends RecyclerView.Adapter<SharedBooksAdapter.MyViewHolder> {
  private Context mContext;
  private List<SharedBook> bookList;
  
  
  public class MyViewHolder extends RecyclerView.ViewHolder {
    public TextView title, author;
    public ImageView thumbnail;
    
    
    public MyViewHolder(View view) {
      super(view);
      title = (TextView) view.findViewById(R.id.book_card_title);
      author = (TextView) view.findViewById(R.id.book_card_author);
      thumbnail = (ImageView) view.findViewById(R.id.book_card_thumbnail);
    }
  }
  
  public SharedBooksAdapter(Context mContext, List<SharedBook> bookList) {
    this.mContext = mContext;
    this.bookList = bookList;
  }
  
  @Override
  public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.book_card, parent, false);
    
    return new MyViewHolder(itemView);
  }
  
  @Override
  public void onBindViewHolder(MyViewHolder holder, int position) {
    SharedBook book = bookList.get(position);
    holder.title.setText(book.getTitle());
    holder.author.setText(book.getAuthor());
  
    if (book.getKey() != null) {
      FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
      StorageReference sr = FirebaseStorage.getInstance().getReference().child("shared_books_pictures/" + book.getKey() + ".jpg");
      sr.getBytes(Constants.SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
        @Override
        public void onSuccess(byte[] bytes) {
          Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
          ByteArrayOutputStream stream = new ByteArrayOutputStream();
          bm.compress(Bitmap.CompressFormat.JPEG, 85, stream);
          Glide
              .with(mContext)
              .load(stream.toByteArray())
              .asBitmap()
              .error(R.drawable.ic_book_blue_grey_900_48dp)
              //.transform(new CircleTransform(this))
              .into(holder.thumbnail);
        }
      }).addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception exception) {
          // Handle any errors
          exception.printStackTrace();

        }
      });
    }
//    else{
//      Glide
//              .with(mContext)
//              .load(R.drawable.ic_book_blue_grey_900_48dp)
//              .asBitmap()
//              .into(holder.thumbnail);
//    }
  }
  
  @Override
  public int getItemCount() {
    return bookList.size();
  }
}
