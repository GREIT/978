package it.polito.mad.greit.project;

import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;


public class SharedBooksOnMap extends Fragment {
  GoogleMap myMap;
  MapView mMapView;
  String ISBN;
  String currentLocation;
  HashMap<String, String> markerLocation;
  
  private DatabaseReference mSharedBookDb;
  private static final String ARG_PARAM1 = "isbn";
  private static final String ARG_PARAM2 = "location";
  private static final float COORDINATE_OFFSET = 0.00002f;
  private static final int MAX_NUMBER_OF_MARKERS = 10;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      ISBN = (String) getArguments().getString(ARG_PARAM1);
      currentLocation = (String) getArguments().getString(ARG_PARAM2);
    }
    mSharedBookDb = FirebaseDatabase.getInstance().getReference("SHARED_BOOKS");
  }
  
  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_shared_books_on_map, container, false);
  
    markerLocation = new HashMap<>();
    
    mMapView = (MapView) rootView.findViewById(R.id.mapView);
    mMapView.onCreate(savedInstanceState);
  
    mMapView.onResume();
  
    try {
      MapsInitializer.initialize(getActivity().getApplicationContext());
    } catch (Exception e) {
      e.printStackTrace();
    }
  
    mMapView.getMapAsync(new OnMapReadyCallback() {
      @Override
      public void onMapReady(GoogleMap mMap) {
        myMap = mMap;
        String[] myCoordinates = currentLocation.split(";");
        
      
        // For showing a move to my location button
        //myMap.setMyLocationEnabled(true);
  
        Query firebaseSearchQuery = mSharedBookDb.orderByChild("isbn").equalTo(ISBN);
        
        firebaseSearchQuery.addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {
            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
              SharedBook sb = snapshot.getValue(SharedBook.class);
              String[] sbCoordinates = sb.getCoordinates().split(";");
              
              if (!mapAlreadyHasMarkerForLocation(Float.valueOf(sbCoordinates[0]) + ";" + Float.valueOf(sbCoordinates[1]))) markerLocation.put((Float.valueOf(sbCoordinates[0]) + ";" + Float.valueOf(sbCoordinates[1])), "");
              else {
                sbCoordinates = coordinateForMarker(Float.valueOf(sbCoordinates[0]), Float.valueOf(sbCoordinates[1]));
                markerLocation.put((Float.valueOf(sbCoordinates[0]) + ";" + Float.valueOf(sbCoordinates[1])), "");
              }
              
              System.out.println(sbCoordinates[0] + "\t" + sbCoordinates[1]);
              
              LatLng sbPosition = new LatLng(Float.valueOf(sbCoordinates[0]), Float.valueOf(sbCoordinates[1]));
              Marker mk =  mMap.addMarker(new MarkerOptions().position(sbPosition));
              mk.setTag((SharedBook) sb);
            }
          }
          @Override
          public void onCancelled(DatabaseError databaseError) {
          }
        });
        
      
        // For dropping a marker at a point on the Map
        LatLng myPosition = new LatLng(Double.valueOf(myCoordinates[0]), Double.valueOf(myCoordinates[1]));
        
        // For zooming automatically to the location of the marker
        CameraPosition cameraPosition = new CameraPosition.Builder().target(myPosition).zoom(12).build();
        myMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        
        myMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
          @Override
          public boolean onMarkerClick(Marker marker) {
            SharedBook sb = (SharedBook) (marker.getTag());

            Bundle bundle = new Bundle();
            
            bundle.putSerializable("book", sb);
            bundle.putSerializable("currentLocation", currentLocation);
  
            FragmentManager fm = getActivity().getFragmentManager();
            
            SharedBookDetailFragment dialogFragment = new SharedBookDetailFragment();
            dialogFragment.setArguments(bundle);
            dialogFragment.show( fm, "dialog");
            return false;
          }
        });
      }
    });
    
    
    
    return rootView;
  }
  
  private String[] coordinateForMarker(float latitude, float longitude) {
    
    String[] location = new String[2];
    float lat = latitude, lon = longitude;
    int k = 1;
    
    location[0] = lat + "";
    location[1] = lon + "";
    
    while (mapAlreadyHasMarkerForLocation(lat + ";" + lon)) {
      if (k % 2 == 0) {
        lat += (k * COORDINATE_OFFSET);
        lon += (k * COORDINATE_OFFSET);
      } else {
        lat -= (k * COORDINATE_OFFSET);
        lon -= (k * COORDINATE_OFFSET);
      }
      k++;
      location[0] = lat + "";
      location[1] = lon + "";
    }
    
    return location;
  }
  
  private boolean mapAlreadyHasMarkerForLocation(String location) {
    return (markerLocation.containsKey(location));
  }
  
  @Override
  public void onResume() {
    super.onResume();
    mMapView.onResume();
  }
  
  @Override
  public void onPause() {
    super.onPause();
    mMapView.onPause();
  }
  
  @Override
  public void onDestroy() {
    super.onDestroy();
    mMapView.onDestroy();
  }
  
  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mMapView.onLowMemory();
  }
}
