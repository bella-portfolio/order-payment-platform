package com.example.order.repository

import com.example.order.entity.OrderOutbox
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrderOutboxRepository : JpaRepository<OrderOutbox, UUID> {

    fun findByPublishedFalse(): List<OrderOutbox>
}
