gradle :app:installDebug
adb shell am start -n "com.rainyseason.cj/.ticker.CoinTickerSettingActivity" \
  -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
  --ei appWidgetId 123 \
  --es coin_id bitcoin