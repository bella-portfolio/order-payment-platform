package com.example.payment.consumer

import com.example.payment.service.PaymentService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class OrderCreatedConsumer(
    private val paymentService: PaymentService,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(OrderCreatedConsumer::class.java)

    @KafkaListener(topics = ["order.created"], groupId = "payment-service-order-created")
    fun onOrderCreated(message: String) {
        try {
            val event = objectMapper.readValue(message, Map::class.java) as? Map<*, *> ?: return
            val orderId = event["orderId"] as String
            val productId = event["productId"] as String
            val amount = BigDecimal(event["amount"].toString())

            log.info("Received order.created event: orderId={}, productId={}, amount={}", orderId, productId, amount)
            paymentService.processPayment(orderId, productId, amount)
        } catch (e: Exception) {
            log.error("Failed to process order.created event", e)
        }
    }
}
