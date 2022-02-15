gradle :app:assembleDebug :app:assembleDebugAndroidTest

c="gcloud firebase test android run"
c="${c} --type instrumentation"
c="${c} --app ./app/build/outputs/apk/debug/app-debug.apk"
c="${c} --test ./app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"

deviceList=(
  "f2q:30"              # Samsung Galaxy Z Fold 2
  "x1q:29"              # Samsung Galaxy S20 5G
  "greatlteks:28"       # Samsung Galaxy Note 8
  "j7popltevzw:27"      # Samsung Galaxy J7 V
  "starqlteue:26"       # Samsung Galaxy S9

  "cactus:27"           # Xiaomi Redmi 6A

  "oriole:31"           # Google Pixel 6
  "redfin:30"           # Google Pixel 5e
  #"flame:29"            # Google Pixel 4  // take to long to acquire, stuck as "pending state"
  "blueline:28"         # Google Pixel 3
  "walleye:27"          # Google Pixel 2

  "OnePlus5T:28"        # OnePlus OnePlus 5T
  "OnePlus3T:26"        # OnePlus OnePlus 3T

  "pettyl:27"           # Motorola Moto E5 Play

  "phoenix_sprout:28"   # LG LG G7 One

  "1725:27"             # Vivo X21

  "ASUS_X00T_3:28"      # Asus Zenfone Max Pro (M1)

  "H9493:28"            # Sony Xperia XZ3

  "AOP_sprout:28"       # Nokia 9
)

for device in "${deviceList[@]}" ; do
    code="${device%%:*}"
    apiVersion="${device##*:}"
    c="${c} --device model=${code},version=${apiVersion},locale=en,orientation=portrait"
done

eval $c