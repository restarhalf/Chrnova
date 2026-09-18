package restarhalf.stellar.schedule.data.local.mmkv

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SettingsListener
import com.tencent.mmkv.kmp.MMKV

/**
 * multiplatform-settings 适配层：底层用 mmkv-kmp，上层保持 ObservableSettings API。
 *
 * 监听只在经由本类 put/remove/clear 的写路径上触发；跨进程/绕过适配层的写不会通知。
 */
class MmkvSettings internal constructor(
    private val kv: MMKV,
) : ObservableSettings {

    private val listeners = mutableMapOf<String, MutableList<() -> Unit>>()

    override val keys: Set<String>
        get() = kv.allKeys.toSet()

    override val size: Int
        get() = kv.count.toInt()

    override fun clear() {
        val snapshot = kv.allKeys
        kv.clearAll()
        snapshot.forEach { notifyKey(it) }
    }

    override fun remove(key: String) {
        kv.removeValueForKey(key)
        notifyKey(key)
    }

    override fun hasKey(key: String): Boolean = kv.containsKey(key)

    override fun putInt(key: String, value: Int) {
        kv.encodeInt(key, value)
        notifyKey(key)
    }

    override fun getInt(key: String, defaultValue: Int): Int = kv.decodeInt(key, defaultValue)

    override fun getIntOrNull(key: String): Int? =
        if (kv.containsKey(key)) kv.decodeInt(key) else null

    override fun putLong(key: String, value: Long) {
        kv.encodeLong(key, value)
        notifyKey(key)
    }

    override fun getLong(key: String, defaultValue: Long): Long = kv.decodeLong(key, defaultValue)

    override fun getLongOrNull(key: String): Long? =
        if (kv.containsKey(key)) kv.decodeLong(key) else null

    override fun putString(key: String, value: String) {
        kv.encodeString(key, value)
        notifyKey(key)
    }

    override fun getString(key: String, defaultValue: String): String =
        kv.decodeString(key) ?: defaultValue

    override fun getStringOrNull(key: String): String? =
        if (kv.containsKey(key)) kv.decodeString(key) else null

    override fun putFloat(key: String, value: Float) {
        kv.encodeFloat(key, value)
        notifyKey(key)
    }

    override fun getFloat(key: String, defaultValue: Float): Float = kv.decodeFloat(key, defaultValue)

    override fun getFloatOrNull(key: String): Float? =
        if (kv.containsKey(key)) kv.decodeFloat(key) else null

    override fun putDouble(key: String, value: Double) {
        kv.encodeDouble(key, value)
        notifyKey(key)
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        kv.decodeDouble(key, defaultValue)

    override fun getDoubleOrNull(key: String): Double? =
        if (kv.containsKey(key)) kv.decodeDouble(key) else null

    override fun putBoolean(key: String, value: Boolean) {
        kv.encodeBool(key, value)
        notifyKey(key)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        kv.decodeBool(key, defaultValue)

    override fun getBooleanOrNull(key: String): Boolean? =
        if (kv.containsKey(key)) kv.decodeBool(key) else null

    override fun addIntListener(key: String, defaultValue: Int, callback: (Int) -> Unit): SettingsListener =
        addListener(key) { callback(getInt(key, defaultValue)) }

    override fun addLongListener(key: String, defaultValue: Long, callback: (Long) -> Unit): SettingsListener =
        addListener(key) { callback(getLong(key, defaultValue)) }

    override fun addStringListener(key: String, defaultValue: String, callback: (String) -> Unit): SettingsListener =
        addListener(key) { callback(getString(key, defaultValue)) }

    override fun addFloatListener(key: String, defaultValue: Float, callback: (Float) -> Unit): SettingsListener =
        addListener(key) { callback(getFloat(key, defaultValue)) }

    override fun addDoubleListener(key: String, defaultValue: Double, callback: (Double) -> Unit): SettingsListener =
        addListener(key) { callback(getDouble(key, defaultValue)) }

    override fun addBooleanListener(key: String, defaultValue: Boolean, callback: (Boolean) -> Unit): SettingsListener =
        addListener(key) { callback(getBoolean(key, defaultValue)) }

    override fun addIntOrNullListener(key: String, callback: (Int?) -> Unit): SettingsListener =
        addListener(key) { callback(getIntOrNull(key)) }

    override fun addLongOrNullListener(key: String, callback: (Long?) -> Unit): SettingsListener =
        addListener(key) { callback(getLongOrNull(key)) }

    override fun addStringOrNullListener(key: String, callback: (String?) -> Unit): SettingsListener =
        addListener(key) { callback(getStringOrNull(key)) }

    override fun addFloatOrNullListener(key: String, callback: (Float?) -> Unit): SettingsListener =
        addListener(key) { callback(getFloatOrNull(key)) }

    override fun addDoubleOrNullListener(key: String, callback: (Double?) -> Unit): SettingsListener =
        addListener(key) { callback(getDoubleOrNull(key)) }

    override fun addBooleanOrNullListener(key: String, callback: (Boolean?) -> Unit): SettingsListener =
        addListener(key) { callback(getBooleanOrNull(key)) }

    private fun addListener(key: String, callback: () -> Unit): SettingsListener {
        val bucket = listeners.getOrPut(key) { mutableListOf() }
        bucket.add(callback)
        return object : SettingsListener {
            override fun deactivate() {
                listeners[key]?.let { list ->
                    list.remove(callback)
                    if (list.isEmpty()) listeners.remove(key)
                }
            }
        }
    }

    private fun notifyKey(key: String) {
        val snapshot = listeners[key]?.toList() ?: return
        snapshot.forEach { it.invoke() }
    }
}

class MmkvSettingsFactory {
    fun create(name: String?): ObservableSettings {
        val kv = if (name.isNullOrBlank()) {
            MMKV.defaultMMKV()
        } else {
            MMKV.mmkvWithID(name)
        }
        return MmkvSettings(kv)
    }
}
