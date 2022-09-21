package tel.rostel.gps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class GpsService extends Service {
    private static final String CHANNEL_ID = "MyCh";
    private LocationManager locationManager;
    private LocationListener locationListener;
    private final double LOCATION_ACCURACY = 3.5;
    private Coordinates currentCoordinates = new Coordinates(0,  0,  0);
    private Coordinates prevCoordinates = new Coordinates(0,  0,  0);

    public GpsService() {
    }

    Handler mHandler = new Handler();


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();}


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        final boolean[] bb = {intent.getBooleanExtra("isRunning", true)};
                        while (bb[0]) {
                            Log.e("MyService","Working...");
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            Log.e("MyService isRunning", "пробуем принять");
                            IntentFilter intentFilter = new IntentFilter("intentStopRunning");
                            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                                @Override
                                public void onReceive(Context context, Intent intent) {
                                    if(intent.getAction().equals("intentStopRunning")){
                                        Log.e("MyService isRunning", "что то пришло");
                                        boolean b;
                                        b = intent.getBooleanExtra("isRunning", true);
                                        bb[0] = b;
                                        Log.e("MyService isRunning", String.valueOf(bb));
                                    }
                                }
                            };
                            registerReceiver(broadcastReceiver, intentFilter);
                        }
                    }
                }
        ).start();

        final String CHANNELID = "Foreground service ID";
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    CHANNELID,
                    CHANNELID,
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            Notification.Builder notification = new Notification.Builder(this, CHANNELID)
                    .setContentText("Running service")
                    .setContentTitle("Running service enabled")
                    .setSmallIcon(R.drawable.girl);
            startForeground(1001, notification.build());
        }





        return super.onStartCommand(intent, flags, startId);

    }




    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    public void locationGetting() {
        if (isLocationEnabled(getBaseContext())) {

            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @SuppressLint(value = "SetTextI18n")
                @Override
                public void onLocationChanged(@NonNull Location location) {

                    if (location.getAccuracy()<LOCATION_ACCURACY){
                        currentCoordinates.setAll(location.getLatitude(), location.getLongitude(), location.getAltitude());
                        /*t_alt.setText(Double.toString(currentCoordinates.getAltitude()));
                        t_lat.setText(Double.toString(currentCoordinates.getLatitude()));
                        t_long.setText(Double.toString(currentCoordinates.getLongitude()));*/
                    }




                    /*accuracyText.setText(String.valueOf(location.getAccuracy()));*/
                    if (isRunning) {

                        sensorMonitor.append(prevCoordinates.getLatitude() +"\n");
                        sensorMonitor.append(prevCoordinates.getLongitude() +"\n");

                        if (prevCoordinates.latitude==0 && prevCoordinates.longitude==0){
                            prevCoordinates.setAll(currentCoordinates.getLatitude(), currentCoordinates.getLongitude(), currentCoordinates.getAltitude());
                            currentCoordinates.setAll(location.getLatitude(), location.getLongitude(), location.getAltitude());
                            sensorMonitor.append("Метка 2\n");
                        } else if(!prevCoordinates.equals(currentCoordinates) && location.getAccuracy()<LOCATION_ACCURACY){


                            sensorMonitor.append("попытка\n");

                            sensorMonitor.append(prevCoordinates.getLatitude() +"\n");
                            sensorMonitor.append(prevCoordinates.getLongitude() +"\n");
                            sensorMonitor.append(currentCoordinates.getLatitude() +"\n");
                            sensorMonitor.append(currentCoordinates.getLongitude() +"\n");

                            double d = distanceCalc(prevCoordinates, currentCoordinates);
                            sensorMonitor.append(d + " _результат\n");

                            if (d>=MIN_DISTANCE_INCREMENT){
                                distance = distance + d;
                                distanceText.setText(String.format("%.4f", distance). toString() + "км");

                                prevCoordinates.setAll(currentCoordinates.getLatitude(), currentCoordinates.getLongitude(), currentCoordinates.getAltitude());
                            }

                            //currentCoordinates.setAll(location.getLatitude(), location.getLongitude(), location.getAltitude());
                        }
                    }else {
                        //prevCoordinates = currentCoordinates;
                        currentCoordinates.setAll(location.getLatitude(), location.getLongitude(), location.getAltitude());
                        sensorMonitor.append("Метка 3\n");
                    }
                }

                @Override
                public void onProviderEnabled(@NonNull String provider) {}
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProciderDisabled(String s){
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            };
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET
                }, 10);
            }else {
                configureButton();
            }
        } else {
            Toast.makeText(getBaseContext(), "определение местоположения отключено\nпроверь настройки", Toast.LENGTH_LONG).show();
        }
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

}