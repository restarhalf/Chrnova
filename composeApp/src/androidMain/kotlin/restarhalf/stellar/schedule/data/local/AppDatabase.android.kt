package restarhalf.stellar.schedule.data.local

import android.content.Context
import androidx.room3.Room

fun buildPlatformAppDatabase(context: Context): AppDatabase =
    buildAppDatabase(
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        )
    )
