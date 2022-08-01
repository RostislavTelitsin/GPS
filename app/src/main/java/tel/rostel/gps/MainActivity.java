package tel.rostel.gps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;


import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener, GestureDetector.OnGestureListener {

    public int i = 0;
    private Button button;//Button Start to start workout
    private Button button_reset;//Button Stop to stop workout
    private LocationManager locationManager;//GPS
    private TextView distanceText;//distance monitor
    private TextView accuracyText;//accuracy monitor
    private LocationListener locationListener;//GPS listener
    public Chronometer chronometer;//timer
    private boolean isRunning = false;//is workout running
    private long pauseOffset;// for chronometer pause to keep correct time of workout
    private ProgressBar progressBar;//to wait decision if workout should be stopped and reset
//    private GestureDetector gestureDetector;
    private volatile boolean stopthread = false;
    private boolean ifReset = false;
    private final Handler mainHandler = new Handler();
    private final Handler mainHandler2 = new Handler();
    private double distance = 0;
    private Coordinates currentCoordinates = new Coordinates(0,  0,  0);
    private Coordinates prevCoordinates = new Coordinates(0,  0,  0);
    private long backPressedTime;
    private final double LOCATION_ACCURACY = 3.5;
    private final double MIN_DISTANCE_INCREMENT = 0.005;



// For sensor
    public TextView sensorMonitor;//sensor monitor
    public TextView heartRateMonitor;//sensor monitor


    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private String bluetoothDeviceAddress;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice mySensor;
    private BluetoothLeScanner myScan;
    private int hearbitRate ;


    private BluetoothGattCharacteristic characteristic;
    private BluetoothGattCallback callback;




// For sensor FINISH

    @RequiresApi(api = Build.VERSION_CODES.R)
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);


// background work
        PowerManager pwmng = (PowerManager)getSystemService(Context.POWER_SERVICE);
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wakeLock = pwmng.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();


//For sensor

        sensorMonitor = (TextView) findViewById(R.id.textSensorMonitor);
        heartRateMonitor = (TextView) findViewById(R.id.textViewHeartRate);
        ExtTreadSensor extTreadSensor = new ExtTreadSensor();
        new Thread(extTreadSensor).start();

//For sensor FINISH





//Start chronometer description
        chronometer = findViewById(R.id.chronometerMonitor);
        button = (Button) findViewById(R.id.button);
        button_reset = (Button) findViewById(R.id.resetButton);
        TextView t_lat = (TextView) findViewById(R.id.textLatitude);
        TextView t_long = (TextView) findViewById(R.id.textLongitude);
        TextView t_alt = (TextView) findViewById(R.id.textAltitude);
        distanceText = (TextView) findViewById(R.id.textViewDistance);
        accuracyText = (TextView) findViewById(R.id.textViewAccuracy);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        button_reset.setOnTouchListener(this);
        progressBar.setVisibility(View.INVISIBLE);

// button Stop is invisible till pause of workout
        button_reset.setVisibility(View.INVISIBLE);
//Start chronometer and workout
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                crono();
            }
        });
//Stop chronometer description

//obtaining of location


        if (isLocationEnabled(getBaseContext())) {

            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @SuppressLint(value = "SetTextI18n")
                @Override
                public void onLocationChanged(@NonNull Location location) {

                    if (location.getAccuracy()<LOCATION_ACCURACY){
                        currentCoordinates.setAll(location.getLatitude(), location.getLongitude(), location.getAltitude());
                        t_alt.setText(Double.toString(currentCoordinates.getAltitude()));
                        t_lat.setText(Double.toString(currentCoordinates.getLatitude()));
                        t_long.setText(Double.toString(currentCoordinates.getLongitude()));
                    }




                    accuracyText.setText(String.valueOf(location.getAccuracy()));
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


//is location enabled in settings?
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

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            return;
        } else {
            Toast.makeText(getBaseContext(), "Вы уверены? \n нажмите ВЫХОД еще", Toast.LENGTH_SHORT).show();
        }
        backPressedTime = System.currentTimeMillis();
    }

    private void sensorThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                UUID uuid = UUID.fromString(
                        "00001101-0000-1000-8000-00805F9B34FB");
                BluetoothSocket socket = null;
                try {
                    socket = mySensor.createInsecureRfcommSocketToServiceRecord(uuid);

                    Thread.sleep(500);
                    socket.connect();


                    Toast tt = Toast.makeText(getApplicationContext(), "ЗАЕБИСЬ коннект", Toast.LENGTH_LONG);
                    tt.show();

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    Toast tt = Toast.makeText(getApplicationContext(), "Хер коннект", Toast.LENGTH_LONG);
                    tt.show();
                }
            }
        }).start();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    configureButton();
                return;
        }
    }

    @SuppressLint("MissingPermission")
    private void configureButton() {
        locationManager.requestLocationUpdates("gps", 5000, 0, locationListener);
    }



    public void crono() {
        if (isRunning==false) {
            button.setBackgroundColor(Color.RED);
            button.setText("Pause");
            button_reset.setVisibility(View.INVISIBLE);
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            isRunning = true;

        }else {
            button.setText("Continue");
            button_reset.setVisibility(View.VISIBLE);
            chronometer.stop();
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            isRunning = false;
        }
    }

