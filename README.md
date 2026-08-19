# Gandarilla API

API REST en Spring Boot para registrar pagos de mantenimiento, generar recibos PDF y enviarlos por WhatsApp. Conserva el arquetipo usado en la API de Dulce Arte: controladores, servicios, repositorios JDBC, DTOs, respuesta estándar `meta/data`, PostgreSQL, Flyway, validaciones, manejo global de errores, seguridad por Bearer Token, CORS, rate limiting y Log4j2.

## Requisitos

- Java 21.
- Maven 3.9 o superior.
- PostgreSQL 14 o superior.
- WhatsApp Cloud API de Meta, únicamente cuando se habilite el envío real.

## Base de datos

La base se llama `Gandarilla` y contiene:

- `residentes`: titular y teléfono asociado a una casa.
- `movimientos_pago`: pagos registrados, periodo, folio y seguimiento del envío por WhatsApp.

Los scripts se encuentran en `database/`:

1. `01_create_database.sql`: crea la base `Gandarilla`.
2. `02_schema.sql`: crea secuencia, tablas, restricciones e índices.
3. `03_residentes_ejemplo.sql`: plantilla para cargar o reemplazar residentes.
4. `04_queries_verificacion.sql`: consultas de revisión.

También se incluye `src/main/resources/db/migration/V1__initial_schema.sql`. Si la base ya existe y está vacía, Flyway crea automáticamente las tablas al iniciar la API.

### Ejecución recomendada en pgAdmin

1. Conéctate a la base `postgres` y ejecuta `database/01_create_database.sql`.
2. Abre Query Tool sobre `Gandarilla`.
3. Ejecuta `database/02_schema.sql`.
4. Edita y ejecuta los INSERT de `database/03_residentes_ejemplo.sql`.

No existe una tabla adicional para formas de pago. Se guarda un entero validado:

| Valor | Forma de pago |
|---:|---|
| 1 | Transferencia |
| 2 | Depósito |
| 3 | Efectivo |
| 4 | Otro |

Se permiten varios movimientos para un mismo residente, mes y año, de modo que puedan registrarse abonos o correcciones sin eliminar información.

## Configuración local

En PowerShell:

```powershell
cd gandarilla-api

$env:DB_URL="jdbc:postgresql://localhost:5432/Gandarilla"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="TU_CONTRASEÑA"
$env:SECURITY_ENABLED="false"
$env:WHATSAPP_ENABLED="false"

mvn clean test
mvn spring-boot:run
```

Swagger estará disponible en:

```text
http://localhost:4000/swagger-ui.html
```

El frontend administrativo estará disponible en:

```text
http://localhost:4000/
http://localhost:4000/web/dashboard
http://localhost:4000/web/payment-movements
```

Está construido con Spring MVC y Thymeleaf dentro del mismo JAR. El dashboard
permite filtrar métricas por mes y año; la pantalla de movimientos ofrece alta
individual, alta masiva y generación mensual de comprobantes.

Cuando `SECURITY_ENABLED=true`, la API utiliza Bearer Tokens JWT con duración
de una hora por defecto. El dashboard es público y el área administrativa usa
login en `/admin/login` con `WEB_USERNAME` y `WEB_PASSWORD`.

Configura las tres variables cuando habilites la seguridad. No utilices
contraseñas de ejemplo en producción.

Configura también una clave aleatoria de al menos 32 caracteres para firmar los
tokens. El administrador obtiene un token nuevo mediante `POST /api/auth/token`
usando HTTP Basic; el token se envía después como `Authorization: Bearer <token>`.

El archivo `.env.example` sirve como referencia, pero Spring Boot no carga archivos `.env` automáticamente.

## Endpoints

### Registrar un movimiento

```http
POST /api/payment-movements
Content-Type: application/json
```

```json
{
  "casa": 42,
  "monto": 900.00,
  "observaciones": "Pago correspondiente a julio",
  "formaPago": 1,
  "fechaPago": "2026-07-07",
  "mes": 7,
  "anio": 2026
}
```

El servicio busca al residente activo de la casa, genera un folio como `GAN-2026-000001` y devuelve `201 Created`.

### Registrar pagos para varias casas

```http
POST /api/payment-movements/batch
Content-Type: application/json
```

```json
{
  "casas": [1, 2, 3, 4, 6, 9, 16, 19],
  "monto": 800.00,
  "observaciones": "Pago correspondiente a agosto",
  "formaPago": 1,
  "fechaPago": "2026-08-04",
  "mes": 8,
  "anio": 2026
}
```

