# Franchise API Management  Entrega 2

## 1. Informacion general

| Campo | Detalle |
| --- | --- |
| Nombre del proyecto | Franchise API Management |
| Version del prototipo | 0.2.0  |
| Fecha de la entrega |  |
| Equipo responsable | |
| Repositorio | https://github.com/carcolon/FRANCHISE-API-MANAGEMENT |
| Enlace Figma | <URL al prototipo de alta fidelidad> |
| Enlace Maze | <URL al informe de pruebas de usabilidad> |

> Sustituir los campos entre `<>` con la informacion real antes de exportar a PDF.

## 2. Prototipo funcional

### 2.1 Enlaces y rutas

| Artefacto | Enlace / Ruta |
| --- | --- |
| Backend API (Swagger UI) | http://localhost:8080/swagger-ui.html |
| Backend API (OpenAPI JSON) | http://localhost:8080/v3/api-docs |
| Coleccion Postman/Insomnia | `docs/assets/postman/franchise-api.postman_collection.json` |
| Frontend Angular Dev Server | http://localhost:4200 |
| Build compilado del frontend | `franchise-management-ui/dist/franchise-management-ui/` |

### 2.2 Funcionalidades clave

- Panel administrativo para:
  - Registrar, editar, activar/inactivar y eliminar franquicias, sucursales y productos.
  - Consultar el producto con mayor stock en cada sucursal.
  - Gestionar cuentas de usuario: creación con nombre completo, correo y contraseña, asignación de roles `ADMIN`/`USER`, activación/desactivación y eliminación de cuentas.
  - Fuerza a los usuarios nuevos a cambiar la contraseña temporal en su primer inicio de sesión mediante un modal de cambio obligatorio.
  - Eliminar cuentas existentes con confirmación previa (disponible solo para administradores).
- Aplicación web (SPA) con autenticación JWT:
  - Usuarios de solo lectura visualizan franquicias, sucursales y productos sin acciones críticas.
  - Administradores ven el menú “Usuarios”, con formulario para crear nuevos admins o usuarios estándar y tabla con controles de estado.
  - Diseño responsive basado en CSS Grid/Flexbox: los módulos (topbar, listados, formularios y modales) se reacomodan automáticamente en tablets y móviles.
- Recuperación de credenciales mediante token temporal:
  - `POST /api/v1/auth/forgot-password` devuelve el token (15 min) y la UI muestra un modal con la clave.
  - El formulario de restablecimiento permite validar previamente el token (`POST /api/v1/auth/validate-reset-token`) antes de definir la nueva contraseña (`POST /api/v1/auth/reset-password`).

### 2.2 Instalacion local (Java + MongoDB)

1. Instalar JDK 17 y Maven 3.9 (verificar con `mvn -v`).
2. Levantar MongoDB local o remoto (Docker Compose usa `mongodb://root:example@mongo:27017/franchise_db?authSource=admin`; si trabajas sin contenedores, conecta desde MongoDB Compass a `mongodb://localhost:27017` o a tu clster Atlas).
3. Ejecutar la API:
   ```bash
   mvn spring-boot:run
   ```
4. Levantar el frontend:
   ```bash
   cd franchise-management-ui
   npm install
   npm start
   ```
5. Validar funcionamiento en `http://localhost:4200` y consumos a `http://localhost:8080/api/v1`.
6. Para gestionar sucursales, recuerda:
   - `DELETE /api/v1/franchises/{id}` borra la franquicia completa (sucursales + productos).
   - `PATCH /api/v1/franchises/{id}/branches/{branchId}/status` activa/desactiva una sucursal. Mientras est inactiva no admite nuevas altas de productos.
   - `DELETE /api/v1/franchises/{id}/branches/{branchId}` remueve la sucursal del documento principal.
### 2.3 Instalacion con Docker Compose

1. Asegurarse de tener Docker Desktop 4.x en ejecucion.
2. Construir e iniciar servicios:
   ```bash
   docker compose up --build
   ```
3. Revisar logs del backend:
   ```bash
   docker compose logs -f franchise-api
   ```
4. Detener y limpiar recursos:
   ```bash
   docker compose down
   ```
5. (Opcional) Eliminar volumenes persistentes con `--volumes`.

### 2.4 Evidencias visuales

- `docs/assets/screenshots/backend-swagger.png`
- `docs/assets/screenshots/frontend-dashboard.png`
- `docs/assets/screenshots/frontend-landing.png`

> Colocar las capturas en la ruta indicada y referenciarlas en el PDF.

## 3. Modelos de comportamiento y estructura

