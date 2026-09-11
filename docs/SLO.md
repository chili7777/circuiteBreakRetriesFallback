# SLI / SLO / SLA del caso

## SLI que deben medir

- **Business success rate:** órdenes `CONFIRMED` / órdenes intentadas. El fallback `REJECTED` NO cuenta como éxito.
- **Latency:** p95 y p99 del endpoint de órdenes, además de la dependencia si disponen de telemetría.
- **Error/fallback rate:** separar errores HTTP, fallos de negocio y fallbacks.
- **Saturation:** CPU y memoria por pod; réplicas deseadas/actuales del HPA.
- **Load-generator health:** iteraciones descartadas de k6. Si k6 no consigue producir la carga solicitada, la prueba no demuestra capacidad.

## SLO de producción del ejercicio

Disponibilidad de negocio: **99,9% en 30 días**. Esto deja un error budget de 0,1%. Una prueba de 3 horas no demuestra este SLO mensual: solo produce evidencia puntual.

Objetivos de rendimiento bajo proveedor sano:
- p95 < 500 ms
- p99 < 1000 ms
- business success ≥ 99%

## SLA

El SLA no es el SLO. Para el ejercicio, redacten un ejemplo de compromiso contractual menos estricto que el SLO interno y expliquen por qué. No inventen que existe un SLA real del banco.

## Error budget

Calculen cuánto tiempo equivale 0,1% de una ventana de 30 días y expliquen cómo usarían ese presupuesto para decidir entre velocidad de cambio y estabilidad.
