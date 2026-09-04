# PlayTube Release ProGuard Rules

# 1. NewPipe Extractor (Critical: uses reflection for many things)
-keep class org.schabi.newpipe.extractor.** { *; }
-keep interface org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# 2. Hilt / Dagger
-keep class dagger.hilt.android.internal.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ComponentManager { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# 3. Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# 4. Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# 5. Coil
-keep class coil3.** { *; }
-dontwarn coil3.**

# 6. Ktor & OkHttp
-keep class io.ktor.** { *; }
-keep class okhttp3.** { *; }
-dontwarn io.ktor.**
-dontwarn okhttp3.**

# 7. Kotlin Serialization & Gson
-keep class kotlinx.serialization.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes Signature, EnclosingMethod, InnerClasses, RuntimeVisibleAnnotations, AnnotationDefault
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# 8. JSOUP (Often used by NewPipe)
-keep class org.jsoup.** { *; }

# 9. PlayTube Models & Navigation (Keep data models, repository backup classes, and navigation)
-keep class com.arslandaim.playtube.domain.model.** { *; }
-keep class com.arslandaim.playtube.data.** { *; }
-keep class com.arslandaim.playtube.ui.navigation.** { *; }
-keepclassmembers class com.arslandaim.playtube.ui.navigation.** { *; }

# 10. Missing classes detected by R8
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn javax.script.ScriptEngineFactory
