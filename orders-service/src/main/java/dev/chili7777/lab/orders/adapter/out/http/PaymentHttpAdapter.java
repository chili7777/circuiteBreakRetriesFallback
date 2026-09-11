package dev.chili7777.lab.orders.adapter.out.http;
import dev.chili7777.lab.orders.application.port.out.AuthorizePaymentPort;
import dev.chili7777.lab.orders.application.port.out.PaymentAuthorization;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
@Component
public class PaymentHttpAdapter implements AuthorizePaymentPort {
 private final RestTemplate http; private final String baseUrl;
 public PaymentHttpAdapter(RestTemplateBuilder builder, @Value("${payment.base-url}") String baseUrl) {
   this.http = builder.connectTimeout(Duration.ofMillis(300)).readTimeout(Duration.ofMillis(700)).build(); this.baseUrl = baseUrl;
 }
 @Override
 @Retry(name="payment")
 @CircuitBreaker(name="payment", fallbackMethod="fallback")
 public PaymentAuthorization authorize(String orderId, BigDecimal amount) {
   PaymentResponse r = http.postForObject(baseUrl + "/api/payments/authorize", new PaymentRequest(orderId, amount), PaymentResponse.class);
   if (r == null || !r.authorized()) throw new PaymentProviderException("payment not authorized");
   return new PaymentAuthorization(true, "provider-authorized");
 }
 private PaymentAuthorization fallback(String orderId, BigDecimal amount, Throwable cause) {
   return new PaymentAuthorization(false, "controlled-fallback:" + cause.getClass().getSimpleName());
 }
}
record PaymentRequest(String orderId, BigDecimal amount) {}
record PaymentResponse(boolean authorized, String paymentId, String reason, String at) {}
class PaymentProviderException extends RuntimeException { PaymentProviderException(String message) { super(message); } }
