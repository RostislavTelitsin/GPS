package tel.rostel.gps;


public class Coordinates {

    double latitude, longitude, altitude;
    public Coordinates (double latitude, double longitude, double altitude) {
        this.latitude =latitude;
        this.longitude =longitude;
        this.altitude =altitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }



    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }
    public void setAll(double latitude, double longitude, double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }
}
