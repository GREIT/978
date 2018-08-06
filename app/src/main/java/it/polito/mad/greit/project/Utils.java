package it.polito.mad.greit.project;

public class Utils {

    //public static double calcDistance(double sourcelat, double sourcelong, double destlat,double destlong){
    public static double calcDistance(String source, String dest){

        String[] s = source.split(";");
        String[] d = dest.split(";");

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
