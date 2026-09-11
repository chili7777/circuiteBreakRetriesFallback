package dev.chili7777.lab.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
public class PaymentApplication {
  public static void main(String[] args) { SpringApplication.run(PaymentApplication.class, args); }
}

@RestController
@RequestMapping("/api/payments")
class PaymentController {
  private final AtomicReference<FailureMode> mode = new AtomicReference<>(new FailureMode(0, 0));

  @PostMapping("/authorize")
  ResponseEntity<Map<String,Object>> authorize(@RequestBody PaymentRequest request) throws InterruptedException {
    FailureMode f = mode.get();
    if (f.delayMs() > 0) Thread.sleep(f.delayMs());
    if (ThreadLocalRandom.current().nextInt(100) < f.failurePercent()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("authorized", false, "reason", "provider-unavailable", "at", Instant.now().toString()));
    }
    return ResponseEntity.ok(Map.of("authorized", true, "paymentId", "pay-" + request.orderId(), "at", Instant.now().toString()));
  }

  @PutMapping("/lab/failure-mode")
  FailureMode failureMode(@RequestBody FailureMode requested) {
    int percent = Math.max(0, Math.min(100, requested.failurePercent()));
    long delay = Math.max(0, Math.min(10_000, requested.delayMs()));
    FailureMode normalized = new FailureMode(percent, delay); mode.set(normalized); return normalized;
  }

  @GetMapping("/lab/failure-mode") FailureMode current() { return mode.get(); }
}
record PaymentRequest(String orderId, BigDecimal amount) {}
record FailureMode(int failurePercent, long delayMs) {}
