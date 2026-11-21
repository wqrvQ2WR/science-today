#!/bin/bash
# iOS 앱 빌드(Xcode 프로젝트 없이 swiftc 로 바로). 결과는 ios/dist/
#   ./ios/build.sh device     -> ScienceToday-ios.ipa (서명 안 됨, 사이드로딩용)
#   ./ios/build.sh sim        -> ScienceToday.app (arm64 시뮬레이터)
#   ./ios/build.sh catalyst   -> ScienceToday.app (맥에서 iOS 화면 점검용)
# 데이터(JSON)와 글꼴은 안드로이드 쪽 원본을 그대로 복사해서 쓴다(원본은 하나).
set -e
cd "$(dirname "$0")"
KIND=${1:-device}
OUT=dist/$KIND
rm -rf $OUT; mkdir -p $OUT
case $KIND in
  device)   SDK=iphoneos;        TARGET=arm64-apple-ios16.0;           PLAT=iPhoneOS ;;
  sim)      SDK=iphonesimulator; TARGET=arm64-apple-ios16.0-simulator; PLAT=iPhoneSimulator ;;
  catalyst) SDK=macosx;          TARGET=arm64-apple-ios16.0-macabi;    PLAT=MacOSX ;;
  *) echo "device | sim | catalyst"; exit 1 ;;
esac
SDKP=$(xcrun --sdk $SDK --show-sdk-path)
EXTRA=()
if [ $KIND = catalyst ]; then
  EXTRA=(-Fsystem $SDKP/System/iOSSupport/System/Library/Frameworks -I $SDKP/System/iOSSupport/usr/include -L $SDKP/System/iOSSupport/usr/lib)
  APP=$OUT/ScienceToday.app; BIN=$APP/Contents/MacOS; RES=$APP/Contents/Resources; PL=$APP/Contents/Info.plist
  mkdir -p $BIN $RES
else
  APP=$OUT/Payload/ScienceToday.app; BIN=$APP; RES=$APP; PL=$APP/Info.plist
  mkdir -p $APP
fi

xcrun --sdk $SDK swiftc -target $TARGET -sdk $SDKP "${EXTRA[@]}" -O -parse-as-library Sources/*.swift -o $BIN/ScienceToday

A=../android/app/src/main
cp $A/assets/{facts,laws,elements,tables}.json $RES/
cp $A/res/font/pretendard_{regular,bold,extrabold}.otf $RES/

ICON_KEYS=""
if [ $KIND != catalyst ]; then
  if xcrun --find actool >/dev/null 2>&1; then
    T=$OUT/xcassets/Assets.xcassets/AppIcon.appiconset; mkdir -p $T
    cp icon.png $T/icon.png
    echo '{"images":[{"filename":"icon.png","idiom":"universal","platform":"ios","size":"1024x1024"}],"info":{"author":"xcode","version":1}}' > $T/Contents.json
    xcrun actool $OUT/xcassets/Assets.xcassets --compile $APP --platform $SDK --minimum-deployment-target 16.0 \
      --app-icon AppIcon --target-device iphone --target-device ipad --output-partial-info-plist $OUT/icon.plist >/dev/null
    rm -rf $OUT/xcassets
  else
    sips -z 120 120 icon.png --out $APP/AppIcon60x60@2x.png >/dev/null
    sips -z 180 180 icon.png --out $APP/AppIcon60x60@3x.png >/dev/null
    ICON_KEYS='<key>CFBundleIcons</key><dict><key>CFBundlePrimaryIcon</key><dict><key>CFBundleIconFiles</key><array><string>AppIcon60x60</string></array></dict></dict>'
  fi
fi

cat > $PL <<P
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleExecutable</key><string>ScienceToday</string>
<key>CFBundleIdentifier</key><string>com.hayoonjae.sciencetoday</string>
<key>CFBundleName</key><string>ScienceToday</string>
<key>CFBundleDisplayName</key><string>오늘의 과학</string>
<key>CFBundlePackageType</key><string>APPL</string>
<key>CFBundleShortVersionString</key><string>1.1.0</string>
<key>CFBundleVersion</key><string>2</string>
<key>CFBundleSupportedPlatforms</key><array><string>$PLAT</string></array>
<key>MinimumOSVersion</key><string>16.0</string>
<key>LSMinimumSystemVersion</key><string>13.0</string>
<key>LSApplicationCategoryType</key><string>public.app-category.education</string>
<key>UIDeviceFamily</key><array><integer>1</integer><integer>2</integer></array>
<key>UILaunchScreen</key><dict/>
<key>UIUserInterfaceStyle</key><string>Light</string>
<key>UIAppFonts</key><array><string>pretendard_regular.otf</string><string>pretendard_bold.otf</string><string>pretendard_extrabold.otf</string></array>
<key>UISupportedInterfaceOrientations</key><array><string>UIInterfaceOrientationPortrait</string><string>UIInterfaceOrientationLandscapeLeft</string><string>UIInterfaceOrientationLandscapeRight</string></array>
<key>UISupportedInterfaceOrientations~ipad</key><array><string>UIInterfaceOrientationPortrait</string><string>UIInterfaceOrientationPortraitUpsideDown</string><string>UIInterfaceOrientationLandscapeLeft</string><string>UIInterfaceOrientationLandscapeRight</string></array>
$ICON_KEYS
</dict></plist>
P
if [ -f $OUT/icon.plist ]; then /usr/libexec/PlistBuddy -c "Merge $OUT/icon.plist" $PL >/dev/null; rm -f $OUT/icon.plist; fi
plutil -lint $PL >/dev/null || { echo "Info.plist 깨짐"; exit 1; }

codesign -fs - "$APP" 2>/dev/null
if [ $KIND = device ]; then
  (cd $OUT && zip -qr ../ScienceToday-ios.ipa Payload)
  ls -la dist/ScienceToday-ios.ipa
else
  echo "만듦: $APP"
fi
