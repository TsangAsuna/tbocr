# 只锁按名查找的入口

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# JNI RegisterNatives
-keep class com.tbocr.app.bridge.NativeBridgeLib {
    native <methods>;
}

# libbridge FindClass + GetStaticMethodID
-keep class com.tbocr.app.maa.DriverClass {
    public static boolean startApp(java.lang.String, int, boolean);
    public static boolean touchDown(int, int, int, int);
    public static boolean touchMove(int, int, int, int);
    public static boolean touchUp(int, int, int, int);
    public static boolean keyDown(int, int);
    public static boolean keyUp(int, int);
}

# JNA：方法名即 C 符号；嵌套 Callback 整包留
-keep class com.tbocr.app.maa.** { *; }
# libjnidispatch 只 FindClass 顶层；ptr/internal/win32 交给 R8
-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.Structure { <fields>; }
-keepclassmembers class * implements com.sun.jna.Callback { <methods>; }
-dontwarn java.awt.**

# Shizuku / Root 按类名拉起
-keep class com.tbocr.app.remote.RemoteServiceImpl { <init>(); }
-keep class com.tbocr.app.remote.LogcatCaptureServiceImpl { <init>(); }
-keep class com.tbocr.app.root.RemoteServiceStarter {
    public static void main(java.lang.String[]);
}
-keep class com.tbocr.app.root.RootUserService { *; }
-keep class com.tbocr.app.root.RootServiceBootstrapProvider { *; }

# AIDL
-keep class com.tbocr.app.RemoteService { *; }
-keep class com.tbocr.app.RemoteService$Stub { *; }
-keep class com.tbocr.app.MaaCoreService { *; }
-keep class com.tbocr.app.MaaCoreService$Stub { *; }
-keep class com.tbocr.app.MaaCoreCallback { *; }
-keep class com.tbocr.app.MaaCoreCallback$Stub { *; }
-keep class com.tbocr.app.ILogcatService { *; }
-keep class com.tbocr.app.ILogcatService$Stub { *; }
-keep class com.tbocr.app.ITouchEventCallback { *; }
-keep class com.tbocr.app.ITouchEventCallback$Stub { *; }
-keep class com.tbocr.app.remote.PermissionGrantRequest { *; }
-keep class com.tbocr.app.remote.PermissionStateInfo { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

# SMTP：ServiceLoader / mailcap 点名的实现，imap/pop3 缺一个就炸
-keep class org.eclipse.angus.mail.smtp.** { *; }
-keep class org.eclipse.angus.mail.imap.** { *; }
-keep class org.eclipse.angus.mail.pop3.** { *; }
-keep class org.eclipse.angus.mail.handlers.** { *; }
-keep class org.eclipse.angus.mail.util.MailStreamProvider { *; }
-keep class org.eclipse.angus.activation.*RegistryProviderImpl { *; }
-dontwarn org.eclipse.angus.**
-dontwarn jakarta.**
-dontwarn javax.**

# 任务覆盖编辑器：Gson 解 language-configuration；OnigRegExp 依赖 joni 静态初始化
-keep class org.eclipse.tm4e.** { *; }
-keep class io.github.rosemoe.sora.langs.textmate.** { *; }
-keep class org.joni.** { *; }
-keep class org.jcodings.** { *; }
-dontwarn org.eclipse.jdt.annotation.**
-dontwarn org.joni.**
-dontwarn org.jcodings.**

# FakeContext / ShellContentResolver：acquireProvider 等对编译期不可见，R8 当死代码删掉
-keep class com.tbocr.app.third.FakeContext { *; }
-keep class com.tbocr.app.third.FakeContext$* { *; }
-keepclassmembers class * extends android.content.ContentResolver {
    *** acquireProvider(...);
    *** acquireUnstableProvider(...);
    *** releaseProvider(...);
    *** releaseUnstableProvider(...);
    *** unstableProviderDied(...);
}

# xzakota 焦点/超级岛模板靠 kotlinx.serialization 反射字段
-keep class com.xzakota.hyper.notification.** { *; }

# 落盘 Enum.name / valueOf
-keepclassmembers enum com.tbocr.app.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
