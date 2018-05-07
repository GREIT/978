package it.polito.mad.greit.project;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.*;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ShowBookActivity extends AppCompatActivity {
  
  private Toolbar t;
  private SharedBook sb;
  private boolean owned = false;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_show_book);
    
    Intent intent = getIntent();
    sb = (SharedBook) intent.getSerializableExtra("book");
    t = findViewById(R.id.show_book_toolbar);
    t.setTitle(R.string.show_book_title);
    setSupportActionBar(t);
    t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
    t.setNavigationOnClickListener(v -> onBackPressed());
    
    if (!FirebaseAuth.getInstance().getCurrentUser().getUid().equals(sb.getOwner())) {
      //activate ask to borrow and star
      owned = false;
    } else {
      //activate edit and delete
      owned = true;
    }
    
    //TODO change eye color if book already in watchlist
    
    setup();
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    if (owned) {
      //inflater.inflate(R.menu.show_sharedbook_menu, menu);
    } else {
      inflater.inflate(R.menu.show_sharedbook_menu, menu);
    }
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    
    if (R.id.add_to_watchlist == item.getItemId()) {
      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
      alertDialogBuilder.setMessage("Book added to your watchlist! You will be notified when it will be available again.");
      String positiveText = getString(android.R.string.ok);
      alertDialogBuilder.setPositiveButton(positiveText,
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
          });
      AlertDialog alertDialog = alertDialogBuilder.create();
      alertDialog.show();
      return true;
    } else
      return super.onOptionsItemSelected(item);
  }
  
  public void setup() {
    TextView tv;
    Intent intent = getIntent();
    
    tv = findViewById(R.id.show_book_title);
    tv.setText(sb.getTitle());
    
    tv = findViewById(R.id.show_book_authors);
    StringBuilder authorsSB = new StringBuilder();
    for (String k : sb.getAuthors().keySet()) {
      authorsSB.append(k + ", ");
    }
    authorsSB.deleteCharAt(authorsSB.lastIndexOf(" "));
    authorsSB.deleteCharAt(authorsSB.lastIndexOf(","));
    tv.setText(authorsSB.toString());
    
    tv = findViewById(R.id.show_book_ISBN);
    tv.setText(sb.getISBN());
    
    tv = findViewById(R.id.show_book_publisher);
    tv.setText(sb.getPublisher());
    
    tv = findViewById(R.id.show_book_year);
    tv.setText(sb.getYear());
    
    ImageView iv = findViewById(R.id.show_book_pic);
    
    if (intent.hasExtra("pic")) {
      try {
        byte[] bytes = intent.getByteArrayExtra("pic");
        Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Resources resources = getApplicationContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = 300 * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        Bitmap scaled = Bitmap.createScaledBitmap(bm, (int) px, (int) px, false);
        iv.setImageBitmap(scaled);
      } catch (Exception e) {
        iv.setImageResource(R.drawable.ic_book_blue_grey_900_48dp);
      }
    } else if (sb.getKey() != null && !sb.getKey().isEmpty()) {
      StorageReference sr = FirebaseStorage.getInstance().getReference().child("shared_books_pictures/" + sb.getKey() + ".jpg");
      sr.getBytes(Constants.SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
        @Override
        public void onSuccess(byte[] bytes) {
          try {
            Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Resources resources = getApplicationContext().getResources();
            DisplayMetrics metrics = resources.getDisplayMetrics();
            float px = 300 * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
            Bitmap scaled = Bitmap.createScaledBitmap(bm, (int) px, (int) px, false);
            iv.setImageBitmap(scaled);
          } catch (Exception e) {
            iv.setImageResource(R.drawable.ic_book_blue_grey_900_48dp);
          }
        }
      }).addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception exception) {
          // Handle any errors
          iv.setImageResource(R.drawable.ic_book_blue_grey_900_48dp);
        }
      });
    } else {
      iv.setImageResource(R.drawable.ic_book_blue_grey_900_48dp);
    }
    
  }
}
