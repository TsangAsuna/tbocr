package com.tbocr.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class R8KeepRulesContractTest {

    @Test
    fun minifyEnabledAndKeepRulesCoverNamedEntryPoints() {
        val gradle = resolve("build.gradle.kts").readText()
        assertTrue(Regex("""isMinifyEnabled\s*=\s*true""").containsMatchIn(gradle))
        assertTrue(Regex("""isShrinkResources\s*=\s*true""").containsMatchIn(gradle))

        val rules = resolve("proguard-rules.pro").readText()
        val activeRules = rules.lineSequence()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .toList()
        assertFalse(activeRules.any { it.startsWith("-dontoptimize") })
        assertFalse(activeRules.any { it.startsWith("-dontshrink") })
        assertFalse(activeRules.any { it.startsWith("-dontobfuscate") })
        assertFalse(activeRules.any { it.startsWith("-keep class com.tbocr.app.**") })
        assertFalse(activeRules.any { it.contains("com.sun.jna.**") })
        listOf(
            "com.tbocr.app.bridge.NativeBridgeLib",
            "com.tbocr.app.maa.DriverClass",
            "com.tbocr.app.maa.**",
            "com.sun.jna.*",
            "com.sun.jna.Structure",
            "com.sun.jna.Callback",
            "com.tbocr.app.remote.RemoteServiceImpl",
            "com.tbocr.app.remote.LogcatCaptureServiceImpl",
            "com.tbocr.app.root.RemoteServiceStarter",
            "touchDown",
            "startApp",
            "org.eclipse.angus.mail.smtp.**",
            "org.eclipse.angus.mail.imap.**",
            "org.eclipse.angus.mail.pop3.**",
            "org.eclipse.angus.mail.handlers.**",
            "org.eclipse.angus.mail.util.MailStreamProvider",
            "org.eclipse.angus.activation.*RegistryProviderImpl",
            "org.eclipse.tm4e.**",
            "io.github.rosemoe.sora.langs.textmate.**",
            "org.joni.**",
            "org.jcodings.**",
            "-keepclassmembers enum com.tbocr.app.**",
            "valueOf(java.lang.String)",
            "com.tbocr.app.third.FakeContext",
            "com.tbocr.app.third.FakeContext$*",
            "android.content.ContentResolver",
            "acquireProvider(",
            "com.xzakota.hyper.notification.**",
        ).forEach { token ->
            assertTrue("missing keep token: $token", rules.contains(token))
        }

        val keepXml = resolve("src/main/res/raw/keep.xml").readText()
        assertTrue(keepXml.contains("@string/maa_*"))
        assertTrue(keepXml.contains("@string/achievement_*"))
    }

    private fun resolve(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath"),
        )
        val file = candidates.firstOrNull { it.isFile }
        checkNotNull(file) { "File not found for test: $relativePath" }
        return file
    }
}
