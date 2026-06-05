package tn.esprit.arabsoftback;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import tn.esprit.arabsoftback.notification.NotificationEvent;

@SpringBootTest
class ArabSoftBackApplicationTests {

    @MockBean
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Test
    void contextLoads() {
    }

}
