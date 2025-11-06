# Franchise API Management

[![CI](https://github.com/carcolon/FRANCHISE-API-MANAGEMENT/actions/workflows/ci.yml/badge.svg)](https://github.com/carcolon/FRANCHISE-API-MANAGEMENT/actions/workflows/ci.yml)

API REST para administrar franquicias, sus sucursales y el inventario de productos. Permite crear, actualizar, consultar y eliminar entidades, ademas de obtener el producto con mayor stock por sucursal.

## Integracion continua

- El repositorio incluye un pipeline de GitHub Actions (`.github/workflows/ci.yml`) que ejecuta:
  - `mvn -B test` sobre el backend para validar la capa de servicios.
  - `npm ci && npm run build` dentro de `franchise-management-ui/` para comprobar que la SPA compila sin errores.
- Se ejecuta en cada *push* a las ramas main/master y en todos los *pull requests* dirigidos a ellas.
- Puedes extenderlo agregando pruebas de UI, linting o despliegues editando ese workflow.

## Autenticacion y roles

- Endpoint de login: `POST /api/v1/auth/login` (body JSON con `username` y `password`). Devuelve JWT, roles y fecha de expiracion.
- El JWT debe enviarse en la cabecera `Authorization: Bearer <token>` en cada peticion. El frontend ya lo agrega via interceptor.
- Roles disponibles:
  - `ADMIN`: crear/eliminar franquicias y sucursales, activar/desactivar sucursales/franquicias, gestionar inventario.
  - `USER`: acceso de solo lectura a franquicias, sucursales, inventario y metricas.
- Rutas protegidas:
  - `GET /api/v1/franchises/**`: `ADMIN` o `USER`.
  - Resto de operaciones `POST/PATCH/DELETE /api/**`: solo `ADMIN`.
- Credenciales iniciales (creadas automaticamente):
  - `admin / Admin123!` (ADMIN + USER)
  - `analyst / Analyst123!` (USER)
- Puedes modificar o crear nuevas cuentas en MongoDB (coleccion `users`), las contraseñas deben almacenarse con BCrypt.

## Arquitectura y tecnologias

- Spring Boot 3.3
- Java 17
- MongoDB como base de datos documental
- Spring Data MongoDB y Spring Validation
- SpringDoc OpenAPI 3 (Swagger UI)
- Maven para construccion y gestion de dependencias
- Docker y Docker Compose (opcional) para ejecucion contenerizada
- Angular 17 (frontend en ranchise-management-ui/)

## Cliente web (Angular)

El repositorio incluye una interfaz SaaS construida en Angular que consume esta API.

1. Cambia al directorio del frontend:
   `ash
   cd franchise-management-ui
   `
2. Instala dependencias y levanta el dev server:
   `ash
   npm install
   npm start
   `
3. La UI quedara disponible en http://localhost:4200 y apunte por defecto a http://localhost:8080/api/v1.

Para builds de produccion:
`ash
npm run build
`
Los artefactos se generan en ranchise-management-ui/dist/franchise-management-ui.

Si la API corre en otra direccion/puerto, actualiza src/app/core/config/app-config.ts o exporta la variable correspondiente en tu pipeline.

> **Credenciales iniciales**: `admin / Admin123!` (rol ADMIN) y `analyst / Analyst123!` (solo lectura). Tras iniciar sesion se habilitan las funciones segun rol.

## Requisitos previos

- Java Development Kit (JDK) 17 o superior
- Maven 3.9.11 (o compatible)
- Docker Desktop 4.x (opcional, requerido para la ejecucion contenerizada)
- Cuenta o instancia de MongoDB (local, contenedor o en la nube)

## Configuracion rapida

1. Clonar el repositorio.
2. (Opcional) Crear un archivo .env o definir la variable MONGODB_URI si se desea apuntar a una instancia distinta (por ejemplo mongodb://localhost:27017/franchise_db o una URI con autenticacion).
3. Verificar versiones:
   `ash
   mvn -v
   java -version
   docker --version   # solo si se utilizara Docker
   `
4. Definir el secreto JWT (produccion): `setx JWT_SECRET "mi-secreto-ultra"` / `export JWT_SECRET="mi-secreto-ultra"` (por defecto se usa `change-me-in-production`).

## Ejecucion local con Maven

1. Asegurate de que MongoDB este disponible:
   - Con Docker Compose: docker compose up -d mongo
   - O bien inicia tu propia instancia de MongoDB (servicio local o remoto administrado con MongoDB Compass).
<<<<<<< HEAD
     - Instala MongoDB Community Server o usa Atlas y conÃƒÆ’Ã‚Â©ctate desde Compass.
     - Crea la base `franchise_db` (se generarÃƒÆ’Ã‚Â¡ automÃƒÆ’Ã‚Â¡ticamente la primera vez que la API escriba datos).
=======
     - Instala MongoDB Community Server o usa Atlas y conéctate desde Compass.
     - Crea la base `franchise_db` (se generará automáticamente la primera vez que la API escriba datos).
>>>>>>> a6d9a4ba5ec46d193c32cc3103da177762b4d9a0
     - Opcional: agrega un usuario dedicado y actualiza la URI con credenciales (`mongodb://usuario:password@host:27017/franchise_db?authSource=admin`).
2. (Opcional) Define la URI personalizada:
   - PowerShell: setx MONGODB_URI "mongodb://usuario:password@host:27017/franchise_db?authSource=admin"
   - Bash: export MONGODB_URI="mongodb://usuario:password@host:27017/franchise_db?authSource=admin"
3. Inicia la aplicacion:
   `ash
   mvn spring-boot:run
   `
4. La API quedara disponible en http://localhost:8080.

### Parar la aplicacion

Presiona Ctrl+C en la terminal donde se ejecuto mvn spring-boot:run.

## Ejecucion con Docker Compose

1. Construir e iniciar todos los servicios:
   `ash
   docker compose up --build
   `
   - Se inicia un contenedor mongo con persistencia en volumen
   - Se construye la imagen de la aplicacion usando el Dockerfile y se expone en http://localhost:8080\n   - La API se conecta a Mongo con la URI mongodb://root:example@mongo:27017/franchise_db?authSource=admin
2. Ver logs:
   `ash
   docker compose logs -f franchise-api
   `
3. Detener y limpiar recursos:
   `ash
   docker compose down
   `
   - Para eliminar volumenes agregue --volumes.

> Nota: si PowerShell indica docker : The term 'docker' is not recognized, agrega C:\Program Files\Docker\Docker\resources\bin al PATH o abre una nueva terminal con soporte Docker.

## Configuracion de la aplicacion

Valor por defecto en src/main/resources/application.yml:
`yaml
spring:
  data:
    mongodb:
      uri: 
`
Se puede sobrescribir mediante variable de entorno MONGODB_URI o propiedades de linea de comando (--spring.data.mongodb.uri=...).

## Endpoints principales

| Recurso | Metodo | Ruta | Descripcion |
|---------|--------|------|-------------|
| Franquicias | POST | /api/v1/franchises | Crea una franquicia |
| Franquicias | GET | /api/v1/franchises | Lista todas las franquicias |
| Franquicias | GET | /api/v1/franchises/{franchiseId} | Obtiene una franquicia |
| Franquicias | PATCH | /api/v1/franchises/{franchiseId} | Actualiza el nombre |
| Franquicias | DELETE | /api/v1/franchises/{franchiseId} | Elimina una franquicia (incluye sucursales y productos) |
| Sucursales | POST | /api/v1/franchises/{franchiseId}/branches | Agrega sucursal |
| Sucursales | PATCH | /api/v1/franchises/{franchiseId}/branches/{branchId} | Actualiza nombre |
| Sucursales | PATCH | /api/v1/franchises/{franchiseId}/branches/{branchId}/status | Activa o desactiva la sucursal |
| Sucursales | DELETE | /api/v1/franchises/{franchiseId}/branches/{branchId} | Elimina sucursal |
| Productos | POST | /api/v1/franchises/{franchiseId}/branches/{branchId}/products | Agrega producto |
| Productos | PATCH | /api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId} | Actualiza nombre |
| Productos | PATCH | /api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock | Actualiza stock |
| Productos | DELETE | /api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId} | Elimina producto |
| Consultas | GET | /api/v1/franchises/{franchiseId}/branches/top-products | Producto con mayor stock por sucursal |

## Documentacion OpenAPI

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- OpenAPI YAML: http://localhost:8080/v3/api-docs?format=yaml

## Pruebas

Ejecutar pruebas unitarias:
`ash
mvn test
`
Generar paquete sin pruebas (build CI/CD):
`ash
mvn -DskipTests clean package
`

## Estructura del proyecto

- Dockerfile
- docker-compose.yml
- pom.xml
- README.md
- src/
  - main/
    - java/com/franchise/api/
      - controller/
      - domain/
      - dto/
      - exception/
      - mapper/
      - service/
    - resources/application.yml
  - test/java/com/franchise/api/service/
- franchise-management-ui/
  - README.md
  - src/app/... (Angular SPA)

## Solucion de problemas

- **docker : command not found**: agrega el directorio de Docker CLI al PATH (C:\Program Files\Docker\Docker\resources\bin) o reinicia Docker Desktop.
- **Fallo al conectar con MongoDB**: valida MONGODB_URI, la base de datos debe existir y aceptar conexiones. Si usas Docker Compose, confirma con docker compose ps que el contenedor este en estado 
unning.
- **No puedo agregar productos a una sucursal inactiva**: vuelve a activar la sucursal con `PATCH /api/v1/franchises/{franchiseId}/branches/{branchId}/status` antes de enviar nuevas altas.
- **Puerto 8080 en uso**: cambia el puerto con --server.port=9090 o ajusta pplication.yml.
- **Advertencia de API deprecada**: compila con mvn compile -Xlint:deprecation para identificar el metodo y sustituirlo en el servicio correspondiente.
<<<<<<< HEAD


=======
>>>>>>> a6d9a4ba5ec46d193c32cc3103da177762b4d9a0

