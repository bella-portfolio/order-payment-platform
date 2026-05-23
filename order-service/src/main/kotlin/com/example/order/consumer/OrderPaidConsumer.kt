package com.example.order.consumer

import com.example.order.entity.OrderStatus
import com.example.order.repository.OrderRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class OrderPaidConsumer(
    private val orderRepository: OrderRepository,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(OrderPaidConsumer::class.java)

    @Transactional
    @KafkaListener(topics = ["order.paid"], groupId = "order-service-paid")
    fun onOrderPaid(message: String) {
        try {
            val event = objectMapper.readValue(message, Map::class.java) as? Map<*, *> ?: return
            val orderId = event["orderId"] as String
            log.info("Received order.paid event: orderId={}", orderId)
            val order = orderRepository.findById(UUID.fromString(orderId))
                .orElseThrow { NoSuchElementException("Order not found: $orderId") }
            order.markPaid()
            orderRepository.save(order)
            log.info("Order marked as PAID: orderId={}", orderId)
        } catch (e: Exception) {
            log.error("Failed to process order.paid event", e)
        }
    }
}
