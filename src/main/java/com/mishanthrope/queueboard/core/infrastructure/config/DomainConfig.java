package com.mishanthrope.queueboard.core.infrastructure.config;

import com.mishanthrope.queueboard.core.domain.employee.EmployeeFactory;
import com.mishanthrope.queueboard.core.domain.employee.EmployeeRepository;
import com.mishanthrope.queueboard.core.domain.order.KitchenSettings;
import com.mishanthrope.queueboard.core.domain.order.OrderFactory;
import com.mishanthrope.queueboard.core.domain.order.OrderRepository;
import com.mishanthrope.queueboard.core.domain.order.PrepTimeCalculator;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public KitchenSettings kitchenSettings(OrdersProperties properties) {
        OrdersProperties.PrepTime prepTime = properties.prepTime();
        return new KitchenSettings(
                prepTime.overhead(),
                prepTime.wrapPerItem(),
                prepTime.grill(),
                prepTime.grillCapacity());
    }

    @Bean
    public PrepTimeCalculator prepTimeCalculator(KitchenSettings kitchenSettings) {
        return new PrepTimeCalculator(kitchenSettings);
    }

    @Bean
    public OrderFactory orderFactory(
            OrderRepository orderRepository,
            PrepTimeCalculator prepTimeCalculator,
            OrdersProperties properties,
            Clock clock) {
        return new OrderFactory(
                orderRepository, prepTimeCalculator, properties.paidAtMaxSkew(), clock);
    }

    @Bean
    public EmployeeFactory employeeFactory(EmployeeRepository employeeRepository) {
        return new EmployeeFactory(employeeRepository);
    }
}
