package com.rndocumentscanner;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;

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

import com.google.android.gms.common.moduleinstall.ModuleInstall;
import com.google.android.gms.common.moduleinstall.ModuleInstallClient;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

public class RNDocumentScannerModule extends ReactContextBaseJavaModule {

    private static final int DOCUMENT_SCAN_REQUEST = 1001;
    private Promise scanPromise;

    private final ActivityEventListener activityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (requestCode == DOCUMENT_SCAN_REQUEST) {
                handleScanResult(resultCode, data);
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

    private void handleScanResult(int resultCode, Intent data) {
        if (scanPromise == null) {
            return;
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            GmsDocumentScanningResult result = GmsDocumentScanningResult.fromActivityResultIntent(data);

            if (result != null) {
                WritableMap response = Arguments.createMap();
                WritableArray scannedImages = Arguments.createArray();

                // Get scanned page images
                if (result.getPages() != null) {
                    for (GmsDocumentScanningResult.Page page : result.getPages()) {
                        Uri imageUri = page.getImageUri();
                        scannedImages.pushString(imageUri.toString());
                    }
                }

                response.putArray("scannedImages", scannedImages);
                response.putString("status", "success");

                // Get PDF if available
                if (result.getPdf() != null) {
                    WritableMap pdfInfo = Arguments.createMap();
                    pdfInfo.putString("uri", result.getPdf().getUri().toString());
                    pdfInfo.putInt("pageCount", result.getPdf().getPageCount());
                    response.putMap("pdf", pdfInfo);
                }

                scanPromise.resolve(response);
            } else {
                // No result
                WritableMap response = Arguments.createMap();
                WritableArray emptyArray = Arguments.createArray();
                response.putArray("scannedImages", emptyArray);
                response.putString("status", "cancel");
                scanPromise.resolve(response);
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // User cancelled
            WritableMap response = Arguments.createMap();
            WritableArray emptyArray = Arguments.createArray();
            response.putArray("scannedImages", emptyArray);
            response.putString("status", "cancel");
            scanPromise.resolve(response);
        } else {
            scanPromise.reject("SCAN_ERROR", "Document scan failed with result code: " + resultCode);
        }

        scanPromise = null;
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
        boolean galleryImportAllowed = options.hasKey("galleryImportAllowed") ? options.getBoolean("galleryImportAllowed") : false;
        int pageLimit = options.hasKey("pageLimit") ? options.getInt("pageLimit") : 10;
        String resultFormats = options.hasKey("resultFormats") ? options.getString("resultFormats") : "JPEG";
        String scannerMode = options.hasKey("scannerMode") ? options.getString("scannerMode") : "FULL";

        try {
            // Determine scanner mode
            int gmsScannerMode;
            switch (scannerMode.toUpperCase()) {
                case "BASE":
                    gmsScannerMode = GmsDocumentScannerOptions.SCANNER_MODE_BASE;
                    break;
                case "BASE_WITH_FILTER":
                    gmsScannerMode = GmsDocumentScannerOptions.SCANNER_MODE_BASE_WITH_FILTER;
                    break;
                case "FULL":
                default:
                    gmsScannerMode = GmsDocumentScannerOptions.SCANNER_MODE_FULL;
                    break;
            }

            // Build scanner options
            GmsDocumentScannerOptions.Builder optionsBuilder = new GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(galleryImportAllowed)
                .setPageLimit(pageLimit)
                .setScannerMode(gmsScannerMode);

            // Set result formats
            switch (resultFormats.toUpperCase()) {
                case "PDF":
                    optionsBuilder.setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF);
                    break;
                case "JPEG_PDF":
                    optionsBuilder.setResultFormats(
                        GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                        GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                    );
                    break;
                case "JPEG":
                default:
                    optionsBuilder.setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG);
                    break;
            }

            GmsDocumentScanner scanner = GmsDocumentScanning.getClient(optionsBuilder.build());

            // Get the scan intent and start activity
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener(intentSender -> {
                    try {
                        activity.startIntentSenderForResult(
                            intentSender,
                            DOCUMENT_SCAN_REQUEST,
                            null,
                            0,
                            0,
                            0
                        );
                    } catch (IntentSender.SendIntentException e) {
                        if (scanPromise != null) {
                            scanPromise.reject("INTENT_ERROR", "Failed to start scan intent: " + e.getMessage());
                            scanPromise = null;
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (scanPromise != null) {
                        scanPromise.reject("SCAN_ERROR", "Failed to get scan intent: " + e.getMessage());
                        scanPromise = null;
                    }
                });

        } catch (Exception e) {
            promise.reject("INITIALIZATION_ERROR", "Failed to initialize document scanner: " + e.getMessage());
            scanPromise = null;
        }
    }

    @ReactMethod
    public void isGoogleDocumentScannerModuleAvailable(Promise promise) {
        try {
            GmsDocumentScanner scanner = GmsDocumentScanning.getClient(
                new GmsDocumentScannerOptions.Builder().build()
            );
            ModuleInstallClient moduleInstallClient = ModuleInstall.getClient(getReactApplicationContext());

            moduleInstallClient.areModulesAvailable(scanner)
                .addOnSuccessListener(response -> {
                    WritableMap result = Arguments.createMap();
                    result.putBoolean("available", response.areModulesAvailable());
                    promise.resolve(result);
                })
                .addOnFailureListener(e -> {
                    promise.reject("CHECK_ERROR", "Failed to check module availability: " + e.getMessage());
                });
        } catch (Exception e) {
            promise.reject("CHECK_ERROR", "Failed to check module availability: " + e.getMessage());
        }
    }
}
