#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'flutter_gamepad'
  s.version          = '0.0.1'
  s.summary          = 'A new flutter plugin project.'
  s.description      = <<-DESC
A new flutter plugin project.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
# Pretty sure this hack will only work when the pod is local path,
# but does not seem to be any way to share code between the ios and
# macos version of the plugin.
  system("rm -rf Classes/ && mkdir Classes && cd .. && for file in `ls ios/Classes/`; do ln ios/Classes/$file macos/Classes/; done > /dev/null")
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'FlutterMacOS'
  s.platform = :osx, '10.11'
  s.swift_version = '5.0'
end

