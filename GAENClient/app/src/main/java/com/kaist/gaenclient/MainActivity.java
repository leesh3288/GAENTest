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
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.kaist.gaenclient.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static java.lang.Math.max;

/* Includes bluetooth permissions related code written by authors dyoung & Matt Tyler.
 */
public class MainActivity extends Activity{
	protected static final String TAG = "GAEN_Test";
	private static final int REQUEST_FINE_LOCATION = 1;
	private static final int REQUEST_BACKGROUND_LOCATION = 2;
    private static final int REQUEST_ENABLE_BT = 1;
    public String deviceId;

    private ActivityMainBinding mBinding;
    private Handler sHandler;
    private Handler uHandler;

    // Config variables
    private long SCAN_PERIOD = Config.SCAN_PERIOD;
    private long SCAN_DURATION = Config.SCAN_DURATION;
    private long UPLOAD_PERIOD = Config.UPLOAD_PERIOD;
    private long MAX_JITTER = Config.MAX_JITTER;
    private int SERVICE_UUID = Config.SERVICE_UUID;
    private ParcelUuid SERVICE_PARCEL_UUID = Utils.UUIDConvert.convertShortToParcelUuid(SERVICE_UUID);
    private byte PROTOCOL_VER = Config.PROTOCOL_VER;
    private int advertiseMode = Config.advertiseMode;
    private int advertiseTxPower = Config.advertiseTxPower;
    private int scanMode = Config.scanMode;
    private String serverUrl = Config.serverUrl;
    private boolean initJitter = Config.initJitter;
    private final UUID NAMESPACE_GAEN = Config.NAMESPACE_GAEN;

    // Socket
    private SocketManager mSocketManager;

    // Current test id
    private String testId;

    // Device info
    private String DEVICE_MODEL = Build.MODEL.toLowerCase();
    private String DEVICE_OEM = Build.MANUFACTURER.toLowerCase();

    // Attenuation = TX_power - (RSSI_measured + RSSI_correction)
    // Default values are set to maximize calculated attenuation.
    private byte txPower = 127;
    private byte rssiCorrection = -128;

    // This will immediately be replaced with deviceID (previously: UUID v5 of supplied deviceId)
    private UUID RPI_UUID = NAMESPACE_GAEN;