//if button stop is pressed, start new thread for timer to wait for decision if make reset of workout
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            stopthread = false;
            progressBar.setVisibility(View.VISIBLE);
            ExtThreadChrono extThreadChrono = new ExtThreadChrono();
            extThreadChrono.start();


//if button is released, stop timer. if timer is finished (isReset )
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            progressBar.setVisibility(View.INVISIBLE);
            stopthread = true;
            if (ifReset) {
                isRunning = false;
                button_reset.setVisibility(View.INVISIBLE);
                ifReset = false;
            }

        }
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {}

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {}

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

//methor for distance calculation between 2 points
    public double distanceCalc (Coordinates firstPoint, Coordinates secondPoint) {
        double zemR=6371;
        double sin_lat = Math.sin(Math.toRadians((secondPoint.latitude - firstPoint.latitude)/2));
        double sin_longt = Math.sin(Math.toRadians((secondPoint.longitude - firstPoint.longitude)/2));
        double alt = secondPoint.altitude - firstPoint.altitude;
        double rez_temp = sin_lat * sin_lat + (sin_longt * sin_longt *Math.cos(Math.toRadians(firstPoint.latitude)) * Math.cos(Math.toRadians(secondPoint.latitude)));
        double rez = zemR * 2 * Math.atan2(Math.sqrt(rez_temp), Math.sqrt(1- rez_temp));
        if (rez<0.001) {rez = 0;}
        return rez;
    }



// class for thread to wait Stop button trigger
    class ExtThreadChrono extends Thread {

        @Override
        public void run(){
            for (int x =0; x<10; x++) {
                if (stopthread) {
                    stopthread=false;
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (x==9) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            chronometer.stop();
                            chronometer.setBase(SystemClock.elapsedRealtime());
                            pauseOffset = 0;
                            button.setText("Start");
                            button.setBackgroundColor(0xFF6200EE);
                            distance = 0;
                            distanceText.setText(String.format("%.2f", distance). toString() + "км");
                            progressBar.setVisibility(View.INVISIBLE);
                            ifReset = true;//workout can be reset
                        }
                    });
                }}
            }
        }
    class ExtTreadSensor implements Runnable {

        @Override
        public void run() {
            boolean isBtWork = true;
            boolean isSensorFound = false;
            BluetoothGattCallback mGattCallback;
            BluetoothGatt btGatt;

            while (isBtWork) {
                BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
                SystemClock.sleep(100);

                if (mBluetoothAdapter.isEnabled()) {

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            sensorMonitor = (TextView) findViewById(R.id.textSensorMonitor);
                            heartRateMonitor = (TextView) findViewById(R.id.textViewHeartRate);
                            sensorMonitor.append("mBluetoothAdapter.isEnabled\n");
                            Toast tt = Toast.makeText(getApplicationContext(), "mBluetoothAdapter.isEnabled", Toast.LENGTH_LONG);
                            tt.show();
                        }
                    });


                    SystemClock.sleep(100);

                    String aboutbtGatt;
                    List<BluetoothGattService> bluetoothServices;
                    String sensorAddr = null; 
                    Set<BluetoothDevice> btDevices = (Set<BluetoothDevice>) mBluetoothAdapter.getBondedDevices();


                    while (!isSensorFound) {
                        for (BluetoothDevice dev : btDevices) {
                            SystemClock.sleep(100);
                            String adr = dev.getAddress();
                            SystemClock.sleep(100);
                            mySensor = mBluetoothAdapter.getRemoteDevice(adr);
                            mGattCallback = new BluetoothGattCallback() {};
                            btGatt = mySensor.connectGatt(getApplicationContext(), false, mGattCallback, BluetoothDevice.TRANSPORT_LE);

                            SystemClock.sleep(100);
                            bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
                            btGatt.discoverServices();
                            aboutbtGatt = btGatt.toString();
                            SystemClock.sleep(100);
                            bluetoothServices = btGatt.getServices();
                            btGatt.discoverServices();
                            for (int dicCount = 0; dicCount < 10; dicCount++) {
                                if (bluetoothServices.size() == 0) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    mySensor = mBluetoothAdapter.getRemoteDevice(adr);
                                    mGattCallback = new BluetoothGattCallback() {};
                                    btGatt = mySensor.connectGatt(getApplicationContext(), false, mGattCallback, BluetoothDevice.TRANSPORT_LE);

                                    SystemClock.sleep(100);
                                    bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
                                    btGatt.discoverServices();
                                    aboutbtGatt = btGatt.toString();
                                    SystemClock.sleep(100);


                                    bluetoothServices = btGatt.getServices();
                                } else break;
                            }
                            if (bluetoothServices.size()!=0) {
                                for (BluetoothGattService serv: bluetoothServices) {
                                    String servUuid = serv.getUuid().toString();
                                    if (servUuid.equals("0000180d-0000-1000-8000-00805f9b34fb")) {
                                        isSensorFound = true;
                                        sensorAddr = adr;
                                        break;
                                    }
                                }
                            }
                            if (isSensorFound) break;
                        };
                    }




