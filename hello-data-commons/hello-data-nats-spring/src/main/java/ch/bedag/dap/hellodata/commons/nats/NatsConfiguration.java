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
package ch.bedag.dap.hellodata.commons.nats;

import ch.bedag.dap.hellodata.commons.nats.actuator.NatsHealthIndicator;
import ch.bedag.dap.hellodata.commons.nats.bean.NatsConfigBeanPostProcessor;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.spring.boot.autoconfigure.NatsAutoConfiguration;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

@Log4j2
@EnableAsync
@Configuration
@NoArgsConstructor
public class NatsConfiguration extends NatsAutoConfiguration implements AsyncConfigurer, ConnectionListener {

    @Override
    public void connectionEvent(Connection cnctn, Events event) {
        log.debug("Connection Event:" + event);
        switch (event) {
            case CONNECTED -> log.debug("CONNECTED to NATS!");
            case DISCONNECTED -> log.warn("RECONNECTED to NATS!");
            case RECONNECTED -> log.debug("RECONNECTED to NATS!");
            case RESUBSCRIBED -> log.debug("RESUBSCRIBED!");
            default -> log.debug("Other event: {}", event);
        }
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public NatsConfigBeanPostProcessor configBeanPostProcessor(Connection natsConnection) {
        return new NatsConfigBeanPostProcessor(natsConnection);
    }

    @Bean
    @ConditionalOnWebApplication
    public NatsHealthIndicator natsHealthIndicator(Connection auditNatsConnection) {
        log.debug("Creating NatsHealthIndicator.");
        return new NatsHealthIndicator(auditNatsConnection);
    }
}
