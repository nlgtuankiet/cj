gradle :app:assembleDebug :app:assembleDebugAndroidTest
gcloud firebase test android run \
  --type instrumentation \
  --app ./app/build/outputs/apk/debug/app-debug.apk \
  --test ./app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
  --device model=blueline,version=28,locale=en,orientation=portrait
#  --device model=Pixel2,version=28,locale=en,orientation=portrait
