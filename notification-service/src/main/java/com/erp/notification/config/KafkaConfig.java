package com.erp.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Explicit listener container factory enforcing MANUAL_IMMEDIATE ack mode.
 *
 * Note: spring.kafka.listener.ack-mode=manual_immediate (set in the shared
 * application.yml served by config-server) already configures Boot's
 * auto-configured "kafkaListenerContainerFactory" bean identically. This bean
 * is defined under the same conventional name ("kafkaListenerContainerFactory")
 * that Spring Boot's KafkaAutoConfiguration would otherwise create, and Boot's
 * auto-configuration backs off (@ConditionalOnMissingBean) when a bean of
 * that name/type already exists - so this bean, not the yml property, is the
 * one actually in effect. They agree in value, so there is no conflicting
 * behavior, only redundancy.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public KafkaListenerContainerFactory<?> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
