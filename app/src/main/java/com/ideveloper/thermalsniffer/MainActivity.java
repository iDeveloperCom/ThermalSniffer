package com.ideveloper.thermalsniffer;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.ideveloper.thermalsniffer.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DataPassListener {

    public static long DEVICE_ID = 0;
    public static int MEAN_TEMP_TIME = 600;

    private SecondFragment secondFragment;

    public void setSecondFragment(SecondFragment secondFragment) {
        this.secondFragment = secondFragment;
    }

    private void startFirstFragment() {
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    ActivityResultLauncher<String[]> mPermissionResultLauncher;
    private boolean isLocationPermissionGranted = false;
    private boolean isScanPermissionGranted = false;
    private boolean isConnectPermissionGranted = false;

    private void handlePermissions() {
        mPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) != null){
                isLocationPermissionGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                if (isLocationPermissionGranted) {
                    startFirstFragment();
                } else {
                    Toast.makeText(getBaseContext(), "No Permission No Thermal Sniffer :-)", Toast.LENGTH_LONG).show();
                    System.exit(1);
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    isScanPermissionGranted = Boolean.TRUE.equals(result.get(Manifest.permission.BLUETOOTH_SCAN));
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    isConnectPermissionGranted = Boolean.TRUE.equals(result.get(Manifest.permission.BLUETOOTH_CONNECT));
                }
                if (isScanPermissionGranted && isConnectPermissionGranted) {
                    startFirstFragment();
                } else {
                    Toast.makeText(getBaseContext(), "No Permission No Thermal Sniffer :-)", Toast.LENGTH_LONG).show();
                    System.exit(1);
                }
            }
        });

        getPermissions();
    }

    // TO-DO ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) ? boolean isGranted = true : permissionRequest.add(Manifest.permission.BLUETOOTH);

    public void getPermissions(){
        List<String> permissionRequest = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            isLocationPermissionGranted = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            if ( !isLocationPermissionGranted) {
                permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                startFirstFragment();
            }
            if (!permissionRequest.isEmpty()) {
                mPermissionResultLauncher.launch(permissionRequest.toArray(new String[0]));

            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) permissionRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)  permissionRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            if ( !permissionRequest.isEmpty() ) {
                mPermissionResultLauncher.launch(permissionRequest.toArray(new String[0]));
            } else {
                startFirstFragment();
            }
        }
    }

    public void writeDevicePrefs() {
        SharedPreferences sharedPref = getSharedPreferences("pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putLong("DEVICE_ID", MainActivity.DEVICE_ID);
        editor.putInt("MEAN_TEMP_TIME", MEAN_TEMP_TIME);
        editor.apply();
    }

    private void readDevicePrefs() {
        SharedPreferences sharedPref = getSharedPreferences("pref", Context.MODE_PRIVATE);
        DEVICE_ID = sharedPref.getLong("DEVICE_ID-TYPE", 0);
        MEAN_TEMP_TIME = sharedPref.getInt("MEAN_TEMP_TIME", 600);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // Keep always awake
        readDevicePrefs();
        if (getSupportActionBar() != null) getSupportActionBar().hide();        // Hide
        handlePermissions();
    }

    @Override
    public void onDestroy() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // Keep always awake
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Back controlled by Bluetooth!", Toast.LENGTH_LONG).show();
    }

    AppBarConfiguration appBarConfiguration;

    @Override
    public boolean onSupportNavigateUp() {

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void passData(long deviceID, float temperature, float wind) {
        if (secondFragment != null) {
            secondFragment.passData(deviceID, temperature, wind);
        }

    }
}