### 3.1 Casos de uso (vision general)

| Id | Actor principal | Objetivo |
| --- | --- | --- |
| CU-01 | Administrador | Registrar franquicia |
| CU-02 | Administrador | Gestionar sucursales |
| CU-03 | Administrador | Gestionar inventario (productos) |
| CU-04 | Administrador | Consultar producto con mayor stock por sucursal |
| CU-05 | Usuario autenticado | Consultar franquicias y sucursales |
| CU-06 | Administrador | Activar o desactivar sucursales |
| CU-07 | Administrador | Eliminar sucursales y franquicias |
| CU-08 | Administrador | Registrar usuarios del sistema y definir roles |
| CU-09 | Usuario autenticado | Recuperar acceso mediante token temporal mostrado en la plataforma |

```mermaid
flowchart LR
    actorAdmin([Administrador])
    actorUser([Usuario])
    uc1((CU-01
Registrar
franquicia))
    uc2((CU-02
Gestionar
sucursales))
    uc3((CU-03
Gestionar
inventario))
    uc4((CU-04
Consultar
producto top))
    uc5((CU-05
Consultar
franquicias))
    uc6((CU-06
Activar/
desactivar
sucursales))
    uc7((CU-07
Eliminar
franquicias))
    uc8((CU-08
Registrar
usuarios))
    uc9((CU-09
Recuperar
acceso))
    actorAdmin --> uc1
    actorAdmin --> uc2
    actorAdmin --> uc3
    actorAdmin --> uc4
    actorUser --> uc5
    actorAdmin --> uc5
    actorAdmin --> uc6
    actorAdmin --> uc7
    actorAdmin --> uc8
    actorUser --> uc9
    actorAdmin --> uc9
```

### 3.2 Casos de uso documentados

| Caso | Descripcion | Precondiciones | Postcondiciones |
| --- | --- | --- | --- |
| CU-01 | El administrador registra una nueva franquicia con nombre unico. | Actor autenticado como administrador. | La franquicia se almacena en MongoDB y queda disponible en la UI. |
| CU-02 | El administrador crea/edita sucursales dentro de una franquicia. | Debe existir la franquicia objetivo. | La sucursal queda persistida en el documento de la franquicia. |
| CU-03 | El administrador agrega/edita/elimina productos por sucursal. | La sucursal debe existir. | Inventario actualizado y reflejado en la UI. |
| CU-04 | El administrador consulta el producto con mayor stock por sucursal. | Deben existir productos registrados. | La API devuelve lista de productos top por sucursal. |
| CU-05 | El usuario consulta informacion en modo lectura. | Usuario autenticado (rol `user`). | Visualiza franquicias, sucursales y top productos. |
| CU-06 | El administrador activa o desactiva sucursales segun disponibilidad operativa. | La sucursal debe existir. | El campo `active` se actualiza y evita nuevas altas de productos mientras est inactiva. |
| CU-07 | El administrador elimina sucursales o franquicias completas. | Debe existir la entidad objetivo. | Se borran documentos y subdocumentos asociados en MongoDB. |
| CU-08 | El administrador registra un nuevo usuario asignando roles y correo institucional. | Actor autenticado con rol administrador. | El usuario queda persistido en `users` con contrasena encriptada y roles definidos. |
| CU-09 | Un usuario solicita restablecer su contrasena y completa el flujo con el token temporal devuelto por la API/UI. | Usuario existente en el sistema. | Se genera token con vigencia de 15 minutos y la contrasena queda actualizada. |

### 3.3 Diagrama de secuencia (Actualizacion de stock)

```mermaid
sequenceDiagram
    participant UI as Frontend Angular
    participant API as FranchiseController
    participant Service as FranchiseService
    participant Repo as FranchiseRepository
    participant DB as MongoDB (franchises)

    UI->>API: PATCH /api/v1/franchises/{id}/branches/{id}/products/{id}/stock
    API->>Service: updateProductStock(franchiseId, branchId, productId, request)
    Service->>Repo: findById(franchiseId)
    Repo-->>Service: Franchise
    Service->>Service: validar sucursal y producto
    Service->>DB: Persistir cambios (save)
    DB-->>Service: Franchise actualizado
    Service-->>API: ProductResponse
    API-->>UI: 200 OK + JSON actualizado
```

> Las sucursales marcadas como inactivas quedan excluidas de clculos de mtricas (p. ej. top productos) y no aceptan nuevas altas hasta reactivarse.

### 3.4 Diagrama de clases (nucleo backend)

