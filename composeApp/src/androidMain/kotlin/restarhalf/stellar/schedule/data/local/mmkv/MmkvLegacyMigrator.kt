package restarhalf.stellar.schedule.data.local.mmkv

import android.content.Context
import com.tencent.mmkv.kmp.MMKV
import restarhalf.stellar.schedule.core.log.AppLogger

object MmkvLegacyMigrator {
    private const val TAG = "MmkvMigration"

    fun migrateAll(context: Context) {
        MmkvStores.ALL.forEach { migrateSharedPreferences(context, it) }
    }

    fun migrateSharedPreferences(context: Context, name: String) {
        val kv = MMKV.mmkvWithID(name)
        if (kv.containsKey(MmkvStores.MIGRATION_FLAG)) return

        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val all = sp.all
        var copied = 0
        for ((key, value) in all) {
            when (value) {
                is Boolean -> kv.encodeBool(key, value)
                is Int -> kv.encodeInt(key, value)
                is Long -> kv.encodeLong(key, value)
                is Float -> kv.encodeFloat(key, value)
                is Double -> kv.encodeDouble(key, value)
                is String -> kv.encodeString(key, value)
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val strings = value as? Set<String>

                    if (strings != null) {
                        AppLogger.log(TAG, "skip string-set key=$key in $name size=${strings.size}")
                    }
                }
                null -> Unit
                else -> AppLogger.log(TAG, "skip unsupported type ${value::class} key=$key in $name")
            }
            copied++
        }
        kv.encodeBool(MmkvStores.MIGRATION_FLAG, true)
        AppLogger.log(TAG, "migrated store=$name keys=$copied from SharedPreferences")
    }
}
