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

    private boolean running = true;
    private short failureCount = 0;
    private final boolean killJvmOnError;
    private final short killJvmCounter;

    SubscribeAnnotationThread(Connection natsConnection,
                              JetStreamSubscribe sub,
                              List<BeanMethodWrapper> beanWrappers,
                              String durableName,
                              ExecutorService executorService,
                              boolean killJvmOnError,
                              short killJvmCounter) {
        log.debug("[NATS] Creating subscription thread for beans listening to {}", beanWrappers.get(0).subscriptionId());
        this.natsConnection = natsConnection;
        this.subscribeAnnotation = sub;
        this.beanWrappers = beanWrappers;
        this.durableName = durableName;
        this.executorService = executorService;
        this.killJvmOnError = killJvmOnError;
        this.killJvmCounter = killJvmCounter;
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
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                checkOrCreateConsumer();
                log.debug("[NATS] ------- Run message fetch for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
                if (subscription != null && subscription.isActive()) {
                    fetchMessage();
                    failureCount = 0;
                } else {
                    log.warn("[NATS] Subscription to NATS is null. Please check if NATS is available for stream {} and subject {}. Failure count {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject(), ++failureCount);
                }
                checkFailureCounter();
            } catch (Exception e) {
                failureCount++;
                log.error("Nats connection failed {}/{}", failureCount, killJvmCounter, e);
                if (killJvmOnError) {
                    if (failureCount >= killJvmCounter) {
                        log.error("Too many connection/subscription failures. Exiting JVM to trigger orchestrator restart.");
                        System.exit(1);
                    }
                } else {
                    log.debug("[NATS] Error on connection for stream {} and subject {}. Re-subscribing...", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject(), e);
                    subscribe();
                }
            }
        }
        log.info("[NATS] Stopped NATS subscription thread!");
        unsubscribe();
        System.exit(1); // Exit the JVM if the thread is stopped
    }

    private void checkFailureCounter() {
        if (killJvmOnError) {
            if (failureCount >= killJvmCounter) {
                log.error("Too many connection failures. Exiting JVM to trigger orchestrator restart.");
                System.exit(1);
            }
        }
    }

    private void fetchMessage() throws InterruptedException {
        Message message = subscription.nextMessage(Duration.ofSeconds(10L));
        if (message != null) {
            log.debug("[NATS] ------- Message fetched from the queue for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
            if (subscribeAnnotation.asyncRun()) {
                processMessageInThread(message);
            } else {
                passMessageToSpringBean(message);
            }
        } else {
            log.debug("[NATS] ------- No message fetched from the queue for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
        }
    }

    public List<String> getSubscriptionIds() {
        return beanWrappers.stream().map(BeanMethodWrapper::subscriptionId).toList();
    }

    public List<BeanMethodWrapper> getBeanWrappers() {
        return beanWrappers;
    }

    public void stopThread() {
        log.info("[NATS] Shutting down subscription thread");
        running = false;
        unsubscribe();
    }

    /**
     * Checks if the consumer still exists and recreates it if missing.
     */
    private void checkOrCreateConsumer() throws IOException, JetStreamApiException {
        JetStreamManagement jsm = natsConnection.jetStreamManagement();
        ConsumerInfo consumerInfo = jsm.getConsumerInfo(subscribeAnnotation.event().getStreamName(), durableName);
        if (consumerInfo == null) {
            log.warn("[NATS] Consumer {} for stream {} not found. Re-subscribing... Failure count {}", durableName, subscribeAnnotation.event().getStreamName(), ++failureCount);
            subscribe();
        }
        checkFailureCounter();
    }

    private void processMessageInThread(Message message) throws InterruptedException {
        // Submit as a CompletableFuture directly
        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> passMessageToSpringBean(message), executorService);

        // Create a ScheduledExecutorService for the timeout task
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> timeoutHandler = scheduler.schedule(() -> {
            if (!completableFuture.isDone()) {
                log.warn("[NATS] Task exceeded timeout. Attempting to cancel...");
                completableFuture.cancel(true);
            }
        }, subscribeAnnotation.timeoutMinutes(), TimeUnit.MINUTES);

        // If completableFuture finishes before timeout, cancel the scheduled task and release resources
        completableFuture.whenComplete((result, throwable) -> {
            timeoutHandler.cancel(false);  // Cancel the timeout task
            scheduler.shutdown();  // Release the scheduled thread
        });

    }

    private void subscribe() {
        try {
            log.debug("[NATS] Subscribing to NATS connection for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
            NatsStreamUtil.createOrUpdateStream(natsConnection.jetStreamManagement(), subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject());
            JetStream jetStream = natsConnection.jetStream();
            ConsumerConfiguration consumerConfig = ConsumerConfiguration
                    .builder()
                    .name(this.durableName)
                    .durable(this.durableName)
                    .ackWait(Duration.ofMinutes(subscribeAnnotation.timeoutMinutes()))
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
            log.error("[NATS] Error subscribing to NATS connection for stream {} and subject {}", subscribeAnnotation.event().getStreamName(), subscribeAnnotation.event().getSubject(), exception);
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
        } catch (IllegalAccessException | InvocationTargetException | IOException | RuntimeException e) {
            log.error("[NATS] Error invoking method", e);
        }
    }

}
