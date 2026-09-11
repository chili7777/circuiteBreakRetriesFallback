# Arquitectura del laboratorio

```text
HTTP POST /api/orders
       |
       v
OrderController                 <-- adaptador de entrada
       |
       v
CreateOrderUseCase              <-- puerto de entrada
       ^
       |
CreateOrderService              <-- aplicación / caso de uso
       |
       v
AuthorizePaymentPort            <-- puerto de salida
       ^
       |
PaymentHttpAdapter              <-- adaptador de salida + Resilience4j
       |
       v
payment-service                 <-- dependencia externa simulada
```

Regla de lectura: el dominio y el caso de uso no deben conocer HTTP, Kubernetes ni el proveedor concreto. El controlador depende del puerto de entrada. `CreateOrderService` implementa ese puerto y consume el puerto de salida. `PaymentHttpAdapter` implementa el puerto de salida.

## Preguntas para defender

1. ¿Por qué Resilience4j está en el adaptador de salida y no en el dominio?
2. ¿Qué cambiaría si Payments fuera Kafka en lugar de HTTP?
3. ¿Qué efecto tiene hacer retry antes de que el Circuit Breaker contabilice el fallo? Comprueben el comportamiento real, no lo supongan.
4. ¿Fallback significa éxito? En este laboratorio, explícitamente no.
