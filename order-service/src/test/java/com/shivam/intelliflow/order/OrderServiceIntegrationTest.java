package com.shivam.intelliflow.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.shivam.intelliflow.common.constants.KafkaTopicConstants;
import com.shivam.intelliflow.common.event.EventLevel;
import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.common.util.JsonUtils;
import com.shivam.intelliflow.order.config.TraceContextFilter;
import com.shivam.intelliflow.order.dto.OrderResponse;
import com.shivam.intelliflow.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    )
            .withDatabaseName("intelliflow_order_test")
            .withUsername("intelliflow")
            .withPassword("intelliflow");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.7.1")
    );

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @BeforeAll
    static void createKafkaTopics() throws Exception {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(properties)) {
            adminClient.createTopics(List.of(
                    new NewTopic(KafkaTopicConstants.LOGS, 1, (short) 1),
                    new NewTopic(KafkaTopicConstants.METRICS, 1, (short) 1)
            )).all().get(30, TimeUnit.SECONDS);
        } catch (Exception exception) {
            if (!isTopicExistsException(exception)) {
                throw exception;
            }
        }
    }

    @Test
    void createOrderEndpointCreatesOrder() {
        String traceId = UUID.randomUUID().toString();

        ResponseEntity<OrderResponse> response = createOrder(traceId, "customer-create", "49.99", 2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst(TraceContextFilter.TRACE_ID_HEADER)).isEqualTo(traceId);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().customerId()).isEqualTo("customer-create");
        assertThat(response.getBody().totalAmount()).isEqualByComparingTo(new BigDecimal("49.99"));
        assertThat(response.getBody().currency()).isEqualTo("USD");
        assertThat(response.getBody().itemCount()).isEqualTo(2);
        assertThat(response.getBody().status()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void getOrderEndpointReturnsCreatedOrder() {
        ResponseEntity<OrderResponse> createResponse = createOrder(
                UUID.randomUUID().toString(),
                "customer-get",
                "25.50",
                1
        );
        OrderResponse createdOrder = createResponse.getBody();

        ResponseEntity<OrderResponse> getResponse = restTemplate.getForEntity(
                "/api/v1/orders/{id}",
                OrderResponse.class,
                createdOrder.id()
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().id()).isEqualTo(createdOrder.id());
        assertThat(getResponse.getBody().customerId()).isEqualTo("customer-get");
        assertThat(getResponse.getBody().totalAmount()).isEqualByComparingTo(new BigDecimal("25.50"));
        assertThat(getResponse.getBody().itemCount()).isEqualTo(1);
    }

    @Test
    void createOrderPublishesLogAndMetricEvents() {
        String traceId = UUID.randomUUID().toString();

        try (
                KafkaConsumer<String, String> logConsumer = kafkaConsumer("logs-" + traceId);
                KafkaConsumer<String, String> metricConsumer = kafkaConsumer("metrics-" + traceId)
        ) {
            logConsumer.subscribe(List.of(KafkaTopicConstants.LOGS));
            metricConsumer.subscribe(List.of(KafkaTopicConstants.METRICS));

            ResponseEntity<OrderResponse> response = createOrder(traceId, "customer-events", "120.75", 3);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            String orderId = response.getBody().id().toString();

            EventSchema logEvent = pollForEvent(
                    logConsumer,
                    event -> traceId.equals(event.traceId()) && "ORDER_CREATED".equals(event.metadata().get("operation"))
            );
            EventSchema metricEvent = pollForEvent(
                    metricConsumer,
                    event -> traceId.equals(event.traceId())
                            && "orders.created".equals(event.metadata().get("metric_name"))
            );

            assertThat(logEvent.level()).isEqualTo(EventLevel.INFO);
            assertThat(logEvent.serviceName()).isEqualTo("order-service");
            assertThat(logEvent.metadata().get("order_id")).isEqualTo(orderId);
            assertThat(logEvent.metadata().get("status")).isEqualTo(OrderStatus.CREATED.name());
            assertThat(logEvent.metadata().get("item_count")).isEqualTo(3);
            assertThat(new BigDecimal(logEvent.metadata().get("amount").toString()))
                    .isEqualByComparingTo(new BigDecimal("120.75"));

            assertThat(metricEvent.level()).isEqualTo(EventLevel.INFO);
            assertThat(metricEvent.metadata().get("order_id")).isEqualTo(orderId);
            assertThat(metricEvent.metadata().get("metric_value")).isEqualTo(1);
            assertThat(metricEvent.metadata().get("item_count")).isEqualTo(3);
        }
    }

    private ResponseEntity<OrderResponse> createOrder(
            String traceId,
            String customerId,
            String totalAmount,
            int itemCount
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(TraceContextFilter.TRACE_ID_HEADER, traceId);

        Map<String, Object> request = Map.of(
                "customerId", customerId,
                "totalAmount", totalAmount,
                "currency", "USD",
                "itemCount", itemCount
        );

        return restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                OrderResponse.class
        );
    }

    private KafkaConsumer<String, String> kafkaConsumer(String groupId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(properties);
    }

    private EventSchema pollForEvent(
            KafkaConsumer<String, String> consumer,
            Predicate<EventSchema> predicate
    ) {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();

        while (System.nanoTime() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                EventSchema event = JsonUtils.fromJson(record.value(), EventSchema.class);
                if (predicate.test(event)) {
                    return event;
                }
            }
        }

        throw new AssertionError("Expected Kafka event was not published in time");
    }

    private static boolean isTopicExistsException(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof TopicExistsException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
