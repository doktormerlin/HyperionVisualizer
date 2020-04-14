package com.merlbot.hyperionvisualizer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    private static final int FLAG_ACTIVITY_NEW_TASK = 1;
    static boolean isActive = false;
    static boolean hasPermission = false;
    private static final int MY_PERMISSIONS_RECORD_AUDIO = 200;
    private static final String TAG = "MainActiviy";

    private static boolean was_started = false;

    static Intent visualizerIntent = null;
    Button btn_start_stop_visualizer = null;
    Button btn_resume = null;
    EditText edittext_ip_address = null;
    EditText edittext_upd_port = null;
    EditText edittext_led_count = null;
    EditText edittext_visualizer_rate = null;
    EditText edittext_color_rate = null;
    EditText editText_color_starting_offset = null;
    RadioButton radioButton_single_color = null;
    RadioButton radioButton_rainbow = null;
    RadioGroup radioGroup_visualizer_method = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //final SharedPreferences prefs = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        final SharedPreferences prefs = MainActivity.this.getSharedPreferences("testPrefs", Context.MODE_PRIVATE);
        btn_start_stop_visualizer = findViewById(R.id.btn_start_stop_visualizer);
        btn_resume = findViewById(R.id.btn_resume);
        edittext_ip_address = findViewById(R.id.input_ip);
        edittext_upd_port = findViewById(R.id.input_port);
        edittext_led_count = findViewById(R.id.input_led_count);
        edittext_visualizer_rate = findViewById(R.id.input_visualizer_rate);
        edittext_color_rate = findViewById(R.id.input_color_rate);
        editText_color_starting_offset = findViewById(R.id.input_color_starting_offset);
        radioButton_single_color = findViewById(R.id.radiobutton_single_colour);
        radioButton_rainbow = findViewById(R.id.radiobutton_rainbow);
        radioGroup_visualizer_method = findViewById(R.id.radiogroup_visualizer_method);

        visualizerIntent = new Intent(this, VisualizerService.class);

        final Context context = MainActivity.this;

        String prefs_ip_address = prefs.getString("ip_address", null);
        int prefs_port = prefs.getInt("udp_port", -1);
        int prefs_led_count = prefs.getInt("led_count", -1);
        int prefs_visualizer_rate = prefs.getInt("visualizer_rate", -1);
        float prefs_color_rate = prefs.getFloat("color_rate", -1);
        int checked_button = prefs.getInt("checked_button", R.id.radiobutton_single_colour);
        int prefs_color_offset = prefs.getInt("color_offset", -1);

        if(prefs_ip_address != null){
            edittext_ip_address.setText(prefs_ip_address);
        }
        if(prefs_port != -1){
            edittext_upd_port.setText(String.valueOf(prefs_port));
        }
        if(prefs_led_count != -1){
            edittext_led_count.setText(String.valueOf(prefs_led_count));
        }
        if(prefs_visualizer_rate != -1){
            edittext_visualizer_rate.setText(String.valueOf(prefs_visualizer_rate));
        }
        if(prefs_color_rate != -1){
            edittext_color_rate.setText(String.valueOf(prefs_color_rate));
        }
        if(prefs_color_offset != -1){
            editText_color_starting_offset.setText(String.valueOf(prefs_color_offset));
        }

        radioGroup_visualizer_method.check(checked_button);

        requestAudioPermissions();

        edittext_ip_address.setEnabled(!isActive);
        edittext_upd_port.setEnabled(!isActive);
        edittext_led_count.setEnabled(!isActive);
        edittext_visualizer_rate.setEnabled(!isActive);
        edittext_color_rate.setEnabled(!isActive);
        radioButton_rainbow.setEnabled(!isActive);
        radioButton_single_color.setEnabled(!isActive);
        editText_color_starting_offset.setEnabled(!isActive);

        if(isActive){
            btn_start_stop_visualizer.setText(R.string.button_pause);
        }
        else{
            btn_start_stop_visualizer.setText(R.string.button_start);
            if(!was_started){
                //btn_resume.setVisibility(View.GONE);
                //btn_resume.setEnabled(false);
            }
        }

        final TextView text_permission_info = findViewById(R.id.text_permission_info);
        if(!hasPermission){
            btn_start_stop_visualizer.setEnabled(false);
            text_permission_info.setText(R.string.permission_info_no_permission);
        }
        btn_start_stop_visualizer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String ip_address = edittext_ip_address.getText().toString();
                int udp_port = Integer.parseInt(edittext_upd_port.getText().toString());
                int visualizer_rate = Integer.parseInt(edittext_visualizer_rate.getText().toString());
                int led_count = Integer.parseInt(edittext_led_count.getText().toString());
                float color_rate = Float.parseFloat(edittext_color_rate.getText().toString());
                int checked_button = radioGroup_visualizer_method.getCheckedRadioButtonId();
                int color_offset = Integer.parseInt(editText_color_starting_offset.getText().toString());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("ip_address", ip_address);
                editor.putInt("udp_port", udp_port);
                editor.putInt("led_count", led_count);
                editor.putInt("visualizer_rate", visualizer_rate);
                editor.putFloat("color_rate", color_rate);
                editor.putInt("checked_button", checked_button);
                editor.putInt("color_offset", color_offset);
                editor.apply();

                visualizerIntent.putExtra("ip_address", ip_address);
                visualizerIntent.putExtra("port", udp_port);
                visualizerIntent.putExtra("led_count", led_count);
                visualizerIntent.putExtra("visualizer_rate", visualizer_rate);
                visualizerIntent.putExtra("color_rate", color_rate);
                visualizerIntent.putExtra("visualization_method", checked_button);
                visualizerIntent.putExtra("color_offset", color_offset);

                if(isActive){

                    stopService(visualizerIntent);
                    btn_start_stop_visualizer.setText(R.string.button_restart);
                    btn_resume.setVisibility(View.VISIBLE);
                    btn_resume.setEnabled(true);
                }
                else{
                    ComponentName visualizerService = startService(visualizerIntent);
                    if(visualizerService == null){
                        Log.d(TAG, "Service not started");
                    }else{
                        Log.d(TAG, "Service not started");
                    }
                    btn_start_stop_visualizer.setText(R.string.button_pause);
                    //btn_resume.setVisibility(View.GONE);
                    btn_resume.setEnabled(false);
                }

                edittext_ip_address.setEnabled(isActive);
                edittext_upd_port.setEnabled(isActive);
                edittext_led_count.setEnabled(isActive);
                edittext_visualizer_rate.setEnabled(isActive);
                edittext_color_rate.setEnabled(isActive);
                radioButton_rainbow.setEnabled(isActive);
                radioButton_single_color.setEnabled(isActive);
                editText_color_starting_offset.setEnabled(isActive);

                isActive = !isActive;
                was_started = true;

            }
        });

        btn_resume.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent emptyvisualizerIntent = new Intent(MainActivity.this, VisualizerService.class);
                if(isActive){
                    stopService(emptyvisualizerIntent);
                    btn_start_stop_visualizer.setText(R.string.button_restart);
                }
                else{
                    startService(emptyvisualizerIntent);
                    btn_start_stop_visualizer.setText(R.string.button_pause);
                    //btn_resume.setVisibility(View.GONE);
                    btn_resume.setEnabled(false);
                }

                edittext_ip_address.setEnabled(isActive);
                edittext_upd_port.setEnabled(isActive);
                edittext_led_count.setEnabled(isActive);
                edittext_visualizer_rate.setEnabled(isActive);
                edittext_color_rate.setEnabled(isActive);
                radioButton_rainbow.setEnabled(isActive);
                radioButton_single_color.setEnabled(isActive);
                editText_color_starting_offset.setEnabled(isActive);

                isActive = !isActive;

            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            }
        }
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            hasPermission = true;
        }
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    hasPermission = true;
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permissions Denied to record audio", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

}

