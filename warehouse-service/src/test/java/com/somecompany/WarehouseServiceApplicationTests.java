package com.somecompany;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"conf.temperature-port=0", "conf.humidity-port=0"}) // any free port, 3344 may be in use
class WarehouseServiceApplicationTests {

    @Test
    void applicationStartupShouldWork() {
    }
}
