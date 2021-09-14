package com.rainyseason.cj.featureflag

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.rainyseason.cj.common.coreComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
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
        return data.value[stringPreferencesKey(flagKey.value)] as? String
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

@Suppress("unused")
object FeatureFlag {
    val HOME_V2 = FeatureKey("home_v2").withDefault("false")
}

object DebugFlag {
    val POSITIVE_WIDGET = DebugKey("positive_widget")
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