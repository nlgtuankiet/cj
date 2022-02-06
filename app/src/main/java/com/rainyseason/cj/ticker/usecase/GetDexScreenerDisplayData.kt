package com.rainyseason.cj.ticker.usecase

import com.rainyseason.cj.common.CurrencyInfo
import com.rainyseason.cj.common.changePercent
import com.rainyseason.cj.common.findApproxIndex
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.notNull
import com.rainyseason.cj.data.dexscreener.BarResponse
import com.rainyseason.cj.data.dexscreener.DexScreenerService
import com.rainyseason.cj.data.dexscreener.TokenDetailResponse
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import com.squareup.moshi.Moshi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetDexScreenerDisplayData @Inject constructor(
    private val dexScreenerService: DexScreenerService,
    private val moshi: Moshi,
) {

    private val mutex = Mutex()
    suspend operator fun invoke(
        param: CoinTickerDisplayData.LoadParam
    ): CoinTickerDisplayData {
        val currency = param.currency
        if (currency !in Backend.DexScreener.supportedCurrency.map { it.code }) {
            error("Not supported currency: $currency")
        }
        val network = param.network ?: error("Missing network")
        return coroutineScope {
            val to = System.currentTimeMillis()

            // TODO how to fix the string() blocking call?
            @Suppress("BlockingMethodInNonBlockingContext")
            val detailAsync = async {
                // TODO ws long polling may return multiple response
                mutex.withLock {
                    val sidRegex = """"sid":"(.+?)",""".toRegex()
                    val random = UUID.randomUUID().toString()
                        .replace("-", "")
                        .takeLast(6)
                    val detail1 = dexScreenerService.detail1(random).string()
                    val sid = sidRegex.find(detail1)?.groupValues?.getOrNull(1)
                        ?.trim() ?: error("Missing sid: $detail1")
                    require(detail1.isNotBlank()) { "Blank sid" }
                    val detail2 = dexScreenerService.detail2(
                        random,
                        sid,
                        DexScreenerService.getDetailBody(network, param.coinId)
                    ).string().trim()
                    require(detail2 == "ok") {
                        "Unknown response for detail2: $detail2"
                    }

                    val detail3 = dexScreenerService.detail3(random, sid).string()
                        .split("\u001E")
                        .last()
                    Timber.d("detail3: $detail3")
                    var commaIndex = -1
                    repeat(2) {
                        commaIndex = detail3.indexOf(',', commaIndex + 1)
                    }
                    Timber.d("comma index: $commaIndex")
                    val jsonString = detail3.substring(commaIndex + 1, detail3.length - 1)
                    Timber.d("json string: $jsonString")
                    moshi.adapter(TokenDetailResponse::class.java).fromJson(jsonString)
                        .notNull()
                }
            }

            val tickerResponseAsync = async {
                dexScreenerService.bars(
                    platformId = network,
                    pairId = param.coinId,
                    from = to - TimeUnit.DAYS.toMillis(1),
                    to = to,
                    res = "15",
                    cb = 110,
                )
            }

            val graphResponseAsync = if (param.changeInterval == TimeInterval.I_24H) {
                tickerResponseAsync
            } else {
                async {
                    dexScreenerService.bars(
                        platformId = network,
                        pairId = param.coinId,
                        from = to - when (param.changeInterval) {
                            TimeInterval.I_7D -> TimeUnit.DAYS.toMillis(7)
                            TimeInterval.I_30D -> TimeUnit.DAYS.toMillis(30)
                            else -> error("Not supported interval: ${param.changeInterval}")
                        },
                        to = to,
                        res = when (param.changeInterval) {
                            TimeInterval.I_7D -> "60" // 1h
                            TimeInterval.I_30D -> "240" // 4h
                            else -> error("Not supported interval: ${param.changeInterval}")
                        },
                        cb = when (param.changeInterval) {
                            TimeInterval.I_7D -> 180
                            TimeInterval.I_30D -> 200
                            else -> error("Not supported interval: ${param.changeInterval}")
                        },
                    )
                }
            }

            val tickerResponse = tickerResponseAsync.await()
            val graphResponse = graphResponseAsync.await()
            val graph = graphResponse.bars.map { listOf(it.timestamp, it.getValue(currency)) }

            val price = tickerResponse.bars.maxByOrNull { it.timestamp }?.getValue(currency)
                ?: error("ticker bars is empty")

            val intervalMilis = param.changeInterval.toMilis()
            val endMilis = graphResponse.bars.last().timestamp
            val startMilis = endMilis - intervalMilis
            val startIndex = graph.findApproxIndex(startMilis)
            val approxCandles = graph.subList(startIndex, graph.size)
            reportIntervalPercent(param, approxCandles)

            val detail = detailAsync.await()
            CoinTickerDisplayData(
                iconUrl = Backend.DexScreener.iconUrl,
                symbol = detail.pair.symbol(),
                name = detail.pair.baseToken.name,
                price = price,
                priceChangePercent = if (param.changeInterval == TimeInterval.I_24H) {
                    detail.pair.priceChange.notNull().h24
                } else {
                    approxCandles.changePercent()?.let { it * 100 }
                },
                priceGraph = approxCandles
            )
        }
    }

    private fun BarResponse.Bar.getValue(currency: String): Double {
        return if (currency == CurrencyInfo.USD.code) {
            closeUsd
        } else {
            close
        }
    }
}
