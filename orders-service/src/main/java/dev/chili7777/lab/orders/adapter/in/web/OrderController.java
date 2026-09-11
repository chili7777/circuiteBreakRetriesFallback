package dev.chili7777.lab.orders.adapter.in.web;
import dev.chili7777.lab.orders.application.port.in.CreateOrderUseCase;
import dev.chili7777.lab.orders.domain.Order;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/orders")
public class OrderController {
 private final CreateOrderUseCase useCase;
 public OrderController(CreateOrderUseCase useCase) { this.useCase = useCase; }
 @PostMapping public Order create(@RequestBody CreateOrderRequest request) { return useCase.create(request.orderId(), request.amount()); }
}
record CreateOrderRequest(String orderId, BigDecimal amount) {}
