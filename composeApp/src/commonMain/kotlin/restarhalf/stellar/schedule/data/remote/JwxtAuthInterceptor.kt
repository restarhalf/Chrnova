package restarhalf.stellar.schedule.data.remote

import io.ktor.client.plugins.api.createClientPlugin

/**
 * 教务系统认证插件配置
 */
class JwxtAuthPluginConfig {
    /** 认证存储实例 */
    var authStore: JwxtAuthStore? = null
}

/**
 * 教务系统认证Ktor插件
 * 
 * 自动在HTTP请求中添加认证头（token、Cookie、Accept）。
 */
val JwxtAuthPlugin = createClientPlugin("JwxtAuthPlugin", ::JwxtAuthPluginConfig) {
    val authStore = pluginConfig.authStore ?: return@createClientPlugin

    onRequest { request, _ ->
        val token = authStore.getToken().orEmpty()
        val serverIdCookie = authStore.getServerIdCookie().orEmpty()

        // 添加token头
        if (token.isNotBlank() && request.headers["token"] == null) {
            request.headers.append("token", token)
        }

        // 添加Cookie头
        if (serverIdCookie.isNotBlank() && request.headers["Cookie"] == null) {
            request.headers.append("Cookie", serverIdCookie)
        }

        // 添加Accept头
        if (request.headers["Accept"] == null) {
            request.headers.append("Accept", "application/json, text/plain, */*")
        }
    }
}
