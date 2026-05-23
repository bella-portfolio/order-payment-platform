package com.example.order.service

import com.example.order.entity.Order
import com.example.order.entity.OrderOutbox
import com.example.order.repository.OrderOutboxRepository
import com.example.order.repository.OrderRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

data class CreateOrderRequest(
    val productId: String,
    val quantity: Int,
    val amount: BigDecimal
)

data class OrderResponse(
    val id: String,
    val productId: String,
    val quantity: Int,
    val amount: BigDecimal,
    val status: String
)

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderOutboxRepository: OrderOutboxRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun createOrder(request: CreateOrderRequest): OrderResponse {
        val order = Order(
            productId = request.productId,
            quantity = request.quantity,
            amount = request.amount
        )
        val savedOrder = orderRepository.save(order)

        val payload = objectMapper.writeValueAsString(
            mapOf(
                "orderId" to savedOrder.id.toString(),
                "productId" to savedOrder.productId,
                "quantity" to savedOrder.quantity,
                "amount" to savedOrder.amount
            )
        )

        // Save outbox entry (transactional — same DB transaction)
        val outbox = OrderOutbox(
            aggregateId = savedOrder.id,
            eventType = "OrderCreated",
            payload = payload
        )
        orderOutboxRepository.save(outbox)

        return OrderResponse(
            id = savedOrder.id.toString(),
            productId = savedOrder.productId,
            quantity = savedOrder.quantity,
            amount = savedOrder.amount,
            status = savedOrder.status.name
        )
    }

    @Transactional
    fun cancelOrder(orderId: String) {
        val order = orderRepository.findById(java.util.UUID.fromString(orderId))
            .orElseThrow { NoSuchElementException("Order not found: $orderId") }
        order.cancel()
        orderRepository.save(order)
    }

    fun getOrder(orderId: String): OrderResponse {
        val order = orderRepository.findById(java.util.UUID.fromString(orderId))
            .orElseThrow { NoSuchElementException("Order not found: $orderId") }
        return OrderResponse(
            id = order.id.toString(),
            productId = order.productId,
            quantity = order.quantity,
            amount = order.amount,
            status = order.status.name
        )
    }
}