```mermaid
classDiagram
    class FranchiseController {
        +createFranchise()
        +getFranchises()
        +updateFranchiseName()
        +addBranch()
        +addProduct()
        +updateProductStock()
        +deleteProduct()
    }

    class FranchiseService {
        -FranchiseRepository franchiseRepository
        +createFranchise()
        +updateFranchiseName()
        +addBranch()
        +addProduct()
        +updateProductStock()
        +deleteProduct()
        +getTopProductPerBranch()
    }

    class FranchiseRepository {
        <<interface>>
        +findById()
        +findAll()
        +findByNameIgnoreCase()
        +save()
    }

    class Franchise {
        String id
        String name
        List~Branch~ branches
    }

    class Branch {
        String id
        String name
        boolean active
        List~Product~ products
    }

    class Product {
        String id
        String name
        int stock
    }

    FranchiseController --> FranchiseService
    FranchiseService --> FranchiseRepository
    FranchiseRepository --> Franchise
    Franchise o-- Branch
    Branch o-- Product
```

### 3.5 Diagrama de componentes

```mermaid
flowchart LR
    subgraph Frontend [Angular SPA]
        UI[Landing & Dashboard]
        ServiceClient[FranchiseApiService]
    end

    subgraph Backend [Spring Boot]
        Controller[FranchiseController]
        ServiceLayer[FranchiseService]
        Mapper[FranchiseMapper]
        ExceptionHandler[GlobalExceptionHandler]
    end

    subgraph Data [Persistencia]
        Mongo[(MongoDB)]
    end

    UI --> ServiceClient --> Controller
    Controller --> ServiceLayer --> Mongo
    ServiceLayer -.-> Mapper
    Controller -.-> ExceptionHandler
```

### 3.6 Diagrama de despliegue

```mermaid
flowchart TB
    subgraph LocalHost
        DockerCompose>Docker Compose]
        subgraph ContainerAPI
            BackendInstance[franchise-api (Spring Boot)]
        end
        subgraph ContainerMongo
            MongoInstance[MongoDB 7]
        end
        subgraph ContainerUI
            AngularInstance[Angular 17]
        end
    end

    Developer[
    Cliente web
    Navegador
    ] --> AngularInstance
    AngularInstance --> BackendInstance
    BackendInstance --> MongoInstance
```

## 4. Modelo de base de datos (NoSQL)

La solucion utiliza MongoDB con una coleccion principal `franchises` donde se embeben sucursales y productos.

### 4.1 Estructura conceptual

```mermaid
flowchart TB
    franchises{{Collection: franchises}}
    branch1[(branches[])]
    product1[(products[])]

    franchises --> branch1 --> product1
```

### 4.2 Ejemplo de documento

```json
{
  "_id": "6747f2e9e4b0f72c1e",
  "name": "Coffee Express",
  "branches": [
    {
      "id": "9b8c-...",
      "name": "Sucursal Centro",
      "active": true,
      "products": [
        { "id": "p-01", "name": "Capuchino", "stock": 125 },
        { "id": "p-02", "name": "Latte", "stock": 98 }
      ]
    }
  ]
}
```

> Para analisis relacional, convertir las estructuras embebidas en colecciones virtuales: `branch` (subdocumento) y `product` (subdocumento) asociados por identificadores.

### 4.3 Coleccion `users`

La autenticacion se apoya en una coleccion separada `users` que almacena credenciales y metadatos de seguridad.

- `username` (string, unico, indexado).
- `password` (hash BCrypt).
- `fullName` (string).
- `email` (string, obligatorio para notificaciones y restablecimiento).
- `roles` (array de enums, valores `ADMIN` / `USER`).
- `active` (boolean).
- `passwordResetToken` + `passwordResetExpiration` (token temporal de 15 minutos).

```json
{
  "_id": "679c9409c49e3c1f87c",
  "username": "admin",
  "fullName": "Administrador Global",
  "email": "cfca5@hotmail.com",
  "password": "$2a$10$...",
  "roles": ["ADMIN", "USER"],
  "active": true,
  "passwordResetToken": null,
  "passwordResetExpiration": null
}
```

## 5. Prototipos de interfaz

### 5.1 Baja fidelidad (Balsamiq o Moqups)

| Pantalla | Descripcion | Link / Archivo |
| --- | --- | --- |
| Landing publica | Listado de franquicias, CTA de login por rol. | `docs/prototipos/low-fi/landing.bmpr` |
| Login (selector de rol) | Formulario de acceso para admin/user. | `docs/prototipos/low-fi/login.bmpr` |
| Panel admin | CRUD de franquicias, sucursales, inventario. | `docs/prototipos/low-fi/panel-admin.bmpr` |
| Gestion de usuarios | Alta de cuentas, asignacion de roles y activacion/desactivacion. | `docs/prototipos/low-fi/user-management.bmpr` |

