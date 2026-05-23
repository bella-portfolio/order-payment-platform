package com.example.order.consumer

import com.example.order.service.OrderService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class OrderCancelledConsumer(
    private val orderService: OrderService,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(OrderCancelledConsumer::class.java)

    @KafkaListener(topics = ["order.cancelled"], groupId = "order-service-cancelled")
    fun onOrderCancelled(message: String) {
        try {
            val event = objectMapper.readValue(message, Map::class.java) as? Map<*, *> ?: return
            val orderId = event["orderId"] as String
            log.info("Received order.cancelled event: orderId={}", orderId)
            orderService.cancelOrder(orderId)
            log.info("Order cancelled successfully: orderId={}", orderId)
        } catch (e: Exception) {
            log.error("Failed to process order.cancelled event", e)
        }
    }
}
