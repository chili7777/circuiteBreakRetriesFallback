# Empezar sin instalar k6

**k6 NO es un requisito del taller.** Elijan una ruta por equipo. No intenten instalar herramientas no autorizadas en una computadora corporativa.

## Qué debe tener cada equipo

| Lo que ya tienen | Ruta |
|---|---|
| JDK 17 o 21 + Maven 3.9 (o Maven integrado en el IDE) | A: Java, sin Docker y sin k6 |
| Docker Engine/Desktop autorizado, activo, con Compose v2 y contenedores Linux | B: servicios y k6 dentro de Docker |
| Equipo restringido sin una de esas rutas | Trabajar en pareja en una máquina habilitada; no perder el taller instalando |
| Acceso a Kubernetes de laboratorio + Metrics Server | Añadir práctica de HPA; no es necesario para arrancar A o B |

**La primera compilación necesita dependencias de Maven. Docker además necesita descargar imágenes.** Usen el proxy/repositorio corporativo permitido. No se promete ejecución offline ni se pide desactivar seguridad. El JDK debe estar accesible desde la terminal; el JRE que ejecuta el IDE no sustituye automáticamente al JDK del proyecto.

## Obtener el proyecto

```text
git clone https://github.com/chili7777/circuiteBreakRetriesFallback.git
cd circuiteBreakRetriesFallback
```

Si ya lo clonaron y no han comenzado a modificarlo: `git pull --ff-only`. También pueden usar GitHub → Code → Download ZIP y abrir la carpeta extraída.

## Ruta A — Java / IntelliJ; no necesita k6, Docker, Node ni Python

**1. Comprobar el JDK y compilar**, desde la raíz donde está el `pom.xml`:

```text
java -version
javac -version
mvn clean verify
```

Si `mvn` no está en PATH pero usan IntelliJ, abran el `pom.xml` raíz como proyecto Maven, seleccionen JDK 17/21 y ejecuten `clean verify` desde la ventana Maven del IDE. También pueden ejecutar `PaymentApplication` y `OrdersApplication` directamente desde el IDE, después de resolver dependencias.

**2. Arrancar los servicios** después del build, en DOS terminales separadas:

Terminal 1:
```text
java -jar payment-service/target/payment-service-1.0.0.jar
```
Terminal 2:
```text
java -jar orders-service/target/orders-service-1.0.0.jar
```

**3. En una tercera terminal**, desde la raíz del repo:

```text
java tools/LabLoad.java check
java tools/LabLoad.java smoke
java tools/LabLoad.java baseline
```

Estos comandos no necesitan k6. La invocación de un archivo `.java` usa el compilador del JDK. Los comandos anteriores son iguales en PowerShell, CMD, macOS y Linux cuando el JDK está en PATH. Windows y Linux tienen verificación automática; macOS no fue ejecutado en esa matriz.

Pruebas adicionales:
```text
java tools/LabLoad.java load
java tools/LabLoad.java stress
java tools/LabLoad.java spike
java tools/LabLoad.java soak
```

`smoke`: 1 req/s durante 30 s. `baseline`: 20/s durante 2 min. `load`: 80/s durante 3 min. `stress`: escalones 20/80/150/200, un minuto cada uno. `spike`: 20/s 30 s → 150/s 60 s → 20/s 120 s, con reporte de recuperación por bloques de 30 s. `soak`: 20/s durante 15 min; solo mini-soak de taller.

Guardar evidencia: el runner escribe un CSV en `reports/` y muestra p95/p99, confirmadas, fallbacks, errores, descartadas y tasa efectivamente enviada. `business_success_sent` = confirmadas / enviadas, incluyendo fallos de transporte; los descartes del generador se informan aparte. Un HTTP 200 con `REJECTED` NO cuenta como compra.

**No es una implementación de k6.** Es una alternativa didáctica sin dependencias. Tiene timeout HTTP de 3 s y máximo 128 solicitudes en vuelo por defecto. Sus descartes incluyen retraso de planificación >100 ms o falta de cupos. No comparar sus cifras directamente con k6 como si fueran la misma herramienta. En Java el stress es por escalones; en k6 es una rampa.

