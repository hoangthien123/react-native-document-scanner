package com.rndocumentscanner;

import android.app.Activity;
import android.content.Intent;
import androidx.activity.result.ActivityResult;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import com.websitebeaver.documentscanner.DocumentScanner;
import com.websitebeaver.documentscanner.constants.DocumentScannerExtra;

import java.util.ArrayList;

public class RNDocumentScannerModule extends ReactContextBaseJavaModule {

    private static final int DOCUMENT_SCAN_REQUEST = 1;
    private Promise scanPromise;
    private DocumentScanner documentScanner;

    private final ActivityEventListener activityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (requestCode == DOCUMENT_SCAN_REQUEST) {
                if (documentScanner != null) {
                    // Use a wrapper ActivityResult object
                    ActivityResult result = new ActivityResult(resultCode, data);
                    documentScanner.handleDocumentScanIntentResult(result);
                }
            }
        }
    };

    public RNDocumentScannerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(activityEventListener);
    }

    @Override
    public String getName() {
        return "RNDocumentScanner";
    }

    @ReactMethod
    public void scanDocument(ReadableMap options, Promise promise) {
        Activity activity = getCurrentActivity();
        
        if (activity == null) {
            promise.reject("NO_ACTIVITY", "Activity doesn't exist");
            return;
        }

        scanPromise = promise;

        // Get options with defaults
        String responseType = options.hasKey("responseType") ? options.getString("responseType") : "imageFilePath";
        Boolean letUserAdjustCrop = options.hasKey("letUserAdjustCrop") ? options.getBoolean("letUserAdjustCrop") : true;
        Integer maxNumDocuments = options.hasKey("maxNumDocuments") ? options.getInt("maxNumDocuments") : 1;
        Integer croppedImageQuality = options.hasKey("croppedImageQuality") ? options.getInt("croppedImageQuality") : 100;

        try {
            // Create document scanner with callbacks
            documentScanner = new DocumentScanner(
                activity,
                (ArrayList<String> documentScanResults) -> {
                    // Success callback
                    WritableMap response = Arguments.createMap();
                    WritableArray scannedImages = Arguments.createArray();
                    
                    for (String imagePath : documentScanResults) {
                        scannedImages.pushString(imagePath);
                    }
                    
                    response.putArray("scannedImages", scannedImages);
                    response.putString("status", "success");
                    
                    if (scanPromise != null) {
                        scanPromise.resolve(response);
                        scanPromise = null;
                    }
                    return null;
                },
                (String errorMessage) -> {
                    // Error callback
                    if (scanPromise != null) {
                        scanPromise.reject("SCAN_ERROR", errorMessage);
                        scanPromise = null;
                    }
                    return null;
                },
                () -> {
                    // Cancel callback
                    WritableMap response = Arguments.createMap();
                    WritableArray emptyArray = Arguments.createArray();
                    response.putArray("scannedImages", emptyArray);
                    response.putString("status", "cancel");
                    
                    if (scanPromise != null) {
                        scanPromise.resolve(response);
                        scanPromise = null;
                    }
                    return null;
                },
                responseType,
                letUserAdjustCrop,
                maxNumDocuments,
                croppedImageQuality
            );

            // Launch the document scanner
            Intent scanIntent = documentScanner.createDocumentScanIntent();
            activity.startActivityForResult(scanIntent, DOCUMENT_SCAN_REQUEST);

        } catch (Exception e) {
            promise.reject("INITIALIZATION_ERROR", "Failed to initialize document scanner: " + e.getMessage());
        }
    }
}
