package com.example.order.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.ReceiveAndReplyCallback;
import org.springframework.amqp.core.ReplyToAddressCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Reactive adapter for AmqpTemplate interface.
 * Wraps RabbitTemplate to provide reactive operations through Mono.fromCallable() pattern.
 */
public class ReactiveAmqpTemplate implements AmqpTemplate {

    private static final Logger log = LoggerFactory.getLogger(ReactiveAmqpTemplate.class);

    private final RabbitTemplate rabbitTemplate;
    private final Duration defaultTimeout;

    public ReactiveAmqpTemplate(RabbitTemplate rabbitTemplate) {
        this(rabbitTemplate, Duration.ofSeconds(10));
    }

    public ReactiveAmqpTemplate(RabbitTemplate rabbitTemplate, Duration defaultTimeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.defaultTimeout = defaultTimeout;
    }

    private <T> T wrapBlocking(Callable<T> callable) {
        return Mono.fromCallable(callable)
                .subscribeOn(Schedulers.boundedElastic())
                .block(defaultTimeout);
    }

    @Override
    public void send(Message message) throws AmqpException {
        send("", "", message);
    }

    @Override
    public void send(String routingKey, Message message) throws AmqpException {
        send("", routingKey, message);
    }

    @Override
    public void send(String exchange, String routingKey, Message message) throws AmqpException {
        wrapBlocking(() -> {
            rabbitTemplate.send(exchange, routingKey, message);
            return null;
        });
    }

    @Override
    public void convertAndSend(Object message) throws AmqpException {
        convertAndSend("", "", message);
    }

    @Override
    public void convertAndSend(String routingKey, Object message) throws AmqpException {
        convertAndSend("", routingKey, message);
    }

    @Override
    public void convertAndSend(String exchange, String routingKey, Object message) throws AmqpException {
        wrapBlocking(() -> {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            return null;
        });
    }

    @Override
    public void convertAndSend(Object message, MessagePostProcessor messagePostProcessor) throws AmqpException {
        convertAndSend("", "", message, messagePostProcessor);
    }

    @Override
    public void convertAndSend(String routingKey, Object message, MessagePostProcessor messagePostProcessor) throws AmqpException {
        convertAndSend("", routingKey, message, messagePostProcessor);
    }

    @Override
    public void convertAndSend(String exchange, String routingKey, Object message, MessagePostProcessor messagePostProcessor) throws AmqpException {
        wrapBlocking(() -> {
            rabbitTemplate.convertAndSend(exchange, routingKey, message, messagePostProcessor);
            return null;
        });
    }

    @Override
    public Message sendAndReceive(Message message) throws AmqpException {
        return sendAndReceive("", "", message);
    }

    @Override
    public Message sendAndReceive(String routingKey, Message message) throws AmqpException {
        return sendAndReceive("", routingKey, message);
    }

