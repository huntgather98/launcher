package hunt.inessgrid;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainMenu extends Activity implements OnTouchListener{
	Button btnAnimations, btnSnake, btnLife, btnMagic8Ball, btnDoodle, btnAllOff;

	private static final String TAG = "bluetooth1";
	Handler h;

	final int RECIEVE_MESSAGE = 1;        // Status  for Handler
	private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
	private BluetoothSocket btSocket = null;
	private StringBuilder sb = new StringBuilder();


	private ConnectedThread mConnectedThread;

	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private static String address = "00:12:12:20:01:12";
	
	private byte byteReceived;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);
		
		btnAnimations = (Button) findViewById(R.id.btnAnimations);
		btnSnake = (Button) findViewById(R.id.btnWave);
		btnLife = (Button) findViewById(R.id.btnLife);
		btnMagic8Ball = (Button) findViewById(R.id.btnMagic8Ball);
		btnDoodle = (Button) findViewById(R.id.btnDoodle);
		btnAllOff = (Button) findViewById(R.id.btnAllOff);
		
		btnAnimations.setOnTouchListener(this);
		btnSnake.setOnTouchListener(this);
		btnLife.setOnTouchListener(this);
		btnMagic8Ball.setOnTouchListener(this);
		btnDoodle.setOnTouchListener(this);
		btnAllOff.setOnTouchListener(this);
		
		h = new Handler() {
			public void handleMessage(android.os.Message msg) {
				switch (msg.what) {
				case RECIEVE_MESSAGE:                                                   // if receive massage
					byte[] readBuf = (byte[]) msg.obj;                                // and clear
					byteReceived = readBuf[0];
					break;
				}
			};
		};

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v.getId() == R.id.btnAnimations){
			if (event.getAction() == MotionEvent.ACTION_UP){
				Intent intent = new Intent(MainMenu.this, Animations.class);
				startActivity(intent);
			}
		} else if (v.getId() == R.id.btnWave){
			if (event.getAction() == MotionEvent.ACTION_UP){
				Intent intent = new Intent(MainMenu.this, Snake.class);
				startActivity(intent);
			}
		} else if (v.getId() == R.id.btnLife){
			if (event.getAction() == MotionEvent.ACTION_UP){
				Intent intent = new Intent(MainMenu.this, Life.class);
				startActivity(intent);
			}
		} else if (v.getId() == R.id.btnMagic8Ball){
			if (event.getAction() == MotionEvent.ACTION_UP){
				Intent intent = new Intent(MainMenu.this, Magic8Ball.class);
				startActivity(intent);
			}
		} else if (v.getId() == R.id.btnDoodle){
			if (event.getAction() == MotionEvent.ACTION_UP){
				Intent intent = new Intent(MainMenu.this, Doodle.class);
				startActivity(intent);
			}
		} else if (v.getId() == R.id.btnAllOff){
			if (event.getAction() == MotionEvent.ACTION_UP){
				for (byte i = 0; i < 10; i++){
					mConnectedThread.write("0");
				}
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
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

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
