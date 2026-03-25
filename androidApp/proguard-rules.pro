-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

-dontwarn javax.annotation.**
-dontwarn androidx.compose.**

# Room KMP + bundled SQLite.
-keep class restarhalf.stellar.schedule.data.local.AppDatabase { *; }
-keep class restarhalf.stellar.schedule.data.local.AppDatabase_Impl { *; }
-keep class restarhalf.stellar.schedule.data.local.AppDatabaseConstructor { *; }
-keep class restarhalf.stellar.schedule.data.local.CourseDao { *; }
-keep class restarhalf.stellar.schedule.data.local.CourseDao_Impl { *; }
-keep class restarhalf.stellar.schedule.data.local.CourseDao_Impl$* { *; }
-keep class androidx.sqlite.driver.bundled.** { *; }
-keepclassmembers class androidx.sqlite.driver.bundled.** {
    native <methods>;
}

# WorkManager, receivers and Glance widgets are runtime entry points.
-keep class restarhalf.stellar.schedule.work.ReminderRescheduleWorker { *; }
-keep class restarhalf.stellar.schedule.ScheduleApp { *; }
-keep class restarhalf.stellar.schedule.only.receiver.BootReceiver { *; }
-keep class restarhalf.stellar.schedule.only.receiver.CourseReminderReceiver { *; }
-keep class restarhalf.stellar.schedule.only.receiver.ExamReminderReceiver { *; }
-keep class restarhalf.stellar.schedule.only.widget.TodaySmallWidgetReceiver { *; }
-keep class restarhalf.stellar.schedule.only.widget.TodayLargeWidgetReceiver { *; }
-keep class restarhalf.stellar.schedule.only.widget.WidgetRefreshReceiver { *; }
-keep class restarhalf.stellar.schedule.widget.** { *; }

# Ktor content-negotiation and kotlinx.serialization.
-keep class io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider { *; }
-keep class **$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coil image loading on Android.
-keep class coil3.** { *; }

# Strip low-value android.util.Log calls.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
