#import "FlutterGamepadPlugin.h"
#import <flutter_gamepad/flutter_gamepad-Swift.h>

@implementation FlutterGamepadPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterGamepadPlugin registerWithRegistrar:registrar];
}
@end
