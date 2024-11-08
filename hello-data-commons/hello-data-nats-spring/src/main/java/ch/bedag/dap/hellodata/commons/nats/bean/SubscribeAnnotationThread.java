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
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

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
        log.debug("[NATS] Creating subscription thread for beans listening to {}", beanWrappers.get(0).subscriptionId());
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
            log.info("[NATS] Unsubscribed from NATS connection for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                log.debug("[NATS] ------- Run message fetch for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
                if (subscription != null) {
                    fetchAndRunInAThread();
                } else {
                    log.warn("[NATS] Subscription to NATS is null. Please check if NATS is available for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
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
        log.info("[NATS] Shutting down subscription thread");
        unsubscribe();

    }

    private void fetchAndRunInAThread() throws InterruptedException {
        Message message = subscription.nextMessage(Duration.ofSeconds(10L));
        if (message != null) {
            log.debug("[NATS] ------- Message fetched from the queue for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
            Future<?> future = executorService.submit(() -> passMessageToSpringBean(message));
            try {
                // Wait for task completion within the given timeout
                future.get(subscribeAnnotation.timeoutMinutes(), TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                log.warn("[NATS] Task exceeded timeout. Attempting to cancel...");
                future.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                log.error("[NATS] Task encountered an exception: ", e);
            }
        } else {
            log.debug("[NATS] ------- No message fetched from the queue for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
        }
    }

    private void subscribe() {
        try {
            log.info("[NATS] Subscribing to NATS connection for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
            NatsStreamUtil.createOrUpdateStream(natsConnection.jetStreamManagement(), subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
            JetStream jetStream = natsConnection.jetStream();
            ConsumerConfiguration consumerConfig = ConsumerConfiguration
                    .builder()
                    .name(this.durableName)
                    .durable(this.durableName)
                    .build();
            PushSubscribeOptions pushSubscribeOptions = PushSubscribeOptions
                    .builder()
                    .name(this.durableName)
                    .durable(this.durableName)
                    .configuration(consumerConfig)
                    .build();
            subscription = jetStream.subscribe(subscribeAnnotation.event().getSubject(), pushSubscribeOptions);
            log.info("[NATS] Subscribed to NATS connection for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
        } catch (IOException | JetStreamApiException exception) {
            unsubscribe();
            throw new RuntimeException(exception);
        }
    }

    private void passMessageToSpringBean(Message message) {
        try {
            for (BeanMethodWrapper beanWrapper : beanWrappers) {
                log.debug("[NATS] Passing NATS message to bean {}", beanWrapper.bean().getClass().getName());
                Class<?> clazz = subscribeAnnotation.event().getDataClass();
                StopWatch watch = new StopWatch();
                watch.start();
                log.debug("[NATS] Expected type: {}", clazz.getName());
                log.debug("[NATS] Method parameter type: {}", beanWrapper.method().getParameterTypes()[0].getName());
                beanWrapper.method().invoke(beanWrapper.bean(), getObjectMapper().readValue(message.getData(), clazz));
                log.debug("[NATS] NATS message processing finished {}. The operation took {}", beanWrapper.bean().getClass().getName(), watch.formatTime());
            }
            message.ack();
        } catch (IllegalAccessException | InvocationTargetException | CompletionException | IOException e) {
            log.error("[NATS] Error invoking method", e);
        }
    }
}