//                    if (!isSensorFound) {
//                        mySensor = mBluetoothAdapter.getRemoteDevice("00:22:D0:80:7F:7A");
//                    } else mySensor = mBluetoothAdapter.getRemoteDevice(sensorAddr);

                    mySensor = mBluetoothAdapter.getRemoteDevice(sensorAddr);

                    mGattCallback = new BluetoothGattCallback() {};
                    btGatt = mySensor.connectGatt(getApplicationContext(), false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
                    SystemClock.sleep(100);
                    bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
                    btGatt.discoverServices();
                    aboutbtGatt = btGatt.toString();
                    SystemClock.sleep(100);
                    bluetoothServices = btGatt.getServices();
                    btGatt.discoverServices();
                    for (int dicCount = 0; dicCount < 10; dicCount++) {
                        if (bluetoothServices.size() == 0) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            bluetoothServices = btGatt.getServices();
                        } else break;
                    }
                    if (bluetoothServices.size() == 0) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                sensorMonitor.append("sensor is NOT available\n");
                            }
                        });


                    } else {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                sensorMonitor.append("sensor is available\n");
                            }
                        });

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        BluetoothGattService btServ = btGatt.getService(UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"));
                        if (btServ!=null) {
                            boolean huynya = true;
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        List<BluetoothGattCharacteristic> btServCharacteristics = btServ.getCharacteristics();
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        BluetoothGattCharacteristic myChar = btServ.getCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"));
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        btGatt.setCharacteristicNotification(myChar, true);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        btGatt.readCharacteristic(myChar);

                        BluetoothGattDescriptor descriptor = myChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        byte[] hhh = descriptor.getValue();

                        if (btGatt.readCharacteristic(myChar)) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    sensorMonitor.append("Характеристика прочитана\n");
                                }
                            });

                        }

                        boolean isCheckHeartBeat = true;
                        while (isCheckHeartBeat) {
                            boolean weGotIt = false;
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            try {
                                hearbitRate = myChar.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                                weGotIt = true;
                            } catch (NullPointerException e) {

                            }

                            finally {}
                            if (hearbitRate ==0) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        heartRateMonitor.setTextSize(8);
                                        heartRateMonitor.setText("не удается получить данные");
                                    }
                                });

                            } else {

                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {

                                        heartRateMonitor.setTextSize(36);
                                        heartRateMonitor.setText(Integer.toString(hearbitRate));
                                    }
                                });

                                weGotIt = false;
                            }
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }





                    }
                } else {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            sensorMonitor = (TextView) findViewById(R.id.textSensorMonitor);
                            sensorMonitor.append("mBluetoothAdapter no enabled\n");
                            Toast tt = Toast.makeText(getApplicationContext(), "mBluetoothAdapter.is NOT Enabled", Toast.LENGTH_LONG);
                            tt.show();
                        }
                    });


                }
            }




        }
    }

}