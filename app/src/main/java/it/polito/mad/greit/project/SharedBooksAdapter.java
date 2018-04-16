package it.polito.mad.greit.project;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class SharedBooksAdapter extends RecyclerView.Adapter<SharedBooksAdapter.MyViewHolder> {
  private Context mContext;
  private List<SharedBook> bookList;
  
  public class MyViewHolder extends RecyclerView.ViewHolder {
    public TextView title, author;
    
    public MyViewHolder(View view) {
      super(view);
      title = (TextView) view.findViewById(R.id.book_card_title);
      author = (TextView) view.findViewById(R.id.book_card_author);
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
  
    // loading album cover using Glide library
    //Glide.with(mContext).load(album.getThumbnail()).into(holder.thumbnail);
//
//    holder.overflow.setOnClickListener(new View.OnClickListener() {
//      @Override
//      public void onClick(View view) {
//        showPopupMenu(holder.overflow);
//      }
//    });
  }
  
//  class MyMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {
//
//    public MyMenuItemClickListener() {
//    }
//
//    @Override
//    public boolean onMenuItemClick(MenuItem menuItem) {
//      switch (menuItem.getItemId()) {
//        case R.id.action_add_favourite:
//          Toast.makeText(mContext, "Add to favourite", Toast.LENGTH_SHORT).show();
//          return true;
//        case R.id.action_play_next:
//          Toast.makeText(mContext, "Play next", Toast.LENGTH_SHORT).show();
//          return true;
//        default:
//      }
//      return false;
//    }
//  }
  
  @Override
  public int getItemCount() {
    return bookList.size();
  }
}
