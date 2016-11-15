package com.tonyostudio.fileextensionscanner;

import android.util.Log;

import com.tonyostudio.library.FileExtSearchService;

/**
 * Created by tonyofrancis on 11/15/16.
 */

public class AppFileScannerService extends FileExtSearchService {

    public static final String TAG = "AppFileScannerService";

    public AppFileScannerService() {
        super(TAG);
    }

    @Override
    public void onResultsDelivered(String[] filePaths) {
        super.onResultsDelivered(filePaths);

        if (filePaths != null && filePaths.length > 0) {
            Log.i(TAG, filePaths.length +" files found");
        } else {
            Log.i(TAG, "No Files found");
        }
    }
}
