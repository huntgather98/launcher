package hunt.inessgrid;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.content.SharedPreferences;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainMenu extends Activity implements OnTouchListener{

    EditText txtID, txtSleepTime, txtUpCommand, txtDownCommand, txtLeftCommand, txtRightCommand, txtLoadCommand, txtFireCommand;
    Button btnSave;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnTouchListener(this);

        txtID = (EditText) findViewById(R.id.txtID);
        txtID.setOnTouchListener(this);
        txtSleepTime = (EditText) findViewById(R.id.txtSleepTime);
        txtSleepTime.setOnTouchListener(this);
        txtUpCommand = (EditText) findViewById(R.id.txtUpCommand);
        txtUpCommand.setOnTouchListener(this);
        txtDownCommand = (EditText) findViewById(R.id.txtDownCommand);
        txtDownCommand.setOnTouchListener(this);
        txtLeftCommand = (EditText) findViewById(R.id.txtLeftCommand);
        txtLeftCommand.setOnTouchListener(this);
        txtRightCommand = (EditText) findViewById(R.id.txtRightCommand);
        txtRightCommand.setOnTouchListener(this);
        txtLoadCommand = (EditText) findViewById(R.id.txtLoadCommand);
        txtLoadCommand.setOnTouchListener(this);
        txtFireCommand = (EditText) findViewById(R.id.txtFireCommand);
        txtFireCommand.setOnTouchListener(this);

        SharedPreferences settings = getSharedPreferences("settings", 0);
        txtID.setText(settings.getString("ID", "20:14:04:23:26:09"));
        txtSleepTime.setText(String.valueOf(settings.getLong("SleepTime", 100)));
        txtUpCommand.setText(settings.getString("UpCommand", "U"));
        txtDownCommand.setText(settings.getString("DownCommand", "D"));
        txtLeftCommand.setText(settings.getString("LeftCommand", "L"));
        txtRightCommand.setText(settings.getString("RightCommand", "R"));
        txtLoadCommand.setText(settings.getString("LoadCommand", "C"));
        txtFireCommand.setText(settings.getString("FireCommand", "F"));
    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.btnSave){
            if (event.getAction() == MotionEvent.ACTION_UP){
                SharedPreferences settings = getSharedPreferences("settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("ID", txtID.getText().toString());
                editor.putLong("SleepTime", Long.parseLong(txtSleepTime.getText().toString()));
                editor.putString("UpCommand", txtUpCommand.getText().toString());
                editor.putString("DownCommand", txtDownCommand.getText().toString());
                editor.putString("LeftCommand", txtLeftCommand.getText().toString());
                editor.putString("RightCommand", txtRightCommand.getText().toString());
                editor.putString("LoadCommand", txtLoadCommand.getText().toString());
                editor.putString("FireCommand", txtFireCommand.getText().toString());
                editor.commit();
            }
        }

		return false;
	}
	

	@Override
	public void onResume() {
        super.onResume();
	}

	@Override
	public void onPause() {
        super.onPause();
	}

	private void errorExit(String title, String message){
		Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
		finish();
	}

}
