package com.example.payment.service

import com.example.payment.entity.Payment
import com.example.payment.entity.PaymentStatus
import com.example.payment.repository.PaymentRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(PaymentService::class.java)

    companion object {
        // Product ID that triggers payment failure for testing
        private const val FAIL_PRODUCT_ID = "FAIL-001"
    }

    @Transactional
    fun processPayment(orderId: String, productId: String, amount: BigDecimal) {
        log.info("Processing payment: orderId={}, amount={}", orderId, amount)

        // Simulate payment processing — fail if productId is the magic failure trigger
        val success = productId != FAIL_PRODUCT_ID

        val payment = Payment(
            orderId = orderId,
            amount = amount,
            status = if (success) PaymentStatus.COMPLETED else PaymentStatus.FAILED
        )
        paymentRepository.save(payment)

        if (success) {
            publishOrderPaidEvent(orderId)
            log.info("Payment completed: orderId={}", orderId)
        } else {
            publishOrderCancelledEvent(orderId, "Payment failed for product: $productId")
            log.warn("Payment failed: orderId={}, productId={}", orderId, productId)
        }
    }

    private fun publishOrderPaidEvent(orderId: String) {
        val payload = objectMapper.writeValueAsString(
            mapOf("orderId" to orderId, "status" to "PAID")
        )
        kafkaTemplate.send("order.paid", orderId, payload).get()
        log.info("Published order.paid event: orderId={}", orderId)
    }

    private fun publishOrderCancelledEvent(orderId: String, reason: String) {
        val payload = objectMapper.writeValueAsString(
            mapOf("orderId" to orderId, "status" to "CANCELLED", "reason" to reason)
        )
        kafkaTemplate.send("order.cancelled", orderId, payload).get()
        log.info("Published order.cancelled event: orderId={}, reason={}", orderId, reason)
    }
}
