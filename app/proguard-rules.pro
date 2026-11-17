# Moshi codegen adapters are located by name at runtime. Moshi and Retrofit ship
# their own consumer rules; only keep what the app itself needs.
-keepattributes Signature
-keepattributes Exceptions
-keep class com.triplog.data.remote.dto.** { *; }

# OkHttp / Okio optional dependencies
-dontwarn okhttp3.**
-dontwarn okio.**
