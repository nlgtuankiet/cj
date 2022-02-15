gradle :app:assembleDebug :app:assembleDebugAndroidTest
gcloud firebase test android run \
  --type instrumentation \
  --app ./app/build/outputs/apk/debug/app-debug.apk \
  --test ./app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
  --device model=oriole,version=31,locale=en,orientation=portrait \
  --device model=x1q,version=29,locale=en,orientation=portrait \
  --device model=f2q,version=30,locale=en,orientation=portrait \
  --device model=cactus,version=27,locale=en,orientation=portrait \
  --device model=OnePlus5T,version=28,locale=en,orientation=portrait \
  --device model=HWCOR,version=27,locale=en,orientation=portrait \
  --device model=pettyl,version=27,locale=en,orientation=portrait