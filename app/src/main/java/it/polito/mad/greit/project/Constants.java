package it.polito.mad.greit.project;

public class Constants {
    public static final int PLACE_PICKER_REQUEST = 1;
    public static final int CAMERA_PERMISSION = 2;
    public static final int REQUEST_IMAGE_CAPTURE = 3;
    public static final int REQUEST_GALLERY = 4;
    //public static final int FINE_LOCATION_PERMISSION = 5;
    //public static final int COARSE_LOCATION_PERMISSION = 6;
    public static final long SIZE = 7 * 1024 * 1024;
    public static final String SAVED_INSTANCE_URI = "uri";
    public static final String SAVED_INSTANCE_RESULT = "result";
    public static final String SERVER_KEY = "AAAA9r68Kx8:APA91bGYxhGJWNx-u3AWoqYO6qwT5mo8I9fi98EeBALXWmnS-lSBONDSt" +
            "_fZX7CNU604jsLTy1n69pK-sX5qLT69mTdvMUqhMzqzlqPkT5bZF_MHfI6LO4eVRDe1SKcqLAU854RuDDYT";

    //public static double calcDistance(double sourcelat, double sourcelong, double destlat,double destlong){
    public static double calcDistance(String source,String dest){

        String[] s = source.split("-");
        String[] d = source.split("-");

        double sourcelat = Double.parseDouble(s[0]);
        double sourcelong = Double.parseDouble(s[1]);
        double destlat = Double.parseDouble(d[0]);
        double destlong = Double.parseDouble(d[1]);

        //returns distance in meters
        int R = 6371*1000;
        double phi1 = sourcelat*(Math.PI/180);
        double phi2 = destlat*(Math.PI/180);
        double deltaphi = (destlat-sourcelat)*(Math.PI/180);
        double deltalambda = (destlong - sourcelong)*(Math.PI/180);

        double a = Math.sin(deltaphi/2) * Math.sin(deltaphi/2) +
                Math.cos(phi1) * Math.cos(phi2) *
                        Math.sin(deltalambda/2) * Math.sin(deltalambda/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return  R * c;
    }
}
