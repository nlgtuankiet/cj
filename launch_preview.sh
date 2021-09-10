gradle :app:installDebug
adb shell am start -n com.rainyseason.cj/.ticker.CoinTickerSettingActivity \
  --ei appWidgetId 123 \
  --es coin_id bitcoin