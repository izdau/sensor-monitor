package com.somecompany;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false") // do not connect to Kafka
class CentralServiceApplicationTests {

    @Test
    void applicationStartupShouldWork() {
    }
}
