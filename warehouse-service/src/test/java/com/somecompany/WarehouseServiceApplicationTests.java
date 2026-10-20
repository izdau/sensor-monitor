package com.somecompany;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"conf.sensor.temperature-port=0", "conf.sensor.humidity-port=0"}) // any free port, in case 3344 or 3355 are in use
class WarehouseServiceApplicationTests {

    @Test
    void applicationStartupShouldWork() {
    }
}
