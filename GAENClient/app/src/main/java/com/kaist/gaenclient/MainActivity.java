package com.kaist.gaenclient;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import androidx.databinding.DataBindingUtil;

import com.kaist.gaenclient.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author dyoung
 * @author Matt Tyler
 */

public class MainActivity extends Activity{
	protected static final String TAG = "GAEN_Test";
	private static final int REQUEST_FINE_LOCATION = 1;
	private static final int REQUEST_BACKGROUND_LOCATION = 2;
    private static final int REQUEST_ENABLE_BT = 1;
    private String deviceId;

    private ActivityMainBinding mBinding;
    private Handler sHandler;
//    private Handler aHandler;

    // Config variables
    private long SCAN_PERIOD = Config.SCAN_PERIOD;
    private long SCAN_DURATION = Config.SCAN_DURATION;
//    private long ADVERTISE_PERIOD = Config.ADVERTISE_PERIOD;
//    private long ADVERTISE_DURATION = Config.ADVERTISE_DURATION;
    private ParcelUuid SERVICE_UUID = Config.SERVICE_UUID;
    private byte PROTOCOL_VER = Config.PROTOCOL_VER;
    private int advertiseMode = Config.advertiseMode;
    private int advertiseTxPower = Config.advertiseTxPower;
    private int scanMode = Config.scanMode;
    private String serverUrl = Config.serverUrl;

    // Device info
    private String DEVICE_MODEL = Build.MODEL.toLowerCase();
    private String DEVICE_OEM = Build.MANUFACTURER.toLowerCase();

    // Attenuation = TX_power - (RSSI_measured + RSSI_correction)
    // Default values are set to maximize calculated attenuation.
    private byte txPower = 127;
    private byte rssiCorrection = -128;

