package nl.hemiron.objectstorage.service;

import lombok.extern.java.Log;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.logging.Level;

@Service
@Log
public class RabbitMQConsumer {
    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void consume(String message) {
        log.log(Level.INFO, "Received message from RabbitMQ: " + message);
    }
}
