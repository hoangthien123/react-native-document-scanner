#import "RNDocumentScanner.h"
#import <React/RCTLog.h>
#import <React/RCTUtils.h>
#import <VisionKit/VisionKit.h>

@interface RNDocumentScanner () <VNDocumentCameraViewControllerDelegate>

@property (nonatomic, strong) RCTPromiseResolveBlock resolveBlock;
@property (nonatomic, strong) RCTPromiseRejectBlock rejectBlock;
@property (nonatomic, assign) NSInteger croppedImageQuality;
@property (nonatomic, strong) NSString *responseType;

@end

@implementation RNDocumentScanner

RCT_EXPORT_MODULE();

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

RCT_EXPORT_METHOD(scanDocument:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    // Check iOS version
    if (@available(iOS 13.0, *)) {
        // Check if document scanning is supported
        if (![VNDocumentCameraViewController isSupported]) {
            reject(@"NOT_SUPPORTED", @"Document scanning is not supported on this device", nil);
            return;
        }
        
        // Store promise handlers
        self.resolveBlock = resolve;
        self.rejectBlock = reject;
        
        // Get options
        self.croppedImageQuality = options[@"croppedImageQuality"] ? [options[@"croppedImageQuality"] integerValue] : 100;
        self.responseType = options[@"responseType"] ?: @"imageFilePath";
        
        // Present document scanner on main thread
        dispatch_async(dispatch_get_main_queue(), ^{
            VNDocumentCameraViewController *documentCameraViewController = [[VNDocumentCameraViewController alloc] init];
            documentCameraViewController.delegate = self;
            
            UIViewController *rootViewController = RCTPresentedViewController();
            if (rootViewController) {
                [rootViewController presentViewController:documentCameraViewController animated:YES completion:nil];
            } else {
                reject(@"NO_VIEWCONTROLLER", @"No view controller available to present scanner", nil);
            }
        });
    } else {
        reject(@"IOS_VERSION", @"Document scanning requires iOS 13.0 or later", nil);
    }
}

#pragma mark - VNDocumentCameraViewControllerDelegate

- (void)documentCameraViewController:(VNDocumentCameraViewController *)controller didFinishWithScan:(VNDocumentCameraScan *)scan API_AVAILABLE(ios(13.0))
{
    NSMutableArray *scannedImages = [NSMutableArray array];
    
    // Process each scanned page
    for (NSUInteger i = 0; i < scan.pageCount; i++) {
        UIImage *image = [scan imageOfPageAtIndex:i];
        
        // Convert to JPEG with quality
        CGFloat quality = (CGFloat)self.croppedImageQuality / 100.0;
        NSData *imageData = UIImageJPEGRepresentation(image, quality);
        
        if (!imageData) {
            [controller dismissViewControllerAnimated:YES completion:^{
                if (self.rejectBlock) {
                    self.rejectBlock(@"IMAGE_ERROR", @"Failed to convert scanned image to JPEG", nil);
                    self.rejectBlock = nil;
                    self.resolveBlock = nil;
                }
            }];
            return;
        }
        
        if ([self.responseType isEqualToString:@"base64"]) {
            // Return base64 encoded image
            NSString *base64String = [imageData base64EncodedStringWithOptions:0];
            [scannedImages addObject:base64String];
        } else {
            // Save to temporary directory and return file path
            NSString *timestamp = [NSString stringWithFormat:@"%.0f", [[NSDate date] timeIntervalSince1970] * 1000];
            NSString *fileName = [NSString stringWithFormat:@"scanned_doc_%@_%lu.jpg", timestamp, (unsigned long)i];
            NSString *filePath = [NSTemporaryDirectory() stringByAppendingPathComponent:fileName];
            
            NSError *error = nil;
            BOOL success = [imageData writeToFile:filePath options:NSDataWritingAtomic error:&error];
            
            if (!success) {
                [controller dismissViewControllerAnimated:YES completion:^{
                    if (self.rejectBlock) {
                        self.rejectBlock(@"FILE_ERROR", [NSString stringWithFormat:@"Failed to save image: %@", error.localizedDescription], error);
                        self.rejectBlock = nil;
                        self.resolveBlock = nil;
                    }
                }];
                return;
            }
            
            // Return file:// URL
            NSString *fileURL = [NSURL fileURLWithPath:filePath].absoluteString;
            [scannedImages addObject:fileURL];
        }
    }
    
    // Dismiss and resolve
    [controller dismissViewControllerAnimated:YES completion:^{
        if (self.resolveBlock) {
            NSDictionary *response = @{
                @"scannedImages": scannedImages,
                @"status": @"success"
            };
            self.resolveBlock(response);
            self.resolveBlock = nil;
            self.rejectBlock = nil;
        }
    }];
}

- (void)documentCameraViewControllerDidCancel:(VNDocumentCameraViewController *)controller API_AVAILABLE(ios(13.0))
{
    [controller dismissViewControllerAnimated:YES completion:^{
        if (self.resolveBlock) {
            NSDictionary *response = @{
                @"scannedImages": @[],
                @"status": @"cancel"
            };
            self.resolveBlock(response);
            self.resolveBlock = nil;
            self.rejectBlock = nil;
        }
    }];
}

- (void)documentCameraViewController:(VNDocumentCameraViewController *)controller didFailWithError:(NSError *)error API_AVAILABLE(ios(13.0))
{
    [controller dismissViewControllerAnimated:YES completion:^{
        if (self.rejectBlock) {
            self.rejectBlock(@"SCAN_ERROR", error.localizedDescription, error);
            self.rejectBlock = nil;
            self.resolveBlock = nil;
        }
    }];
}

@end
