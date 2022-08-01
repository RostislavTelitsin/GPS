package tel.rostel.gps;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class GpsService extends Service {
    public GpsService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}