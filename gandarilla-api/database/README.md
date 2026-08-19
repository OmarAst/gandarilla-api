# Scripts PostgreSQL

## Instalación manual

1. Ejecuta `01_create_database.sql` conectado a `postgres`.
2. Cambia la conexión a `Gandarilla`.
3. Ejecuta `02_schema.sql`.
4. Agrega residentes usando `03_residentes_ejemplo.sql`.
5. Revisa la instalación con `04_queries_verificacion.sql`.

Para la carga inicial de las 50 casas también está disponible
`05_carga_residentes_50_casas.sql`. Completa primero todos los teléfonos; el
script valida los datos y se detiene si detecta una casa con residente activo.

## Instalación con Flyway

Crea solamente la base con `01_create_database.sql`. Al iniciar la aplicación con `FLYWAY_ENABLED=true`, Flyway ejecuta `V1__initial_schema.sql` y crea las tablas.

Si ejecutas el esquema manualmente, Flyway detectará una base no vacía y la marcará con la versión base configurada. Aun así, es preferible elegir instalación manual o Flyway para evitar confusión.
