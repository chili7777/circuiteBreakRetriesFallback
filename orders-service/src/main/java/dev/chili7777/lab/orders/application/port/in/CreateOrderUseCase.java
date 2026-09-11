package dev.chili7777.lab.orders.application.port.in;
import dev.chili7777.lab.orders.domain.Order;
import java.math.BigDecimal;
public interface CreateOrderUseCase { Order create(String orderId, BigDecimal amount); }
