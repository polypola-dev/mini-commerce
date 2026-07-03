package com.minicommerce;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityVerificationTest {

    @Test
    void verifiesModularStructure() {
        ApplicationModules.of(OrderApiApplication.class).verify();
    }
}
