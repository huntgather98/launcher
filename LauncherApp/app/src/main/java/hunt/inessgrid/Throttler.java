package hunt.inessgrid;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

/**
 * Created by tanderson on 2/20/2015.
 */
public class Throttler extends Thread {
    // name of object for logging
    private static final String TAG = "Throttler";
    // output stream we will write commands on
    private final OutputStream mmOutStream;
    // maximum frequency to write commands out at
    private long sleepTime;
    // the string that will be transmitted when timer fires
    private String stringToTransmit;
    // semaphore protecting the stringToTransmit string
    private Semaphore semaphore;
    // the thread that handles sending messages
    private Thread thread;

    // takes in the output stream to write to and the max frequency to send at.
    public Throttler(OutputStream stream, long s) {
        mmOutStream = stream;
        sleepTime = s;
        semaphore = new Semaphore(1);
        stringToTransmit = "";
        this.start();
    }

    public void write(String message) throws InterruptedException {
        semaphore.acquire();
        stringToTransmit = message;
        semaphore.release();
    }

    public void start(){
        thread = new Thread(new Runnable() {
            public void run(){
                while (true){
                    try {
                        Thread.sleep(sleepTime);
                        if (stringToTransmit != "") {
                            semaphore.acquire();
                            Log.d(TAG, "...Data to send: " + stringToTransmit + "...");
                            byte[] msgBuffer = stringToTransmit.getBytes();
                            try {
                                mmOutStream.write(msgBuffer);
                            } catch (IOException e) {
                            }
                            semaphore.release();
                        }
                    } catch (InterruptedException e) {
                    }
                }
            }
        });
        thread.start();
    }

    public void terminate(){
        thread.interrupt();
    }
}
