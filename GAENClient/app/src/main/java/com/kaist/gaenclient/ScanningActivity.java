package com.kaist.gaenclient;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.scanner.NonBeaconLeScanCallback;
import org.altbeacon.bluetooth.BleAdvertisement;
import org.altbeacon.bluetooth.Pdu;

import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author dyoung
 * @author Matt Tyler
 */
public class ScanningActivity extends Activity implements NonBeaconLeScanCallback {
	protected static final String TAG = "ScanningActivity";
	private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
	private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;
	private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scanning);
		verifyBluetooth();

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
											PERMISSION_REQUEST_BACKGROUND_LOCATION);
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
							PERMISSION_REQUEST_FINE_LOCATION);
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

		beaconManager.setNonBeaconLeScanCallback(this);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_FINE_LOCATION: {
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
			case PERMISSION_REQUEST_BACKGROUND_LOCATION: {
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

	public void onEnableClicked(View view) {
		GAENClientApplication application = ((GAENClientApplication) this.getApplicationContext());
		if (beaconManager.getMonitoredRegions().size() > 0) {
			application.disableScanning();
			((Button)findViewById(R.id.enableButton)).setText("Enable Scanning");
		}
		else {
			((Button)findViewById(R.id.enableButton)).setText("Disable Scanning");
			application.enableScanning();
		}

	}

    @Override
    public void onResume() {
        super.onResume();
		GAENClientApplication application = ((GAENClientApplication) this.getApplicationContext());
        application.setScanningActivity(this);
        updateLog(application.getLog());
    }

    @Override
    public void onPause() {
        super.onPause();
        ((GAENClientApplication) this.getApplicationContext()).setScanningActivity(null);
    }

	private void verifyBluetooth() {
		try {
			if (!beaconManager.checkAvailability()) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Bluetooth not enabled");
				builder.setMessage("Please enable bluetooth in settings and restart this application.");
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						//finish();
			            //System.exit(0);
					}
				});
				builder.show();
			}
		}
		catch (RuntimeException e) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Bluetooth LE not available");
			builder.setMessage("Sorry, this device does not support Bluetooth LE.");
			builder.setPositiveButton(android.R.string.ok, null);
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					//finish();
		            //System.exit(0);
				}

			});
			builder.show();
		}
	}

    public void updateLog(final String log) {
    	runOnUiThread(new Runnable() {
    	    public void run() {
    	    	EditText editText = (EditText) ScanningActivity.this
    					.findViewById(R.id.monitoringText);
       	    	editText.setText(log);
    	    }
    	});
    }

	@Override
	public void onNonBeaconLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		Log.i(TAG, "onNonBeaconLeScan:\ndevice = " + device + ", rssi = " + rssi +
				",\nscanRecord = " + Arrays.toString(scanRecord));
		BleAdvertisement advert = new BleAdvertisement(scanRecord);
		for (Pdu pdu: advert.getPdus()) {
			// 3rd Pdu must exactly match GAEN service data format
			// TODO: enforce exact GAEN payload? (1st flag, 2nd Service UUID, 3rd Service Data)
			int st = pdu.getStartIndex();

			if (pdu.getDeclaredLength() != 0x17 ||
				pdu.getActualLength() + 1 < pdu.getDeclaredLength() ||
				pdu.getType() != Pdu.GATT_SERVICE_UUID_PDU_TYPE ||
				scanRecord[st] != (byte) 0x6f || scanRecord[st + 1] != (byte) 0xfd) {  // 0xfd6f
				continue;
			}
			((GAENClientApplication) this.getApplicationContext()).logToDisplay(
					"scanRecord: " + Arrays.toString(scanRecord) + ", RSSI: " + rssi);
			break;  // this should've been the last pdu
		}
	}
}
