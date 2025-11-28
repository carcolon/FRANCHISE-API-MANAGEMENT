# Franchise API Management

[![CI](https://github.com/carcolon/FRANCHISE-API-MANAGEMENT/actions/workflows/ci.yml/badge.svg)](https://github.com/carcolon/FRANCHISE-API-MANAGEMENT/actions/workflows/ci.yml)

API REST para administrar franquicias, sus sucursales y el inventario de productos. Permite crear, actualizar, consultar y eliminar entidades, ademas de obtener el producto con mayor stock por sucursal.

## Funcionalidades clave

- Gestion completa de franquicias, sucursales y productos con controles de activacion/desactivacion y eliminacion cascada.
- Panel de administracion de usuarios (`/users`) accesible solo para administradores:
  - Crear cuentas con nombre completo, correo y contrasena.
  - Asignar roles `ADMIN` y/o `USER`, activar/desactivar usuarios y eliminarlos definitivamente.
  - Restablecer contrasenas de cuentas comprometidas directamente desde el panel (invoca `POST /api/v1/users/{userId}/reset-password`).
  - Forzar el cambio de contrasena en el primer inicio de sesion cuando se entrega una contrasena temporal.
- Autenticacion basada en JWT con roles diferenciados: `ADMIN` (permisos totales) y `USER` (solo lectura). La interfaz es responsive: topbar, formularios y tablas se adaptan automaticamente a tablets y moviles.
- Recuperacion de contrasenas con token temporal (15 min). La UI muestra el token, permite validarlo (`POST /api/v1/auth/validate-reset-token`) y habilita el cambio de contrasena (`POST /api/v1/auth/reset-password`).
- SPA Angular que consume la API, utiliza guards para proteger rutas y oculta acciones segun el rol.

## Integracion continua

- El repositorio incluye un pipeline de GitHub Actions (`.github/workflows/ci.yml`) que ejecuta:
  - `mvn -B test` sobre el backend para validar la capa de servicios.
  - `npm ci && npm run build` dentro de `franchise-management-ui/` para comprobar que la SPA compila sin errores.
- Se ejecuta en cada *push* a las ramas main/master y en todos los *pull requests* dirigidos a ellas.
- Puedes extenderlo agregando pruebas de UI, linting o despliegues editando ese workflow.

## Autenticacion y roles

- Endpoint de login: `POST /api/v1/auth/login` (body JSON con `username` y `password`). Devuelve JWT, roles y fecha de expiracion.
- El JWT debe enviarse en la cabecera `Authorization: Bearer <token>` en cada peticion. El frontend ya lo agrega via interceptor.
- Recuperacion de contrasena: `POST /api/v1/auth/forgot-password` genera un token temporal y lo devuelve en la respuesta; puedes validarlo con `POST /api/v1/auth/validate-reset-token` y aplicar el cambio definitivo con `POST /api/v1/auth/reset-password`.
- Roles disponibles:
  - `ADMIN`: crear/eliminar franquicias y sucursales, activar/desactivar sucursales/franquicias, gestionar inventario, administrar usuarios.
  - `USER`: acceso de solo lectura a franquicias, sucursales, inventario y metricas.
- Rutas protegidas:
  - `GET /api/v1/franchises/**`: `ADMIN` o `USER`.
  - Resto de operaciones `POST/PATCH/DELETE /api/**` y `/api/v1/users/**`: solo `ADMIN`.
- Credenciales iniciales (creadas automaticamente):
  - `admin / Admin123!` (ADMIN + USER)
  - `analyst / Analyst123!` (USER)
- Puedes modificar o crear nuevas cuentas en MongoDB (coleccion `users`), las contrasenas deben almacenarse con BCrypt.

## Arquitectura y tecnologias

- Spring Boot 3.3
- Java 17
- MongoDB como base de datos documental
- Spring Data MongoDB y Spring Validation
- SpringDoc OpenAPI 3 (Swagger UI)
- Maven para construccion y gestion de dependencias
- Docker y Docker Compose (opcional) para ejecucion contenerizada
- Angular 17 (frontend en `franchise-management-ui/`)
- Arquitectura: backend monolitico en **capas** (Controller → Service → Repository → MongoDB) con DTOs/Mapper y componentes transversales (security filter, exception handler); frontend SPA modular (core/services/guards, features desacopladas) siguiendo el esquema component-based/MVVM.

## Cliente web (Angular)

El repositorio incluye una interfaz SaaS construida en Angular que consume esta API.

1. Cambia al directorio del frontend:
   ```bash
   cd franchise-management-ui
   ```
2. Instala dependencias y levanta el dev server:
   ```bash
   npm install
   npm start
   ```
   - Para previsualizar el comportamiento de produccion (Angular en modo prod, rutas relativas y sin credenciales por defecto) ejecuta `npm run start:prod`.
3. La UI quedara disponible en http://localhost:4200 y apunta por defecto a http://localhost:8080/api/v1.

Para builds de produccion:
```bash
npm run build
```
Los artefactos se generan en `franchise-management-ui/dist/franchise-management-ui`.

Si la API corre en otra direccion/puerto, ajusta `src/environments/environment*.ts` (dev/prod) o exporta la variable correspondiente en tu pipeline.

> **Credenciales iniciales**: `admin / Admin123!` (rol ADMIN) y `analyst / Analyst123!` (solo lectura). Tras iniciar sesion se habilitan las funciones segun rol. La pantalla de login solo muestra estas cuentas cuando corres la SPA en modo desarrollo.

## Requisitos previos

- Java Development Kit (JDK) 17 o superior
- Maven 3.9.11 (o compatible)
- Docker Desktop 4.x (opcional, requerido para la ejecucion contenerizada)
- Cuenta o instancia de MongoDB (local, contenedor o en la nube)

## Configuracion rapida

1. Clonar el repositorio.
2. Duplica `.env.example` como `.env` y ajusta los valores segun tu entorno (el archivo `.env` esta ignorado en Git). Tambien puedes exportar las variables manualmente si prefieres no usar archivos.
3. Verificar versiones:
   ```bash
   mvn -v
   java -version
   docker --version   # solo si se utilizara Docker
   ```
4. Definir el secreto JWT (produccion). Para evitar errores como `Illegal base64 character '-'` debes generar un valor aleatorio en Base64/Base64URL:
   - PowerShell (Windows):
     ```powershell
     $bytes = New-Object byte[] 32
     [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
     $secret = [Convert]::ToBase64String($bytes)
     setx JWT_SECRET $secret
     ```
   - Bash/Zsh (macOS/Linux):
     ```bash
     export JWT_SECRET="$(openssl rand -base64 32)"
     ```
     (añade la línea a `~/.zshrc` o `~/.bashrc` si quieres que sea permanente).

## Perfiles y configuraciones por entorno

- `dev` (predeterminado): pensado para desarrollo local. Usa `application-dev.yml`, conecta por defecto a `mongodb://localhost:27017/franchise_dev` (puedes redefinir `MONGODB_URI` si utilizas usuarios/contraseñas, Docker, etc.) y asigna un `JWT_SECRET` Base64 de prueba si no existe.
- `prod`: no define valores por defecto, exige que `MONGODB_URI` y `JWT_SECRET` vengan del entorno o del gestor de secretos. El archivo `application-prod.yml` unicamente referencia esas variables.

Para desplegar en otro ambiente ajusta la variable `SPRING_PROFILES_ACTIVE`. Ejemplos:
- Local/development (default): no hagas nada o exporta `SPRING_PROFILES_ACTIVE=dev`.
- Produccion: `SPRING_PROFILES_ACTIVE=prod` y define `MONGODB_URI`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `SERVER_PORT`, etc.

El `docker-compose.yml` provisto en el repo esta pensado solo para desarrollo y levanta la API con el perfil `dev`, credenciales ficticias (`devadmin/devpass123`) y un JWT secreto de laboratorio. Todos estos valores se leen desde `.env`; no reutilices el archivo de ejemplo en ambientes reales y define tus propias variables en el servidor/CI.

### Manejo de secretos

- No compartas ni subas el archivo `.env` real (solo `.env.example` vive en el repositorio).  
- En pipelines o servidores configura `MONGODB_URI`, `JWT_SECRET`, etc., como variables/protected secrets.  
- Si necesitas diferentes valores por entorno, crea archivos `.env.dev`, `.env.staging`, etc., sin rastrearlos en Git y cargalos segun corresponda (`docker compose --env-file .env.staging up`).  
- Ante cualquier filtracion, genera credenciales nuevas inmediatamente y actualiza las variables correspondientes.

## Guia paso a paso por entorno

### 1. Preparar archivos `.env`

1. Duplica alguno de los archivos provistos segun el ambiente:
   - Desarrollo: `cp .env.dev .env.local` (Bash) o `Copy-Item .env.dev .env.local` (PowerShell).
   - Produccion: `cp .env.prod .env.prod.local` o el nombre que prefieras.
2. Edita el archivo duplicado con tus valores reales:
   - `MONGODB_URI`: URI completa con usuario, password y base (`franchise_dev`, `franchise_prod`, etc.).
   - `JWT_SECRET`: cadena Base64/Base64URL; genera una nueva para cada ambiente.
   - `SPRING_PROFILES_ACTIVE`: `dev` para desarrollo, `prod` para despliegues reales.
   - `SERVER_PORT`, `JWT_EXPIRATION_MS` y cualquier otra variable que quieras personalizar.
3. Nunca hagas `git add` de esos archivos. Cargalos manualmente cuando ejecutes Maven, Java o Docker Compose:
   - Bash/Zsh: `set -a && source .env.local && set +a`.
   - PowerShell (solo para la sesion actual):
     ```powershell
     Get-Content .env.dev | ForEach-Object {
       if ($_ -and $_ -notmatch '^#') {
         $k,$v = $_ -split '=',2
         [Environment]::SetEnvironmentVariable($k,$v,'Process')
       }
     }
     ```
   - Cambiar de perfil en PowerShell:
     ```powershell
     # Cambiar a desarrollo
     Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction Ignore
     Get-Content .env.dev | ForEach-Object {
       if ($_ -and $_ -notmatch '^#') {
         $k,$v = $_ -split '=',2
         [Environment]::SetEnvironmentVariable($k,$v,'Process')
       }
     }
     mvn spring-boot:run

     # Cambiar a produccion
     Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction Ignore
     Get-Content .env.prod | ForEach-Object {
       if ($_ -and $_ -notmatch '^#') {
         $k,$v = $_ -split '=',2
         [Environment]::SetEnvironmentVariable($k,$v,'Process')
       }
     }
     mvn spring-boot:run
     ```
   - Si prefieres mantener los archivos separados (`.env.dev.local`, `.env.prod.local`), cambia el nombre en los comandos anteriores. Lo importante es limpiar `SPRING_PROFILES_ACTIVE` antes de cargar el archivo nuevo para evitar que el backend siga arrancando con el perfil anterior.

> Consejo: adopta una convencion (`.env.dev.local`, `.env.prod.local`) y documenta donde estan guardados (por ejemplo, un gestor de secretos).

### 2. Desarrollo (`dev`) con Docker Compose

1. Carga las variables: `docker compose --env-file .env.dev config >/dev/null` para validar que todas existen. (Si usas `.env.local`, ajusta el nombre).
2. Arranca los servicios:
   ```bash
   docker compose --env-file .env.dev up --build -d
   ```
   - `mongo` inicia con el usuario definido (`MONGO_INITDB_ROOT_USERNAME/MONGO_INITDB_ROOT_PASSWORD`).
   - `franchise-api` se construye (Dockerfile) y arranca con `SPRING_PROFILES_ACTIVE=dev`.
3. Comprueba el estado:
   - `docker compose ps` debe mostrar ambos contenedores `healthy`.
   - `docker compose logs -f franchise-api` hasta leer `Started FranchiseApiManagementApplication`.
   - `curl http://localhost:${SERVER_PORT:-8080}/actuator/health`.
4. Levanta el frontend en paralelo:
   ```bash
   cd franchise-management-ui
   npm install
   npm start
   ```
   Cambia `src/app/core/config/app-config.ts` si el backend usa otro host/puerto.
5. Para detener todo: `docker compose --env-file .env.dev.local down` (agrega `-v` para borrar `mongo_data`).

> ¿Cambiar de dev a prod con Docker? Usa distintos archivos `.env.*` y especifica el que corresponda en cada comando. Ejemplo:  
> ```bash
> docker compose --env-file .env.prod up --build -d      # levanta prod
> docker compose --env-file .env.prod down               # detiene prod
> docker compose --env-file .env.dev up --build -d       # vuelve a dev
> ```  
> Como cada `--env-file` carga sus variables al vuelo, no necesitas limpiar `SPRING_PROFILES_ACTIVE`; solo asegúrate de derribar los contenedores anteriores antes de levantar el siguiente perfil.

### 3. Desarrollo (`dev`) sin contenedores (Maven + Mongo local)

1. Asegurate de tener una instancia de Mongo corriendo:
   - Docker: `docker run -d --name mongo-dev -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=devadmin -e MONGO_INITDB_ROOT_PASSWORD=devpass123 mongo:7.0`.
   - Servicio local: instala MongoDB Community Server y crea el usuario/BD.
2. Exporta las variables del archivo `.env.local` (ver paso 1) para que Spring pueda leerlas.
3. Ejecuta la aplicacion:
   ```bash
   mvn spring-boot:run
   ```
   - Maven usa `application-dev.yml` y se conecta usando `MONGODB_URI`.
   - Los logs aparecen en la misma terminal; presiona `Ctrl+C` para detener.
4. Verifica:
   - `curl http://localhost:${SERVER_PORT:-8080}/api/v1/franchises`.
   - Inicia sesion en la SPA (`admin/Admin123!`) y revisa que puedas listar franquicias.

### 4. Produccion (`prod`) empaquetando el JAR

1. Compila el backend:
   ```bash
   mvn clean package -DskipTests
   ```
   El artefacto queda en `target/franchise-api-management-0.0.1-SNAPSHOT.jar`.
2. Copia el `.jar` y tu `.env.prod.local` al servidor (ejemplo `scp target/... user@host:/opt/franchise`).
3. En el servidor:
   - Carga las variables (`set -a && source .env.prod.local && set +a` en Linux, comando PowerShell mostrado arriba en Windows).
   - Comprueba que `SPRING_PROFILES_ACTIVE=prod` y que `MONGODB_URI/JWT_SECRET` apuntan al cluster real.
4. Ejecuta el servicio:
   ```bash
   java -jar target/franchise-api-management-0.0.1-SNAPSHOT.jar
   ```
   - Para dejarlo corriendo en segundo plano: `nohup java -jar ... > app.log 2>&1 &` (Linux) o `Start-Process -NoNewWindow -FilePath "java" -ArgumentList "-jar", ...` (Windows).
   - Considera crear un servicio `systemd` o `nssm` para reinicios automaticos.
5. Checklist de verificacion:
   - `curl https://tu-dominio/actuator/health`.
   - Revisar logs (`tail -f app.log`).
   - Ejecutar una autenticacion contra `/api/v1/auth/login`.

### 5. Produccion con Docker Compose

1. Edita la copia de `.env` para que defina el perfil `prod` y todas las variables sensibles (no reutilices los valores de desarrollo).
2. Construye y arranca:
   ```bash
   docker compose --env-file .env.prod.local up --build -d
   ```
   - Puedes intercambiar la referencia a Mongo por una base administrada exponiendo `MONGODB_URI` (por ejemplo, Atlas).
   - Si no necesitas Mongo embebido, elimina el servicio `mongo` del compose y deja solo `franchise-api`.
3. Valida:
   - `docker compose logs -f franchise-api` debe mostrar `Profiles active: prod`.
   - Ejecuta `docker exec -it franchise-api env | Select-String SPRING_PROFILES_ACTIVE` (PowerShell) o `grep` en Bash para confirmar.
   - Ajusta `SERVER_PORT` y el mapeo `ports` para servir en el puerto requerido (por ejemplo `8443:8443` detras de un proxy TLS).
4. Actualiza la imagen cuando publiques una nueva version:
   ```bash
   git pull
   mvn clean package -DskipTests
   docker compose --env-file .env.prod.local build franchise-api
   docker compose --env-file .env.prod.local up -d franchise-api
   ```
   Esto recrea unicamente el servicio de la API con el nuevo binario.

## Backend: ejecucion por perfil

### Desarrollo (`dev`)

1. Copia la plantilla y ajusta valores si hace falta:
   ```bash
   cp .env.dev .env.local   # Bash/macOS/Linux
   # Copy-Item .env.dev .env.local  # PowerShell
   ```
2. Carga las variables en tu shell (o usa tu IDE para inyectarlas).
3. Ejecuta la aplicacion:
   ```bash
   mvn spring-boot:run
   ```
   - Spring usara `application-dev.yml` y el perfil `dev` automaticamente (`SPRING_PROFILES_ACTIVE=dev`).
   - Puedes comprobar el perfil en los logs: `Profiles active: dev`.
4. Verifica salud:
   ```bash
   curl http://localhost:${SERVER_PORT:-8080}/actuator/health
   ```

### Produccion (`prod`)

1. Prepara las variables reales en un archivo (por ejemplo `.env.prod.local`) con `SPRING_PROFILES_ACTIVE=prod`, `MONGODB_URI`, `JWT_SECRET`, etc.
2. Construye el paquete:
   ```bash
   mvn clean package -DskipTests
   ```
3. Copia el `.jar` (y tu archivo `.env`) al servidor destino.
4. En el servidor, exporta las variables y arranca:
   ```bash
   set -a && source .env.prod.local && set +a   # Bash/Linux
   java -jar target/franchise-api-management-0.0.1-SNAPSHOT.jar
   ```
   En Windows/PowerShell usa el snippet indicado antes para cargar variables y `Start-Process` si quieres dejarlo en background.
5. Comprueba:
   ```bash
   curl https://tu-dominio/actuator/health
   ```
   Los logs deben mostrar `Profiles active: prod`.

## Flujo local de desarrollo

### Escenario 1: todo con Docker
1. **Levanta backend + Mongo**  
   ```bash
   docker compose up --build -d
   ```  
   - Mongo queda disponible en `mongodb://root:example@localhost:27017/?authSource=admin`.  
   - La API corre en `http://localhost:8080` usando las variables definidas en `docker-compose.yml`.
2. **Verifica salud**  
   - `docker compose ps` para revisar contenedores.  
   - `docker compose logs -f franchise-api` hasta ver `Started FranchiseApiManagementApplication`.
3. **Frontend Angular**  
   ```bash
   cd franchise-management-ui
   npm install
   npm start
   ```  
   La SPA queda en `http://localhost:4200` consumiendo `http://localhost:8080/api/v1`.
4. **Apaga todo**  
   ```bash
   docker compose down
   ```  
   (agrega `-v` si quieres borrar el volumen `mongo_data`).

### Escenario 2: servicios locales
1. **MongoDB**  
   - Usa tu instalacion local o ejecuta `docker run -d --name mongo-dev -p 27017:27017 mongo:7.0`.  
   - Actualiza `MONGODB_URI` si requieres usuario/clave.
2. **Backend Spring Boot**  
   ```bash
   mvn spring-boot:run
   ```  
   - Lee `MONGODB_URI`, `JWT_SECRET` y `JWT_EXPIRATION_MS` desde el entorno o de `src/main/resources/application.yml`.  
   - Alternativa: `mvn clean package` seguido de `java -jar target/franchise-api-management-0.0.1-SNAPSHOT.jar`.
3. **Frontend Angular** (en otra terminal)  
   ```bash
   cd franchise-management-ui
   npm install   # primera vez
   npm start
   ```  
   Cambia `src/app/core/config/app-config.ts` si el backend corre en otro host/puerto.
4. **Validaciones rapidas**  
   - `curl http://localhost:8080/actuator/health` o abre Swagger (`/swagger-ui.html`).  
   - Inicia sesion en la SPA con `admin / Admin123!` y verifica que puedes listar franquicias.

> Consejo: puedes mezclar ambos enfoques (por ejemplo, Mongo en Docker, backend local). Solo ajusta `MONGODB_URI` para que apunte al servicio correcto.

## Ejecucion local con Maven

1. Asegurate de que MongoDB este disponible:
   - Con Docker Compose: `docker compose up -d mongo`
   - O bien inicia tu propia instancia de MongoDB (servicio local o remoto administrado con MongoDB Compass).
     - Instala MongoDB Community Server o usa Atlas y conectate desde Compass.
     - Crea la base `franchise_db` (se generara automaticamente la primera vez que la API escriba datos).
     - Opcional: agrega un usuario dedicado y actualiza la URI con credenciales (`mongodb://usuario:password@host:27017/franchise_db?authSource=admin`).
2. (Opcional) Define la URI personalizada:
   - PowerShell: `setx MONGODB_URI "mongodb://usuario:password@host:27017/franchise_db?authSource=admin"`
   - Bash: `export MONGODB_URI="mongodb://usuario:password@host:27017/franchise_db?authSource=admin"`
3. Inicia la aplicacion:
   ```bash
   mvn spring-boot:run
   ```
4. La API quedara disponible en http://localhost:8080.

### Parar la aplicacion

Presiona Ctrl+C en la terminal donde se ejecuto `mvn spring-boot:run`.

## Ejecucion con Docker Compose

1. Construir e iniciar todos los servicios:
   ```bash
   docker compose up --build
   ```
   - Se inicia un contenedor Mongo con persistencia en volumen.
   - Se construye la imagen de la aplicacion usando el Dockerfile y se expone en http://localhost:8080.
   - La API se conecta a Mongo con la URI `mongodb://root:example@mongo:27017/franchise_db?authSource=admin`.
2. Ver logs:
   ```bash
   docker compose logs -f franchise-api
   ```
3. Detener y limpiar recursos:
   ```bash
   docker compose down
   ```
   - Para eliminar volumenes agrega `--volumes`.

> Nota: si PowerShell indica `docker : The term 'docker' is not recognized`, agrega `C:\Program Files\Docker\Docker\resources\bin` al PATH o abre una nueva terminal con soporte Docker.

## Configuracion de la aplicacion

Valor por defecto en `src/main/resources/application.yml`:
```yaml
spring:
  data:
    mongodb:
      uri:
```
Se puede sobrescribir mediante variable de entorno `MONGODB_URI` o propiedades de linea de comando (`--spring.data.mongodb.uri=...`).

## Endpoints principales

| Recurso | Metodo | Ruta | Descripcion |
|---------|--------|------|-------------|
| Franquicias | POST | /api/v1/franchises | Crea una franquicia |
| Franquicias | GET | /api/v1/franchises | Lista todas las franquicias |
| Franquicias | GET | /api/v1/franchises/{franchiseId} | Obtiene una franquicia |
| Franquicias | PATCH | /api/v1/franchises/{franchiseId} | Actualiza el nombre |
| Franquicias | PATCH | /api/v1/franchises/{franchiseId}/status | Activa o desactiva una franquicia |
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
| Usuarios | GET | /api/v1/users | Lista usuarios registrados (solo ADMIN) |
| Usuarios | POST | /api/v1/users | Crea un nuevo usuario con roles asignados (solo ADMIN) |
| Usuarios | PATCH | /api/v1/users/{userId}/status | Activa o desactiva un usuario (solo ADMIN) |
| Usuarios | POST | /api/v1/users/{userId}/reset-password | Restablece la contrasena de un usuario (solo ADMIN) |
| Usuarios | DELETE | /api/v1/users/{userId} | Elimina un usuario (solo ADMIN) |
| Auth | POST | /api/v1/auth/forgot-password | Genera token temporal de recuperacion |
| Auth | POST | /api/v1/auth/validate-reset-token | Valida vigencia del token de recuperacion |
| Auth | POST | /api/v1/auth/reset-password | Actualiza contrasena usando token valido |

## Documentacion OpenAPI

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- OpenAPI YAML: http://localhost:8080/v3/api-docs?format=yaml

### Acceso en produccion

- Backend (Swagger UI prod): https://franchise-api-management-prod.onrender.com/swagger-ui/index.html
- Frontend (SPA Angular prod): https://franchise-api-management-ui-prod.onrender.com

## Pruebas

### Backend (unitarias + integracion)
- `mvn test`: ejecuta las pruebas unitarias (`**/*UnitTest.java`).
- `mvn verify -Pintegration-tests`: ejecuta solo las pruebas de integracion (`**/*IntegrationTest.java`) con Spring Boot + MockMvc + Mongo embebido.
- `mvn -DskipTests clean package`: build sin pruebas.

### Frontend
- `npm run test`: suite Karma/Jasmine.
- `npm run e2e`: levanta `ng serve` y corre Cypress headless (flujos auth). Para modo interactivo usa `npm run cy:open`.

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
      - security/
      - service/
    - resources/application.yml
  - test/java/com/franchise/api/
- franchise-management-ui/
  - README.md
  - src/app/... (Angular SPA)

## Solucion de problemas

- **`docker` : command not found**: agrega el directorio de Docker CLI al PATH (`C:\Program Files\Docker\Docker\resources\bin`) o reinicia Docker Desktop.
- **Fallo al conectar con MongoDB**: valida `MONGODB_URI`, la base de datos debe aceptar conexiones. Con Docker Compose, confirma con `docker compose ps` que el contenedor esta en estado `running`.
- **No puedo agregar productos a una sucursal inactiva**: vuelve a activar la sucursal con `PATCH /api/v1/franchises/{franchiseId}/branches/{branchId}/status` antes de enviar nuevas altas.
- **Puerto 8080 en uso**: cambia el puerto con `--server.port=9090` o ajusta `application.yml`.
- **Advertencia de API deprecada**: compila con `mvn compile -Xlint:deprecation` para identificar el metodo y sustituirlo en el servicio correspondiente.

## Buenas practicas de colaboracion (VS Code + Git)

1. **Actualiza main antes de trabajar**
   ```bash
   git checkout master
   git pull origin master
   ```
2. **Crea una rama descriptiva por funcionalidad**
   - Convencion: `feature/<breve-descripcion>` o `fix/<breve-descripcion>`.
   - Ejemplo (nueva vista para administrar franquicias): `feature/franchises-dashboard`.
   - VS Code: vista Source Control → menú “…” → `Create Branch…`, escribe `feature/franchises-dashboard` y presiona Enter.
   - Desde la terminal del proyecto:
     ```bash
     git checkout -b feature/franchises-dashboard
     ```
3. **Desarrolla y haz commits atomicos**
   - Incluye pruebas (`mvn test`, `npm run test -- --watch=false`, etc.) antes de cada commit.
   - Mensajes cortos y en imperativo: `git commit -m "Add password change modal"`.
4. **Revisa tu rama antes de abrir PR**
   ```bash
   git status
   git diff
   npm run build
   mvn test
   ```
5. **Sube la rama y crea Pull Request**
   ```bash
   git push -u origin feature/nueva-funcion
   ```
   Luego abre el PR en GitHub, describe alcance y pruebas realizadas.
6. **Revisiones y merges**
   - Al incorporar comentarios, usa nuevos commits; evita `--force` salvo coordinacion previa.
   - Antes de mergear, haz `git pull --rebase origin master`, resuelve conflictos y vuelve a ejecutar pruebas.
7. **Convenciones de pruebas**
   - Unitarias backend: archivos `*UnitTest.java`, se ejecutan con `mvn test`.
   - Integracion backend: archivos `*IntegrationTest.java`, se ejecutan con `mvn verify -Pintegration-tests`.
   - E2E Cypress: `cypress/e2e/*.cy.ts`, se ejecutan con `npm run e2e` (o `npm run cy:open`).
8. **Limpieza**
   - Tras el merge, borra la rama remota y local (`git branch -d feature/...` y en GitHub → Delete Branch).
   - Documenta los cambios relevantes en README/`docs/entregaX.md` si aplica.

> Tip VS Code: habilita “Auto Fetch” para mantenerse sincronizado; usa “GitLens” para historial y “Tasks” para scripts frecuentes (build/test).

## Solucion de problemas (auth/JWT)

- **Illegal base64 character '-'**: significa que `JWT_SECRET` contiene caracteres no válidos para Base64. Genera un secreto con los comandos anteriores (PowerShell u OpenSSL) antes de iniciar el backend. El servicio ya soporta Base64 y Base64URL, pero necesitas un valor nuevo después de clonar el repo.
- **“JWT secret must be configured”**: la variable, secret o `.env` no está presente. Define `JWT_SECRET` antes de `mvn spring-boot:run` o de levantar Docker Compose.
- **Olvidé el secreto**: basta con generar uno nuevo y reiniciar el backend desde visual; los tokens existentes quedarán invalidados automáticamente.



