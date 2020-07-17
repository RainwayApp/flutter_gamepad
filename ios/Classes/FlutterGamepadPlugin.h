#import <Foundation/Foundation.h>
#if TARGET_OS_IPHONE || TARGET_OS_TV
#import <Flutter/Flutter.h>
#else // TARGET_OS_OSX
#import <FlutterMacOS/FlutterMacOS.h>
#endif

@interface FlutterGamepadPlugin : NSObject<FlutterPlugin>
@end
