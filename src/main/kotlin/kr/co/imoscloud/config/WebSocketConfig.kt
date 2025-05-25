package kr.co.imoscloud.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.*

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val dgsHandler: GraphQLWsHandler,
    private val powerHandler: PowerSocketHandler
): WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(dgsHandler, "/graphql-ws")
            .setAllowedOrigins("*")

        registry.addHandler(powerHandler, "/ws/power")
            .setAllowedOrigins("*")
    }
}