### 5.2 Alta fidelidad (Figma)

| Pantalla | URL en Figma | Comentarios |
| --- | --- | --- |
| Landing publica | <https://www.figma.com/file/...> | Incluye estados de cards y CTA. |
| Dashboard admin | <https://www.figma.com/file/...> | Navegacion lateral y tablas editables. |
| Explorer usuario | <https://www.figma.com/file/...> | Vista solo lectura con filtros. |
| Administracion de usuarios | <https://www.figma.com/file/...> | Formularios con validacion y tabla de cuentas. |

### 5.3 Capturas clave (para PDF)

- Insertar capturas exportadas desde Figma (PNG 1440px de ancho).
- Mantener consistencia de branding y tipografia.

## 6. Pruebas de usabilidad

### 6.1 Datos generales de la sesion

| Campo | Valor |
| --- | --- |
| Fecha de la prueba | <dd/mm/aaaa> |
| Herramientas | Maze + Figma (actualizar segun uso real) |
| Objetivos | Validar el flujo de gestion de franquicias e inventario. |
| Numero de participantes | 5 (minimo) |
| Perfil | Admins operativos y usuarios finales. |

### 6.2 Tareas evaluadas

| Tarea | Descripcion | Exito (Si/No) | Tiempo promedio |
| --- | --- | --- | --- |
| T1 | Crear una franquicia desde cero. | | |
| T2 | Anadir una sucursal a la franquicia creada. | | |
| T3 | Registrar un producto y actualizar su stock. | | |
| T4 | Consultar producto con mayor stock por sucursal. | | |
| T5 | Navegar en modo lectura como usuario estandar. | | |
| T6 | Crear un usuario con rol especifico desde el panel de administracion. | | |
| T7 | Solicitar restablecimiento de contrasena, copiar el token mostrado y completar el flujo. | | |

### 6.3 Metricas consolidadas

- Tiempo promedio global: `<mm:ss>`
- Tasa de exito: `<%>`
- Numero de errores criticos: `<n>`
- Observaciones generales: `<resumen>`

### 6.4 Hallazgos y recomendaciones

| Prioridad | Hallazgo | Recomendacion |
| --- | --- | --- |
| Alta | | |
| Media | | |
| Baja | | |

### 6.5 Evidencias

- Informe Maze: `<URL>`
- Videos de sesiones: `docs/assets/usability/sesiones/`
- Elevator pitch (<=10 min): `docs/assets/usability/elevator-pitch.mp4`

## 7. Patrones y arquitectura de software

| Patron / Practica | Implementacion | Referencia |
| --- | --- | --- |
| Arquitectura en capas | Separacion Controller -> Service -> Repository. | `src/main/java/com/franchise/api/controller/FranchiseController.java` |
| Patron Repositorio | Interfaz `FranchiseRepository` delega persistencia en MongoDB. | `src/main/java/com/franchise/api/repository/FranchiseRepository.java` |
| DTO + Mapper | Conversion entre entidades y respuestas REST para aislar el dominio. | `src/main/java/com/franchise/api/mapper/FranchiseMapper.java` |
| Validacion y manejo centralizado de errores | `@RestControllerAdvice` captura excepciones y produce `ApiError`. | `src/main/java/com/franchise/api/exception/GlobalExceptionHandler.java` |
| Inyeccion de dependencias (IoC) | Servicios y controladores anotados con `@Service`, `@RestController`, `@RequiredArgsConstructor`. | `src/main/java/com/franchise/api/service/FranchiseService.java` |
| Configuracion transversal | CORS y OpenAPI definidos en beans reutilizables. | `src/main/java/com/franchise/api/config/CorsConfig.java` |
| Testing unitario del dominio | Validacion de reglas en `FranchiseServiceTest`. | `src/test/java/com/franchise/api/service/FranchiseServiceTest.java` |
| Gobernanza de sucursales | `updateBranchStatus`, `deleteBranch` y `deleteFranchise` encapsulan reglas para activar/desactivar y depurar datos. | `src/main/java/com/franchise/api/service/FranchiseService.java` |
| Seguridad con JWT | Spring Security + filtros JWT para proteger rutas segun rol. | `src/main/java/com/franchise/api/security` |
| Gestion de usuarios | Servicio dedicado `UserManagementService` para crear y activar/desactivar cuentas con roles. | `src/main/java/com/franchise/api/security/UserManagementService.java` |

