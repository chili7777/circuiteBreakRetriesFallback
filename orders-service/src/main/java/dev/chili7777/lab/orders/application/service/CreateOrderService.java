package dev.chili7777.lab.orders.application.service;
import dev.chili7777.lab.orders.application.port.in.CreateOrderUseCase;
import dev.chili7777.lab.orders.application.port.out.AuthorizePaymentPort;
import dev.chili7777.lab.orders.application.port.out.PaymentAuthorization;
import dev.chili7777.lab.orders.domain.Order;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
@Service
public class CreateOrderService implements CreateOrderUseCase {
 private final AuthorizePaymentPort payments;
 public CreateOrderService(AuthorizePaymentPort payments) { this.payments = payments; }
 @Override public Order create(String orderId, BigDecimal amount) {
   PaymentAuthorization result = payments.authorize(orderId, amount);
   return new Order(orderId, amount, result.authorized() ? "CONFIRMED" : "REJECTED", result.detail());
 }
}
