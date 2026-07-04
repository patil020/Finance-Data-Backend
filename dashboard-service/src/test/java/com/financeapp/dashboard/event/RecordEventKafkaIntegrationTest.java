package com.financeapp.dashboard.event;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

import com.financeapp.common.Constants;
import com.financeapp.dashboard.DashboardServiceApplication;
import com.financeapp.dashboard.service.DashboardCacheService;
import com.financeapp.event.FinancialRecordEvent;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = DashboardServiceApplication.class,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
                "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
                "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
        })
class RecordEventKafkaIntegrationTest {

    @Container
    static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @MockitoBean
    private DashboardCacheService dashboardCacheService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void recordCreatedKafkaEventInvalidatesDashboardCache() throws Exception {
        FinancialRecordEvent event = FinancialRecordEvent.builder()
                .recordId("record-1")
                .amount(BigDecimal.valueOf(100))
                .type("INCOME")
                .category("Salary")
                .action("CREATE")
                .build();
        event.setUserId("user-1");
        event.setEventType("FINANCIAL_RECORD_CREATE");
        event.setSourceService("record-service");

        kafkaTemplate.send(Constants.KAFKA_RECORD_CREATED_TOPIC, event.getRecordId(), event)
                .get(10, TimeUnit.SECONDS);

        await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    verify(dashboardCacheService).invalidateSummaryCache("user-1");
                    verify(dashboardCacheService).invalidateCategoryWiseCache("user-1");
                });
    }
}
