package com.linkedin.gms.factory.kafka;

import com.linkedin.metadata.config.kafka.KafkaConfiguration;
import com.linkedin.gms.factory.config.ConfigurationProvider;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;


@Slf4j
@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class SimpleKafkaConsumerFactory {

  @Bean(name = "simpleKafkaConsumer")
  protected KafkaListenerContainerFactory<?> createInstance(@Qualifier("configurationProvider") ConfigurationProvider
      provider, KafkaProperties properties) {
    KafkaConfiguration kafkaConfiguration = provider.getKafka();
    KafkaProperties.Consumer consumerProps = properties.getConsumer();

    // Specify (de)serializers for record keys and for record values.
    consumerProps.setKeyDeserializer(StringDeserializer.class);
    consumerProps.setValueDeserializer(StringDeserializer.class);
    // Records will be flushed every 10 seconds.
    consumerProps.setEnableAutoCommit(true);
    consumerProps.setAutoCommitInterval(Duration.ofSeconds(10));

    // KAFKA_BOOTSTRAP_SERVER has precedence over SPRING_KAFKA_BOOTSTRAP_SERVERS
    if (kafkaConfiguration.getBootstrapServers() != null && kafkaConfiguration.getBootstrapServers().length() > 0) {
      consumerProps.setBootstrapServers(Arrays.asList(kafkaConfiguration.getBootstrapServers().split(",")));
    } // else we rely on KafkaProperties which defaults to localhost:9092

    Map<String, Object> customizedProperties = properties.buildConsumerProperties();
    customizedProperties.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG,
            kafkaConfiguration.getConsumer().getMaxPartitionFetchBytes());

    ConcurrentKafkaListenerContainerFactory<String, GenericRecord> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setContainerCustomizer(new ThreadPoolContainerCustomizer());
    factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(customizedProperties));

    log.info("Simple KafkaListenerContainerFactory built successfully");

    return factory;
  }
}