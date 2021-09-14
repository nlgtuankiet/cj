key_value=$1
value_value=$2
rand=$(head -n 4096 /dev/urandom | openssl sha1)

echo "set $key_value to $value_value rand $rand"
adb shell am start -n "com.rainyseason.cj/com.rainyseason.cj.featureflag.DebugFlagSetter" \
  -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
  --es key "$key_value" \
  --es value "$value_value" \
  --es rand "$rand"
