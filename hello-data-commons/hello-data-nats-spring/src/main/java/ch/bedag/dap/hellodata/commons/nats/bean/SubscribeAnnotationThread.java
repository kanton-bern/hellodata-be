/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.commons.nats.bean;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.nats.util.NatsStreamUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/**
 * One stream subject configuration = one corresponding thread below to delegate messages to beans subscribing
 */
@Log4j2
public class SubscribeAnnotationThread extends Thread {
    private final Connection natsConnection;
    private final JetStreamSubscribe subscribeAnnotation;
    private final List<BeanMethodWrapper> beanWrappers;
    private final String durableName;
    private final ExecutorService executorService;
    private JetStreamSubscription subscription;

    SubscribeAnnotationThread(Connection natsConnection, JetStreamSubscribe sub, List<BeanMethodWrapper> beanWrappers, String durableName, ExecutorService executorService) {
        log.debug("Creating subscription thread for beans listening to {}", beanWrappers.get(0).subscriptionId());
        this.natsConnection = natsConnection;
        this.subscribeAnnotation = sub;
        this.beanWrappers = beanWrappers;
        this.durableName = durableName;
        this.executorService = executorService;
        //keep one subscription
        subscribe();
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    public void unsubscribe() {
        if (this.subscription != null) {
            this.subscription.unsubscribe();
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                log.debug("------- Run message fetch for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
                if (subscription != null) {
                    Message message = subscription.nextMessage(Duration.ofSeconds(10L));
                    if (message != null) {
                        log.debug("------- Message fetched from the queue for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
                        executorService.submit(() -> passMessageToSpringBean(message));
                    } else {
                        log.debug("------- No message fetched from the queue for for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
                    }
                } else {
                    log.warn("Subscription to NATS is null. Please check if NATS is available for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
                }
            } catch (IllegalStateException e) {
                log.error("", e);
                subscribe();
            } catch (InterruptedException e) {
                log.error("", e);
                Thread.currentThread().interrupt(); // Re-interrupt the thread
            }
        }
    }

    public List<String> getSubscriptionIds() {
        return beanWrappers.stream().map(BeanMethodWrapper::subscriptionId).toList();
    }

    public List<BeanMethodWrapper> getBeanWrappers() {
        return beanWrappers;
    }

    public void shutdown() {
        log.info("Shutting down subscription thread");
        unsubscribe();

    }

    private void subscribe() {
        try {
            NatsStreamUtil.createOrUpdateStream(natsConnection.jetStreamManagement(), subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
            JetStream jetStream = natsConnection.jetStream();
            ConsumerConfiguration consumerConfig = ConsumerConfiguration.builder().durable(this.durableName).build();
            ConsumerInfo consumerInfo = NatsStreamUtil.getOrCreateConsumer(natsConnection.jetStreamManagement(), subscribeAnnotation.event().getStreamName(), durableName, consumerConfig);
            PushSubscribeOptions pushSubscribeOptions = PushSubscribeOptions.builder().configuration(consumerInfo.getConsumerConfiguration()).build();
            subscription = jetStream.subscribe(subscribeAnnotation.event().getSubject(), pushSubscribeOptions);
        } catch (IOException | JetStreamApiException exception) {
            unsubscribe();
            throw new RuntimeException(exception);
        }
    }

    private void passMessageToSpringBean(Message message) {
        try {
            for (BeanMethodWrapper beanWrapper : beanWrappers) {
                log.debug("Passing NATS message to bean {}", beanWrapper.bean().getClass().getName());
                Class<?> clazz = subscribeAnnotation.event().getDataClass();
                beanWrapper.method().invoke(beanWrapper.bean(), getObjectMapper().readValue(message.getData(), clazz));
                log.debug("NATS message processing finished {}", beanWrapper.bean().getClass().getName());
            }
            message.ack();
        } catch (IllegalAccessException | InvocationTargetException | CompletionException | IOException e) {
            log.error("Error invoking method", e);
        }
    }
}
