# Challenge: ¿Lo mandarías a producción?

## Incidente

A las 14:00 inicia una campaña. Orders puede recibir 20 rps normalmente, 80 rps en pico sostenido y ráfagas de 150 rps. Payments puede responder lento o fallar. La configuración inicial tiene decisiones deliberadamente discutibles. Ustedes son el equipo responsable del go/no-go.

## Parte A — Observabilidad y SLO

Ejecuten smoke y baseline. Registren p95, p99, éxito de negocio, fallbacks, CPU/memoria y réplicas. Respondan: ¿qué SLI permite saber si el cliente realmente pudo comprar? ¿Por qué `HTTP 200` puede engañar? ¿Una prueba de 2 minutos demuestra 99,9% mensual?

**Aceptación:** evidencia reproducible y tabla antes/después; definición explícita del denominador de disponibilidad.

## Parte B — Resiliencia

Usen el endpoint de laboratorio de Payments para probar al menos: 100% de fallo y 1200 ms de latencia. Observen Retry, Circuit Breaker y Fallback. Cambien la configuración si su evidencia lo justifica.

Respondan: ¿qué excepciones son reintentables? ¿Cuándo un retry multiplica una caída? ¿Cuántos intentos máximos caben en el presupuesto de latencia? ¿Cuándo abre el breaker? ¿Cómo comprueban HALF_OPEN y recuperación? ¿El fallback conserva la semántica de negocio?

**Aceptación:** no aprobar pagos inexistentes; configuración razonada; evidencia de apertura/recuperación; comparación de latencia y carga al proveedor.

## Parte C — Kubernetes y HPA

Revisen `requests/limits`, min/max, objetivo CPU y ventana de scale-down. Ajusten solo después de medir. Desplieguen en un clúster de laboratorio cuando esté disponible y observen con Lens o kubectl.

Respondan: ¿por qué el HPA de CPU depende de `requests.cpu`? ¿Qué diferencia hay entre escalado horizontal y vertical? ¿Qué pasa si `maxReplicas=6` no alcanza 150 rps? ¿Por qué un spike puede terminar antes de que el HPA reaccione?

**Aceptación:** `kubectl get hpa` y `kubectl top pods` o evidencia equivalente; explicar el cálculo y no solo mostrar YAML.

## Parte D — Performance

Ejecuten, en orden: smoke → baseline → load → stress → spike → mini-soak. Para el soak del taller se permiten 15 minutos; expliquen por qué esto NO sustituye un soak real de varias horas. Si no hay tiempo, cada equipo puede ejecutar un subconjunto distinto y compartir resultados, pero debe saber interpretar todos.

**Aceptación:** identificar el punto de degradación; distinguir capacidad del servicio de capacidad del generador; correlacionar carga, latencia, fallos y réplicas.

## Parte E — Presentación final

En máximo 5 minutos respondan: **GO o NO-GO**, tres evidencias, dos riesgos y la siguiente mejora prioritaria. Se puede aprobar un NO-GO si está técnicamente bien demostrado.

## Bonus

- Propongan métricas Prometheus/alertas para burn rate.
- Expliquen cuándo KEDA sería preferible a HPA para una carga basada en cola/eventos.
- Añadan un test que proteja una regla de arquitectura hexagonal.