    // Callbacks
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "Advertisement start failed with code: " + errorCode);
            log("Advertisement start failed with code: " + errorCode, true);
        }
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "Advertisement start succeeded.");
            log("Advertisement start succeeded.",true);
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
            ScanLogEntry entry = ScanLogEntry.fromScanResult(result, SERVICE_UUID, PROTOCOL_VER, deviceId, rssiCorrection, testId);
            if (entry == null)
                return;
            log(entry.toString());
            scanned.add(entry);
            scanning.add(entry);
        }
    };

    // Bluetooth variables
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothLeScanner mBluetoothLeScanner;
    private boolean mScanning = false;
    private boolean enabledScanning = false;
    private boolean enabledAdvertising = false;
    private boolean enabledUploading = false;
    private int scanChannelCounter = 0;

    // Scan results
    final List<ScanLogEntry> scanned = Collections.synchronizedList(new ArrayList<ScanLogEntry>());
    final List<ScanLogEntry> scanning = Collections.synchronizedList(new ArrayList<ScanLogEntry>());
    final List<ScanInstance> scanInstances = Collections.synchronizedList(new ArrayList<ScanInstance>());
    private int secondsSinceLastScan = 0;

    // Log
    private ArrayList<String> logArrayList = new ArrayList<>();
    private RecyclerView.Adapter logAdapter;
    private int maxLogs = 200;

    // General Log
    final List<GeneralLogEntry> genLogs = Collections.synchronizedList(new ArrayList<GeneralLogEntry>());

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

        // Set recyclerview and log adapter
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.setActivity(this);
        logAdapter = new LogAdapter(logArrayList);
        mBinding.logRecyclerview.setAdapter(logAdapter);
        mBinding.logRecyclerview.smoothScrollToPosition(max(logArrayList.size()-1,0));

        // Set listeners
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
        mBinding.uploadSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { if(isChecked) {
                    enableUpload();
                } else {
                    disableUpload();
                }}});
        mBinding.fetchConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    fetchConfig();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        mBinding.uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadServer();
            }
        });
        mBinding.clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logArrayList.clear();
                logAdapter.notifyDataSetChanged();
            }
        });
        mBinding.clearScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLog();
                log("Cleared scan log pending for upload.");
            }
        });

        // Set device id
        deviceId = this.getIntent().getStringExtra("deviceId");
        // DeviceId <= 16 bytes
        deviceId = deviceId.substring(0, Math.min(deviceId.length(), 16));
        byte[] padded = new byte[16], deviceIdBytes = deviceId.getBytes();
        System.arraycopy(deviceIdBytes, 0, padded, 0, Math.min(deviceIdBytes.length, padded.length));
        RPI_UUID = Utils.UUIDConvert.asUuid(padded);  // Utils.HashUuidCreator.getSha1Uuid(NAMESPACE_GAEN, deviceId);
        mBinding.deviceId.setText("Device ID: " + deviceId + "\nTest ID: -");

        // Disable button if not prefixed w/ "beacon"
        if (deviceId.length() < 6 || !deviceId.substring(0, 6).equals("beacon")) {
            mBinding.advertiseSwitch.setClickable(false);
            mBinding.scanSwitch.setClickable(false);
            mBinding.uploadSwitch.setClickable(false);
            mBinding.fetchConfigButton.setClickable(false);
            mBinding.uploadButton.setClickable(false);
            mBinding.clearButton.setClickable(false);
            mBinding.clearScanButton.setClickable(false);
        }

        // Load calibration data
        loadCalibrationData();

        // Show current config
        log(String.format(Locale.getDefault(),
                "Current Config:\nSCAN_PERIOD: %d\nSCAN_DURATION: %d\nUPLOAD_PERIOD: %d\nSERVICE_UUID: 0x%04x\nPROTOCOL_VER: 0x%02x\nadvertiseMode: %d\nadvertiseTxPower: %d\nscanMode: %d\ninitJitter: %b\n",
                SCAN_PERIOD, SCAN_DURATION, UPLOAD_PERIOD, SERVICE_UUID, PROTOCOL_VER, advertiseMode, advertiseTxPower, scanMode, initJitter));

        // DEBUG
        addInitLogs();
	}

	@Override
	public void onStart() {
	    super.onStart();
        // Socket connection
        mSocketManager = new SocketManager(deviceId, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSocketManager.disconnectSocket();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAdvertising();
    }

    @Override
    public void onBackPressed() {
	    // do nothing
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
    // Setting advertiseSwitch
    public void setAdvertise(boolean adv) {
        runOnUiThread(() -> mBinding.advertiseSwitch.setChecked(adv));
    }

    // Listener method for advertiseSwitch
    private void enableAdvertising() {
        if(!hasPermissions()) {
            mBinding.advertiseSwitch.setChecked(false);
            logError("Permissions & bluetooth requirement not met");
        } else {
            log("Enabled advertising.",true);
            startAdvertising();
        }
    }

    // Listener method for advertiseSwitch
    private void disableAdvertising() {
        log("Disabled advertising.",true);
        stopAdvertising();
    }

    private void startAdvertising() {
        if (mBluetoothLeAdvertiser == null) {
            return;
        }
        enabledAdvertising = true;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(advertiseMode)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(advertiseTxPower)
                .build();

        AdvertisingSetParameters setParams = new AdvertisingSetParameters.Builder()
                .setInterval(400)
                .setConnectable(true)
                .setTxPowerLevel(advertiseTxPower)
//                .setScannable(true)
                .build();

        // Service data payload, as in GAEN protocol
        // RPI is just deviceId string
        byte[] RPI = deviceId.getBytes();
        byte[] AEM = {PROTOCOL_VER, txPower, 0, 0};
        byte[] advertisingBytes = new byte[20];

        System.arraycopy(RPI, 0, advertisingBytes, 0, RPI.length);
        System.arraycopy(AEM, 0, advertisingBytes, 16, AEM.length);

        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceData(SERVICE_PARCEL_UUID, advertisingBytes)
                .addServiceUuid(SERVICE_PARCEL_UUID)
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(false)
                .build();

        Log.i(TAG,"This is the advertising data");
        Log.i(TAG, data.getServiceData().toString());
        Log.i(TAG, data.toString());

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
//        mBluetoothLeAdvertiser.startAdvertisingSet(setParams,data,null,null,null,mAdvertisingSetCallback);
        log("Started advertising.",true);
    }

    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser != null) {
            enabledAdvertising = false;
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
//            mBluetoothLeAdvertiser.stopAdvertisingSet(mAdvertisingSetCallback);
            log("Stopped advertising.",true);
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

        /* 1. Model w/ conf > 1
         * 2. Heuristics on stripped model name (matches oem & stripped model name) w/ conf > 1
         * 3. Model
         * 4. Average over OEM match
         * 5. Average over all devices
         */

        String[] fullMatch = null, stripMatch = null;
        byte fullTx = 0, fullCorr = 0, stripTx = 0, stripCorr = 0;
        int fullConf = 1, stripConf = 1;
        int allTx = 0, allCorr = 0, allCount = 0, oemTx = 0, oemCorr = 0, oemCount = 0;

        for (String[] deviceData: data) {
            if (deviceData.length < maxIndex + 1)
                continue;

            int conf;
            byte tx, corr;
            try {
                tx = (byte) Integer.parseInt(deviceData[idx_tx]);
                corr = (byte) Integer.parseInt(deviceData[idx_corr]);
                conf = Integer.parseInt(deviceData[idx_conf]);
            } catch (NumberFormatException ignored) {
                continue;  // ignore malformed data
            }

            if (deviceData[idx_oem].toLowerCase().equals(DEVICE_OEM)) {
                // Model match
                if (fullMatch == null && deviceData[idx_model].toLowerCase().equals(DEVICE_MODEL)) {
                    fullTx = tx;
                    fullCorr = corr;
                    fullConf = conf;
                    fullMatch = deviceData;
                }

                if (stripMatch == null && conf > 1 && Utils.strippedModelEquals(deviceData[idx_model].toLowerCase(), DEVICE_MODEL)) {
                    stripTx = tx;
                    stripCorr = corr;
                    stripConf = conf;
                    stripMatch = deviceData;
                }
                oemTx += tx;
                oemCorr += corr;
                oemCount++;
            }

            allTx += tx;
            allCorr += corr;
            allCount++;
        }

        // model match
        if (fullMatch != null && (fullConf > 1 || stripMatch == null)) {
            txPower = fullTx;
            rssiCorrection = fullCorr;
            log(String.format(Locale.getDefault(),
                    "Using model-matched calibration data (confidence %d).\n > OEM: %s\n > Model: %s\n > TX_power: %d\n > RSSI_correction: %d",
                    fullConf, DEVICE_OEM, DEVICE_MODEL, txPower, rssiCorrection));
            return;
        }

        // Heuristics match
        if (stripMatch != null) {
            txPower = stripTx;
            rssiCorrection = stripCorr;
            log(String.format(Locale.getDefault(),
                    "Using heuristically model-matched calibration data (confidence %d).\n > OEM: %s\n > Model: (original) %s -> (heuristics) %s\n > TX_power: %d\n > RSSI_correction: %d",
                    stripConf, DEVICE_OEM, DEVICE_MODEL, stripMatch[idx_model].toLowerCase(), txPower, rssiCorrection));
            return;
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
    // Setting scanSwitch
    public void setScan(boolean scan) {
        runOnUiThread(() -> mBinding.scanSwitch.setChecked(scan));
    }

    // Listener method for scanSwitch
    private void enableScan() {
        enabledScanning = true;
        if(!hasPermissions()) {
            mBinding.scanSwitch.setChecked(false);
            enabledScanning = false;
            logError("Permissions & bluetooth requirement not met");
        } else {
            scanChannelCounter = 0;
            log("Enabled scanning.",true);
            sHandler = new Handler();
            if (initJitter) {
                secondsSinceLastScan = (int) (Math.random() * SCAN_PERIOD);
            } else {
                secondsSinceLastScan = 0;
            }
            sHandler.postDelayed(this::startScan, secondsSinceLastScan);
        }
    }

    // Listener method for scanSwitch
    private void disableScan() {
        enabledScanning = false;
        log("Disabled scanning.",true);
        stopScan();
    }

    // Start scanning
    private void startScan() {
        if (!hasPermissions() || mScanning) {
            log("Failed to start scan. Permission is not granted, or the device is already scanning.",true);
            return;
        }
        if (!enabledScanning) {
            log("Failed to start scan. Scanning is disabled now.",true);
            return;
        }

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // Note: Filtering does not work the same (or at all) on most devices. It also is unable to
        // search for a mask or anything less than a full UUID.
        // Unless the full UUID of the server is known, manual filtering may be necessary.
        // For example, when looking for a brand of device that contains a char sequence in the UUID

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(SERVICE_PARCEL_UUID)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(scanFilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(scanMode)
                .setReportDelay(0)
                .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        sHandler = new Handler();
        sHandler.postDelayed(this::stopScan, SCAN_DURATION / 3);

        mScanning = true;
        log("Started scanning.",true);
    }

    // Stop scanning
    private void stopScan() {
        if (sHandler != null) {
            sHandler.removeCallbacksAndMessages(null);
            sHandler = null;
        }
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            if (enabledScanning) {
                if (++scanChannelCounter < 3) {
                    log("Scanning next channel...", true);
                    mScanning = false;
                    startScan();
                    return;  // aggregate into ScanInstance after 3 cycles pass
                } else {
                    scanChannelCounter = 0;
                    sHandler = new Handler();
                    int jitter = (int) (Math.random() * MAX_JITTER);   // 0 ~ 1.5 min jitter
                    secondsSinceLastScan = (int) (SCAN_PERIOD) - jitter;
                    sHandler.postDelayed(this::startScan, SCAN_PERIOD - SCAN_DURATION - jitter);
                    log("Scan cycle complete.",true);
                }
            } else {
                log("Stopped scanning.",true);
            }
        } else if (!mScanning) {
            log("Failed to stop scanning. The device is not scanning now.",true);
        } else {
            logError("Failed to stop scanning. mScanning: " + mScanning + ", mBluetoothAdapter: " +mBluetoothAdapter + ", isEnables: " + mBluetoothAdapter.isEnabled() + ", mBleutoothLeScanner: " + mBluetoothLeScanner);
        }
        mScanning = false;

        // Aggregating ScanLogEntries
        List<ScanLogEntry> scansToAggregate;
        synchronized (scanning) {
            scansToAggregate = new ArrayList<>(scanning);
            scanning.clear();
        }
        List<ScanInstance> newInstances = ScanInstance.fromScanResults(scansToAggregate, secondsSinceLastScan);
        scanInstances.addAll(newInstances);
        for (ScanInstance si: newInstances) {
            log(si.toString());
        }
    }

    /**
     * Logging
     */

    public void log(String msg) {
        runOnUiThread(() -> {
            logArrayList.add(0,msg);
//            logArrayList.add(0, String.valueOf(logArrayList.size()));
            if(logArrayList.size() > maxLogs) logArrayList.remove(logArrayList.size()-1);
            logAdapter.notifyDataSetChanged();
        });
//	    runOnUiThread(() -> mBinding.logTextview.setText( + "\n" + mBinding.logTextview.getText()));
    }

    public void log(String msg, boolean upload) {
        log(msg);
        if (upload) {
            logGen(msg);
        }
    }

    public void logError(String msg) {
        log("Error: " + msg);
    }

    public void logError(String msg, boolean upload) {
        log("Error: " + msg, upload);
    }

    public void logGen(String msg) {
        GeneralLogEntry gle = GeneralLogEntry.createLog(testId,deviceId,msg);
        if (gle == null) {
            return;
        }
        genLogs.add(gle);
    }

    /**
     * Server related functions
     */

    private void enableUpload() {
        log("Enabled periodic uploading.",true);
        enabledUploading = true;
        uHandler = new Handler();
        uHandler.post(this::periodicUpload);
    }

    private void disableUpload() {
        log("Disabled periodic uploading.",true);
        enabledUploading = false;
        if (uHandler != null) {
            uHandler.removeCallbacksAndMessages(null);
            uHandler = null;
        }
    }

    private void periodicUpload() {
        uploadServer();
        uHandler = new Handler();
        uHandler.postDelayed(this::periodicUpload, (long) ((Math.random() + 1) * 0.5 * UPLOAD_PERIOD));
    }

    public <T extends IJsonConvertible> void uploadConvertibles(String endpoint, List<T> objects) {
        log("uploadConvertibles called for endpoint " + endpoint);

        List<T> toUpload;
        synchronized (objects) {
            toUpload = new ArrayList<T>(objects);
            objects.clear();
        }
        if (toUpload.size() == 0) {
            log("No scan results or logs to upload.");
            return;
        }

        JSONArray jsonArray = new JSONArray();
        for (T entry: toUpload) {
            JSONObject jsonObject = entry.getJSONObject();
            if (jsonObject != null)
                jsonArray.put(jsonObject);
        }
        if (jsonArray.length() == 0) {
            log("No valid scan results to upload.");
            return;
        }

        String jsonMessage = jsonArray.toString();
        log("uploadServer data count: " + jsonArray.length());

        writeToFile(endpoint, jsonMessage);

        HttpURLConnection c = null;
        try {
            URL u = new URL("http://" + serverUrl + endpoint);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("PUT");
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Content-Encoding", "gzip");
            c.setUseCaches(false);
            c.setDefaultUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setDoOutput(true);
            c.setDoInput(true);
            c.setConnectTimeout(2000);
            c.setReadTimeout(300000);

            OutputStreamWriter wr = new OutputStreamWriter(new GZIPOutputStream(c.getOutputStream()));
            wr.write(jsonMessage);
            wr.flush();
            wr.close();

            int status = c.getResponseCode();

            if (status == 200 || status == 201) {
                StringBuilder sb = new StringBuilder();
                InputStream is = c.getInputStream();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String result;
                    while ((result = br.readLine()) != null) {
                        sb.append(result).append("\n");
                    }
                }
                log("uploadServer success, response: " + sb.toString());
            } else {
                StringBuilder sb = new StringBuilder();
                InputStream es = c.getErrorStream();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8))) {
                    String result;
                    while ((result = br.readLine()) != null) {
                        sb.append(result).append("\n");
                    }
                }
                logError("uploadServer failed (" + status + "), response: " + sb.toString());
                objects.addAll(toUpload);  // reinsert data
            }
        } catch (SocketTimeoutException e) {
            logError("uploadServer timed out, data reinserted for later upload.");
            e.printStackTrace();
            objects.addAll(toUpload);
        } catch (IOException e) {
            logError("uploadServer IOException raised, data reinserted for later upload.");
            e.printStackTrace();
            objects.addAll(toUpload);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception e) {
                    logError("uploadServer exception caught while disconnecting.");
                    e.printStackTrace();
                }
            }
        }
    }

    public void uploadRawFile(String endpoint, String filename) {
        HttpURLConnection c = null;
        try {
            URL u = new URL("http://" + serverUrl + endpoint);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("PUT");
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Content-Encoding", "gzip");
            c.setUseCaches(false);
            c.setDefaultUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setDoOutput(true);
            c.setDoInput(true);
            c.setConnectTimeout(2000);
            c.setReadTimeout(300000);

            OutputStreamWriter wr = new OutputStreamWriter(new GZIPOutputStream(c.getOutputStream()));

            wr.write("{ \"title\": \""+filename+"\", \"data\": \"");
            String rff = readFromFile(filename);
            wr.write(rff);
            wr.write(" \" }");
            wr.flush();
            wr.close();

            int status = c.getResponseCode();

            if (status == 200 || status == 201) {
                StringBuilder sb = new StringBuilder();
                InputStream is = c.getInputStream();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String result;
                    while ((result = br.readLine()) != null) {
                        sb.append(result).append("\n");
                    }
                }
                log("uploadRawfile success, response: " + sb.toString());
            } else {
                StringBuilder sb = new StringBuilder();
                InputStream es = c.getErrorStream();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8))) {
                    String result;
                    while ((result = br.readLine()) != null) {
                        sb.append(result).append("\n");
                    }
                }
                logError("uploadRawfile failed (" + status + "), response: " + sb.toString());
            }
        } catch (SocketTimeoutException e) {
            logError("uploadRawfile timed out, data reinserted for later upload.");
            e.printStackTrace();
        } catch (IOException e) {
            logError("uploadRawfile IOException raised, data reinserted for later upload.");
            e.printStackTrace();
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception e) {
                    logError("uploadRawfile exception caught while disconnecting.");
                    e.printStackTrace();
                }
            }
        }
    }

    public void uploadRawlogs() {
        new Thread() {
            @Override
            public void run() {
                log("uploadRawlogs called.");
                uploadSplitFile("/raw_log");
                uploadSplitFile("/raw_log_si");
                uploadSplitFile("/raw_log_gen");
            }
        }.start();
    }

    public void uploadServer() {
        new Thread() {
            @Override
            public void run() {
                log("uploadServer called.");
                uploadConvertibles("/log", scanned);
                uploadConvertibles("/log_si", scanInstances);
                uploadConvertibles("/log_gen", genLogs);
            }
        }.start();
    }

    public void writeToFile(String endpoint, String msg) {
        log("writeToFile called.");
        String type;
        if (endpoint.equals("/log_si")) { type = "s"; }
        else if (endpoint.equals("/log_gen")) { type = "g"; }
        else { type = "l"; }
        String filename = type + "_" + deviceId + "_" + testId;

        try {
            FileOutputStream outputStream = openFileOutput(filename, MODE_APPEND);
            outputStream.write((","+msg.substring(1,msg.length()-1)).getBytes());
//            outputStream.write((msg+"\n").getBytes());
            outputStream.close();
            log("writeToFile Successful", true);
        } catch (IOException e) {
            e.printStackTrace();
            logError("IOexception in writeToFile", true);
        }
        catch (Exception e) {
            e.printStackTrace();
            logError("writeToFile Failed", true);
        }
    }

    public int splitFile(String filename) {
        log("Splitting file.");
        int splitnum = 1;
        FileInputStream inputStream;
        BufferedReader bfr;
        int bit;
        try{
            inputStream = openFileInput(filename);
            bfr = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            FileOutputStream outputStream = openFileOutput(filename + "_" + splitnum, MODE_PRIVATE);
            bfr.read();

            outputStream.write('[');
            int cnt = 0;
            while ((bit = bfr.read()) != -1) {
                if (bit == '"') {
                    outputStream.write('\'');
                } else {
                    outputStream.write(bit);
                }
                cnt++;
                if (cnt == 300000) {
                    cnt = 0;
                    splitnum++;
                    log("Splitting into "+splitnum+" files...");
                    outputStream.close();
                    outputStream = openFileOutput(filename + "_" + splitnum, MODE_PRIVATE);
                }
            }
            outputStream.write(']');
            outputStream.close();
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return splitnum;
    }

    public void uploadSplitFile(String endpoint) {
        String type;

        if (endpoint.equals("/raw_log")) { type = "l"; }
        else if (endpoint.equals("/raw_log_si")) { type = "s"; }
        else { type = "g"; }

        String filename = type + "_" + deviceId + "_" + testId;

        int splitnum = splitFile(filename);

        for (int i=1; i<=splitnum; i++) {
            log("Uploading files... ("+i+"/"+splitnum+")");
            uploadRawFile(endpoint, filename+"_"+i);
        }
    }

    public String readFromFile(String filename) {
        log("readFromFile called.");
        StringBuilder msg = new StringBuilder();

        try {
            log("filename: "+filename);
            FileInputStream inputStream = openFileInput(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;

            while ((line = br.readLine()) != null) {
                msg.append(line);
            }

            inputStream.close();
            log("readFromFile Successful", true);
        } catch (IOException e) {
            e.printStackTrace();
            logError("IOexception in readFromFile", true);
        } catch (Exception e) {
            e.printStackTrace();
            logError("readFromFile Failed", true);
        }

        return msg.toString();
    }

    public void fetchConfig() throws InterruptedException {
        Thread th = new Thread(){
            @Override
            public void run() {
                try {
                    String fetched = getURL("http://" + serverUrl + "/config",  5000);
                    if (fetched == null) {
                        logError("Configuration fetch failed.");
                        return;
                    }
                    JSONObject config = new JSONObject(fetched);
                    SCAN_PERIOD = config.getLong("SCAN_PERIOD");
                    SCAN_DURATION = config.getLong("SCAN_DURATION");
                    MAX_JITTER = config.getLong("MAX_JITTER");
                    UPLOAD_PERIOD = config.getLong("UPLOAD_PERIOD");
                    SERVICE_UUID = config.getInt("SERVICE_UUID");
                    SERVICE_PARCEL_UUID = Utils.UUIDConvert.convertShortToParcelUuid(SERVICE_UUID);
                    PROTOCOL_VER = (byte)config.getInt("version");
                    advertiseMode = config.getInt("advertiseMode");
                    advertiseTxPower = config.getInt("advertiseTxPower");
                    scanMode = config.getInt("scanMode");
                    initJitter = config.getBoolean("initJitter");
                } catch (JSONException e) {
                    logError("JSONException while parsing config.");
                    e.printStackTrace();
                } finally {
                    log(String.format(Locale.getDefault(),
                            "Current Config:\nSCAN_PERIOD: %d\nSCAN_DURATION: %d\nUPLOAD_PERIOD: %d\nSERVICE_UUID: 0x%04x\nPROTOCOL_VER: 0x%02x\nadvertiseMode: %d\nadvertiseTxPower: %d\nscanMode: %d\ninitJitter: %b\n",
                            SCAN_PERIOD, SCAN_DURATION, UPLOAD_PERIOD, SERVICE_UUID, PROTOCOL_VER, advertiseMode, advertiseTxPower, scanMode, initJitter));
                    if (enabledScanning) {
                        runOnUiThread(() -> {
                            disableScan();
                            enableScan();
                        });
                    }
                    if (enabledAdvertising) {
                        runOnUiThread(() -> {
                            disableAdvertising();
                            enableAdvertising();
                        });
                    }
                    if (enabledUploading) {
                        runOnUiThread(() -> {
                            disableUpload();
                            enableUpload();
                        });
                    }
                }
            }
        };
        th.start();
        th.join();
    }

    private String getURL(String url, int timeout) {
        HttpURLConnection c = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setUseCaches(false);
            c.setDefaultUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setDoOutput(false);
            c.setDoInput(true);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            c.connect();
            int status = c.getResponseCode();

            if (status == 200 || status == 201) {
                StringBuilder sb = new StringBuilder();
                InputStream is = c.getInputStream();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String result;
                    while ((result = br.readLine()) != null) {
                        sb.append(result).append("\n");
                    }
                }
                return sb.toString();
            }
        } catch (SocketTimeoutException e) {
            logError("getURL timed out.");
            e.printStackTrace();
        } catch (IOException e) {
            logError("getURL IOException raised.");
            e.printStackTrace();
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception e) {
                   logError("getURL exception caught while disconnecting.");
                   e.printStackTrace();
                }
            }
        }
        return null;
    }

    public void setTestId(String newId) {
        Log.i("TEST_ID", newId);
        testId = newId.substring(0,Math.min(newId.length(), 100));
        Log.i("TEST_ID", testId);
        runOnUiThread(() -> mBinding.deviceId.setText("Device ID: " + deviceId + "\nTest ID: " + testId));
    }

    public void clearLog() {
        scanned.clear();
        scanInstances.clear();
        // clear genLogs?
    }

    /** DEBUG **/
    public void addInitLogs() {
        for (int i=0; i<80000; i++) {
            scanned.add(ScanLogEntry.test());
        }
        for (int i=0; i<20000; i++) {
            scanInstances.add(ScanInstance.test());
        }
        for (int i=0; i<20000; i++) {
            genLogs.add(GeneralLogEntry.test());
        }
    }
}
