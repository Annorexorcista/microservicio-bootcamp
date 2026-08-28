# Microservicio de Bootcamps — HU4: Registrar Bootcamp

Microservicio reactivo (Spring WebFlux + R2DBC/MySQL + WebClient) que expone el registro de bootcamps siguiendo **arquitectura hexagonal** (puertos y adaptadores). Implementa la **Historia de Usuario 4 (HU4)**: registrar un bootcamp con nombre, descripción, fecha de lanzamiento, duración en días y un conjunto de capacidades asociadas (entre 1 y 4, sin repetidos), validando la existencia de esas capacidades en el microservicio de Capacidad.

## Tabla de contenido

- [Funcionalidad](#funcionalidad)
- [Arquitectura hexagonal](#arquitectura-hexagonal)
- [Relación entre microservicios y WebClient](#relación-entre-microservicios-y-webclient)
- [Programación reactiva: Mono, Flux y operadores](#programación-reactiva-mono-flux-y-operadores)
- [Recorrido del flujo de una petición](#recorrido-del-flujo-de-una-petición)
- [Reglas de negocio](#reglas-de-negocio)
- [API REST](#api-rest)
- [Estrategia de pruebas](#estrategia-de-pruebas)
- [Cómo ejecutar](#cómo-ejecutar)

## Funcionalidad

El endpoint `POST /api/v1/bootcamps` recibe un nombre, una descripción, una fecha de lanzamiento, una duración en días y una lista de `capabilityIds`, y registra un bootcamp nuevo. Antes de persistir:

1. **Normaliza** nombre y descripción aplicando `trim`.
2. **Valida** obligatoriedad y longitud: nombre 1–50 caracteres, descripción 1–90 caracteres.
3. **Valida** que la fecha de lanzamiento esté presente y que la duración en días sea un entero positivo.
4. **Valida** la cantidad de capacidades (entre 1 y 4) y que no haya identificadores repetidos.
5. **Verifica la existencia** de todas las capacidades consultando al microservicio de Capacidad vía `WebClient`.
6. **Persiste** el bootcamp y sus asociaciones en MySQL vía R2DBC, de forma transaccional, y devuelve el bootcamp creado con su `id` autogenerado.

Cualquier fallo se traduce a una respuesta HTTP uniforme: `400` (datos inválidos o capacidad inexistente), `502` (Capability_Service no disponible), `500` (error inesperado).

## Arquitectura hexagonal

El código separa el **dominio** (reglas de negocio puras, sin Spring) de la **infraestructura** (adaptadores que hablan con el mundo exterior). El dominio define **puertos** (interfaces) y la infraestructura provee **adaptadores** (implementaciones). Las dependencias apuntan siempre hacia el dominio.

```
src/main/java/com/bootcamp/bootcamp
├── domain                        # Núcleo puro (sin anotaciones de framework)
│   ├── model/Bootcamp                # Modelo de dominio inmutable
│   ├── api/IBootcampServicePort      # Puerto de ENTRADA (lo consume la capa web)
│   ├── spi/IBootcampPersistencePort  # Puerto de SALIDA (persistencia)
│   ├── spi/ICapabilityGatewayPort    # Puerto de SALIDA (validación de capacidades)
│   ├── usecase/BootcampUseCase       # Reglas de negocio (implementa el puerto de entrada)
│   └── exception/                    # Errores de dominio + DomainErrorCode
├── application/config            # Cableado de beans (wiring) + R2DBC + WebClient + OpenAPI
└── infrastructure/adapters
    ├── driving/webflux           # Adaptador de ENTRADA (HTTP)
    │   ├── router/                   # RouterFunction (rutas funcionales)
    │   ├── handler/                  # Handler que compone el pipeline reactivo
    │   ├── dto/                      # Request/Response/Error (transporte)
    │   ├── mapper/                   # DTO <-> dominio
    │   └── exception/                # Handler global de errores reactivo
    └── driven
        ├── r2dbc                 # Adaptador de SALIDA (base de datos)
        │   ├── entity/               # Entidades @Table (bootcamp, bootcamp_capability)
        │   ├── repository/           # ReactiveCrudRepository
        │   ├── mapper/               # entidad <-> dominio
        │   └── adapter/              # Implementa IBootcampPersistencePort
        └── http                  # Adaptador de SALIDA (gateway HTTP)
            ├── dto/                  # CapabilityGatewayResponse
            └── CapabilityGatewayAdapter # Implementa ICapabilityGatewayPort (WebClient)
```

Ventaja clave: el dominio (`BootcampUseCase`, `Bootcamp`, los puertos) **no conoce Spring, HTTP ni R2DBC**. Se prueba de forma unitaria con mocks, y los adaptadores se pueden sustituir sin tocar las reglas de negocio. El cableado se hace en `BeanConfiguration`, por eso las clases de dominio y los adaptadores no llevan `@Component`.

## Relación entre microservicios y WebClient

Las capacidades son propiedad del **microservicio de Capacidad** y viven en su propia base de datos. Cumpliendo la regla del reto ("cada microservicio persiste únicamente su base de datos"), el microservicio de Bootcamp **no** duplica el catálogo: persiste solo los `capabilityId` en una tabla puente (`bootcamp_capability`) y consulta al de Capacidad cuando necesita validar existencia.

Esa consulta se hace con **`WebClient`**, el cliente HTTP reactivo y no bloqueante de Spring. El microservicio de Capacidad expone una **consulta por identificadores** `GET /api/v1/capabilities?ids=1,2,3`, que devuelve solo las capacidades existentes; comparando lo solicitado con lo devuelto, Bootcamp detecta identificadores inexistentes. Es el mismo patrón que Capacidad usa contra Tecnología (`GET /api/v1/technologies?ids=...`).

> **Nota de contrato.** Esta consulta por ids es distinta del listado paginado del catálogo (pensado para la vista del admin). La consulta por ids está pensada para validar la existencia de un conjunto concreto de identificadores. La `baseUrl` se configura por la variable `CAPABILITY_SERVICE_URL` (por defecto `http://localhost:8081`).

## Programación reactiva: Mono, Flux y operadores

Este servicio es **100% no bloqueante**. Nunca se usa `.block()` en el código de producción; todo se compone con operadores de **Project Reactor**. El registro combina dos fuentes de I/O —la base de datos (R2DBC) y una llamada HTTP saliente (`WebClient`)— en un único pipeline.

### Mono y Flux

- **`Mono<T>`**: flujo asíncrono que emite **0 o 1** elemento. El registro de un bootcamp retorna `Mono<Bootcamp>`.
- **`Flux<T>`**: flujo asíncrono que emite **0..N** elementos. La consulta de capacidades existentes retorna `Flux<Long>` desde el gateway.

Nada se ejecuta hasta la **suscripción** (evaluación perezosa); en WebFlux el framework se suscribe al enviar la respuesta HTTP.

### Operadores usados en este proyecto

| Operador | Dónde | Para qué sirve |
|----------|-------|----------------|
| `flatMap` | `BootcampUseCase`, `BootcampHandler`, adaptador | Encadena un paso que devuelve otro publisher (asíncrono): validación → existencia → guardado. |
| `map` | `BootcampHandler`, mappers, gateway | Transforma el valor con una función síncrona (entidad → dominio, dominio → DTO, response → id). |
| `Mono.defer` | `BootcampUseCase.validate` | Difiere las validaciones en memoria hasta la suscripción, para emitir `Mono.error` de forma perezosa. |
| `collectList` | `BootcampUseCase.ensureCapabilitiesExist` | Recoge el `Flux<Long>` de capacidades existentes en una lista para compararla con lo solicitado. |
| `Mono.just(x)` / `Mono.error(ex)` | `BootcampUseCase` | Emite un valor disponible, o **falla** cortocircuitando el pipeline (así nunca se persiste tras un error). |
| `Flux.fromIterable` / `Flux.empty` | gateway, adaptador de persistencia | Construye flujos a partir de colecciones; `Flux.empty()` cortocircuita el gateway ante entrada vacía. |
| `concatMap` | `BootcampPersistenceAdapter.save` | Inserta las filas de la tabla puente en orden, una por capacidad. |
| `onErrorMap(...)` | gateway | Traduce el fallo del Capability_Service a `CapabilityValidationUnavailableException` (502). |
| `.as(transactionalOperator::transactional)` | `BootcampPersistenceAdapter.save` | Envuelve la escritura (bootcamp + asociaciones) en una **transacción reactiva** atómica. |
| `bodyToMono` / `bodyToFlux` | `BootcampHandler`, gateway | Deserializa cuerpos JSON de forma no bloqueante. |

### La "regla de oro" reactiva

Nunca bloquear. Todo se compone con operadores y WebFlux ejecuta el pipeline sobre un número reducido de hilos de event-loop, escalando mejor bajo carga que el modelo de un hilo por petición.

## Recorrido del flujo de una petición

`POST /api/v1/bootcamps` con `{ "name": "Backend 2026", "description": "...", "launchDate": "2026-03-01", "durationInDays": 84, "capabilityIds": [1,2,3] }`:

1. **`BootcampRouter`** declara la ruta funcional (`RouterFunction`) y la delega en el handler. La documentación OpenAPI se declara con `@RouterOperation` porque las rutas funcionales no se auto-documentan como los `@RestController`.
2. **`BootcampHandler.register`** compone el pipeline:
   `bodyToMono(BootcampRequest) → map(toDomain) → flatMap(servicePort::registerBootcamp) → map(toResponse) → flatMap(ServerResponse 201)`.
3. **`BootcampUseCase.registerBootcamp`** (dominio) ejecuta las reglas:
   `validate(...) → flatMap(ensureCapabilitiesExist) → flatMap(persistencePort::save)`.
   - `validate` aplica `trim`, comprueba obligatoriedad, longitudes, fecha, duración, cantidad (1–4) y no repetición; ante fallo emite `Mono.error(InvalidBootcampDataException)`.
   - `ensureCapabilitiesExist` consulta el gateway; si falta alguna capacidad emite `Mono.error(CapabilitiesNotFoundException)`; si el servicio está caído se propaga `CapabilityValidationUnavailableException`.
4. **`BootcampPersistenceAdapter.save`** guarda el bootcamp (obtiene el id generado), inserta las filas de `bootcamp_capability`, todo en una transacción reactiva.
5. Si algo falla, el error viaja hasta **`GlobalErrorWebExceptionHandler`**, que lo traduce a un `ErrorResponse` JSON con el código HTTP adecuado.

## Reglas de negocio

| Regla | Detalle | Error / código HTTP |
|-------|---------|---------------------|
| Nombre obligatorio | No null/vacío/solo espacios (tras `trim`) | `NAME_REQUIRED` → 400 |
| Longitud del nombre | Máximo 50 caracteres (tras `trim`) | `NAME_TOO_LONG` → 400 |
| Descripción obligatoria | No null/vacía/solo espacios (tras `trim`) | `DESCRIPTION_REQUIRED` → 400 |
| Longitud de la descripción | Máximo 90 caracteres (tras `trim`) | `DESCRIPTION_TOO_LONG` → 400 |
| Fecha de lanzamiento obligatoria | No nula | `LAUNCH_DATE_REQUIRED` → 400 |
| Duración válida | Entero positivo de días (> 0) | `DURATION_INVALID` → 400 |
| Cantidad mínima de capacidades | Al menos 1 | `CAPABILITIES_TOO_FEW` → 400 |
| Cantidad máxima de capacidades | Máximo 4 | `CAPABILITIES_TOO_MANY` → 400 |
| No repetición de capacidades | Sin identificadores duplicados | `CAPABILITIES_DUPLICATED` → 400 |
| Existencia de capacidades | Todas deben existir en el Capability_Service | `CapabilitiesNotFoundException` → 400 |
| Disponibilidad del Capability_Service | Si no responde, no se puede validar | `CapabilityValidationUnavailableException` → 502 |
| No persistir ante error | Ninguna validación fallida debe escribir en BD | invariante verificada por tests |

El esquema (`schema.sql`) refuerza reglas en la BD: `name VARCHAR(50)`, `description VARCHAR(90)`, `launch_date DATE`, `duration_days INT`, y `bootcamp_capability` con PK compuesta `(bootcamp_id, capability_id)` (impide filas duplicadas) y FK a `bootcamp`.

## API REST

### Registrar bootcamp

`POST /api/v1/bootcamps`

Request body:

```json
{
  "name": "Bootcamp Backend 2026",
  "description": "Formación intensiva backend",
  "launchDate": "2026-03-01",
  "durationInDays": 84,
  "capabilityIds": [1, 2, 3]
}
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "name": "Bootcamp Backend 2026",
  "description": "Formación intensiva backend",
  "launchDate": "2026-03-01",
  "durationInDays": 84,
  "capabilityIds": [1, 2, 3]
}
```

Respuesta de error (`400` / `502`):

```json
{
  "status": 400,
  "code": "CAPABILITIES_TOO_MANY",
  "message": "Un bootcamp debe tener como máximo 4 capacidades",
  "timestamp": "2026-01-01T00:00:00Z"
}
```

Documentación interactiva (Swagger UI): `http://localhost:8083/swagger-ui.html`.

## Estrategia de pruebas

El reto exige pruebas para cada regla de negocio. Se combinan tres niveles:

### 1. Tests unitarios del caso de uso — `BootcampUseCaseTest`

JUnit 5 + Mockito + `StepVerifier`. Mockean **ambos** puertos SPI (persistencia y gateway) y verifican ejemplos y casos borde de cada regla: registro válido, límites de longitud (1/50, 1/90), fecha nula, duración (0/negativa/1/positiva), cantidad de capacidades (0/1/4/5), duplicados, capacidad inexistente, error del gateway y normalización por `trim`. Cada caso de rechazo verifica `verify(persistencePort, never()).save(any())` — nunca se persiste ante un error.

### 2. Property-based tests (jqwik) — `BootcampProperty1..8Test`

jqwik genera cientos de entradas aleatorias (mínimo 100 iteraciones por propiedad; aquí 200/300) y comprueba que una propiedad universal se cumple siempre. Ambos puertos SPI se mockean, aislando las reglas del dominio.

| Test | Propiedad |
|------|-----------|
| Property 1 | Un registro válido conserva los datos normalizados y persiste exactamente una vez. |
| Property 2 | Nombre inválido (vacío o > 50) se rechaza sin persistir. |
| Property 3 | Descripción inválida (vacía o > 90) se rechaza sin persistir. |
| Property 4 | Fecha nula o duración ≤ 0 se rechazan sin persistir. |
| Property 5 | Cantidad de capacidades fuera de rango (< 1 o > 4) se rechaza sin persistir. |
| Property 6 | Capacidades repetidas se rechazan sin persistir. |
| Property 7 | Una capacidad inexistente rechaza el registro sin persistir. |
| Property 8 | Invariante global: ante cualquier error, `save` no se invoca jamás. |

### 3. Tests de integración (Testcontainers) — persistencia, gateway y endpoint

- **`BootcampPersistenceAdapterIT`**: MySQL real en Docker (Testcontainers). Verifica que `save` asigna id y persiste el bootcamp y sus asociaciones en la tabla puente.
- **`CapabilityGatewayAdapterTest`**: usa `MockWebServer` (OkHttp) para simular el Capability_Service sin depender de Docker ni del microservicio real. Verifica que emite solo los ids existentes, que un 5xx o un error de conexión se traducen a `CapabilityValidationUnavailableException`, y que una entrada vacía no realiza llamada HTTP.
- **`BootcampEndpointIT`**: arranca el contexto completo de Spring Boot y usa `WebTestClient` contra MySQL real (Testcontainers) y `MockWebServer` (Capability_Service). Ejercita el endpoint real: `201`, `400` (nombre/descripción, fecha/duración, cantidad, repetidos, inexistentes) y `502`.
- **`BootcampOpenApiIT`**: smoke de OpenAPI; verifica que `/v3/api-docs` contiene el path `POST /api/v1/bootcamps`.

#### Nota técnica: MySQL con Testcontainers en un proyecto solo-R2DBC

Este proyecto no incluye el driver JDBC de MySQL (es puramente R2DBC). `MySQLContainer` comprueba el arranque abriendo una conexión JDBC, lo que provocaría `ClassNotFoundException`. Por eso los tests de integración levantan MySQL con un `GenericContainer<>("mysql:8.0")` y una wait strategy basada en el log de arranque:

```java
new GenericContainer<>("mysql:8.0")
    .withEnv("MYSQL_DATABASE", "bootcamp_db")
    .withEnv("MYSQL_USER", "test")
    .withEnv("MYSQL_PASSWORD", "test")
    .withEnv("MYSQL_ROOT_PASSWORD", "root")
    .withExposedPorts(3306)
    .waitingFor(Wait.forLogMessage(".*port: 3306  MySQL Community Server.*", 1)
        .withStartupTimeout(Duration.ofSeconds(180)));
```

La conexión de la aplicación sigue siendo R2DBC (`spring.r2dbc.*`), enlazada al contenedor mediante `@DynamicPropertySource`.

> Los tests de integración requieren **Docker en ejecución**.

## Cómo ejecutar

Requisitos: JDK 17, Docker (solo para los tests de integración), un MySQL accesible y el microservicio de Capacidad en ejecución para el flujo real.

Ejecutar la suite de pruebas completa:

```bash
./gradlew clean test
```

Levantar el servicio (variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `CAPABILITY_SERVICE_URL`; por defecto BD `localhost:3306/bootcamp_db` y Capability_Service `http://localhost:8081`):

```bash
./gradlew bootRun
```

El esquema `schema.sql` se ejecuta automáticamente al arrancar (`spring.sql.init.mode=always`).

Endpoints útiles (el servicio escucha en el puerto **8083**):

- API: `POST http://localhost:8083/api/v1/bootcamps`
- Swagger UI: `http://localhost:8083/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8083/v3/api-docs`

---

**Estado:** HU4 (Registrar Bootcamp) completa — dominio, persistencia, gateway y endpoint implementados y cubiertos por tests unitarios, property-based (jqwik, 8 propiedades) e integración (Testcontainers + MockWebServer).
