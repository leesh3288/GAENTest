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
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import androidx.databinding.DataBindingUtil;

import com.kaist.gaenclient.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
//    private ScanCallback mScanCallback = new BtleScanCallback();
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
    private boolean mScanning;

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
                    startAdvertising();
                } else {
                    stopAdvertising();
                } }
        });
        mBinding.scanSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { if(isChecked) {
                    startScan();
                } else {
                    stopScan();
                } }
        });
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

        // Set device id
        deviceId = this.getIntent().getStringExtra("deviceId");
        mBinding.deviceId.setText("Device ID: "+deviceId);
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

    private void startAdvertising() {
        if (mBluetoothLeAdvertiser == null) {
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(Config.advertiseMode)
                .setConnectable(true)   // NOTE: This is set to true because we want connection (thus adds a flag)
                .setTimeout(0)
                .setTxPowerLevel(Config.advertiseTxPower)
                .build();

        // Service data payload, as in GAEN protocol
        byte[] RPI = new byte[16], AEM = new byte[4];
        byte[] advertisingBytes = new byte[20];

        // TODO: Customize advertisingBytes using deviceId
        // TEST: RPI as "01020304-0506-0708-090a-0b0c0d0e0f10"
        // TEST: AEM as 0xc001caf3
        for (int i = 0; i < RPI.length; i++)
            RPI[i] = (byte)(i + 1);
        AEM[0] = (byte)0xc0; AEM[1] = (byte)0x01;
        AEM[2] = (byte)0xca; AEM[3] = (byte)0xf3;

        for (int i = 0; i < RPI.length; i++)
            advertisingBytes[i] = RPI[i];
        for (int i = 0; i < AEM.length; i++)
            advertisingBytes[i + 16] = AEM[i];

        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceData(Config.SERVICE_UUID, advertisingBytes)
                .addServiceUuid(Config.SERVICE_UUID)
                .setIncludeTxPowerLevel(false)  // txPower in AEM
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

    /**
     * Scanning
     */

    //TODO: Turn scanning on/off periodically (always on, for now)
    private void startScan() {
        if (!hasPermissions() || mScanning) {
            return;
        }

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // Note: Filtering does not work the same (or at all) on most devices. It also is unable to
        // search for a mask or anything less than a full UUID.
        // Unless the full UUID of the server is known, manual filtering may be necessary.
        // For example, when looking for a brand of device that contains a char sequence in the UUID

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(Config.SERVICE_UUID)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(scanFilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        mScanning = true;
        log("Started scanning.");
    }

    private void stopScan() {
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
        }

        mScanning = false;

        log("Stopped scanning.");
    }

    /**
     * Logging
     */

    //TODO: Logging is only done on screen. Must save it somewhere.

	private void log(String msg) {
	    mBinding.logTextview.setText(msg+"\n"+mBinding.logTextview.getText());
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
}
