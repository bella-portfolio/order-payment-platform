package com.example.order.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "order_outbox")
class OrderOutbox(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val aggregateId: UUID,

    @Column(nullable = false, length = 50)
    val eventType: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(nullable = false)
    var published: Boolean = false,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    fun markPublished() {
        this.published = true
    }
}
