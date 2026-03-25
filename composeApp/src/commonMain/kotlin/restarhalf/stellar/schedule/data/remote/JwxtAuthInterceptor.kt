package restarhalf.stellar.schedule.data.remote

import io.ktor.client.plugins.api.createClientPlugin

class JwxtAuthPluginConfig {
    var authStore: JwxtAuthStore? = null
}

val JwxtAuthPlugin = createClientPlugin("JwxtAuthPlugin", ::JwxtAuthPluginConfig) {
    val authStore = pluginConfig.authStore ?: return@createClientPlugin

    onRequest { request, _ ->
        val token = authStore.getToken().orEmpty()
        val serverIdCookie = authStore.getServerIdCookie().orEmpty()

        if (token.isNotBlank() && request.headers["token"] == null) {
            request.headers.append("token", token)
        }

        if (serverIdCookie.isNotBlank() && request.headers["Cookie"] == null) {
            request.headers.append("Cookie", serverIdCookie)
        }

        if (request.headers["Accept"] == null) {
            request.headers.append("Accept", "application/json, text/plain, */*")
        }
    }
}
