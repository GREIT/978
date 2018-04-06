package it.polito.mad.greit.project;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;


public class showProfile extends AppCompatActivity {

    Toolbar t;
    Profile profile;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_show_profile);
        t = findViewById(R.id.show_toolbar);
        setSupportActionBar(t);
        Setup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.show_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (R.id.edit == item.getItemId()) {
            Intent swap = new Intent(showProfile.this, editProfile.class);

            swap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(swap);
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    private void Setup() {

        if (ContextCompat.checkSelfPermission(showProfile.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(showProfile.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.STORAGE_PERMISSION);
        }

        profile = Profile.getInstance(getApplicationContext());

        TextView tv = findViewById(R.id.name);
        tv.setText(profile.getName()+" "+profile.getSurname());
        tv = findViewById(R.id.nickname);
        tv.setText(profile.getNickname());
        tv = findViewById(R.id.email);
        tv.setText(profile.getEmail());
        tv = findViewById(R.id.location);
        tv.setText(profile.getLocation());
        tv = findViewById(R.id.biography);
        tv.setText(profile.getBio());

        if (profile.getPic() != null) {
            ImageView iw = findViewById(R.id.pic);
            iw.setImageURI(profile.getPic());
        } else {
            ImageView iw = findViewById(R.id.pic);
            iw.setImageResource(R.mipmap.ic_launcher);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == Constants.STORAGE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("You should grant permissions to storage for the app to work properly")
                    .setPositiveButton(R.string.ok,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    protected void onRestart(){
        super.onRestart();
        if (ContextCompat.checkSelfPermission(showProfile.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(showProfile.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.STORAGE_PERMISSION);
        }
    }
}
