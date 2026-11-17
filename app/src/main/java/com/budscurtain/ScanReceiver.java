package com.budscurtain;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.ArrayList;

/** Receives results of the system-held (PendingIntent) BLE scan. */
public final class ScanReceiver extends BroadcastReceiver {
    static final String ACTION = "com.budscurtain.SCAN_RESULT";

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return;
        MonitorService service = MonitorService.instance;
        if (service == null) {
            // The system kept the scan while the process was gone: bring monitoring back.
            Autostart.resume(context, "сигнал BLE");
            return;
        }
        int error = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, 0);
        if (error != 0) { service.onSystemScanFailed(error); return; }
        ArrayList<ScanResult> results = Build.VERSION.SDK_INT >= 33
            ? intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT, ScanResult.class)
            : intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        if (results != null) for (ScanResult result : results) service.onSystemScanResult(result);
    }
}
