package it.polito.mad.greit.project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.*;
import android.util.Log;
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

public class ShowBookActivity extends AppCompatActivity {

    private Toolbar t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_book);

        t = findViewById(R.id.show_book_toolbar);
        t.setTitle("Book");
        setSupportActionBar(t);
        t.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        t.setNavigationOnClickListener(v -> onBackPressed());

        setup();
    }

    public void setup(){
        TextView tv;
        Intent intent = getIntent();
        tv = findViewById(R.id.show_book_title);
        tv.setText(intent.getStringExtra("title"));
        tv = findViewById(R.id.show_book_authors);
        tv.setText(intent.getStringExtra("authors"));
        tv = findViewById(R.id.show_book_ISBN);
        tv.setText(intent.getStringExtra("ISBN"));
        tv = findViewById(R.id.show_book_publisher);
        tv.setText(intent.getStringExtra("pub"));
        tv = findViewById(R.id.show_book_year);
        tv.setText(intent.getStringExtra("yeat"));
        tv = findViewById(R.id.show_book_tags);
        tv.setText(intent.getStringExtra("tags"));

        if (!intent.getStringExtra("key").isEmpty()) {
            StorageReference sr = FirebaseStorage.getInstance().getReference().child("shared_books_pictures/" + intent.getStringExtra("key") + ".jpg");
            sr.getBytes(2*Constants.SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    ImageView iv = findViewById(R.id.show_book_pic);
                    int width = iv.getWidth();
                    int height = iv.getHeight();
                    Bitmap scaled = Bitmap.createScaledBitmap(bm, width, height, false);
                    iv.setImageBitmap(scaled);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                    exception.printStackTrace();
                    ImageView iv = findViewById(R.id.show_book_pic);
                    iv.setImageResource(R.drawable.ic_book_blue_grey_900_48dp);
                }
            });
        }
        else {
            ImageView iv = findViewById(R.id.show_book_pic);
            iv.setImageResource(R.drawable.ic_book_blue_grey_900_48dp);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ShowBookActivity.this, SharedBooksByUser.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