Los resultados de stress/spike son exploratorios: un código de salida 0 valida el generador, no el SLO ni la recuperación. Inspeccionen cada fase. Los percentiles incluyen todas las solicitudes terminadas, también las fallidas. Si el generador y los servicios están en la misma laptop comparten recursos: los números no certifican capacidad de producción.

## Inyectar y recuperar fallos sin curl ni JSON en PowerShell

Con ambos servicios levantados, contra SU simulador local:

```text
java tools/LabLoad.java failure 100 0
java tools/LabLoad.java outage
java tools/LabLoad.java failure 0 0
```

El perfil `outage` espera proveedor totalmente indisponible: cero autorizaciones falsas, respuestas controladas y p95 <1000 ms. No usarlo como prueba de 20% de fallos intermitentes.

Proveedor lento y recuperación:
```text
java tools/LabLoad.java failure 0 1200
java tools/LabLoad.java outage
java tools/LabLoad.java failure 0 0
```

Después de restaurar Payments, dar oportunidad a OPEN → HALF_OPEN → CLOSED; no diagnosticar fallo de recuperación solo por la primera petición rechazada.

Para una comprobación abreviada, declarando que NO es el perfil completo:
```text
java tools/LabLoad.java baseline http://localhost:8080 10
```
El tercer argumento es duración en segundos, solo para perfiles de una fase. El cuarto es máximo de solicitudes en vuelo. No reducir duración/tasa y afirmar que pasaron la prueba completa.

## Ruta B — Docker, sin instalar Java, Maven ni k6 en el host

Docker y Compose deben existir y estar autorizados. Comprobar:
```text
docker version
docker compose version
```

Construir y arrancar:
```text
docker compose up --build -d payment-service orders-service
```
Los Dockerfiles compilan el código en una etapa Maven; ya NO requieren un JAR precompilado en `target/`.

Ejecutar k6 dentro del contenedor:
```text
docker compose --profile tools run --rm -e SCENARIO=smoke k6
docker compose --profile tools run --rm -e SCENARIO=baseline k6
docker compose --profile tools run --rm -e SCENARIO=load k6
```

El servicio k6 usa `http://orders-service:8080` dentro de la red de Compose, no `localhost` del contenedor. El script espera a que Orders esté listo. Cambiar SCENARIO por stress, spike o soak para los demás perfiles.

Parar y revisar logs:
```text
docker compose logs --tail=100 orders-service payment-service
docker compose down
```

Las imágenes están fijadas para el laboratorio, no se anuncian como últimas versiones ni como baseline de seguridad de producción. Puertos publicados solo en 127.0.0.1. No exponer controles de fallos a Internet.

## HPA no es lo mismo que ejecutar los servicios localmente

Sin clúster no hay demostración real de HPA. Para esa parte: usar un clúster de laboratorio autorizado por equipo/namespace o una demostración compartida del instructor. Sin acceso, entregar diseño y declarar explícitamente que no se ejecutó. No hay penalización por no instalar k6.

No usar `kubectl port-forward service/orders-service` para concluir que la carga se repartió entre todas las réplicas: el reenvío termina en un pod seleccionado. Para medir HPA, usar un generador dentro del clúster contra el Service, o el ingreso/balanceador de laboratorio autorizado. No ejecutar cargas de varios equipos contra el mismo destino sin coordinar la suma de solicitudes/s.

## Antes de comenzar el reloj del taller

Un equipo está listo cuando: ambos servicios arrancan, `check` responde UP y `smoke` envía órdenes confirmadas. Si no puede, compartir una máquina preparada; no convertir las tres horas en una instalación de herramientas.

## Fuentes y evidencia

- Java source-file mode: https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html
- k6 en Docker: https://grafana.com/docs/k6/latest/set-up/install-k6/
- Verificación real Windows/Linux: https://github.com/chili7777/circuiteBreakRetriesFallback/actions/runs/34609037951
- Alcance exacto y limitaciones: [VALIDATION.md](VALIDATION.md).
