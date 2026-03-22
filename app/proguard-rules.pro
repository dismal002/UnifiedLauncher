# Keep all launcher modules intact - they use reflection extensively
-keep class com.oreo.launcher3.** { *; }
-keep class com.callmewill.launcher2.** { *; }
-keep class com.gingerbread.launcher2.** { *; }
-keep class com.donut.launcher.** { *; }
-keep class cn.kingway.launcher.** { *; }
-keep class com.android.launcher2.** { *; }
-keep class com.dismal.unifiedlauncher.** { *; }

# Keep protobuf classes used by launcher3-o
-keep class com.google.protobuf.** { *; }
