package com.example.order.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "orders")
class Order(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var productId: String,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.CREATED,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    fun cancel() {
        this.status = OrderStatus.CANCELLED
    }

    fun markPaid() {
        this.status = OrderStatus.PAID
    }
}