> Documentar cualquier patron adicional (p. ej. guards y servicios en Angular) con enlaces a los archivos pertinentes del frontend.

**Resumen arquitectonico**  
- Backend monolitico siguiendo **arquitectura por capas**: controladores REST (exponen API), servicios (procesan reglas de negocio, forzado de cambio de contrasenia, flujos JWT), repositorios Spring Data MongoDB y capa de dominio/DTOs con mapeadores. Seguridad y excepciones se manejan via componentes transversales (JWT filter, `GlobalExceptionHandler`).  
- Frontend Angular estructurado en **SPA modular** (core/shared vs features: `auth`, `franchises`, `admin/users`) con servicios HttpClient, guards y formularios reactivos → corresponde a un enfoque component-based/MVVM que desacopla UI y logica.  
- Comunicación exclusiva via REST JSON, sin microservicios ni CQRS; la modularidad permite extender hacia microfrontends/microservicios en el futuro si se separan módulos clave.

## 8. Seguridad y roles

- Login REST: `POST /api/v1/auth/login` retorna token JWT, roles y expiracin (`AuthController`).
- Recuperacion: `POST /api/v1/auth/forgot-password` genera token de 15 minutos y lo devuelve en la respuesta para que el usuario lo use en `POST /api/v1/auth/reset-password`. Para verificar vigencia existe `POST /api/v1/auth/validate-reset-token`.
- Roles:
  - `ADMIN`: CRUD completo de franquicias, sucursales y productos; activacin/desactivacin global.
  - `USER`: acceso de solo lectura a catlogo y mtricas.
- Proteccin granular (`SecurityConfig`):
  - `GET /api/v1/franchises/**` -> `ADMIN` o `USER`.
  - Mutaciones (`POST`, `PATCH`, `DELETE`) bajo `/api/**` -> solo `ADMIN`.
  - `GET/POST/PATCH /api/v1/users/**` -> solo `ADMIN` (gestion de cuentas).
  - `POST /api/v1/auth/change-password` -> disponible para cualquier usuario autenticado (cambio obligatorio de contrasena temporal).
- Frontend:
  - Interceptor agrega `Authorization: Bearer` automticamente (`auth.interceptor.ts`).
  - Guards redirigen a `/login` si la sesin expira (`auth.guard.ts`).
  - La UI oculta acciones administrativas a usuarios de solo lectura.
  - Los administradores cuentan con el panel `/users` (`UserManagementComponent`) para crear cuentas, asignar roles, activar/desactivar y eliminar usuarios.
- Credenciales iniciales (semilla automtica):
  - `admin / Admin123!` (roles `ADMIN`, `USER`)
  - `analyst / Analyst123!` (rol `USER`)
- Variables clave:
  - `JWT_SECRET` define el secreto de firma para los tokens (por defecto `change-me-in-production`).
  - `JWT_EXPIRATION_MS` controla la vigencia del token (1h por defecto).

## 9. Proximos pasos sugeridos

1. Completar los espacios pendientes (links reales, metricas, capturas) antes de la exportacion.
2. Generar el PDF con `pandoc docs/entrega2.md -o docs/entrega2.pdf` o la herramienta preferida.
3. Subir evidencias (capturas, videos, archivos Balsamiq) al repositorio o a un drive compartido.
4. Validar coherencia de version entre backend y frontend y actualizar la tabla inicial.
5. Compartir el enlace al repositorio, Figma y Maze en la plataforma de entrega oficial.

## 10. Estrategia de pruebas automatizadas

| Tipo | Herramienta / Alcance | Comando |
| --- | --- | --- |
| Unitarias backend | JUnit 5 + Mockito (`FranchiseService`, `PasswordResetService`, `UserManagementService`) | `mvn test` |
| Integración backend | `@SpringBootTest` + MockMvc + Mongo embebido (`AuthControllerIT`, `FranchiseControllerIT`) | `mvn test` (incluye ambas capas) |
| Unitarias frontend | Karma + Jasmine sobre componentes/servicios Angular | `npm run test` |
| End-to-End frontend | Cypress (`cypress/e2e/auth-flow.cy.ts`: login, recuperación, cambio forzado de contraseña) con intercepts y verificación de UI responsive | `npm run e2e` (o `npm run cy:open` para modo interactivo) |

> Recomendación: ejecutar `mvn test && npm run test && npm run e2e` antes de abrir un Pull Request. Los pipelines de CI deben revisar al menos las suites backend y frontend unitarias; los E2E pueden ejecutarse nightly o antes de releases.


