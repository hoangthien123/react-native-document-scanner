import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-document-scanner' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const RNDocumentScanner = NativeModules.RNDocumentScanner
  ? NativeModules.RNDocumentScanner
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

/**
 * Options for document scanning
 */
export interface ScanDocumentOptions {
  /**
   * Maximum number of documents to scan (Android only)
   * @default 1
   * @platform android
   */
  maxNumDocuments?: number;

  /**
   * Allow user to adjust crop manually (Android only)
   * @default true
   * @platform android
   */
  letUserAdjustCrop?: boolean;

  /**
   * Quality of the cropped image (0-100)
   * @default 100
   */
  croppedImageQuality?: number;

  /**
   * Response type for scanned images
   * @default 'imageFilePath'
   */
  responseType?: 'base64' | 'imageFilePath';
}

/**
 * Response from document scanning
 */
export interface ScanDocumentResponse {
  /**
   * Array of scanned images (base64 strings or file paths)
   */
  scannedImages: string[];

  /**
   * Status of the scan operation
   */
  status: 'success' | 'cancel';
}

/**
 * Document Scanner module
 */
export const DocumentScanner = {
  /**
   * Start document scanning
   * 
   * @param options - Optional configuration for the document scanner
   * @returns Promise that resolves with scanned document data or rejects with error
   * 
   * @example
   * ```typescript
   * import { DocumentScanner } from 'react-native-document-scanner';
   * 
   * try {
   *   const result = await DocumentScanner.scanDocument({
   *     maxNumDocuments: 3,
   *     letUserAdjustCrop: true,
   *     croppedImageQuality: 100,
   *     responseType: 'base64'
   *   });
   *   
   *   if (result.status === 'success') {
   *     console.log('Scanned images:', result.scannedImages);
   *   } else {
   *     console.log('User cancelled');
   *   }
   * } catch (error) {
   *   console.error('Scan error:', error);
   * }
   * ```
   */
  scanDocument(options?: ScanDocumentOptions): Promise<ScanDocumentResponse> {
    const defaultOptions: ScanDocumentOptions = {
      maxNumDocuments: 1,
      letUserAdjustCrop: true,
      croppedImageQuality: 100,
      responseType: 'imageFilePath',
    };

    const finalOptions = { ...defaultOptions, ...options };

    return RNDocumentScanner.scanDocument(finalOptions);
  },
};

export default DocumentScanner;
