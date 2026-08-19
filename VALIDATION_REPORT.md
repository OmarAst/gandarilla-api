# Reporte de validación

## Alcance revisado

- Proyecto Spring Boot y estructura de paquetes.
- Endpoint para registrar movimientos de pago.
- Consulta de movimientos y envío de recibos por ID.
- Compatibilidad de envío mediante casa y mes.
- Configuración PostgreSQL y Flyway.
- Scripts de creación de base, tablas, índices y triggers.
- Ausencia de credenciales reales dentro del proyecto.

## Resultados

- `pom.xml` analizado correctamente como XML.
- 59 archivos Java de código y pruebas revisados.
- El esquema manual y la migración Flyway `V1` son idénticos.
- No se encontraron referencias activas al repositorio JSON anterior.
- Se ejecutó `javac --release 17 -proc:none` como revisión sintáctica.
- No se detectaron errores de sintaxis como llaves sin cerrar, sentencias incompletas o declaraciones inválidas.
- Los errores restantes de esa revisión corresponden a dependencias externas no disponibles en este contenedor: Spring Boot, Jakarta Validation, Lombok, PDFBox y Log4j2.
- No se encontraron contraseñas, tokens de Meta ni Bearer Tokens reales hardcodeados.

## Limitaciones de validación

No fue posible ejecutar `mvn clean test` porque Maven y las dependencias del proyecto no están instalados en este entorno y el contenedor no tiene acceso a los repositorios remotos. Tampoco se ejecutaron los scripts contra PostgreSQL porque no hay servidor ni cliente `psql` disponibles.

Validación final recomendada en el equipo local:

```powershell
$env:DB_PASSWORD="TU_CONTRASEÑA"
mvn clean test
mvn clean package
```

Después inicia PostgreSQL y prueba las solicitudes incluidas en `requests.http`.
