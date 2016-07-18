package shawn.thesis.osmnavigation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.util.GeoPoint;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements FragmentManager.OnBackStackChangedListener,
        MapViewFragment.OnFragmentMapViewListener,
        RouteSearchFragment.OnFragmentRouteSearchListener,
        CommandSettingFragment.OnFragmentCommandListener,
        DeviceScanFragment.OnFragmentDeviceScanListener{

    private MapViewFragment fMapview;
    private RouteSearchFragment fRouteSearch;
    private CommandSettingFragment fCommandSetting;
    private DeviceScanFragment fDeviceScan;

    private Toolbar mToolbar;
    private Menu mMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        //Listen for changes in the back stack
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        //Handle when activity is recreated like on orientation Change
        shouldDisplayHomeUp();

        if(savedInstanceState == null){
            setDefaultFragment();
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }



    private void setDefaultFragment()
    {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        fMapview = new MapViewFragment();
        ft.replace(R.id.main_content, fMapview);
        ft.commit();
    }

    @Override
    public void onStartRouteSearchFragment(){
        if(fRouteSearch == null){
            fRouteSearch = new RouteSearchFragment();
        }
        showOverflowMenu(false);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.hide(fMapview);
        ft.add(R.id.main_content, fRouteSearch);
        ft.addToBackStack(null);
        ft.commit();
    }
    /*
    *
    * */
    @Override
    public void onTurnCommandToBLE(int d){
        if(!connStatus) return;
        switch (d){
            case 0:
                sendValueToBleReceiver(getResources().getString(R.string.light_off),"Off");
                break;
            case 1:
                sendValueToBleReceiver(getResources().getString(R.string.left_command),"Left");
                break;
            case 2:
                sendValueToBleReceiver(getResources().getString(R.string.right_command),"Right");
                break;
            case 3:
                sendValueToBleReceiver(getResources().getString(R.string.forward_command),"Forward");
                break;
            case 4:
                sendValueToBleReceiver(getResources().getString(R.string.stop_command),"Stop");
                break;
            default:
                break;
        }
    }

    @Override
    public void onCommandSettingFragment(){
        if(fCommandSetting == null){
            fCommandSetting = new CommandSettingFragment();
        }
        showOverflowMenu(false);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.hide(fMapview);
        ft.add(R.id.main_content, fCommandSetting);
        ft.addToBackStack(null);
        ft.commit();
    }



    @Override
    public void onUpdateRoad(Road road){
        if(fMapview == null){
            fMapview = new MapViewFragment();
        }
        fMapview.updateRouteLine(road);
//        FragmentManager fm = getSupportFragmentManager();
//        fm.popBackStack();
        onSupportNavigateUp();
//        FragmentTransaction ft = fm.beginTransaction();
//        ft.remove(fRouteSearch);
//        ft.show(fMapview);
//        ft.commit();

    }

    @Override
    public void onMyLocation(){
        GeoPoint p = fMapview.postCurrentPosition();
        fRouteSearch.onCurrentPosition(p);
    }


    @Override
    public void onBackStackChanged() {
        shouldDisplayHomeUp();
    }

    public void shouldDisplayHomeUp(){
        //Enable Up button only  if there are entries in the back stack
        boolean canback = getSupportFragmentManager().getBackStackEntryCount()>0;
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(canback);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        //This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStack();
        showOverflowMenu(true);
        setActionBarTitle("OSMNavigation");
        return true;
    }

    @Override
    protected void onPause(){
        super.onPause();
//        savePrefs();
        unregisterReceiver(mGattUpdateReceiver);
        System.out.println("onPause");
    }

    public void setActionBarTitle(String title){
        mToolbar.setTitle(title);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

//        mPrefs = this.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
//        if(mPrefs.getFloat(PREFS_LONGITUDE, 0f) != 0f){
//            this.mLocationOverlay.setEnabled(true);
//            this.mLocationOverlay.enableMyLocation();
//            this.mapController.setCenter(
//                    new GeoPoint(mPrefs.getFloat(PREFS_LATITUDE,0f),
//                            mPrefs.getFloat(PREFS_LONGITUDE, 0f))
//            );
//            this.mapController.setZoom(mPrefs.getInt(PREFS_ZOOM, 16));
//        }
//        if(mPrefs.getBoolean(PREFS_TRACKING,false)){
//            this.updateUIWithTrackingMode();
//        }
//        System.out.println("onResume");
    }
    public void showOverflowMenu(boolean showMenu){
        if(mMenu == null)
            return;
        mMenu.setGroupVisible(R.id.main_menu_group, showMenu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.action_commands:
                sendValueToBleReceiver(getResources().getString(R.string.stop_command), "stop");
                break;
            case R.id.action_connect:
                connectBLEDevice();
                break;
            case R.id.action_disconnect:
                disConnectBLEDevice();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDeviceScanFragment(){
        if(fDeviceScan == null){
            fDeviceScan = new DeviceScanFragment();
        }
        showOverflowMenu(false);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.hide(fMapview);
        ft.add(R.id.main_content, fDeviceScan);
        ft.addToBackStack(null);
        ft.commit();
    }

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;
    public BluetoothGattCharacteristic mWriteCharacteristic;
    public final static String TAG = "BLE";
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_CONNECTED");
                Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_DISCONNECTED");
                Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
                // Show all the supported services and characteristics on the user interface.
                searchGattServices(mBluetoothLeService.getSupportedGattServices());

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "ACTION_DATA_AVAILABLE");
            }
        }
    };

    private void searchGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        final BluetoothGattCharacteristic characteristic = gattServices.get(2).getCharacteristics().get(0);
        final int charaProp = characteristic.getProperties();
        if (((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) |
                (charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
            mWriteCharacteristic = characteristic;
        }
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.writeCharacteristic(characteristic);
        }
    }

    public void sendValueToBleReceiver(String CommandValue, String turn) {
        byte[] strBytes = hexStringToByteArray(BinaryToHex(CommandValue));
        //mWriteCharacteristic.
        if(mWriteCharacteristic == null) {
            Toast.makeText(this, "disconnected null", Toast.LENGTH_SHORT).show();
            return;
        }
        if (strBytes == null) {
            Toast.makeText(this, "CommandValue null", Toast.LENGTH_SHORT).show();
            return;
        }
        byte[] bytes = this.mWriteCharacteristic.getValue();
        if( Arrays.equals(strBytes, bytes)){
            return;
        }
        mWriteCharacteristic.setValue(strBytes);
        writeCharacteristic(mWriteCharacteristic);
//        MyLogger.getInstance().appendLog(turn);
//        Toast.makeText(this, "Sent!!!", Toast.LENGTH_SHORT).show();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public boolean checkBLESupport(){
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    private boolean connStatus = false;
    public void connectBLEDevice(){
        if (!checkBLESupport()) return;
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else {
            connStatus = mBluetoothLeService.connect(getResources().getString(R.string.ble_device_address));
        }
    }

    public void disConnectBLEDevice(){
        if (mBluetoothLeService != null) {
             mBluetoothLeService.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(REQUEST_ENABLE_BT == requestCode){
            connStatus = mBluetoothLeService.connect(getResources().getString(R.string.ble_device_address));
        }
    }

    private void scanLeDevice(final boolean enable) {
//        if (enable) {
//            // Stops scanning after a pre-defined scan period.
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mScanning = false;
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
////                    getActivity().invalidateOptionsMenu();
//                }
//            }, SCAN_PERIOD);
//            mScanning = true;
//            mBluetoothAdapter.startLeScan(mLeScanCallback);
//        } else {
//            mScanning = false;
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//        }
    }

    // Device scan callBackLinphoneMessageReceived.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            mBluetoothLeService.connect(device.getAddress());
                        }
                    });
                }
            };
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String BinaryToHex(String BinaryValue){
        String HexValue="";
        for(int i =0; i<40; i++)
        {
            HexValue=HexValue+String.valueOf(Integer.toHexString(Integer.parseInt(BinaryValue.substring(0 + 4 * i, 4 + 4 * i), 2)));
        }
        return HexValue;
    }
//    @Override
//    public void onBackToParent(){
//        if(fMapview == null){
//            fMapview = new MapViewFragment();
//        }
//        FragmentManager fm = getSupportFragmentManager();
//        FragmentTransaction ft = fm.beginTransaction();
//        ft.remove(fRouteSearch);
//        ft.show(fMapview);
//        ft.commit();
//    }

}
