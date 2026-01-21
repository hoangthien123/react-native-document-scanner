import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  'The package \'rn-document-scanner-vision\' doesn\'t seem to be linked. Make sure: \n\n' +
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
 * Scanner mode options (Android only)
 * - BASE: Basic editing (crop, rotate, reorder pages)
 * - BASE_WITH_FILTER: Adds image filters (grayscale, auto enhancement)
 * - FULL: Adds ML-enabled cleaning (erase stains, fingers, etc.)
 */
export type ScannerMode = 'BASE' | 'BASE_WITH_FILTER' | 'FULL';

/**
 * Result format options (Android only)
 * - JPEG: Return JPEG images only
 * - PDF: Return PDF only
 * - JPEG_PDF: Return both JPEG images and PDF
 */
export type ResultFormats = 'JPEG' | 'PDF' | 'JPEG_PDF';

/**
 * Options for document scanning
 */
export interface ScanDocumentOptions {
  /**
   * Maximum number of pages to scan (Android)
   * @default 10
   * @platform android
   */
  pageLimit?: number;

  /**
   * Allow importing from photo gallery (Android)
   * @default false
   * @platform android
   */
  galleryImportAllowed?: boolean;

  /**
   * Scanner mode (Android)
   * @default 'FULL'
   * @platform android
   */
  scannerMode?: ScannerMode;

  /**
   * Result formats (Android)
   * @default 'JPEG'
   * @platform android
   */
  resultFormats?: ResultFormats;

  // iOS specific options
  /**
   * Quality of the cropped image (0-100) - iOS only
   * @default 100
   * @platform ios
   */
  croppedImageQuality?: number;
}

/**
 * PDF info from scan result (Android only)
 */
export interface PdfInfo {
  /**
   * URI to the generated PDF file
   */
  uri: string;
  /**
   * Number of pages in the PDF
   */
  pageCount: number;
}

/**
 * Response from document scanning
 */
export interface ScanDocumentResponse {
  /**
   * Array of scanned image URIs
   */
  scannedImages: string[];

  /**
   * Status of the scan operation
   */
  status: 'success' | 'cancel';

  /**
   * PDF info if resultFormats includes PDF (Android only)
   */
  pdf?: PdfInfo;
}

/**
 * Result of module availability check
 */
export interface ModuleAvailableResult {
  /**
   * Whether the Google Document Scanner module is available
   */
  available: boolean;
}

/**
 * Document Scanner module using Google ML Kit
 */
export const DocumentScanner = {
  /**
   * Start document scanning using Google ML Kit Document Scanner (Android)
   * or VisionKit (iOS)
   *
   * @param options - Optional configuration for the document scanner
   * @returns Promise that resolves with scanned document data or rejects with error
   *
   * @example
   * ```typescript
   * import { DocumentScanner } from 'rn-document-scanner-vision';
   *
   * try {
   *   const result = await DocumentScanner.scanDocument({
   *     pageLimit: 5,
   *     galleryImportAllowed: true,
   *     scannerMode: 'FULL',
   *     resultFormats: 'JPEG_PDF'
   *   });
   *
   *   if (result.status === 'success') {
   *     console.log('Scanned images:', result.scannedImages);
   *     if (result.pdf) {
   *       console.log('PDF:', result.pdf.uri);
   *     }
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
      pageLimit: 10,
      galleryImportAllowed: false,
      scannerMode: 'FULL',
      resultFormats: 'JPEG',
      croppedImageQuality: 100,
    };

    const finalOptions = { ...defaultOptions, ...options };

    return RNDocumentScanner.scanDocument(finalOptions);
  },

  /**
   * Check if Google Document Scanner module is available (Android only)
   * The ML Kit Document Scanner models are downloaded on-demand.
   * 
   * @returns Promise that resolves with availability status
   * @platform android
   *
   * @example
   * ```typescript
   * const { available } = await DocumentScanner.isGoogleDocumentScannerModuleAvailable();
   * if (!available) {
   *   console.log('Module needs to be downloaded');
   * }
   * ```
   */
  isGoogleDocumentScannerModuleAvailable(): Promise<ModuleAvailableResult> {
    if (Platform.OS !== 'android') {
      return Promise.resolve({ available: true });
    }
    return RNDocumentScanner.isGoogleDocumentScannerModuleAvailable();
  },
};

export default DocumentScanner;
