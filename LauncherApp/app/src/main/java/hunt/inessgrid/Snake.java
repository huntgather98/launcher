package hunt.inessgrid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.Toast;
import com.zerokol.views.JoystickView;
import com.zerokol.views.JoystickView.OnJoystickMoveListener;

public class Snake extends Activity implements OnTouchListener{
    final double JOYSTICK_SEND_FREQUENCY = 20;
    final int deadzone = 20;
    final int speed1end = 50;
    final int speed2end = 80;
    final int speed3end = 100;
    final String UP_COMMAND = "U";
    final String LEFT_COMMAND = "L";
    final String RIGHT_COMMAND = "R";
    final String DOWN_COMMAND = "D";

	Button btnFire, btnLoad;
    JoystickView joystick;
	
	private static final String TAG = "LauncherApp";
	Handler h;

	final int RECIEVE_MESSAGE = 1;        // Status  for Handler
	private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
	private BluetoothSocket btSocket = null;
	private StringBuilder sb = new StringBuilder();
	private Vibrator myVib;

	private ConnectedThread mConnectedThread;
    private Throttler joystickThrottler;

	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private static String address = "00:14:01:03:3A:EB";

	private byte byteReceived;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.snake);

		btnFire = (Button) findViewById(R.id.btnFire);
		btnFire.setOnTouchListener(this);
        btnLoad = (Button) findViewById(R.id.btnLoad);
        btnLoad.setOnTouchListener(this);



        joystick = (JoystickView) findViewById(R.id.joystickView);

		DisplayMetrics metrics = this.getResources().getDisplayMetrics();
		myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        joystick.setOnJoystickMoveListener(new OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                int speed = 0;
                String command;

                if (power <= speed1end) {
                    speed = 1;
                } else if (power <= speed2end) {
                    speed = 2;
                } else if (power <= speed3end) {
                    speed = 3;
                } else {
                    speed = 0;
                }

                if (power > deadzone) {
                    if (angle >= -45 && angle <= 45) {
                        command = UP_COMMAND;
                    } else if (angle > -135 && angle < -45) {
                        command = LEFT_COMMAND;
                    } else if (angle < 135 && angle > 45){
                        command = RIGHT_COMMAND;
                    } else {
                        command = DOWN_COMMAND;
                    }

                    try {
                        joystickThrottler.write(command);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        joystickThrottler.write("");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL/10);

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE: // if receive massage
                        byte[] readBuf = (byte[]) msg.obj; // and clear
                        byteReceived = readBuf[0];
                        break;
                    }
                };
            };
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

	     if (v.getId() == R.id.btnFire){
			if (event.getAction() == MotionEvent.ACTION_UP){
                mConnectedThread.write("F");
                return true;
			}
		} else if (v.getId() == R.id.btnLoad) {
             if (event.getAction() == MotionEvent.ACTION_UP){
                 mConnectedThread.write("C");
                 return true;
             }
         }
		return false;
	}

	private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
		if(Build.VERSION.SDK_INT >= 10){
			try {
				final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
				return (BluetoothSocket) m.invoke(device, MY_UUID);
			} catch (Exception e) {
				Log.e(TAG, "Could not create Insecure RFComm Connection",e);
			}
		}
		return  device.createRfcommSocketToServiceRecord(MY_UUID);
	}

	
	
	@Override
	public void onResume() {
		super.onResume();

		Log.d(TAG, "...onResume - try connect...");
		
		// Set up a pointer to the remote node using it's address.
		
		BluetoothDevice device = btAdapter.getRemoteDevice(address);

		// Two things are needed to make a connection:
		//   A MAC address, which we got above.
		//   A Service ID or UUID.  In this case we are using the
		//     UUID for SPP.
		
		try {
			btSocket = createBluetoothSocket(device);
		} catch (IOException e) {
			errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
		}
		
		// Discovery is resource intensive.  Make sure it isn't going on
		// when you attempt to connect and pass your message.
		
		btAdapter.cancelDiscovery();
		
		// Establish the connection.  This will block until it connects.
		Log.d(TAG, "...Connecting...");
		
		try {
			btSocket.connect();
			Log.d(TAG, "....Connection ok...");
		} catch (IOException e) {
			try {
				btSocket.close();
			} catch (IOException e2) {
				errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
			}
		}

		// Create a data stream so we can talk to server.
		Log.d(TAG, "...Create Socket...");

		mConnectedThread = new ConnectedThread(btSocket);
		mConnectedThread.start();
        joystickThrottler = new Throttler(mConnectedThread.mmOutStream, JOYSTICK_SEND_FREQUENCY);
	}
	
	

	@Override
	public void onPause() {
		super.onPause();

		Log.d(TAG, "...In onPause()...");

		try     {
			btSocket.close();
		} catch (IOException e2) {
			errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
		}
	}
	
	

	private void errorExit(String title, String message){
		Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
		finish();
	}

	private class ConnectedThread extends Thread {
		public final InputStream mmInStream;
		public final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) { }

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[256];  // buffer store for the stream
			int bytes; // bytes returned from read()

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
					h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
				} catch (IOException e) {
					break;
				}
			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(String message) {
			Log.d(TAG, "...Data to send: " + message + "...");
			byte[] msgBuffer = message.getBytes();
			try {
				mmOutStream.write(msgBuffer);
			} catch (IOException e) {
				Log.d(TAG, "...Error data send: " + e.getMessage() + "...");    
			}
		}
	}
}


