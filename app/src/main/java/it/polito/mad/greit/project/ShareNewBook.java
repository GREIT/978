package it.polito.mad.greit.project;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShareNewBook extends AppCompatActivity {
  private static final String TAG = "Barcode Scanner API";
  
  private BarcodeDetector detector;
  private Button button_scan;
  private Uri imageUri;
  private TextView tw_ISBN;
  private String ISBN;
  Toolbar t;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_share_new_book);
    
    t = findViewById(R.id.share_new_book_toolbar);
    t.setTitle(R.string.activity_share_book);
    setSupportActionBar(t);
    
    t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
    t.setNavigationOnClickListener(v -> finish());
    
    button_scan = (Button) findViewById(R.id.share_new_book_scan);
    tw_ISBN = (TextView) findViewById(R.id.share_new_book_ISBN);
    
    button_scan.setOnClickListener(v -> ActivityCompat.requestPermissions(ShareNewBook.this,
        new String[]{Manifest.permission.CAMERA}, Constants.CAMERA_PERMISSION));
    
    detector = new BarcodeDetector.Builder(getApplicationContext())
        .setBarcodeFormats(Barcode.ALL_FORMATS)
        .build();
    if (!detector.isOperational()) {
      return;
    }
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.sharenewbook_menu, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    
    if (R.id.share_new_book_confirm == item.getItemId()) {
      EditText et_ISBN = (EditText) findViewById(R.id.share_new_book_ISBN);
      this.ISBN = et_ISBN.getText().toString();
      ISBNUtilities V = new ISBNUtilities(et_ISBN.getText().toString());
      if (V.isValid()) {
        this.ISBN = V.convertToISBN13();
        getBookInfo(this.ISBN);
      } else {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.isbn_not_valid)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "Dialog clicked");
              }
            });
        AlertDialog alert = builder.create();
        alert.show();
      }
      return true;
    } else return super.onOptionsItemSelected(item);
  }
  
  private void getBookInfo(String ISBN) {
    DatabaseReference db = FirebaseDatabase.getInstance().getReference();
    DatabaseReference dbref = db.child("BOOKS").child(ISBN);
    ValueEventListener eventListener = new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        if (!dataSnapshot.exists()) {
          searchOnGoogleDB(ISBN);
        } else {
          Book tmpBook = new Book();
          tmpBook = dataSnapshot.getValue(Book.class);
          
          Intent I = new Intent(ShareNewBook.this, CompleteBookRegistration.class);
          I.putExtra("book", tmpBook);
          startActivity(I);
        }
      }
      
      @Override
      public void onCancelled(DatabaseError databaseError) {
      }
    };
    dbref.addListenerForSingleValueEvent(eventListener);
  }
  
  private void searchOnGoogleDB(String ISBN) {
    RequestQueue queue = Volley.newRequestQueue(this);
    
    String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + ISBN;
    
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
        (Request.Method.GET, url, (String) null, new Response.Listener<JSONObject>() {
          @Override
          public void onResponse(JSONObject response) {
            try {
              
              Integer numberOfPossibleBooks = Integer.valueOf(
                  response.getString("totalItems"));
              
              if (numberOfPossibleBooks > 0) {
                
                Book B = parseJSONintoBook(response);
                B.setLentBooks(0);
                B.setBooksOnLoan(0);
                
                Map<String, String> tags = B.getTags();
                
                List<String> title = cleanTitle(B.getTitle());
                
                for (String s : title) {
                  tags.put(s, "true");
                }
                
                for (String s : B.getAuthors().keySet()) {
                  tags.put(cleanAuthor(s), "true");
                }
                tags.put(B.getISBN(), "true");
                if (!B.getPublisher().equals("")) {
                  List<String> pubs = cleanTitle(B.getPublisher());
                  for (String s : pubs) {
                    tags.put(s, "true");
                  }
                }
                
                FirebaseDatabase db = FirebaseDatabase.getInstance();
                
                DatabaseReference dbref = db.getReference("BOOKS").child(B.getISBN());
                dbref.setValue(B);
                
                Intent I = new Intent(ShareNewBook.this,
                    CompleteBookRegistration.class);
                I.putExtra("book", B);
                startActivity(I);
                
              } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(ShareNewBook.this);
                builder.setMessage(R.string.book_not_found)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Dialog clicked");
                      }
                    });
                AlertDialog alert = builder.create();
                alert.show();
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
            
          }
        }, new Response.ErrorListener() {
          @Override
          public void onErrorResponse(VolleyError error) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ShareNewBook.this);
            builder.setMessage(R.string.request_error)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    Log.d(TAG, "Dialog clicked");
                  }
                });
            AlertDialog alert = builder.create();
            alert.show();
          }
        });
    
    queue.add(jsonObjectRequest);
  }
  
  
  private Book parseJSONintoBook(JSONObject J) throws JSONException {
    Book toBeReturned = new Book();
    JSONArray items;
    JSONObject book;
    JSONObject bookInfo = null;
    try {
      items = J.getJSONArray("items");
      book = (JSONObject) items.get(0);
      bookInfo = book.getJSONObject("volumeInfo");
    } catch (Exception e) {
      AlertDialog.Builder builder = new AlertDialog.Builder(ShareNewBook.this);
      builder.setMessage(R.string.book_not_found)
          .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              Log.d(TAG, "Dialog clicked");
            }
          });
      AlertDialog alert = builder.create();
      alert.show();
    }
    
    try {
      toBeReturned.setTitle(bookInfo.getString("title"));
    } catch (Exception e) {
      toBeReturned.setTitle("");
    }
    
    try {
      toBeReturned.setPublisher(bookInfo.getString("publisher"));
    } catch (Exception e) {
      toBeReturned.setPublisher("");
    }
    
    try {
      toBeReturned.setYear(bookInfo.getString("publishedDate").substring(0, 4).
          replaceAll("-", "/"));
    } catch (Exception e) {
      toBeReturned.setYear("");
    }
    
    Map<String, String> as = new HashMap<>();
    try {
      JSONArray authors = bookInfo.getJSONArray("authors");
      
      for (int i = 0; i < authors.length(); i++) {
        as.put((String) authors.get(i), (String) authors.get(i));
      }
      toBeReturned.setAuthors(as);
    } catch (Exception e) {
      as.put("", "");
      toBeReturned.setAuthors(as);
    }
    
    try {
      JSONArray industryIdentifiers = bookInfo.getJSONArray("industryIdentifiers");
      JSONObject ISBN13 = (JSONObject) industryIdentifiers.get(1);
      toBeReturned.setISBN(ISBN13.getString("identifier"));
    } catch (Exception E) {
      toBeReturned.setISBN(this.ISBN);
    }
    
    try {
      JSONObject images = bookInfo.getJSONObject("imageLinks");
      toBeReturned.setCover(images.getString("thumbnail"));
    } catch (Exception E) {
      toBeReturned.setCover("");
    }
    
    return toBeReturned;
  }
  
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case Constants.CAMERA_PERMISSION:
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          takePicture();
        } else {
          Toast.makeText(ShareNewBook.this, R.string.denied, Toast.LENGTH_SHORT).show();
        }
    }
  }
  
  private void takePicture() {
    
    if (ContextCompat.checkSelfPermission(ShareNewBook.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(ShareNewBook.this, new String[]{Manifest.permission.CAMERA}, Constants.CAMERA_PERMISSION);
    } else {
      Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      try {
        File img = File.createTempFile("barcode", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        imageUri = FileProvider.getUriForFile(this, "it.polito.mad.greit.project", img);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(takePictureIntent, Constants.REQUEST_IMAGE_CAPTURE);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == Constants.REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      launchMediaScanIntent();
      try {
        Bitmap bitmap = decodeBitmapUri(this, imageUri);
        if (detector.isOperational() && bitmap != null) {
          Frame frame = new Frame.Builder().setBitmap(bitmap).build();
          SparseArray<Barcode> barcodes = detector.detect(frame);
          for (int index = 0; index < barcodes.size(); index++) {
            Barcode code = barcodes.valueAt(index);
            tw_ISBN.setText(code.displayValue);
          }
          if (barcodes.size() == 0) {
            tw_ISBN.setText(R.string.scan_failed);
          }
        } else {
          tw_ISBN.setText(R.string.detector_error);
        }
      } catch (Exception e) {
        Toast.makeText(this, R.string.image_upload_failed, Toast.LENGTH_SHORT)
            .show();
        Log.e(TAG, e.toString());
      }
    }
  }
  
  private void launchMediaScanIntent() {
    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
    mediaScanIntent.setData(imageUri);
    this.sendBroadcast(mediaScanIntent);
  }
  
  protected void onSaveInstanceState(Bundle outState) {
    if (imageUri != null) {
      outState.putString(Constants.SAVED_INSTANCE_URI, imageUri.toString());
      outState.putString(Constants.SAVED_INSTANCE_RESULT, tw_ISBN.getText().toString());
    }
    super.onSaveInstanceState(outState);
  }
  
  private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
    int targetW = 600;
    int targetH = 600;
    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
    bmOptions.inJustDecodeBounds = true;
    BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
    int photoW = bmOptions.outWidth;
    int photoH = bmOptions.outHeight;
    
    int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
    bmOptions.inJustDecodeBounds = false;
    bmOptions.inSampleSize = scaleFactor;
    
    return BitmapFactory.decodeStream(ctx.getContentResolver()
        .openInputStream(uri), null, bmOptions);
  }
  
  private static String cleanAuthor(String s) {
    String[] lasts = s.split(" ");
    String last = lasts[lasts.length - 1];
    last = last.replaceAll("[\\/\\#\\.\\/\\$\\[]", "");
    last = last.toLowerCase();
    return last;
  }
  
  private static List<String> cleanTitle(String s) {
    String[] lasts = s.split(" ");
    List<String> ret = new ArrayList<>();
    String tmp;
    for (String last : lasts) {
      tmp = last.replaceAll("[\\/\\#\\.\\/\\$\\[]", "");
      tmp = tmp.toLowerCase();
      ret.add(new String(tmp));
    }
    return ret;
  }
}