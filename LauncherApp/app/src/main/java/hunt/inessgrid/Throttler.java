package hunt.inessgrid;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by tanderson on 2/20/2015.
 */
public class Throttler extends Thread {

    private static final String TAG = "Throttler";
    private long lastWriteTime;
    private final OutputStream mmOutStream;
    private double maxFrequency;

    // takes in the output stream to write to and the max frequency to send at.
    public Throttler(OutputStream stream, double mF) {
        mmOutStream = stream;
        maxFrequency = mF;
    }

    public void write(String message) {
        if ((System.currentTimeMillis() - lastWriteTime) > (1000/maxFrequency)){
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
                lastWriteTime = System.currentTimeMillis();
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }
}
