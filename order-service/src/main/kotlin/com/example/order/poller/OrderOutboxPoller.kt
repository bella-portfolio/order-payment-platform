package com.example.order.poller

import com.example.order.repository.OrderOutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderOutboxPoller(
    private val orderOutboxRepository: OrderOutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {

    private val log = LoggerFactory.getLogger(OrderOutboxPoller::class.java)

    @Scheduled(fixedDelay = 2000) // Poll every 2 seconds
    @Transactional
    fun pollAndPublish() {
        val unpublished = orderOutboxRepository.findByPublishedFalse()
        for (outbox in unpublished) {
            try {
                kafkaTemplate.send("order.created", outbox.aggregateId.toString(), outbox.payload).get()
                outbox.markPublished()
                orderOutboxRepository.save(outbox)
                log.info("Published order.created event: orderId={}", outbox.aggregateId)
            } catch (e: Exception) {
                log.error("Failed to publish outbox event: id={}", outbox.id, e)
            }
        }
    }
}
