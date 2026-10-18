plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("assetManifest") {
            id = "com.tbocr.app.asset-manifest"
            implementationClass = "com.tbocr.app.buildlogic.AssetManifestPlugin"
        }
        register("i18nVerify") {
            id = "com.tbocr.app.i18n-verify"
            implementationClass = "com.tbocr.app.buildlogic.I18nVerifyPlugin"
        }
    }
}
