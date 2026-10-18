package com.tbocr.app.constant

object Packages : Iterable<Map.Entry<String, String>> {
    private val packageName = mapOf(
        "Official" to "com.taobao.taobao",
        "Taobao" to "com.taobao.taobao",
        // 保留原 MAA 客户端定义，避免 UI/资源档逻辑取不到值
        "Bilibili" to "com.hypergryph.arknights.bilibili",
        "YoStarEN" to "com.YoStarEN.Arknights",
        "YoStarJP" to "com.YoStarJP.Arknights",
        "YoStarKR" to "com.YoStarKR.Arknights",
        "txwy" to "tw.txwy.and.arknights"
    )

    operator fun get(type: String): String? = packageName[type]

    override fun iterator(): Iterator<Map.Entry<String, String>> {
        return packageName.iterator()
    }
}
