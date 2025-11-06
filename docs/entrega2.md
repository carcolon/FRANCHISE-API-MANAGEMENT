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

### 2.2 Instalacion local (Java + MongoDB)

1. Instalar JDK 17 y Maven 3.9 (verificar con `mvn -v`).
2. Levantar MongoDB local o remoto (Docker Compose usa `mongodb://root:example@mongo:27017/franchise_db?authSource=admin`; si trabajas sin contenedores, conecta desde MongoDB Compass a `mongodb://localhost:27017` o a tu clúster Atlas).
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
   - `PATCH /api/v1/franchises/{id}/branches/{branchId}/status` activa/desactiva una sucursal. Mientras esté inactiva no admite nuevas altas de productos.
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
    actorAdmin --> uc1
    actorAdmin --> uc2
    actorAdmin --> uc3
    actorAdmin --> uc4
    actorUser --> uc5
    actorAdmin --> uc5
```

### 3.2 Casos de uso documentados

| Caso | Descripcion | Precondiciones | Postcondiciones |
| --- | --- | --- | --- |
| CU-01 | El administrador registra una nueva franquicia con nombre unico. | Actor autenticado como administrador. | La franquicia se almacena en MongoDB y queda disponible en la UI. |
| CU-02 | El administrador crea/edita sucursales dentro de una franquicia. | Debe existir la franquicia objetivo. | La sucursal queda persistida en el documento de la franquicia. |
| CU-03 | El administrador agrega/edita/elimina productos por sucursal. | La sucursal debe existir. | Inventario actualizado y reflejado en la UI. |
| CU-04 | El administrador consulta el producto con mayor stock por sucursal. | Deben existir productos registrados. | La API devuelve lista de productos top por sucursal. |
| CU-05 | El usuario consulta informacion en modo lectura. | Usuario autenticado (rol `user`). | Visualiza franquicias, sucursales y top productos. |
| CU-06 | El administrador activa o desactiva sucursales segun disponibilidad operativa. | La sucursal debe existir. | El campo `active` se actualiza y evita nuevas altas de productos mientras esté inactiva. |
| CU-07 | El administrador elimina sucursales o franquicias completas. | Debe existir la entidad objetivo. | Se borran documentos y subdocumentos asociados en MongoDB. |

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

> Las sucursales marcadas como inactivas quedan excluidas de cálculos de métricas (p. ej. top productos) y no aceptan nuevas altas hasta reactivarse.

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

## 5. Prototipos de interfaz

### 5.1 Baja fidelidad (Balsamiq o Moqups)

| Pantalla | Descripcion | Link / Archivo |
| --- | --- | --- |
| Landing publica | Listado de franquicias, CTA de login por rol. | `docs/prototipos/low-fi/landing.bmpr` |
| Login (selector de rol) | Formulario de acceso para admin/user. | `docs/prototipos/low-fi/login.bmpr` |
| Panel admin | CRUD de franquicias, sucursales, inventario. | `docs/prototipos/low-fi/panel-admin.bmpr` |

### 5.2 Alta fidelidad (Figma)

| Pantalla | URL en Figma | Comentarios |
| --- | --- | --- |
| Landing publica | <https://www.figma.com/file/...> | Incluye estados de cards y CTA. |
| Dashboard admin | <https://www.figma.com/file/...> | Navegacion lateral y tablas editables. |
| Explorer usuario | <https://www.figma.com/file/...> | Vista solo lectura con filtros. |

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
| Arquitectura en capas | Separacion Controller  Service  Repository. | `src/main/java/com/franchise/api/controller/FranchiseController.java` |
| Patron Repositorio | Interfaz `FranchiseRepository` delega persistencia en MongoDB. | `src/main/java/com/franchise/api/repository/FranchiseRepository.java` |
| DTO + Mapper | Conversion entre entidades y respuestas REST para aislar el dominio. | `src/main/java/com/franchise/api/mapper/FranchiseMapper.java` |
| Validacion y manejo centralizado de errores | `@RestControllerAdvice` captura excepciones y produce `ApiError`. | `src/main/java/com/franchise/api/exception/GlobalExceptionHandler.java` |
| Inyeccion de dependencias (IoC) | Servicios y controladores anotados con `@Service`, `@RestController`, `@RequiredArgsConstructor`. | `src/main/java/com/franchise/api/service/FranchiseService.java` |
| Configuracion transversal | CORS y OpenAPI definidos en beans reutilizables. | `src/main/java/com/franchise/api/config/CorsConfig.java` |
| Testing unitario del dominio | Validacion de reglas en `FranchiseServiceTest`. | `src/test/java/com/franchise/api/service/FranchiseServiceTest.java` |
| Gobernanza de sucursales | `updateBranchStatus`, `deleteBranch` y `deleteFranchise` encapsulan reglas para activar/desactivar y depurar datos. | `src/main/java/com/franchise/api/service/FranchiseService.java` |

> Documentar cualquier patron adicional (p. ej. guards y servicios en Angular) con enlaces a los archivos pertinentes del frontend.

## 8. Proximos pasos sugeridos

1. Completar los espacios pendientes (links reales, metricas, capturas) antes de la exportacion.
2. Generar el PDF con `pandoc docs/entrega2.md -o docs/entrega2.pdf` o la herramienta preferida.
3. Subir evidencias (capturas, videos, archivos Balsamiq) al repositorio o a un drive compartido.
4. Validar coherencia de version entre backend y frontend y actualizar la tabla inicial.
5. Compartir el enlace al repositorio, Figma y Maze en la plataforma de entrega oficial.

