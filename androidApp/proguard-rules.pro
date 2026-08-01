-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

-dontwarn javax.annotation.**
-dontwarn androidx.compose.**

# Room KMP + bundled SQLite.
-keep class restarhalf.stellar.schedule.data.local.AppDatabase { *; }
-keep class restarhalf.stellar.schedule.data.local.AppDatabase_Impl { *; }
-keep class restarhalf.stellar.schedule.data.local.AppDatabaseConstructor { *; }
-keep class restarhalf.stellar.schedule.data.local.dao.CourseDao { *; }
-keep class restarhalf.stellar.schedule.data.local.dao.*_Impl { *; }
-keep class restarhalf.stellar.schedule.data.local.dao.*_Impl$* { *; }
-keep class androidx.sqlite.driver.bundled.** { *; }
-keepclassmembers class androidx.sqlite.driver.bundled.** {
    native <methods>;
}

-keep class androidx.work.impl.WorkDatabase_Impl$* { *; }
-keep class restarhalf.stellar.schedule.AndroidApp { *; }
-keep class restarhalf.stellar.schedule.widget.TodaySmallWidgetReceiver { *; }
-keep class restarhalf.stellar.schedule.widget.TodayLargeWidgetReceiver { *; }
-keep class restarhalf.stellar.schedule.widget.WidgetRefreshReceiver { *; }
-keep class restarhalf.stellar.schedule.widget.** { *; }

# kotlinx.serialization.
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
