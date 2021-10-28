package com.rainyseason.cj.common

import android.os.Build
import androidx.annotation.RequiresApi
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.featureflag.FeatureFlag
import com.rainyseason.cj.featureflag.isEnable
import okhttp3.Dns
import timber.log.Timber
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDnsSelector @Inject constructor(
    configChangeManager: ConfigChangeManager
) : Dns, ConfigChangeListener {
    private var mode: Mode = Mode.IPV4_ONLY

    init {
        configChangeManager.registerListener(this)
    }

    enum class Mode {
        SYSTEM, IPV6_FIRST, IPV4_FIRST, IPV6_ONLY, IPV4_ONLY
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun lookup(hostname: String): List<InetAddress> {
        if (BuildConfig.DEBUG) {
            Timber.d("lookup $hostname with $mode")
        }
        var addresses = Dns.SYSTEM.lookup(hostname)
        addresses = when (mode) {
            Mode.IPV6_FIRST -> addresses.sortedBy { Inet4Address::class.java.isInstance(it) }
            Mode.IPV4_FIRST -> addresses.sortedByDescending {
                Inet4Address::class.java.isInstance(it)
            }
            Mode.IPV6_ONLY -> addresses.filter { Inet6Address::class.java.isInstance(it) }
            Mode.IPV4_ONLY -> addresses.filter { Inet4Address::class.java.isInstance(it) }
            Mode.SYSTEM -> addresses
        }
        return addresses
    }

    override fun onConfigChange() {
        val newMode = if (FeatureFlag.DISABLE_V4_ONLY.isEnable) {
            Mode.SYSTEM
        } else {
            Mode.IPV4_ONLY
        }
        Timber.d("mode change from $mode to $newMode")
        mode = newMode
    }
}