El lote admite entre 1 y 50 casas únicas. Primero valida que todas tengan residente activo y después registra un movimiento y folio independiente para cada una dentro de una sola transacción. Si alguna casa falla, se revierte el lote completo.

### Consultar el movimiento

```http
GET /api/payment-movements/{id}
```

### Enviar el recibo del movimiento

Este es el endpoint recomendado porque identifica un pago exacto:

```http
POST /api/payment-movements/{id}/send-receipt
```

La API obtiene residente y movimiento desde PostgreSQL, reserva atómicamente el envío, genera un PDF con fuente Unicode embebida, lo archiva y lo envía al teléfono normalizado del residente. Responde `200 OK`. Cuando WhatsApp está deshabilitado, devuelve estado `SIMULATED`, libera la reserva y no marca el movimiento como enviado.

La migración `V2__payment_send_reservation.sql` agrega la reserva persistente que evita envíos simultáneos incluso con varias instancias. Una reserva abandonada puede recuperarse después de `RECEIPT_DUPLICATE_WINDOW` (30 segundos por defecto); un fallo de PDF o WhatsApp libera la reserva y deja el pago sin marcar para permitir el reintento.

### Endpoint compatible por casa y mes

```http
POST /api/receipts/send
Content-Type: application/json
```

```json
{
  "casa": 42,
  "mes": 7
}
```

Busca el movimiento `REGISTRADO` más reciente de esa casa y mes usando el año actual del servidor. Para pagos históricos o cuando existan varios abonos, usa el endpoint por ID.

### Generar los comprobantes pagados de un mes

```http
POST /api/receipts/generate-monthly
Content-Type: application/json
```

```json
{
  "mes": 8,
  "anio": 2026
}
```

Genera un comprobante para el movimiento `REGISTRADO` más reciente de cada residente en el periodo. No envía WhatsApp ni modifica el estado del movimiento. Los archivos se guardan en `recibos/08-2026/` con nombres como `1-Omar Astorga.pdf`. Una ejecución posterior reemplaza de forma controlada los archivos del mismo periodo.

## WhatsApp Cloud API

Para envío real configura:

```powershell
$env:WHATSAPP_ENABLED="true"
$env:WHATSAPP_GRAPH_VERSION="vXX.X"
$env:WHATSAPP_PHONE_NUMBER_ID="..."
$env:WHATSAPP_ACCESS_TOKEN="..."
$env:WHATSAPP_TEMPLATE_NAME="recibo_pago_mantenimiento"
$env:WHATSAPP_LANGUAGE_CODE="es_MX"
$env:WHATSAPP_CONNECT_TIMEOUT="5s"
$env:WHATSAPP_READ_TIMEOUT="20s"
```

La plantilla esperada usa:

- Encabezado de tipo documento para adjuntar el PDF.
- Variable 1: nombre del residente.
- Variable 2: mes.
- Variable 3: año.

Meta debe tener aprobada exactamente la plantilla `recibo_pago_mantenimiento`, idioma `es_MX`, categoría Utilidad, encabezado Documento PDF y tres parámetros de cuerpo en ese orden. La aplicación valida al arrancar que versión Graph, phone-number ID, token y plantilla estén presentes cuando el modo real está habilitado.

## Verificación

```powershell
mvn clean test
mvn clean package
mvn spring-boot:run
```

Para una validación manual, registra un pago, consulta el ID retornado y llama una sola vez a `POST /api/payment-movements/{id}/send-receipt`. Con WhatsApp deshabilitado comprueba `estadoEnvio=SIMULATED` y `whatsappEnviado=false`. En modo real comprueba el `wamid`, `whatsappEnviado=true` y que un segundo envío responda `409`.

## Seguridad

Para proteger `/api/**`:

```powershell
$env:SECURITY_ENABLED="true"
$env:API_BEARER_SECRET="CLAVE_SECRETA_DE_AL_MENOS_32_CARACTERES"
$env:WEB_USERNAME="admin"
$env:WEB_PASSWORD="CONTRASEÑA_ADMIN"
```

Después envía:

```http
Authorization: Bearer TOKEN_LARGO_Y_ALEATORIO
```

No guardes contraseñas, tokens de Meta ni credenciales en Git.

## Estructura principal

```text
src/main/java/com/riogandarilla/api/
├── configs/
├── controllers/
├── dto/
├── entities/
├── exception/
├── filters/
├── repositories/
├── security/
├── services/
└── utils/

database/
├── 01_create_database.sql
├── 02_schema.sql
├── 03_residentes_ejemplo.sql
└── 04_queries_verificacion.sql
```