    @Override
    public Message sendAndReceive(String exchange, String routingKey, Message message) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.sendAndReceive(exchange, routingKey, message));
    }

    @Override
    public Object convertSendAndReceive(Object message) throws AmqpException {
        return convertSendAndReceive("", "", message);
    }

    @Override
    public Object convertSendAndReceive(String routingKey, Object message) throws AmqpException {
        return convertSendAndReceive("", routingKey, message);
    }

    @Override
    public Object convertSendAndReceive(String exchange, String routingKey, Object message) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.convertSendAndReceive(exchange, routingKey, message));
    }

    @Override
    public Object convertSendAndReceive(Object message, MessagePostProcessor messagePostProcessor) throws AmqpException {
        return convertSendAndReceive("", "", message, messagePostProcessor);
    }

    @Override
    public Object convertSendAndReceive(String routingKey, Object message, MessagePostProcessor messagePostProcessor) throws AmqpException {
        return convertSendAndReceive("", routingKey, message, messagePostProcessor);
    }

    @Override
    public Object convertSendAndReceive(String exchange, String routingKey, Object message, MessagePostProcessor messagePostProcessor) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.convertSendAndReceive(exchange, routingKey, message, messagePostProcessor));
    }

    @Override
    public <T> T convertSendAndReceiveAsType(Object message, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType("", "", message, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(String routingKey, Object message, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType("", routingKey, message, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(String exchange, String routingKey, Object message, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.convertSendAndReceiveAsType(exchange, routingKey, message, responseType));
    }

    @Override
    public <T> T convertSendAndReceiveAsType(Object message, MessagePostProcessor messagePostProcessor, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType("", "", message, messagePostProcessor, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(String routingKey, Object message, MessagePostProcessor messagePostProcessor, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType("", routingKey, message, messagePostProcessor, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(String exchange, String routingKey, Object message, MessagePostProcessor messagePostProcessor, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.convertSendAndReceiveAsType(exchange, routingKey, message, messagePostProcessor, responseType));
    }

    @Override
    public Message receive() throws AmqpException {
        return wrapBlocking(rabbitTemplate::receive);
    }

    @Override
    public Message receive(String queueName) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.receive(queueName));
    }

    @Override
    public Message receive(long timeoutMillis) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.receive(timeoutMillis));
    }

    @Override
    public Message receive(String queueName, long timeoutMillis) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.receive(queueName, timeoutMillis));
    }

    @Override
    public Object receiveAndConvert() throws AmqpException {
        return wrapBlocking(rabbitTemplate::receiveAndConvert);
    }

    @Override
    public Object receiveAndConvert(String queueName) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.receiveAndConvert(queueName));
    }

    @Override
    public Object receiveAndConvert(long timeoutMillis) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.receiveAndConvert(timeoutMillis));
    }

    @Override
    public Object receiveAndConvert(String queueName, long timeoutMillis) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.receiveAndConvert(queueName, timeoutMillis));
    }

    @Override
    public <T> T receiveAndConvert(ParameterizedTypeReference<T> type) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.receiveAndConvert(type));
    }

    @Override
    public <T> T receiveAndConvert(String queueName, ParameterizedTypeReference<T> type) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.receiveAndConvert(queueName, type));
    }

    @Override
    public <T> T receiveAndConvert(long timeoutMillis, ParameterizedTypeReference<T> type) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.receiveAndConvert(timeoutMillis, type));
    }

    @Override
    public <T> T receiveAndConvert(String queueName, long timeoutMillis, ParameterizedTypeReference<T> type) throws AmqpException {
        return wrapBlocking(() -> rabbitTemplate.receiveAndConvert(queueName, timeoutMillis, type));
    }

    @Override
    public <R, S> boolean receiveAndReply(ReceiveAndReplyCallback<R, S> callback) throws AmqpException {
        return rabbitTemplate.receiveAndReply(callback);
    }

    @Override
    public <R, S> boolean receiveAndReply(String queueName, ReceiveAndReplyCallback<R, S> callback) throws AmqpException {
        return rabbitTemplate.receiveAndReply(queueName, callback);
    }

    @Override
    public <R, S> boolean receiveAndReply(ReceiveAndReplyCallback<R, S> callback, String replyExchange, String replyRoutingKey) throws AmqpException {
        return rabbitTemplate.receiveAndReply(callback, replyExchange, replyRoutingKey);
    }

    @Override
    public <R, S> boolean receiveAndReply(String queueName, ReceiveAndReplyCallback<R, S> callback, String replyExchange, String replyRoutingKey) throws AmqpException {
        return rabbitTemplate.receiveAndReply(queueName, callback, replyExchange, replyRoutingKey);
    }

    @Override
    public <R, S> boolean receiveAndReply(ReceiveAndReplyCallback<R, S> callback, ReplyToAddressCallback<S> replyToAddressCallback) throws AmqpException {
        return rabbitTemplate.receiveAndReply(callback, replyToAddressCallback);
    }

    @Override
    public <R, S> boolean receiveAndReply(String queueName, ReceiveAndReplyCallback<R, S> callback, ReplyToAddressCallback<S> replyToAddressCallback) throws AmqpException {
        return rabbitTemplate.receiveAndReply(queueName, callback, replyToAddressCallback);
    }

    public MessageConverter getMessageConverter() {
        return rabbitTemplate.getMessageConverter();
    }

    public void setRoutingKey(String routingKey) {
        rabbitTemplate.setRoutingKey(routingKey);
    }

    public void setExchange(String exchange) {
        rabbitTemplate.setExchange(exchange);
    }

    public void setQueue(String queue) {
        rabbitTemplate.setDefaultReceiveQueue(queue);
    }

    public void setMessageConverter(MessageConverter messageConverter) {
        rabbitTemplate.setMessageConverter(messageConverter);
    }

    public Mono<Void> convertAndSendMono(String exchange, String routingKey, Object message) {
        return Mono.fromCallable(() -> {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            return (Void) null;
        }).subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("Failed to send message to {}.{}", exchange, routingKey, e);
                    return Mono.empty();
                });
    }

    public Mono<Void> convertAndSendWithRetry(String exchange, String routingKey, Object message, int maxRetries, Duration backoff) {
        return convertAndSendMono(exchange, routingKey, message)
                .retryWhen(reactor.util.retry.Retry.backoff(maxRetries, backoff)
                        .maxAttempts(maxRetries)
                        .filter(this::isRecoverableError)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new AmqpException("Failed to send after " + maxRetries + " retries", retrySignal.failure())));
    }

    public Flux<Void> convertAndSendBatch(String exchange, String routingKey, Flux<Object> messages) {
        return messages.flatMap(message ->
                convertAndSendMono(exchange, routingKey, message), 10);
    }

    private boolean isRecoverableError(Throwable throwable) {
        return !(throwable instanceof IllegalArgumentException);
    }
}