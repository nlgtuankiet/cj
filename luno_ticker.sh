response=$(curl https://ajax.luno.com/ajax/1/ticker?pair=XBTUSDC)
bid=$(echo "$response" | jq -r .bid)
ask=$(echo "$response" | jq -r .ask)
echo $bid
echo $ask
echo "scale=2 ; ($bid + $ask) / 2" | bc
