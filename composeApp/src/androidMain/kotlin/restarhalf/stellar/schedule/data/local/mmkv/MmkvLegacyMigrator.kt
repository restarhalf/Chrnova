package restarhalf.stellar.schedule.data.local.mmkv

import android.content.Context
import com.tencent.mmkv.kmp.MMKV

object MmkvLegacyMigrator {
    fun migrateAll(context: Context) {
        MmkvStores.ALL.forEach { migrateSharedPreferences(context, it) }
    }

    fun migrateSharedPreferences(context: Context, name: String) {
        val kv = MMKV.mmkvWithID(name)
        if (kv.containsKey(MmkvStores.MIGRATION_FLAG)) return

        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val all = sp.all
        for ((key, value) in all) {
            when (value) {
                is Boolean -> kv.encodeBool(key, value)
                is Int -> kv.encodeInt(key, value)
                is Long -> kv.encodeLong(key, value)
                is Float -> kv.encodeFloat(key, value)
                is Double -> kv.encodeDouble(key, value)
                is String -> kv.encodeString(key, value)
                else -> Unit
            }
        }
        kv.encodeBool(MmkvStores.MIGRATION_FLAG, true)
    }
}
