# Franchise API Management

API REST para administrar franquicias, sus sucursales y el inventario de productos. Permite crear, actualizar, consultar y eliminar entidades, ademas de obtener el producto con mayor stock por sucursal.

## Arquitectura y tecnologias

- Spring Boot 3.3
- Java 17
- MongoDB como base de datos documental
- Spring Data MongoDB y Spring Validation
- SpringDoc OpenAPI 3 (Swagger UI)
- Maven para construccion y gestion de dependencias
- Docker y Docker Compose (opcional) para ejecucion contenerizada

## Requisitos previos

- Java Development Kit (JDK) 17 o superior
- Maven 3.9.11 (o compatible)
- Docker Desktop 4.x (opcional, requerido para la ejecucion contenerizada)
- Cuenta o instancia de MongoDB (local, contenedor o en la nube)

## Configuracion rapida

1. Clonar el repositorio.
2. (Opcional) Crear un archivo `.env` o definir la variable `MONGODB_URI` si se desea apuntar a una instancia distinta a `mongodb://localhost:27017/franchise_db`.
3. Verificar versiones:
   ```bash
   mvn -v
   java -version
   docker --version   # solo si se utilizara Docker
   ```

## Ejecucion local con Maven

1. Asegurate de que MongoDB este disponible:
   - Con Docker Compose: `docker compose up -d mongo`
   - O bien inicia tu propia instancia de MongoDB.
2. (Opcional) Define la URI personalizada:
   - PowerShell: `setx MONGODB_URI "mongodb://usuario:password@host:27017/franchise_db"`
   - Bash: `export MONGODB_URI="mongodb://usuario:password@host:27017/franchise_db"`
3. Inicia la aplicacion:
   ```bash
   mvn spring-boot:run
   ```
4. La API quedara disponible en `http://localhost:8080`.

### Parar la aplicacion

Presiona `Ctrl+C` en la terminal donde se ejecuto `mvn spring-boot:run`.

## Ejecucion con Docker Compose

1. Construir e iniciar todos los servicios:
   ```bash
   docker compose up --build
   ```
   - Se inicia un contenedor `mongo` con persistencia en volumen
   - Se construye la imagen de la aplicacion usando el Dockerfile y se expone en `http://localhost:8080`
2. Ver logs:
   ```bash
   docker compose logs -f franchise-api
   ```
3. Detener y limpiar recursos:
   ```bash
   docker compose down
   ```
   - Para eliminar volumenes agregue `--volumes`.

> Nota: si PowerShell indica `docker : The term 'docker' is not recognized`, agrega `C:\Program Files\Docker\Docker\resources\bin` al PATH o abre una nueva terminal con soporte Docker.

## Configuracion de la aplicacion

Valor por defecto en `src/main/resources/application.yml`:
```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/franchise_db}
```
Se puede sobrescribir mediante variable de entorno `MONGODB_URI` o propiedades de linea de comando (`--spring.data.mongodb.uri=...`).

## Endpoints principales

| Recurso | Metodo | Ruta | Descripcion |
|---------|--------|------|-------------|
| Franquicias | POST | `/api/v1/franchises` | Crea una franquicia |
| Franquicias | GET | `/api/v1/franchises` | Lista todas las franquicias |
| Franquicias | GET | `/api/v1/franchises/{franchiseId}` | Obtiene una franquicia |
| Franquicias | PATCH | `/api/v1/franchises/{franchiseId}` | Actualiza el nombre |
| Sucursales | POST | `/api/v1/franchises/{franchiseId}/branches` | Agrega sucursal |
| Sucursales | PATCH | `/api/v1/franchises/{franchiseId}/branches/{branchId}` | Actualiza nombre |
| Productos | POST | `/api/v1/franchises/{franchiseId}/branches/{branchId}/products` | Agrega producto |
| Productos | PATCH | `/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}` | Actualiza nombre |
| Productos | PATCH | `/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock` | Actualiza stock |
| Productos | DELETE | `/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}` | Elimina producto |
| Consultas | GET | `/api/v1/franchises/{franchiseId}/branches/top-products` | Producto con mayor stock por sucursal |

## Documentacion OpenAPI

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs?format=yaml`

## Pruebas

Ejecutar pruebas unitarias:
```bash
mvn test
```
Generar paquete sin pruebas (build CI/CD):
```bash
mvn -DskipTests clean package
```

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
## Solucion de problemas

- **`docker : command not found`**: agrega el directorio de Docker CLI al PATH (`C:\Program Files\Docker\Docker\resources\bin`) o reinicia Docker Desktop.
- **Fallo al conectar con MongoDB**: valida `MONGODB_URI`, la base de datos debe existir y aceptar conexiones. Si usas Docker Compose, confirma con `docker compose ps` que el contenedor este en estado `running`.
- **Puerto 8080 en uso**: cambia el puerto con `--server.port=9090` o ajusta `application.yml`.
- **Advertencia de API deprecada**: compila con `mvn compile -Xlint:deprecation` para identificar el metodo y sustituirlo en el servicio correspondiente.

