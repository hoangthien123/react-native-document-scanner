# React Native Document Scanner

A React Native library for scanning documents on iOS and Android. This library provides a native document scanning experience using VisionKit for iOS and a document scanner library for Android.

## Features

- 📷 Native document scanning on iOS (VisionKit) and Android
- 🔲 Automatic document detection and edge detection
- ✂️ Automatic cropping and perspective correction
- 📄 Support for scanning multiple documents (Android)
- 🎨 Adjustable image quality
- 📤 Export as base64 or file paths
- 🎯 User-friendly interface with native look and feel
- ⚡ Full TypeScript support

## Installation

```sh
npm install react-native-document-scanner
```

or

```sh
yarn add react-native-document-scanner
```

### iOS Setup

1. **Minimum iOS Version**: iOS 13.0+

2. **Install CocoaPods dependencies**:
   ```sh
   cd ios && pod install
   ```

3. **Add Camera Permission**: Add the following to your `ios/YourApp/Info.plist`:
   ```xml
   <key>NSCameraUsageDescription</key>
   <string>We need camera access to scan documents</string>
   ```

### Android Setup

1. **Minimum SDK**: 21

2. **Add Camera Permission**: The library already includes the necessary permissions in its AndroidManifest.xml, but make sure your app requests runtime permissions. Add the following to your `android/app/src/main/AndroidManifest.xml` if not already present:
   ```xml
   <uses-permission android:name="android.permission.CAMERA" />
   ```

3. **Request Runtime Permissions**: In your React Native component, request camera permission before scanning:
   ```javascript
   import { PermissionsAndroid, Platform } from 'react-native';

   async function requestCameraPermission() {
     if (Platform.OS === 'android') {
       try {
         const granted = await PermissionsAndroid.request(
           PermissionsAndroid.PERMISSIONS.CAMERA,
           {
             title: 'Camera Permission',
             message: 'App needs camera permission to scan documents',
             buttonNeutral: 'Ask Me Later',
             buttonNegative: 'Cancel',
             buttonPositive: 'OK',
           }
         );
         return granted === PermissionsAndroid.RESULTS.GRANTED;
       } catch (err) {
         console.warn(err);
         return false;
       }
     }
     return true;
   }
   ```

## Usage

### Basic Example

```typescript
import React from 'react';
import { Button, View, Text, Image } from 'react-native';
import { DocumentScanner } from 'react-native-document-scanner';

function App() {
  const [scannedImages, setScannedImages] = React.useState<string[]>([]);

  const handleScan = async () => {
    try {
      const result = await DocumentScanner.scanDocument({
        croppedImageQuality: 100,
        responseType: 'imageFilePath',
      });

      if (result.status === 'success') {
        setScannedImages(result.scannedImages);
        console.log('Scanned images:', result.scannedImages);
      } else {
        console.log('User cancelled the scan');
      }
    } catch (error) {
      console.error('Error scanning document:', error);
    }
  };

  return (
    <View>
      <Button title="Scan Document" onPress={handleScan} />
      {scannedImages.map((image, index) => (
        <Image key={index} source={{ uri: image }} style={{ width: 200, height: 300 }} />
      ))}
    </View>
  );
}
```

### Advanced Example with All Options

```typescript
import { DocumentScanner } from 'react-native-document-scanner';

async function scanMultipleDocuments() {
  try {
    const result = await DocumentScanner.scanDocument({
      maxNumDocuments: 3,              // Android only: max number of documents to scan
      letUserAdjustCrop: true,         // Android only: allow manual crop adjustment
      croppedImageQuality: 100,        // Image quality (0-100)
      responseType: 'base64',          // 'base64' or 'imageFilePath'
    });

    if (result.status === 'success') {
      // Process scanned images
      result.scannedImages.forEach((image, index) => {
        console.log(`Image ${index + 1}:`, image.substring(0, 50) + '...');
      });
    }
  } catch (error) {
    console.error('Scan failed:', error);
  }
}
```

## API Reference

### `DocumentScanner.scanDocument(options?)`

Starts the document scanning process.

#### Parameters

- `options` (optional): Configuration object

| Option | Type | Default | Platform | Description |
|--------|------|---------|----------|-------------|
| `maxNumDocuments` | `number` | `1` | Android | Maximum number of documents to scan |
| `letUserAdjustCrop` | `boolean` | `true` | Android | Allow user to manually adjust crop |
| `croppedImageQuality` | `number` | `100` | Both | Image quality from 0 to 100 |
| `responseType` | `'base64' \| 'imageFilePath'` | `'imageFilePath'` | Both | Format of returned images |

#### Returns

`Promise<ScanDocumentResponse>`

```typescript
interface ScanDocumentResponse {
  scannedImages: string[];  // Array of base64 strings or file paths
  status: 'success' | 'cancel';
}
```

#### Errors

The promise will be rejected with an error if:
- Camera permission is denied
- Document scanning is not supported on the device
- An error occurs during scanning
- Device is running iOS < 13.0

## TypeScript Support

This library is written in TypeScript and includes type definitions out of the box.

```typescript
import { 
  DocumentScanner, 
  ScanDocumentOptions, 
  ScanDocumentResponse 
} from 'react-native-document-scanner';
```

## Platform Differences

### iOS
- Uses VisionKit's `VNDocumentCameraViewController`
- Automatically detects document edges
- Provides built-in crop and perspective correction
- Returns multiple pages if user scans multiple documents
- Options `maxNumDocuments` and `letUserAdjustCrop` are ignored

### Android
- Uses `com.websitebeaver:documentscanner` library
- Supports configurable maximum number of documents
- Allows user to adjust crop boundaries
- Supports all configuration options

## Troubleshooting

### iOS Issues

**Problem**: "Document scanning is not supported on this device"
- **Solution**: VisionKit document scanning requires iOS 13.0 or later. Ensure your deployment target is iOS 13.0+.

**Problem**: Camera permission denied
- **Solution**: Make sure you've added `NSCameraUsageDescription` to your Info.plist.

**Problem**: Module not found
- **Solution**: Run `cd ios && pod install && cd ..` and rebuild your app.

### Android Issues

**Problem**: Camera permission denied
- **Solution**: Request camera permission at runtime using `PermissionsAndroid`.

**Problem**: Build errors with Gradle
- **Solution**: Ensure your `android/build.gradle` has `minSdkVersion` 21 or higher.

**Problem**: "Failed to initialize document scanner"
- **Solution**: Make sure the camera permission is granted before calling `scanDocument`.

### General Issues

**Problem**: "The package doesn't seem to be linked"
- **Solution**: 
  - For React Native 0.60+: Autolinking should work automatically. Try cleaning and rebuilding.
  - iOS: Run `cd ios && pod install && cd ..`
  - Android: Clean gradle cache `cd android && ./gradlew clean && cd ..`
  - Rebuild the app completely

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License - see the [LICENSE](LICENSE) file for details.

## Credits

This library is based on the Capacitor Document Scanner plugin and uses:
- iOS: VisionKit framework by Apple
- Android: [Document Scanner Library](https://github.com/websitebeaver/documentscanner) by WebsiteBeaver

## Support

For issues and feature requests, please use the [GitHub issue tracker](https://github.com/hoangthien123/react-native-document-scanner/issues).
