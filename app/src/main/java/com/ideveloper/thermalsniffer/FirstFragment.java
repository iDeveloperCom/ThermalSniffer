package com.ideveloper.thermalsniffer;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.ideveloper.thermalsniffer.databinding.FragmentFirstBinding;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FirstFragment extends Fragment {

    @SuppressWarnings("FieldCanBeLocal")
    private final String THERMISTOR_NAME = "TH-Sniffer";

    private static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static boolean FoundCorrectDevice = false;

    private BluetoothDevice connectedDevice = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothLeScanner bluetoothLeScanner = null;

    private static boolean scanning = false;
    private static boolean connected = false;
    private boolean pushedForward = false;
    private boolean pushedBack = false;

    private FragmentFirstBinding binding;
    Animation anim;

    @SuppressLint("MissingPermission")
    public final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == STATE_CONNECTED) {
                connected = true;
                mBluetoothAdapter.cancelDiscovery();
                gatt.discoverServices();
            } else if (newState == STATE_DISCONNECTED) {
                if (!pushedBack) {
                    pushedBack = true;
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(
                            ()-> NavHostFragment.findNavController(FirstFragment.this).popBackStack());
                }
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    UUID uuid = service.getUuid();
                    if (uuid.equals(UUID.fromString("1ee1520a-450e-11ea-b77f-2e728ce88125"))) {
                        List<BluetoothGattCharacteristic> allCharacteristics = service.getCharacteristics();
                        for (BluetoothGattCharacteristic characteristic : allCharacteristics) {
                            uuid = characteristic.getUuid();
                            if (uuid.equals(UUID.fromString("2bbc248c-450e-11ea-b77f-2e728ce88125"))) {
                                gatt.setCharacteristicNotification(characteristic, true);
                                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                                gatt.readCharacteristic(characteristic);
                                if (!pushedForward) {
                                    pushedForward = true;
                                    Handler mainHandler = new Handler(Looper.getMainLooper());
                                    mainHandler.post(
                                            ()-> NavHostFragment.findNavController(FirstFragment.this)
                                                    .navigate(R.id.action_FirstFragment_to_SecondFragment)
                                    );
                                }
                            }
                        }
                        break;
                    }
                }
            } else {
                System.out.println("SERVICE DISCOVERY FAILED !!! ");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            long deviceID = 0;

            byte [] val  = characteristic.getValue();

            for (int i = 0; i < 8; i++) {
                deviceID = (deviceID << 8) + (val[i] & 255);
            }
            byte[] temp = {0, 0, 0, 0};
            temp[3] = val[8]; // characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 2).byteValue();
            temp[2] = val[9]; // characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 3).byteValue();
            temp[1] = val[10]; // characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 4).byteValue();
            temp[0] = val[11]; // characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 5).byteValue();
            float temperature = ByteBuffer.wrap(temp).order(ByteOrder.BIG_ENDIAN).getFloat();

            temp[3] = val[12]; // characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 6).byteValue();
            temp[2] = val[13]; // characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 7).byteValue();
            temp[1] = val[14]; // characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 8).byteValue();
            temp[0] = val[15]; // characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 9).byteValue();
            float wind = ByteBuffer.wrap(temp).order(ByteOrder.BIG_ENDIAN).getFloat() ;

            MainActivity main = (MainActivity) getActivity();
            assert main != null;
            main.passData(deviceID, temperature, wind);
            if (MainActivity.DEVICE_ID == 0) {
                MainActivity.DEVICE_ID = deviceID;
                main.writeDevicePrefs();
            }
        }
    };

    @SuppressLint("MissingPermission")
    public ScanCallback leScanCallback = new ScanCallback() {

        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if ( !FirstFragment.FoundCorrectDevice ) {
                BluetoothDevice device = result.getDevice();
                String dbname = device.getName();
                if ( !connected && (dbname != null) && dbname.equalsIgnoreCase(THERMISTOR_NAME) ) {
                    FirstFragment.FoundCorrectDevice = true;
                    stopScanning();
                    connectedDevice = device;
                    connectedDevice.connectGatt(getContext(), false, mGattCallback);
                }
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void startScanning() {
        if (!scanning) {
            scanning = true;
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .setReportDelay(0L)
                    .build();

            String[] names = new String[]{"TH-Sniffer"};
            UUID BLP_SERVICE_UUID = UUID.fromString("1ee1520a-450e-11ea-b77f-2e728ce88125");
            UUID[] serviceUUIDs = new UUID[]{BLP_SERVICE_UUID};

            List<ScanFilter> filters;
            filters = new ArrayList<>();
            ScanFilter filter;
            for (String name : names) {
                filter = new ScanFilter.Builder()
                        .setDeviceName(name)
                        .build();
                filters.add(filter);
            }
            for (UUID serviceUUID : serviceUUIDs) {
                filter = new ScanFilter.Builder()
                        .setServiceUuid(new ParcelUuid(serviceUUID))
                        .build();
                filters.add(filter);
            }
            bluetoothLeScanner.startScan(filters, scanSettings, leScanCallback);
        }
    }

    @SuppressLint("MissingPermission")
    private void stopScanning() {
        if (scanning) {
            scanning = false;
            mBluetoothAdapter.cancelDiscovery();
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    private void setupAndStartBluetooth() {
        scanning = false;
        connected = false;
        pushedForward = false;
        pushedBack = false;
        connectedDevice = null;
        mBluetoothAdapter = null;
        bluetoothLeScanner = null;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        startScanning();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(650); //You can manage the blinking time with this parameter
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }



    @SuppressWarnings("ConstantConditions")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.textView).startAnimation(anim);
        ImageView bleImage = getView().findViewById(R.id.imageView);
        bleImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.icon_bluetooth_150x150, null));
        setupAndStartBluetooth();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        anim.cancel();
    }

}