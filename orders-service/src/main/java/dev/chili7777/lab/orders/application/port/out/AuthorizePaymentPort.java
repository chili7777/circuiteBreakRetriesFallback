package dev.chili7777.lab.orders.application.port.out;
import java.math.BigDecimal;
public interface AuthorizePaymentPort { PaymentAuthorization authorize(String orderId, BigDecimal amount); }
