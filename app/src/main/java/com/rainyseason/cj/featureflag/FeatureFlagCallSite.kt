package com.rainyseason.cj.featureflag

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.rainyseason.cj.common.coreComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@JvmInline
value class FeatureKey(override val value: String) : FlagKey

@JvmInline
value class DebugKey(override val value: String) : FlagKey

@Singleton
class DebugFlagProvider @Inject constructor(context: Context) : FlagValueProvider {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val store = PreferenceDataStoreFactory.create(
        corruptionHandler = null,
        migrations = emptyList(),
        scope = scope,
        produceFile = {
            context.preferencesDataStoreFile("debug_feature_flag")
        }
    )

    fun awaitFirstValue() {
        runBlocking {
            store.data.first()
        }
    }

    private val data = MutableStateFlow(emptyMap<Preferences.Key<*>, Any>())

    init {
        scope.launch {
            store.data.map {
                it.asMap()
            }.distinctUntilChanged()
                .collect {
                    data.value = it
                }
        }
    }

    override fun get(flagKey: FlagKey): String? {
        val result = data.value[stringPreferencesKey(flagKey.value)] as? String
        Timber.d("Flag ${flagKey.value} return $result")
        return result
    }

    suspend fun set(flagKey: FlagKey, value: String?) {
        store.edit {
            val key = stringPreferencesKey(flagKey.value)
            if (value == null) {
                it.remove(key)
            } else {
                it[key] = value
            }
        }
    }
}

@Singleton
class RemoteConfigFlagProvider @Inject constructor(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
) : FlagValueProvider {
    override fun get(flagKey: FlagKey): String {
        return firebaseRemoteConfig.getString(flagKey.value).also {
            Timber.d("flag ${flagKey.value} return $it")
        }
    }
}

@Suppress("unused")
object FeatureFlag {
    val HOME_V2 = FeatureKey("home_v2").withDefault("false")
    val DISABLE_V4_ONLY = FeatureKey("disable_ipv4_only")
}

object DebugFlag {
    val POSITIVE_WIDGET = DebugKey("positive_widget")
    val SHOW_PREVIEW_LAYOUT_BOUNDS = DebugKey("show_preview_layout_bounds")
    val SHOW_HTTP_LOG = DebugKey("show_http_log").withDefault("true")
    val SHOW_NETWORK_LOG = DebugKey("show_network_log").withDefault("false")
    val SHOW_TRIGGER_REVIEW_BUTTON = DebugKey("show_trigger_review_button")
        .withDefault("false")
    val SHOW_CAPTURE_BUTTON = DebugKey("show_capture_button")
    val USE_REMOTE_CONFIG = DebugKey("use_remote_config")
}

class DebugFlagSetter : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val key = intent.getStringExtra("key") ?: ""
        val value = intent.getStringExtra("value")
        Timber.d("set $key to $value")
        runBlocking {
            coreComponent.debugFlagProvider.set(DebugKey(key), value)
        }
        finish()
    }
}
