package it.polito.mad.greit.project;


import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.content.Context.MODE_PRIVATE;

public class Profile {

    private Context context;
    private static Profile instance;

    private String name;
    private String surname;
    private String nickname;
    private String location;
    private String bio;
    private String email;
    private Uri pic;

    private static final String __NAME__ = "Mario";
    private static final String __SURNAME__ = "Rossi";
    private static final String __NICKNAME__ = "@mariorossi";
    private static final String __EMAIL__ = "mr@gmail.com";
    private static final String __LOCATION__ = "Turin, Italy";
    private static final String __BIO__ = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam metus eros, maximus non ipsum ac, luctus ultricies urna. Suspendisse dignissim volutpat sodales. Nullam tincidunt lectus vitae dui finibus, nec egestas neque venenatis.";


    private Profile(Context C) {
        this.context = C;

        String profileJSON = loadJSONFromFile();
        if (profileJSON != null) {
            try {
                JSONObject profile = new JSONObject(profileJSON);
                setName(profile.getString("name"));
                setSurname(profile.getString("surname"));
                setNickname(profile.getString("nickname"));
                setEmail(profile.getString("email"));
                setLocation(profile.getString("location"));
                setBio(profile.getString("bio"));
                setPic(Uri.parse(profile.getString("pic")));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Profile getInstance(){
        if (instance == null){
            return null;
        }
        return instance;
    }

    public static Profile getInstance(Context c){
        if (instance == null){
            instance = new Profile(c);
        }
        return instance;
    }

    public String getName() { return this.name; }
    public void setName(String newName) { this.name = newName; }

    public String getSurname() { return surname; }
    public void setSurname(String newSurname) { this.surname = newSurname; }

    public String getNickname() { return nickname; }
    public void setNickname(String newNickname) { this.nickname = newNickname; }

    public String getEmail() { return email; }
    public void setEmail(String newEmail) { this.email = newEmail; }

    public String getLocation() { return location; }
    public void setLocation(String newLocation) { this.location = newLocation; }

    public String getBio() { return bio; }
    public void setBio(String newBio) { this.bio = newBio; }

    public Uri getPic() { return pic; }
    public void setPic(Uri newAvatar) { this.pic = newAvatar; }


    private void createDefaultProfile() {
        this.setName(__NAME__);
        this.setSurname(__SURNAME__);
        this.setNickname(__NICKNAME__);
        this.setEmail(__EMAIL__);
        this.setLocation(__LOCATION__);
        this.setBio(__BIO__);
        this.setPic(null);
    }

    public void saveProfileJSONOnFile() {
        FileOutputStream outputStream;

        try {
            JSONObject profileJSON = new JSONObject();
            profileJSON.put("name", getName());
            profileJSON.put("surname", getSurname());
            profileJSON.put("nickname", getNickname());
            profileJSON.put("email", getEmail());
            profileJSON.put("location", getLocation());
            profileJSON.put("bio", getBio());
            profileJSON.put("pic", getPic());

            outputStream = context.openFileOutput("profile.json", MODE_PRIVATE);
            outputStream.write(profileJSON.toString().getBytes());
            outputStream.close();
        } catch (Exception E) {
            E.printStackTrace();
        }
    }

    private String loadJSONFromFile() {
        String json = null;

        try {
            InputStream inputStream =  context.openFileInput("profile.json");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null )
                    stringBuilder.append(receiveString);

                inputStream.close();
                json = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            createDefaultProfile();
            Log.e("login activity", "ProfileJSON not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read ProfileJSON: " + e.toString());
        }

        return json;
    }
}