    // Callbacks
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "Advertisement start failed with code: " + errorCode);
            log("Advertisement start failed with code: " + errorCode);
        }
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "Advertisement start succeeded.");
            log("Advertisement start succeeded.");
        }
    };
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            logScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                logScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            logError("BLE Scan Failed with code " + errorCode);
        }

        private void logScanResult(ScanResult result) {
            Log.i(TAG, "ScanResult:");
            // These four elements are probably the complete information.
            // Reference: https://github.com/AltBeacon/android-beacon-library/blob/master/lib/src/main/java/org/altbeacon/beacon/service/scanner/CycledLeScannerForLollipop.java#L350
            Log.i(TAG, result.getDevice() +  " / " + result.getRssi() + " / " + result.getTimestampNanos());
            ScanRecord scanRecord = result.getScanRecord();
            Log.i(TAG, scanRecord == null ? "(ScanRecord null)" : Arrays.toString(scanRecord.getBytes()));
            log(result.getDevice() +  " / " + result.getRssi() + " / " + result.getTimestampNanos());
            log(scanRecord == null ? "(ScanRecord null)" : Arrays.toString(scanRecord.getBytes()));
        }
    };

    // Bluetooth variables
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothLeScanner mBluetoothLeScanner;
    private boolean mScanning = false;
    private boolean enabledScanning = false;

    /**
     * Lifecycle
     */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Version check & check permission
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
					== PackageManager.PERMISSION_GRANTED) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
							!= PackageManager.PERMISSION_GRANTED) {
						if (!this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
							final AlertDialog.Builder builder = new AlertDialog.Builder(this);
							builder.setTitle("This app needs background location access");
							builder.setMessage("Please grant location access so this app can detect beacons in the background.");
							builder.setPositiveButton(android.R.string.ok, null);
							builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

								@TargetApi(23)
								@Override
								public void onDismiss(DialogInterface dialog) {
									requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
											REQUEST_BACKGROUND_LOCATION);
								}

							});
							builder.show();
						}
						else {
							final AlertDialog.Builder builder = new AlertDialog.Builder(this);
							builder.setTitle("Functionality limited");
							builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.");
							builder.setPositiveButton(android.R.string.ok, null);
							builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

								@Override
								public void onDismiss(DialogInterface dialog) {
								}

							});
							builder.show();
						}
					}
				}
			} else {
				if (!this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
					requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
									Manifest.permission.ACCESS_BACKGROUND_LOCATION},
							REQUEST_FINE_LOCATION);
				}
				else {
					final AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle("Functionality limited");
					builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location access to this app.");
					builder.setPositiveButton(android.R.string.ok, null);
					builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

						@Override
						public void onDismiss(DialogInterface dialog) {
						}

					});
					builder.show();
				}
			}
		}

		// Get bluetooth adapter
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Check bluetooth support
        verifyBluetooth();

        // Get bluetooth advertiser
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        // Set listeners
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.setActivity(this);
        mBinding.advertiseSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { if(isChecked) {
                    enableAdvertising();
                } else {
                    disableAdvertising();
                } }
        });
        mBinding.scanSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { if(isChecked) {
                    enableScan();
                } else {
                    disableScan();
                }}});
        mBinding.fetchConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { fetchConfig(); }
        });
        mBinding.uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadServer();
            }
        });
        mBinding.clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { mBinding.logTextview.setText("");
            }
        });
        mBinding.testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test();
            }
        });

        // Set device id
        deviceId = this.getIntent().getStringExtra("deviceId");
        mBinding.deviceId.setText("Device ID: "+deviceId);

        // Load calibration data
        loadCalibrationData();
	}


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAdvertising();
    }

    /**
     * Permissions
     */

    private void verifyBluetooth() {
        // Check if bluetooth is enabled
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            // Request user to enable it
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        // Check low energy support
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            // Get a newer device
            Log.d(TAG,"No LE Support.");
            finish();
            return;
        }

        // Check advertising
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            // Unable to run the server on this device, get a better device
            Log.d(TAG,"No Advertising Support.");
            finish();
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "fine location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
            case REQUEST_BACKGROUND_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "background location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    private boolean hasPermissions() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
            return false;
        } else if (!hasLocationPermissions()) {
            requestLocationPermission();
            return false;
        }
        return true;
    }

    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        log("Requested user enables Bluetooth. Try starting the scan again.");
    }

    private boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        log("Requested user enable Location. Try starting the scan again.");
    }

    /**
     * Advertising
     */

    // Listener method for scanSwitch
    private void enableAdvertising() {
        if(!hasPermissions()) {
            mBinding.advertiseSwitch.setChecked(false);
            logError("Permissions & bluetooth requirement not met");
        } else {
            log("Enabled advertising.");
            startAdvertising();
        }
    }

    // Listener method for scanSwitch
    private void disableAdvertising() {
        log("Disabled advertising.");
        stopAdvertising();
    }

    private void startAdvertising() {
        if (mBluetoothLeAdvertiser == null) {
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(advertiseMode)
                .setConnectable(true)   // NOTE: This is set to true because we want connection (thus adds a flag)
                .setTimeout(0)
                .setTxPowerLevel(advertiseTxPower)
                .build();

        // Service data payload, as in GAEN protocol
        byte[] RPI = new byte[16], AEM = new byte[4];
        byte[] advertisingBytes = new byte[20];

        // TODO: Customize RPI using deviceId
        // TEST: RPI as "01020304-0506-0708-090a-0b0c0d0e0f10"
        for (int i = 0; i < RPI.length; i++)
            RPI[i] = (byte)(i + 1);
        AEM[0] = PROTOCOL_VER;
        AEM[1] = txPower;
        AEM[2] = AEM[3] = 0;

        System.arraycopy(RPI, 0, advertisingBytes, 0, RPI.length);
        System.arraycopy(AEM, 0, advertisingBytes, RPI.length, AEM.length);

        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceData(SERVICE_UUID, advertisingBytes)
                .addServiceUuid(SERVICE_UUID)
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(false)
                .build();

        Log.i(TAG,"This is the advertising data");
        Log.i(TAG, data.getServiceData().toString());
        Log.i(TAG, data.toString());

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
        log("Started advertising.");
    }

    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            log("Stopped advertising.");
        }
    }

    private void loadCalibrationData() {
        InputStream inputStream = getResources().openRawResource(R.raw.calibration);
        List<String[]> data = new Utils.CSVFile(inputStream).read();

        if (data.size() < 2) {
            logError("Calibration data load failed.");
            return;
        }

        // Load indices
        List<String> headerRow = Arrays.asList(data.remove(0));
        int idx_oem = headerRow.indexOf("oem");
        int idx_model = headerRow.indexOf("model");
        int idx_corr = headerRow.indexOf("rssi correction");
        int idx_tx = headerRow.indexOf("tx");
        int idx_conf = headerRow.indexOf("calibration confidence");

        List<Integer> indices = Arrays.asList(idx_oem, idx_model, idx_corr, idx_tx, idx_conf);
        int maxIndex = Collections.max(indices);

        if (indices.contains(-1)) {
            logError("Calibration data CSV header malformed.");
            return;
        }

        /* 1. Model
         * 2. Average over OEM match
         * 3. Average over all devices
         */

        int allTx = 0, allCorr = 0, allCount = 0, oemTx = 0, oemCorr = 0, oemCount = 0;

        for (String[] deviceData: data) {
            if (deviceData.length < maxIndex + 1)
                continue;

            byte tx, corr;
            try {
                tx = (byte) Integer.parseInt(deviceData[idx_tx]);
                corr = (byte) Integer.parseInt(deviceData[idx_corr]);
            } catch (NumberFormatException ignored) {
                continue;  // ignore malformed data
            }

            // Model match
            if (deviceData[idx_model].toLowerCase().equals(DEVICE_MODEL)) {
                txPower = tx;
                rssiCorrection = corr;
                log(String.format(Locale.getDefault(),
                        "Using model-matched calibration data.\n > OEM: %s (CSV indicates: %s)\n > Model: %s\n > TX_power: %d\n > RSSI_correction: %d\n > Confidence: %s",
                        DEVICE_OEM, deviceData[idx_oem].toLowerCase(), DEVICE_MODEL, txPower, rssiCorrection, deviceData[idx_conf]));
                return;
            }

            if (deviceData[idx_oem].toLowerCase().equals(DEVICE_OEM)) {
                oemTx += tx;
                oemCorr += corr;
                oemCount++;
            }

            allTx += tx;
            allCorr += corr;
            allCount++;
        }

        if (oemCount > 0) {
            // OEM matches
            txPower = (byte)((float)oemTx / oemCount + 0.5);
            rssiCorrection = (byte)((float)oemCorr / oemCount + 0.5);
            log(String.format(Locale.getDefault(),
                    "Using average of OEM-matched calibration data.\n > OEM: %s\n > Model: %s\n > TX_power: %d\n > RSSI_correction: %d",
                    DEVICE_OEM, DEVICE_MODEL, txPower, rssiCorrection));
        } else if (allCount > 0) {
            // No match, use average of all devices
            txPower = (byte)((float)allTx / allCount + 0.5);
            rssiCorrection = (byte)((float)allCorr / allCount + 0.5);
            log(String.format(Locale.getDefault(),
                    "Using average of all calibration data.\n > OEM: %s\n > Model: %s\n > TX_power: %d\n > RSSI_correction: %d",
                    DEVICE_OEM, DEVICE_MODEL, txPower, rssiCorrection));
        } else {
            logError("Failed to find any valid calibration data.");
        }
    }

    /**
     * Scanning
     */

    // Listener method for scanSwitch
    private void enableScan() {
        enabledScanning = true;
        if(!hasPermissions()) {
            mBinding.scanSwitch.setChecked(false);
            enabledScanning = false;
            logError("Permissions & bluetooth requirement not met");
        } else {
            log("Enabled scanning.");
            //TODO: Turn scanning on/off periodically (always on for now)
            startScan();
        }
    }

    // Listener method for scanSwitch
    private void disableScan() {
        enabledScanning = false;
        log("Disabled scanning.");
        stopScan();
    }

    // Start scanning
    private void startScan() {
        if (!hasPermissions() || mScanning) {
            log("Failed to start scan. Permission is not granted, or the device is already scanning.");
            return;
        }
        if (!enabledScanning) {
            log("Failed to start scan. Scanning is disabled now.");
            return;
        }

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // Note: Filtering does not work the same (or at all) on most devices. It also is unable to
        // search for a mask or anything less than a full UUID.
        // Unless the full UUID of the server is known, manual filtering may be necessary.
        // For example, when looking for a brand of device that contains a char sequence in the UUID

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(SERVICE_UUID)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(scanFilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(scanMode)
                .setReportDelay(0)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        sHandler = new Handler();
        sHandler.postDelayed(this::stopScan, SCAN_DURATION);

        mScanning = true;
        log("Started scanning.");
    }

    // Stop scanning
    private void stopScan() {
        sHandler = null;
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            if (enabledScanning) {
                sHandler = new Handler();
                sHandler.postDelayed(this::startScan, SCAN_PERIOD - SCAN_DURATION);
            }
            log("Stopped scanning.");
        } else if (!mScanning) {
            log("Failed to stop scanning. The device is not scanning now.");
        } else {
            logError("Failed to stop scanning. mScanning: " + mScanning + ", mBluetoothAdapter: " +mBluetoothAdapter + ", isEnables: " + mBluetoothAdapter.isEnabled() + ", mBleutoothLeScanner: " + mBluetoothLeScanner);
        }
        mScanning = false;
    }

    /**
     * Logging
     */

    //TODO: Logging is only done on screen. Must save it somewhere.

    private void log(String msg) {
	    runOnUiThread(() -> mBinding.logTextview.setText(msg + "\n" + mBinding.logTextview.getText()));
    }

    private void logError(String msg) {
        log("Error: " + msg);
    }

    /**
     * Server related functions
     */

    //TODO: Implement server & related functions
    private void uploadServer() {
        log("Upload data.");
    }

    //TODO: Implement server & related functions
    private void fetchConfig() {
        log("Fetch config.");
    }

    /**
     * Test functions
     */
    private void test() {
        new Thread(){
            @Override
            public void run() {
                try {
                    URL url = new URL("http://" + serverUrl + "/config");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET"); //전송방식
                    connection.setDoOutput(false);       //데이터를 쓸 지 설정
                    connection.setDoInput(true);        //데이터를 읽어올지 설정
                    connection.setConnectTimeout(2000);
                    connection.setReadTimeout(2000);

                    StringBuilder sb = new StringBuilder();
                    InputStream is = connection.getInputStream();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String result;
                        while ((result = br.readLine()) != null) {
                            sb.append(result).append("\n");
                        }
                    }
                    log("config: " + sb.toString());
                } catch (SocketTimeoutException e) {
                    logError("Socket timed out.");
                    e.printStackTrace();
                } catch (IOException e) {
                    logError("IOException raised.");
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
