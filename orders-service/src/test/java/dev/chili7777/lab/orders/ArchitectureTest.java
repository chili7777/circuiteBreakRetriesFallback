package dev.chili7777.lab.orders;
import static org.assertj.core.api.Assertions.assertThat;
import dev.chili7777.lab.orders.application.port.in.CreateOrderUseCase;
import dev.chili7777.lab.orders.application.port.out.AuthorizePaymentPort;
import org.junit.jupiter.api.Test;
class ArchitectureTest {
 @Test void ports_are_interfaces() {
   assertThat(CreateOrderUseCase.class.isInterface()).isTrue();
   assertThat(AuthorizePaymentPort.class.isInterface()).isTrue();
 }
}
