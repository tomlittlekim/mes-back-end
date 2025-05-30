package kr.co.imoscloud.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.graphql.dgs.DgsQueryExecutor
import kr.co.imoscloud.service.sensor.EquipmentPowerService
import org.json.JSONObject
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class GraphQLWsHandler(
    private val dgsQueryExecutor: DgsQueryExecutor
) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        println("WebSocket 연결됨: ${session.id}")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val json = message.payload
            val query = JSONObject(json).getJSONObject("payload").getString("query")
            val result = dgsQueryExecutor.execute(query)
            val response = TextMessage(result.toSpecification().toString())
            session.sendMessage(response)
        } catch (e: Exception) {
            println("WebSocket 처리 중 오류: ${e.message}")
        }
    }
}

data class PowerRequestDto(
    val site: String,
    val compCd: String
)

@Component
class PowerSocketHandler(
    private val equipmentPowerService: EquipmentPowerService,
    private var objectMapper: ObjectMapper
) : TextWebSocketHandler() {
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        println("WebSocket 메시지 수신: ${message.payload}")
        val dto = objectMapper.readValue(message.payload, PowerRequestDto::class.java)

        val powerData = equipmentPowerService.getPowerDataForWS(dto.site, dto.compCd)
        val json = objectMapper.writeValueAsString(powerData)
        session.sendMessage(TextMessage(json))
    }
}