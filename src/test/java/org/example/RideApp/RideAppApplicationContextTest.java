package org.example.RideApp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class RideAppApplicationContextTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsAgainstRealMongo() {
        assertThat(context.getBean(org.example.RideApp.services.PassengerService.class)).isNotNull();
        assertThat(context.getBean(org.example.RideApp.services.DriverService.class)).isNotNull();
    }
}
