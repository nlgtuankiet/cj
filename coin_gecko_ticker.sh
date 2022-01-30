response=$(curl https://api.coingecko.com/api/v3/coins/bitcoin)
printf '%s\n' "$response" | jq '.market_data.current_price.usd'