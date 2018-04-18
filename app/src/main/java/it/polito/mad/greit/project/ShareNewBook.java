package it.polito.mad.greit.project;

import android.Manifest;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;

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
    t.setTitle("Share a new book");
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
      ISBNValidator V = new ISBNValidator(et_ISBN.getText().toString());
      if (V.isValid()) {
        getBookInfo(et_ISBN.getText().toString());
      } else {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("ISBN is not valid 😕")
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
  
  public void getBookInfo(String ISBN) {
    RequestQueue queue = Volley.newRequestQueue(this);
    String url = "https://www.googleapis.com/books/v1/volumes?q=" + ISBN;
    
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
        (Request.Method.GET, url, (String) null, new Response.Listener<JSONObject>() {
          @Override
          public void onResponse(JSONObject response) {
            try {
              
              Integer numberOfPossibleBooks = Integer.valueOf(response.getString("totalItems"));
              
              if (numberOfPossibleBooks > 0) {
                
                SharedBook B = parseJSONintoBook(response);
                
                Intent I = new Intent(ShareNewBook.this, CompleteBookRegistration.class);
                I.putExtra("book", B);
                startActivity(I);
                
              } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(ShareNewBook.this);
                builder.setMessage("Book not found 😞")
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
            builder.setMessage("Error during the request 🤯")
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
  
  private SharedBook parseJSONintoBook(JSONObject J) throws JSONException {
    SharedBook toBeReturned = new SharedBook();
    JSONArray items;
    JSONObject book;
    JSONObject bookInfo = null;
    try {
       items = J.getJSONArray("items");
       book = (JSONObject) items.get(0);
       bookInfo = book.getJSONObject("volumeInfo");
    }catch (Exception e){
      AlertDialog.Builder builder = new AlertDialog.Builder(ShareNewBook.this);
      builder.setMessage("Book not found")
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
    }
    catch (Exception e){
      toBeReturned.setTitle("");
    }

    try {
      toBeReturned.setPublisher(bookInfo.getString("publisher"));
    }
    catch (Exception e){
      toBeReturned.setPublisher("");
    }

    try {
      toBeReturned.setYear(bookInfo.getString("publishedDate").substring(0, 5).replaceAll("-", "/"));
    }
    catch (Exception e){
      toBeReturned.setYear("");
    }

    try {
      JSONArray authors = bookInfo.getJSONArray("authors");
      toBeReturned.setAuthor((String) authors.get(0));
    }
    catch (Exception e){
      toBeReturned.setAuthor("");
    }

    toBeReturned.setOwner(FirebaseAuth.getInstance().getCurrentUser().getUid());
    
    //JSONArray industryIdentifiers = bookInfo.getJSONArray("industryIdentifiers");
    //JSONObject ISBN13 = (JSONObject) industryIdentifiers.get(0);
    
    toBeReturned.setISBN(this.ISBN);
    
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
          Toast.makeText(ShareNewBook.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    }
  }
  
  private void takePicture() {

    if (ContextCompat.checkSelfPermission(ShareNewBook.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(ShareNewBook.this, new String[]{Manifest.permission.CAMERA}, Constants.CAMERA_PERMISSION);
    }
    else{
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
            
            //Required only if you need to extract the type of barcode
            int type = barcodes.valueAt(index).valueFormat;
            switch (type) {
              case Barcode.CONTACT_INFO:
                Log.i(TAG, code.contactInfo.title);
                break;
              case Barcode.EMAIL:
                Log.i(TAG, code.email.address);
                break;
              case Barcode.ISBN:
                Log.i(TAG, code.rawValue);
                break;
              case Barcode.PHONE:
                Log.i(TAG, code.phone.number);
                break;
              case Barcode.PRODUCT:
                Log.i(TAG, code.rawValue);
                break;
              case Barcode.SMS:
                Log.i(TAG, code.sms.message);
                break;
              case Barcode.TEXT:
                Log.i(TAG, code.rawValue);
                break;
              case Barcode.URL:
                Log.i(TAG, "url: " + code.url.url);
                break;
              case Barcode.WIFI:
                Log.i(TAG, code.wifi.ssid);
                break;
              case Barcode.GEO:
                Log.i(TAG, code.geoPoint.lat + ":" + code.geoPoint.lng);
                break;
              case Barcode.CALENDAR_EVENT:
                Log.i(TAG, code.calendarEvent.description);
                break;
              case Barcode.DRIVER_LICENSE:
                Log.i(TAG, code.driverLicense.licenseNumber);
                break;
              default:
                Log.i(TAG, code.rawValue);
                break;
            }
          }
          if (barcodes.size() == 0) {
            tw_ISBN.setText("Scan Failed: Found nothing to scan");
          }
        } else {
          tw_ISBN.setText("Could not set up the detector!");
        }
      } catch (Exception e) {
        Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT)
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
}
