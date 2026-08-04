# Documentación general del proyecto

**Proyecto:** Mantec Inspector (Mantec_ins)
**Versión actual:** 1.7.6 (versionCode 8)
**Lenguaje:** Kotlin 100%
**Plataforma:** Android nativo
**Fecha de análisis inicial:** Mayo 2026
**Última actualización:** 2026-08-04 (ver v1.7.6 en la sección 16 para el detalle del cambio)

---

## 1. Resumen del sistema

**Mantec Inspector** es una aplicación Android nativa para inspectores de campo que realizan mantenimiento e inspección de equipos industriales (principalmente correas transportadoras y componentes relacionados). La app permite:

- **Registrar reportes de inspección** por componente, diagnóstico y condición, con soporte para adjuntar fotos y videos como evidencia.
- **Trabajar completamente offline**: el catálogo completo (áreas, elementos, componentes, diagnósticos, condiciones) se descarga al iniciar sesión y se almacena localmente en Room.
- **Sincronizar datos cuando hay conexión**: los reportes generados en campo se sincronizan automáticamente (o manualmente) al servidor cuando hay WiFi o datos móviles.
- **Registrar mediciones de espesor** de cubiertas de correas, con valores por posición (izquierda, centro, derecha) en capas superiores e inferiores y datos de dureza.
- **Visualizar estado semanal** de inspecciones: qué diagnósticos están pendientes, cuáles se completaron y cuántos reportes están sin sincronizar.

**Problema que resuelve:** Permite a inspectores operar sin conexión a internet en campo, almacenar toda la información de inspecciones localmente y luego sincronizarla al servidor central cuando hay conectividad disponible, garantizando que ningún dato de inspección se pierda.

---

## 2. Tecnologías utilizadas

| Tecnología | Versión | Propósito |
|---|---|---|
| **Kotlin** | 100% | Lenguaje principal |
| **Jetpack Compose** | BOM reciente | UI declarativa reactiva |
| **Material 3** | Compose | Sistema de diseño |
| **Room** | KSP | Base de datos local SQLite |
| **Retrofit** | 2.11.0 | Cliente HTTP para API REST |
| **OkHttp** | 4.12.0 | Cliente HTTP base, interceptores |
| **Gson** | Via Retrofit | Serialización/deserialización JSON |
| **WorkManager** | 2.9.0 | Sincronización en background |
| **Kotlin Coroutines** | kotlinx | Operaciones asincrónicas |
| **StateFlow / LiveData** | Jetpack | Estado reactivo en ViewModels |
| **KSP** | Plugin | Procesador de anotaciones Room |
| **SharedPreferences** | Android SDK | Persistencia de sesión/token |
| **MediaStore** | Android SDK | Acceso a cámara y almacenamiento |
| **Compose Navigation** | - | Navegación entre pantallas |

---

## 3. Estructura general de carpetas

```
Mantec_ins/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/mantec_ins/
│   │       │   ├── data/
│   │       │   │   ├── local/                  # Room: DB, Entities, DAOs
│   │       │   │   │   ├── dao/                # Interfaces de acceso a datos
│   │       │   │   │   ├── entity/             # Tablas Room
│   │       │   │   │   ├── AppDatabase.kt      # Clase principal Room
│   │       │   │   │   ├── DatabaseProvider.kt # Singleton de la DB
│   │       │   │   │   └── SessionManager.kt   # Persistencia de sesión
│   │       │   │   ├── remote/                 # Retrofit: Servicios, DTOs
│   │       │   │   │   ├── dto/                # Data Transfer Objects
│   │       │   │   │   ├── service/            # Interfaces Retrofit
│   │       │   │   │   └── RetrofitClient.kt   # Configuración OkHttp/Retrofit
│   │       │   │   └── repository/             # Repositorios de acceso a datos
│   │       │   ├── domain/
│   │       │   │   └── model/                  # Modelos de dominio
│   │       │   ├── presentation/
│   │       │   │   ├── navigation/             # Definición de pantallas (sealed class)
│   │       │   │   ├── ui/                     # Pantallas Compose
│   │       │   │   └── viewmodel/              # ViewModels + UI States
│   │       │   ├── sync/                       # WorkManager: Worker + Manager
│   │       │   ├── util/                       # Utilidades (NetworkUtils, etc.        )
│   │       │   ├── ui/theme/                   # Colores, tipografía, tema Compose
│   │       │   └── MainActivity.kt             # Actividad única
│   │       ├── res/
│   │       │   ├── drawable/                   # Recursos gráficos
│   │       │   ├── layout/                     # Layouts XML (si aplica)
│   │       │   ├── values/                     # Strings, colores, estilos
│   │       │   └── xml/                        # network_security_config.xml
│   │       └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts (root)
├── settings.gradle.kts
└── gradle.properties
```

### Propósito de cada carpeta

| Carpeta | Propósito |
|---|---|
| `data/local/` | Todo lo relacionado con Room: entidades, DAOs, base de datos y gestión de sesión. |
| `data/remote/` | Retrofit: DTOs de red, interfaces de servicios, cliente HTTP con interceptores. |
| `data/repository/` | Capa de abstracción: los repositorios coordinan acceso local y remoto. |
| `domain/model/` | Modelos de dominio puros (sin anotaciones de Room ni Retrofit). |
| `presentation/ui/` | Pantallas y secciones Compose (composables de nivel pantalla y sub-secciones reutilizables). |
| `presentation/viewmodel/` | ViewModels, UI States, factories de inyección manual. |
| `presentation/navigation/` | Definición de rutas de navegación (AppScreen sealed class). |
| `sync/` | SyncWorker y SyncWorkManager para sincronización en background. |
| `util/` | Utilidades compartidas: NetworkUtils para detectar tipo de conexión. |
| `ui/theme/` | Configuración de tema Material3 de Compose (colores, tipografía). |

---

## 4. Arquitectura actual

El proyecto aplica **MVVM + Clean Architecture** con capas claramente separadas:

```
Presentation Layer
    └── Compose Screens ← ViewModels (StateFlow<UiState>)

Domain Layer
    └── Models de dominio (UserSession, modelos UI)

Data Layer
    ├── Repositories (coordinan local + remoto)
    ├── Local: Room (DAOs + Entities)
    └── Remote: Retrofit (Services + DTOs)
```

### Patrones y prácticas aplicados

- **MVVM**: Cada pantalla tiene su ViewModel con un `UiState` expuesto como `StateFlow`.
- **Repository Pattern**: Los repositorios abstraen el origen del dato (local o remoto).
- **Single Activity**: `MainActivity` es la única actividad; toda la navegación es con Compose.
- **Offline-First**: El catálogo se almacena localmente; los reportes se guardan en Room antes de intentar sincronizar.
- **Singleton DB**: `DatabaseProvider` mantiene una única instancia de Room.
- **Mutex en sync**: `RemoteCatalogRepository` usa `Mutex` para evitar sincronizaciones concurrentes.
- **Inyección manual de dependencias**: No se usa Hilt ni Dagger. Las dependencias se crean en `MainActivity` y se inyectan via `ViewModelFactory`.

### Debilidades arquitectónicas detectadas

- La inyección de dependencias es completamente manual desde `MainActivity` (~1481 líneas), lo que hace que esta clase sea muy grande y difícil de mantener.
- `fallbackToDestructiveMigration()` en Room borra todos los datos ante cualquier cambio de esquema; apropiado solo para desarrollo, **no para producción**.
- Los `ViewModelFactory` son clases boilerplate repetitivas que podrían eliminarse con Hilt.

---

## 5. Flujo general de la aplicación

```
App inicia
    └── MainActivity.onCreate()
           ├── Inicializa WorkManager (sync en background)
           ├── Crea repositorios y ViewModels
           └── Restaura sesión guardada (SharedPreferences)
                    ├── Sin sesión → LoginScreen
                    ├── Sesión + rol no inspector → UnsupportedRoleScreen
                    └── Sesión + rol inspector → HomeScreen
                                                    ├── Reportes → MainScreenHost
                                                    │               └── Flujo de inspección
                                                    └── Mediciones → MeasurementThicknessScreen
```

### Navegación entre pantallas

La navegación se controla via `AppNavigationViewModel` que mantiene la pantalla actual como estado. Las transiciones son:

- **LoginScreen → HomeScreen**: Login exitoso con rol inspector.
- **LoginScreen → UnsupportedRoleScreen**: Login exitoso pero rol distinto a inspector.
- **HomeScreen → MainScreenHost**: Botón "Reportes".
- **HomeScreen → MeasurementThicknessScreen**: Botón "Mediciones".
- **Cualquier pantalla → LoginScreen**: Logout.

---

## 6. Módulos, paquetes y responsabilidades

### `data/local/entity/`
Contiene todas las clases `@Entity` de Room. Mapea exactamente a las tablas de la base de datos local.

| Entidad | Tabla | Propósito |
|---|---|---|
| `ClientEntity` | clients | Clientes del sistema |
| `GroupEntity` | groups | Grupos de inspección asignados al inspector |
| `AreaEntity` | areas | Áreas físicas (plantas, zonas) |
| `ElementEntity` | elements | Elementos inspeccionables (correas, etc.) |
| `ElementTypeEntity` | element_types | Tipos de elementos (clasificación) |
| `ComponentEntity` | components | Componentes de un elemento (cubierta, núcleo, etc.) |
| `DiagnosticEntity` | diagnostics | Diagnósticos posibles por componente |
| `ConditionEntity` | conditions | Condiciones (severidad, color) |
| `ElementComponentCrossRef` | element_component_cross_ref | Relación M:N elementos-componentes |
| `ComponentDiagnosticCrossRef` | component_diagnostic_cross_ref | Relación M:N componentes-diagnósticos |
| `ComponentConditionCrossRef` | component_condition_cross_ref | Relación M:N componentes-condiciones |
| `ReportEntity` | reports | Cabecera de reporte de inspección |
| `ReportDetailEntity` | report_details | Detalle de un reporte (componente+diagnóstico+condición) |
| `EvidenceEntity` | evidences | Fotos/videos adjuntos a un detalle de reporte |
| `MeasurementThicknessDraftEntity` | measurement_thickness_drafts | Borrador de medición de espesor |
| `MeasurementThicknessDraftLineEntity` | measurement_thickness_draft_lines | Línea de medición (cubierta) |
| `MeasurementElementTypeAccessEntity` | measurement_element_type_access | Control de acceso por tipo de elemento al módulo de mediciones |
| `WeeklyDiagnosticStatusCacheEntity` | weekly_diagnostic_status_cache | Caché de estado semanal de diagnósticos |
| `PendingDiagnosticCacheEntity` | pending_diagnostic_cache | Caché de diagnósticos pendientes por semana |
| `WeeklyElementStatusCacheEntity` | weekly_element_status_cache | Caché de estado semanal por elemento |

### `data/local/dao/`
Interfaces `@Dao` de Room. Una por entidad principal.

### `data/remote/dto/`
Data Transfer Objects: clases Kotlin planas usadas para serializar/deserializar JSON de la API. No contienen lógica de negocio.

### `data/remote/service/`
Interfaces Retrofit anotadas con `@GET`, `@POST`, `@Multipart`, etc.

| Servicio | Responsabilidad |
|---|---|
| `AuthApiService` | Login, descarga de catálogo offline, queries remotas de catálogo |
| `SyncApiService` | Sincronización de reportes y carga de archivos (multipart) |
| `MeasurementApiService` | CRUD de mediciones de espesor |

### `data/repository/`

| Repositorio | Responsabilidad |
|---|---|
| `AuthRepository` | Login, logout, persistencia de sesión |
| `CatalogLocalRepository` | Lectura del catálogo desde Room |
| `RemoteCatalogRepository` | Descarga y refresco del catálogo desde la API |
| `InspectionLocalRepository` | Guardar y leer reportes e inspecciones en Room |
| `SyncRepository` | Sincronizar reportes pendientes al servidor |
| `MeasurementThicknessRepository` | Gestión de borradores de mediciones (local + remoto) |
| `PendingDiagnosticsRepository` | Caché de diagnósticos pendientes semanales |
| `WeeklyElementStatusRepository` | Caché de estado semanal de elementos |

### `presentation/viewmodel/`

| ViewModel | Pantalla asociada | Responsabilidad |
|---|---|---|
| `LoginViewModel` | LoginScreen | Autenticación, manejo de errores de red |
| `CatalogViewModel` | MainScreenHost | Carga cascada de catálogo (área→elemento→componente→...) |
| `InspectionViewModel` | MainScreenHost / ReportFormScreen | Estado del formulario de inspección, guardar reporte individual. Cambiar componente o diagnóstico conserva la recomendación escrita; solo cambiar el elemento la limpia. |
| `InspectionBatchViewModel` | MainScreenHost | Evaluación en lote: evalúa múltiples diagnósticos a la vez para un componente seleccionado |
| `RemoteCatalogViewModel` | MainScreenHost | Estrategia local-first: carga catálogo local primero y luego refresca desde remoto en segundo plano |
| `DashboardViewModel` | HomeScreen | Pendientes, estado semanal, reportes recientes |
| `ReportListViewModel` | HomeScreen | Lista de reportes pendientes de sync |
| `ReportDetailViewModel` | HomeScreen | Carga el detalle completo de un reporte guardado (componentes, diagnósticos, condiciones, evidencias) |
| `SyncViewModel` | HomeScreen | Ejecuta `syncPendingReports()` y expone el conteo de reportes sincronizados |
| `MeasurementPendingViewModel` | HomeScreen | Conteo de borradores de medición pendientes; verifica acceso al módulo de mediciones |
| `MeasurementThicknessViewModel` | MeasurementThicknessScreen | Selección, edición y guardado de mediciones |
| `AppNavigationViewModel` | MainActivity | Control central de navegación |
| `InspectorProfileViewModel` | HomeScreen | Perfil del inspector logueado |

---

## 7. Base de datos local

**Nombre:** `mantec_inspector_db`
**Versión:** 19
**Motor:** Room (SQLite)
**Estrategia de migración:** `fallbackToDestructiveMigration()` (destructiva — solo para desarrollo)

### Relaciones principales

```
Client (1)──(N) Group
Client (1)──(N) Area
Area (1)──(N) Element
Element (N)──(N) Component  [via ElementComponentCrossRef]
Component (N)──(N) Diagnostic [via ComponentDiagnosticCrossRef]
Component (N)──(N) Condition  [via ComponentConditionCrossRef]

Report (1)──(N) ReportDetail
ReportDetail (1)──(N) Evidence

Element (1)──(1) MeasurementThicknessDraft
MeasurementThicknessDraft (1)──(N) MeasurementThicknessDraftLine
```

### Estados de sincronización

Los reportes y mediciones usan un campo `syncStatus` con dos valores:
- `PENDING_SYNC`: Guardado localmente, aún no enviado al servidor.
- `SYNCED`: Enviado y confirmado por el servidor.

Las evidencias (`EvidenceEntity`) tienen adicionalmente `serverFileId` (ID remoto una vez subida la foto).

### Cachés de estado semanal

Se usan tres tablas de caché para evitar consultas remotas repetidas:
- `WeeklyDiagnosticStatusCacheEntity`: Estado de cada diagnóstico en la semana actual.
- `PendingDiagnosticCacheEntity`: Diagnósticos que aún no se han completado.
- `WeeklyElementStatusCacheEntity`: Conteo de inspecciones esperadas vs realizadas por elemento.

---

## 8. Comunicación con backend/API

**Base URL:** `https://mantecsas.com/`
**Protocolo:** HTTPS (con cleartext habilitado en `network_security_config.xml`, aparentemente para desarrollo/testing)

### Endpoints detectados

#### Autenticación y catálogo
| Método | Endpoint | Descripción | Servicio |
|---|---|---|---|
| POST | `/api/login` | Autenticación de usuario | `AuthApiService` |
| GET | `/api/inspector/offline-catalog` | Descarga catálogo completo offline | `AuthApiService` |
| GET | `/api/inspector/elements/{elementId}/pending-diagnostics` | Diagnósticos pendientes de un elemento | `AuthApiService` |
| GET | `/api/inspector/clients/{clientId}/areas` | Áreas de un cliente | `AuthApiService` |
| GET | `/api/inspector/elements/{elementId}/conditions` | Condiciones de un elemento | `AuthApiService` |
| GET | `/api/inspector/areas/{areaId}/elements` | Elementos de un área | `AuthApiService` |
| GET | `/api/inspector/elements/{elementId}/components` | Componentes de un elemento | `AuthApiService` |
| GET | `/api/inspector/elements/{elementId}/weekly-diagnostic-status` | Estado semanal de diagnósticos | `AuthApiService` |
| GET | `/api/inspector/components/{componentId}/diagnostics` | Diagnósticos de un componente | `AuthApiService` |
| GET | `/api/inspector/areas/{areaId}/weekly-elements-status` | Estado semanal de elementos del área | `AuthApiService` |
| GET | `/api/catalog/version` | Versión actual del catálogo | `ApiService` |
| GET | `/inspector/elements/{elementId}/pending-diagnostics` | Diagnósticos pendientes (versión alternativa sin prefijo `/api/`) | `ApiService` |

#### Sincronización de reportes
| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/inspector/reports/sync` | Sincroniza un reporte de inspección |
| POST | `/api/inspector/report-details/{id}/files` | Sube archivos de evidencia (multipart) |

#### Mediciones de espesor
| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/inspector/measurements/element-types` | Tipos de elemento habilitados para medición |
| GET | `/api/inspector/measurements/element-types/{id}/areas` | Áreas del tipo de elemento |
| GET | `/api/inspector/measurements/areas/{areaId}/element-types/{typeId}/elements` | Elementos del área y tipo |
| GET | `/api/inspector/measurements/elements/{elementId}/thickness` | Estado de medición del elemento |
| POST | `/api/inspector/measurements/elements/{elementId}/thickness/draft/sync` | Sincroniza borrador de medición |

### Autenticación HTTP

- Se usa un `AuthInterceptor` (OkHttp) que inyecta automáticamente en cada request:
  - Header `Authorization: Bearer {token}`
  - Header `Accept: application/json`
- El token se obtiene del `SessionManager` (SharedPreferences).

### DTOs principales

**Request de login:**
```kotlin
LoginRequest(username: String, password: String)
```

**Response de login:**
```kotlin
LoginResponse(success, message, token, user: LoginUserDto)
LoginUserDto(id, name, username, email, role: LoginRoleDto, clients, allowed_element_types)
```

**Catálogo offline:**
```kotlin
OfflineCatalogResponse(
    success, message, client, group, elementTypes, areas,
    conditions, elements, components, diagnostics, relations,
    measurementElementTypes
)
```

**Sincronización de reporte:**
```kotlin
SyncReportRequest(
    local_report_id, client_id, area_id, element_id,
    component_id, diagnostic_id, condition_id, recommendation,
    week, year, execution_date, is_belt_change
)
SyncReportResponse(success, message, server_report_detail_id)
```

---

## 9. Sincronización y trabajo offline

### Estrategia Offline-First

1. Al hacer login, se descarga el **catálogo completo** (`/api/inspector/offline-catalog`) y se persiste en Room. A partir de ese momento, el catálogo es accesible sin conexión.
2. Los **reportes de inspección** se guardan en Room con estado `PENDING_SYNC` antes de intentar enviarlos al servidor.
3. Las **mediciones de espesor** también se guardan como borrador local antes de sincronizar.

### Políticas de sincronización

La sincronización está controlada por:
- `group.autoSync`: Flag del grupo del inspector (descargado en catálogo).
- Tipo de conexión: WiFi o datos móviles.
- `NetworkUtils.canSyncByPolicy()`: Combina los dos factores anteriores.

### Mecanismos de sincronización

| Mecanismo | Cuándo se activa |
|---|---|
| **SyncWorker** (WorkManager) | En background, periódicamente |
| **Auto-sync en HomeScreen** | Al entrar a HomeScreen si hay conexión |
| **Auto-sync post-reporte** | Inmediatamente después de guardar un reporte |
| **Auto-sync en mediciones** | Al entrar a MeasurementThicknessScreen |
| **Sync manual** | Botón "Sincronizar" en HomeScreen |

### Flujo de sincronización de reportes

```
SyncRepository.syncPendingReports()
    1. Obtiene reportes con syncStatus = PENDING_SYNC
    2. Para cada reporte:
       a. Envía SyncReportRequest → POST /api/inspector/reports/sync
       b. Recibe server_report_detail_id
       c. Sube evidencias (fotos/videos) → POST /api/inspector/report-details/{id}/files (multipart)
       d. Actualiza syncStatus = SYNCED en Room
```

### Elementos aún incompletos / a verificar

- No se evidencia manejo de conflictos si el mismo elemento se sincroniza desde dos dispositivos distintos.
- ~~No hay retry automático granular por evidencia fallida~~ **→ RESUELTO PARCIALMENTE (v1.7.6)**: cada evidencia se reintenta de forma independiente (ya existía), y desde v1.7.6 una evidencia rechazada permanentemente por el backend (422/413) se marca `ERROR` y deja de reintentarse en cada sync. Sigue pendiente: si una evidencia falla de forma *transitoria* de manera repetida (red inestable), el reporte queda indefinidamente en `PENDING_SYNC` sin límite de reintentos ni backoff.
- La estrategia de caché de diagnósticos pendientes (`PendingDiagnosticCacheEntity`) indica que el servidor es la fuente de verdad para el estado semanal, pero no hay lógica clara de invalidación de caché.

---

## 10. Pantallas y funcionalidades

### LoginScreen
- **Componentes:** Campo usuario, campo contraseña, botón "Ingresar".
- **Funcionalidad:** Llama a `LoginViewModel.login()`, maneja estados de carga y errores.
- **Errores manejados:** Timeout, sin conexión, credenciales incorrectas (401), errores 403/404/422/500/502/503/504, JSON inválido.
- **Post-login:** Si rol es inspector → HomeScreen. Si es otro rol → UnsupportedRoleScreen.

### HomeScreen (Dashboard)
- **Componentes:**
  - Nombre del usuario y grupo asignado.
  - Indicador de tipo de conexión (WiFi / Datos / Sin conexión).
  - Conteo de reportes `PENDING_SYNC`.
  - Conteo de borradores de mediciones pendientes.
  - Lista de reportes recientes (últimas 24 horas).
  - Botón de sincronización manual.
  - Botones "Reportes" y "Mediciones".
- **Mensajes:** Muestra mensajes de éxito o advertencia post-sincronización.

### MainScreenHost (Módulo de Reportes / Inspección)
- **Flujo cascada de selección:**
  1. Seleccionar área geográfica.
  2. Seleccionar tipo de elemento (se omite si el grupo solo tiene uno).
  3. Ver lista de elementos del área y tipo.
  4. Seleccionar elemento.
  5. Seleccionar componente del elemento.
  6. Seleccionar diagnóstico del componente.
  7. Seleccionar condición (con código de severidad y color).
- **Composable del formulario:** el formulario completo de creación de reporte está extraído en `ReportFormScreen.kt` (ver sección siguiente).
- **Guardar:** Crea `ReportEntity` + `ReportDetailEntity` + `EvidenceEntity` en Room con `PENDING_SYNC`. Al guardar se limpian diagnóstico, condición, recomendación y evidencias, pero se conservan el elemento y componente seleccionados para agilizar reportes consecutivos del mismo activo.

### ReportFormScreen (`ReportFormScreen.kt`)
Composable que contiene toda la UI del formulario de nuevo reporte. Fue extraído de `MainScreenHost` para mantener la pantalla principal más manejable.
- **Campos progresivos:** los dropdowns se habilitan en cascada (Área → Tipo de activo → Activo → Componente → Diagnóstico → Condición). Cada picker se muestra como un campo tipo botón que abre un `AlertDialog` con la lista de opciones.
- **Indicadores de progreso visual en las listas:**
  - Ícono verde `CheckCircle`: el ítem ya fue completado y confirmado por el servidor.
  - Badge "P" naranja: el ítem tiene un reporte guardado localmente (`PENDING_SYNC`) que aún no fue sincronizado.
- **Campo "Cambio de banda":** aparece solo cuando el componente es "Banda" y el diagnóstico es "Estado". Son radio buttons Sí/No.
- **Sección de evidencias:** muestra thumbnail de imagen/video. Botones disponibles: "Tomar foto" y "Grabar video" (abren la cámara), y "Galería" (abre el selector nativo del SO con selección múltiple de imágenes y videos). Las evidencias se limpian al cambiar componente o diagnóstico, pero la recomendación no.
- **Botón "Guardar reporte"** solo visible cuando se ha seleccionado al menos un diagnóstico (o hay uno solo disponible).
- **Header:** muestra la semana ISO actual (S{semana} / año) usando `GregorianCalendar` con `firstDayOfWeek = MONDAY` y `minimalDaysInFirstWeek = 4`.

### Secciones de HomeScreen
La pantalla `HomeScreen` tiene componentes Compose extraídos en archivos propios:
- **`PendingDiagnosticsSection.kt`:** muestra los diagnósticos pendientes de la semana actual.
- **`RecentReports24hSection.kt`:** muestra los reportes creados en las últimas 24 horas.

### MeasurementThicknessScreen (Mediciones de Espesor)
- **Flujo de selección:**
  1. Seleccionar tipo de elemento (solo los habilitados para medición).
  2. Seleccionar área.
  3. Seleccionar elemento específico.
- **Tabla de mediciones:**
  - Filas numeradas por cubierta (cover).
  - Columnas: Top Left, Top Center, Top Right, Bottom Left, Bottom Center, Bottom Right.
  - Columnas de dureza: Hardness Left, Center, Right.
  - Acepta números con coma o punto decimal (conversión automática).
- **Acciones:** Agregar cubierta, Eliminar última cubierta, Guardar borrador.
- **Sincronización:** Intenta sincronizar inmediatamente; si falla, queda como `PENDING_SYNC`.

### UnsupportedRoleScreen
- Pantalla informativa para usuarios con rol diferente a inspector.
- Solo contiene un botón de logout.

---

## 11. Seguridad y autenticación

### Login y token
- El usuario se autentica con `username` + `password` vía POST `/api/login`.
- El servidor devuelve un JWT/Bearer token.
- El token se almacena en **SharedPreferences** (contexto privado de la app).
- `SessionManager` gestiona la lectura/escritura de la sesión.

### Interceptor HTTP
- `AuthInterceptor` inyecta `Authorization: Bearer {token}` en cada request automáticamente.
- Si no hay token, el header no se adjunta (el servidor retornaría 401).
- Si el servidor responde 401 en cualquier endpoint autenticado (token expirado), `AuthInterceptor` llama a `TokenExpirationEvent.emit()`.

### TokenExpirationEvent
Objeto singleton (`data/remote/TokenExpirationEvent.kt`) que expone un `SharedFlow<Unit>`. El `AuthInterceptor` lo emite cuando detecta un 401. `MainActivity` (u otro observador) debe colectar este flow y ejecutar el logout/relogin automático. Resuelve el riesgo #5 de las versiones anteriores.

### Datos de sesión almacenados
- `userId`, `userName`, `username`, `roleKey`, `clientId`, `clientName`, `elementTypeId`, `elementTypeName`, `token`.

### Riesgos de seguridad identificados

1. **Token en SharedPreferences sin cifrado**: SharedPreferences no cifra por defecto. Se recomienda usar `EncryptedSharedPreferences` de Jetpack Security.
2. **cleartext traffic habilitado**: El archivo `network_security_config.xml` permite tráfico HTTP no cifrado. Debe validarse que en producción esto esté restringido.
3. **Sin manejo de expiración de token**: No se evidencia lógica para detectar token expirado (respuesta 401 en endpoints autenticados) y forzar nuevo login. Actualmente solo el login maneja el 401.
4. **Sin mecanismo de logout remoto**: El logout solo limpia la sesión local; no hay invalidación del token en el servidor.

---

## 12. Errores, riesgos o inconsistencias detectadas

### Críticos

1. **`fallbackToDestructiveMigration()` en producción**
   - Room está configurado con migración destructiva (versión 19). Cualquier cambio en el esquema de la base de datos eliminará todos los datos locales del usuario. Esto es crítico si hay reportes `PENDING_SYNC` que aún no se han sincronizado.
   - **Riesgo:** Pérdida de datos de inspección en campo al actualizar la app.

2. ~~**Posible pérdida de evidencias en fallo parcial de sync**~~ **→ RESUELTO (v1.6.3)**
   - Si la carga de evidencias falla, `ReportEntity` queda `PENDING_SYNC` aunque el detail ya esté `SYNCED`. En sync posteriores se omite el reenvío al servidor (evitando concatenación duplicada de recomendación) y se reintenta solo la subida de evidencias usando el `serverId` almacenado.

3. **MainActivity de ~1481 líneas**
   - Toda la inyección de dependencias, lógica de restauración de sesión, configuración de launchers de cámara y lógica de navegación está concentrada en `MainActivity`. Esto viola el principio de responsabilidad única y hace el código difícil de mantener, testear y escalar.

### Importantes

4. **Token JWT en SharedPreferences sin cifrado**
   - Ver sección 11.

5. ~~**Sin manejo de token expirado en runtime**~~ **→ RESUELTO PARCIALMENTE**
   - `AuthInterceptor` ahora detecta respuestas 401 y emite `TokenExpirationEvent`. El mecanismo de detección está implementado. Pendiente: verificar que `MainActivity` colecta el flow y fuerza el logout automático.

6. **cleartext traffic en producción**
   - El `network_security_config.xml` tiene `cleartextTrafficPermitted="true"`. Si la URL base es siempre HTTPS, esto no es un problema práctico, pero es un riesgo de configuración.

7. **Inyección manual de dependencias sin Hilt/Dagger**
   - Todas las dependencias se crean y conectan manualmente en `MainActivity`. Esto genera código boilerplate, dificulta el testing unitario y hace frágil la creación del grafo de dependencias.

8. **Sin manejo de concurrencia entre sync automático y manual**
   - Si el usuario presiona "Sincronizar" al mismo tiempo que el `SyncWorker` ejecuta en background, podrían ocurrir sincronizaciones paralelas del mismo reporte. El `Mutex` está en `RemoteCatalogRepository` pero no necesariamente en `SyncRepository`.

9. **`getRecentReportDetailsFromDate()` con enriquecimiento de datos en ViewModel**
   - El `DashboardViewModel` enriquece los datos de reportes recientes haciendo múltiples consultas a Room para obtener nombres de elementos, componentes, etc. Esto podría hacerse con un JOIN en Room para mayor eficiencia.

### Menores

10. ~~**Logging verboso en producción**~~ **→ RESUELTO (v1.7.4)**
    - `HttpLoggingInterceptor.Level.BODY` causaba `OutOfMemoryError` al descargar el catálogo offline (~145 MB confirmados por crash log real; el cliente/grupo exacto detrás de ese tamaño aún no está identificado, ver nota en historial v1.7.4). Reemplazado por `Level.HEADERS` en debug y `Level.NONE` en release.

11. **Seed de base de datos comentado**
    - Hay código comentado en `MainActivity` para sembrar datos de prueba. Debe eliminarse o moverse a un archivo de debug dedicado.

12. **Sin paginación en listas**
    - Consultas como `getAll()` en algunos DAOs retornan todos los registros. Si el catálogo es muy grande, esto puede causar problemas de memoria o lentitud.

13. **Nombres de campos mixtos (snake_case y camelCase) en DTOs**
    - Algunos DTOs usan `snake_case` (como `LoginRequest`) y otros `camelCase`. Aunque Gson maneja la conversión, genera inconsistencia en el código.

---

## 13. Recomendaciones de mejora

### Prioridad Alta

1. **Implementar migraciones Room apropiadas**
   - Reemplazar `fallbackToDestructiveMigration()` por migraciones explícitas con `addMigrations()`. Esto evita pérdida de datos en actualizaciones.

2. **Cifrar SharedPreferences**
   - Migrar a `EncryptedSharedPreferences` de Jetpack Security para proteger el token y datos de sesión.

3. **Migrar a Hilt para inyección de dependencias**
   - Eliminar la inyección manual en `MainActivity`. Hilt generará el grafo automáticamente, reducirá el boilerplate y facilitará el testing.

4. ~~**Implementar manejo de token expirado (401 interceptor)**~~ **→ RESUELTO PARCIALMENTE**
   - `AuthInterceptor` ya emite `TokenExpirationEvent` en respuestas 401. Verificar que `MainActivity` colecta el flow y ejecuta el logout automático.

5. **Agregar Mutex o flag en SyncRepository**
   - Proteger `syncPendingReports()` con un `Mutex` o un `AtomicBoolean` para evitar sincronizaciones concurrentes desde el Worker y el botón manual.

### Prioridad Media

6. **Reducir tamaño de MainActivity**
   - Extraer la lógica de restauración de sesión a un `SessionRestorationUseCase`.
   - Extraer la lógica de cámara/video a una clase `MediaCaptureManager`.
   - El resultado debería ser una `MainActivity` de menos de 200 líneas.

7. **Deshabilitar logging en producción**
   - Usar `BuildConfig.DEBUG` para condicionar `HttpLoggingInterceptor.Level.BODY` solo en debug.

8. ~~**Implementar retry granular para evidencias**~~ **→ RESUELTO (v1.6.3)**
   - `SyncRepository` ya verifica `detail.syncStatus` antes de reenviar al servidor. Si el detail está `SYNCED`, solo reintenta las evidencias pendientes.

9. **Optimizar queries con JOINs**
   - Reemplazar el enriquecimiento de datos en ViewModel (múltiples queries) por consultas Room con `@Relation` o JOINs SQL.

10. **Invalidación de caché de diagnósticos**
    - Definir una estrategia explícita de cuándo se invalida y recarga el caché de `PendingDiagnosticCacheEntity` y `WeeklyElementStatusCacheEntity`.

### Prioridad Baja

11. **Paginación en listas largas**
    - Usar `Pager` de Jetpack Paging 3 para DAOs con conjuntos de datos potencialmente grandes.

12. **Estandarizar nombres de campos en DTOs**
    - Usar consistentemente `@SerializedName` con `snake_case` en todos los DTOs para claridad.

13. **Eliminar código comentado**
    - Limpiar el seed de datos comentado y otros bloques de código comentado en `MainActivity`.

14. **Agregar tests unitarios**
    - No se evidencian tests en el proyecto. Comenzar con tests de repositorios con base de datos en memoria (Room in-memory) y tests de ViewModels con coroutines.

15. **Logout remoto**
    - Implementar invalidación del token en el servidor al hacer logout.

---

## 14. Guía para continuar el desarrollo

### Antes de modificar el proyecto

1. **Entender la estrategia offline-first**: Todo cambio que afecte el flujo de datos debe considerar que el inspector puede estar sin conexión. Los datos siempre van a Room primero.

2. **Cuidado con cambios en entidades Room**: La versión actual es 19 con migración destructiva. Si agregas/modificas un campo en una `@Entity`, **debes incrementar la versión** de la base de datos. En producción, también deberías escribir una `Migration` explícita para no perder datos.

3. **No crear lógica de negocio en ViewModels de forma directa**: Los ViewModels deben delegar a repositorios. La lógica de datos va en repositorios.

4. **No llamar a la API directamente desde ViewModels**: Toda comunicación remota pasa por repositorios.

5. **Usar `viewModelScope` para coroutines en ViewModels**: Ya está establecido este patrón; respetarlo garantiza cancelación automática.

6. **Inyección de dependencias manual**: Al agregar un nuevo repositorio o ViewModel, debes:
   - Crear el repositorio con sus dependencias.
   - Crear o modificar su `Factory`.
   - Instanciar todo en `MainActivity` y conectarlo.

7. **Semanas ISO**: El proyecto usa semanas ISO (lunes como primer día, mínimo 4 días). Al calcular semana/año de un reporte, usar el mismo patrón que `InspectionViewModel.saveInspectionReport()`.

8. **Estados de sincronización**: Al crear nuevas entidades que deban sincronizarse, seguir el patrón `PENDING_SYNC` / `SYNCED` en el campo `syncStatus`.

9. **Flujo de catálogo**: El catálogo se descarga en login. Si el servidor agrega nuevos campos a las entidades del catálogo, actualizar: DTO remoto → Entity Room → DAO → Repositorio → (potencialmente) ViewModel.

10. **Nuevo módulo de pantalla**: Crear: Screen Compose → ViewModel → UiState → AppScreen (ruta) → enlazar en MainActivity/NavigationVM.

### Configuración de entorno

- Requiere Android Studio con soporte KSP.
- Min SDK: 24 (Android 7.0).
- Target SDK: 35 (Android 15).
- Java/Kotlin target: VERSION_11.
- La base URL de la API está hardcodeada en `RetrofitClient.kt`: `https://mantecsas.com/`.

---

## 15. Resumen ejecutivo final

**Mantec Inspector** es una aplicación Android Kotlin para inspectores de campo en entornos industriales. Permite registrar inspecciones de equipos (con diagnóstico, condición, fotos y videos) y mediciones de espesor de correas, todo funcionando offline. Los datos se sincronizan al servidor central (`mantecsas.com`) cuando hay conectividad disponible, de forma automática o manual.

Arquitectónicamente usa MVVM + Clean Architecture con Room, Retrofit y WorkManager. El mayor riesgo técnico actual es la migración destructiva de base de datos y la ausencia de Hilt (dependencias manuales en una MainActivity de ~1500 líneas). El proyecto está funcional y bien estructurado en cuanto a separación de capas, pero necesita refactoring de la actividad principal, implementación de migraciones Room explícitas, cifrado del token y manejo de errores de autenticación en runtime antes de considerarse production-ready.

---

## 16. Historial de versiones

### v1.7.6 — Fix OutOfMemoryError al subir evidencia de video + soporte de archivos de hasta 1 GB

**Fecha:** 2026-08-04

**Archivos modificados:** `SyncRepository.kt`, `EvidenceDao.kt`, `RetrofitClient.kt`, `HomeScreen.kt`, `app/build.gradle.kts`

**Este es un bug distinto al de v1.7.4/v1.7.5.** Aquel era al *descargar* el catálogo offline; este es al *subir* evidencia (foto/video) durante la sincronización de un reporte. Comparten el mismo patrón de causa raíz (bufferizar algo grande completo en memoria en vez de transmitirlo en streaming), pero en puntos distintos del código.

**El error — evidencia real (reporte de usuario vía Play Store):**
```
java.lang.OutOfMemoryError: Failed to allocate a 167548040 byte allocation
with 25165824 free bytes and 91MB until OOM, target footprint 197782128,
growth limit 268435456
    at java.util.Arrays.copyOf(Arrays.java:4276)
    at java.io.ByteArrayOutputStream.toByteArray(ByteArrayOutputStream.java:211)
    at kotlin.io.ByteStreamsKt.readBytes(IOStreams.kt:137)
    at com.example.mantec_ins.data.repository.SyncRepository.buildMultipartFromUri(SyncRepository.kt:211)
    at com.example.mantec_ins.data.repository.SyncRepository.doSyncPendingReports(SyncRepository.kt:120)
    ...
```
`167548040` bytes ≈ **159.8 MB** — el tamaño de un video de evidencia que el inspector intentó sincronizar.

**Causa raíz:** `buildMultipartFromUri()` hacía `inputStream.use { it.readBytes() }`, cargando el archivo completo (foto o video) a un `ByteArray` en memoria antes de armar el `MultipartBody.Part` para subirlo. Con un video de ~160 MB y ~24-25 MB libres de heap en el dispositivo, la asignación fallaba. Es el mismo patrón que en v1.7.4 (bufferizar en vez de transmitir), pero acá afecta a **cualquier subida de evidencia grande**, sin relación con `HttpLoggingInterceptor` ni con el catálogo.

**Contexto de negocio que amplió el alcance del fix:** en paralelo se descubrió que el backend limitaba el tamaño de archivo de evidencia a 100 MB (validación Laravel `max:102400` + `php.ini`). La opción evaluada fue limitar la duración de grabación de video en la app para mantenerse bajo ese límite, pero se descartó: los videos de inspección a veces son legítimamente largos y superan los 100 MB. Se optó en cambio por **subir el límite del backend a 1024 MB (1 GB)**, lo que hizo que arreglar el streaming en la app dejara de ser opcional — sin el fix, subir el nuevo límite del backend solo habría aumentado la frecuencia del crash, no resuelto nada, porque el crash ocurre en el dispositivo *antes* de que el archivo llegue a la red.

**Verificación de coordinación con el backend (Laravel), con evidencia real, no solo lectura de código:**
- **Streaming end-to-end confirmado en el backend:** `InspectorSyncFileController`/`AdminReportEvidenceController` usan `fopen()` + `Storage::disk('r2')->writeStream()`, y la librería AWS S3 (`ObjectUploader`/`MultipartUploader`) sube en partes de ~5 MB leyendo el stream progresivamente. Nunca carga el archivo completo a una variable de PHP — se descartó explícitamente el riesgo de memoria del lado servidor.
- **Validación de Laravel actualizada:** `max:102400` → `max:1048576` en ambos controladores.
- **`php.ini` de producción (Railway) confirmado vía `/php-upload-check`:** `upload_max_filesize=1024M`, `post_max_size=1100M`, `memory_limit=1024M` (subido desde 128M como margen de seguridad, aunque el streaming ya lo hacía innecesario), `max_execution_time=300` (5 minutos).
- **Sin Cloudflare de por medio** (dominio pega directo al edge de Railway) — descarta el límite duro de 100 MB que Cloudflare Free/Pro impone.
- **Pendiente, no verificable desde el código:** límite propio del edge de Railway y comportamiento real de timeouts en producción — el backend recomendó una prueba real con un archivo de ~500-900 MB desde la app para confirmarlo en vivo. **Esa prueba end-to-end todavía no se ha ejecutado.**
- Los errores 422 del backend llegan en inglés (`"The file field must not be greater than 1048576 kilobytes."`) porque falta `lang/es` en el proyecto Laravel — brecha de localización identificada y dejada pendiente del lado del backend, no se tradujo el mensaje crudo en la app (ver más abajo por qué).

**Fix aplicado (tres cambios coordinados):**

1. **Streaming real en `buildMultipartFromUri()`** (`SyncRepository.kt`): se elimina `readBytes()`. El `RequestBody` ahora abre el `InputStream` dentro de `writeTo()` y lo transmite directo al `sink` de red vía `inputStream.source().use { sink.writeAll(source) }` (Okio), sin retener nunca el archivo completo en memoria. El tamaño para `contentLength()` se obtiene sin leer el archivo, vía `ContentResolver.openAssetFileDescriptor(uri, "r")?.length`; si no se puede determinar, se devuelve `-1` y OkHttp usa `Transfer-Encoding: chunked` (el backend lo soporta sin problema). Con esto, subir un archivo de 10 MB o de 1 GB consume la misma memoria pico (unos pocos KB del buffer de streaming de Okio), en vez de escalar linealmente con el tamaño del archivo.

2. **Timeouts en `RetrofitClient.kt`:** se agregan `connectTimeout` (20s) y `writeTimeout`/`readTimeout` (15 minutos) al `OkHttpClient.Builder()` — antes no había ninguno configurado, así que aplicaban los defaults de OkHttp (10s), insuficientes para subir archivos grandes. Se descartó alinear el timeout al `max_execution_time=300s` del backend (idea inicial): ese contador de PHP mide tiempo de ejecución del script, no el tiempo que tarda el archivo en transmitirse hasta el servidor, así que no acota de forma confiable cuánto puede tardar una subida real. Se optó por 15 minutos para darle margen a un inspector subiendo evidencia grande con datos móviles y señal débil en campo, en vez de cortar la subida prematuramente por un timeout del lado del cliente.

3. **Evidencia con rechazo permanente ya no se reintenta indefinidamente** (`SyncRepository.kt` + `EvidenceDao.kt`): si `uploadReportFile` responde 422 o 413 (rechazo de validación, ej. archivo que igual supera 1 GB), la evidencia se marca `syncStatus = "ERROR"` (nuevo método `EvidenceDao.updateStatus()`) y el loop de sync la salta en intentos futuros (`if (evidence.syncStatus == "SYNCED" || evidence.syncStatus == "ERROR")`). Antes, cualquier fallo de subida dejaba la evidencia en `PENDING_SYNC` para siempre, reintentando en cada sync sin posibilidad de éxito. Fallos transitorios (red caída, 5xx, timeout) siguen sin marcarse como `ERROR` y se reintentan normalmente — solo se corta el reintento cuando el backend confirma que el archivo nunca va a ser aceptado.

4. **Mensaje de progreso durante la subida** (`HomeScreen.kt`): con el timeout de red en 15 minutos, un sync manual con video grande podía dejar el botón "Sincronizar" deshabilitado en silencio por varios minutos sin ninguna explicación, dando la impresión de que la app se congeló. Se agregó un banner visible mientras `isManualSyncRunning == true` ("Subiendo evidencia, esto puede tardar varios minutos con datos móviles. No cierres la app."), reutilizando los colores ya definidos para el estado "SYNCING" de los badges de reportes pendientes. Es solo un mensaje informativo, no una barra de progreso real — la app no tiene forma de saber cuánto falta de una subida en curso sin instrumentar el propio `RequestBody`, algo que se dejó fuera de este alcance.

**Decisiones de alcance (por qué se hizo así y no de otra forma):**
- **Se reutilizó el valor `"ERROR"` en la columna `syncStatus` ya existente**, siguiendo el mismo patrón que `MeasurementThicknessRepository` ya usa para sus borradores (`syncStatus IN ('PENDING_SYNC', 'CONFLICT', 'ERROR')`). Esto evitó agregar una columna nueva a `EvidenceEntity`, lo que a su vez evitó tener que subir la versión de Room (hoy 19, con `fallbackToDestructiveMigration()` — ver riesgo crítico #1 en sección 12). Subir la versión de Room habría borrado todos los reportes `PENDING_SYNC` de los inspectores en el próximo update de la app, un efecto secundario mucho peor que el bug que se estaba arreglando.
- **No se tradujo el mensaje de error 422 del backend ni se guardó en un campo nuevo `lastError`** para mostrarlo en la UI del inspector. Por ahora el rechazo permanente solo queda registrado en logs (`Log.e`) y en el estado interno `ERROR` de la evidencia; el reporte en la UI actual sigue viéndose como "no sincronizado" sin detalle del motivo. Mostrar un mensaje claro en español al inspector requiere trabajo de UI (`ReportListViewModel`, pantallas de HomeScreen) que no se hizo en este cambio — queda como tarea de seguimiento explícita, no como omisión accidental.
- **No se limitó la duración de grabación de video en la app.** Fue una decisión de negocio explícita: los videos de inspección pueden ser legítimamente largos. La combinación streaming (app) + límite de 1 GB (backend) + timeout de red de 15 minutos (app) es la solución elegida en su lugar.

**Verificación de compilación (2026-08-04):** se corrió `gradlew compileDebugKotlin` con `--rerun-tasks` (recompilación forzada, sin caché) sobre el JDK embebido de Android Studio. `BUILD SUCCESSFUL`, 16/16 tareas ejecutadas. Sin errores ni warnings nuevos en `SyncRepository.kt`, `EvidenceDao.kt`, `RetrofitClient.kt` ni `HomeScreen.kt`. Los únicos warnings del build son preexistentes y no relacionados (índices faltantes en `ElementComponentCrossRef`/`ComponentDiagnosticCrossRef`, e íconos deprecados `Icons.Outlined.ExitToApp` / `Icons.Filled.ArrowBack` en pantallas no tocadas en este cambio). Antes de esta verificación se confirmó además, contra los `.jar` reales de Okio en el caché de Gradle del proyecto, que `InputStream.source()` y `BufferedSink.writeAll(Source)` existen con la firma exacta usada en el fix.

**Pendientes explícitos para cerrar el ciclo (a la fecha, 2026-08-04):**
- Ejecutar la prueba real de subida con un archivo de ~500-900 MB en una conexión de datos móviles típica, para confirmar que el edge de Railway no corta la conexión antes de completarse. **Esta es la única verificación que sigue sin hacerse** — todo lo demás (código, compilación, coordinación con backend) ya quedó confirmado.
- Backend: publicar `lang/es` para que los mensajes de validación lleguen en español (hoy la app no depende de ese texto para nada crítico, pero sería una mejora de UX si en el futuro se decide mostrarlo).
- Opcional/futuro: si se decide mostrar al inspector por qué una evidencia específica no se pudo sincronizar, agregar un campo `lastError` a `EvidenceEntity` (esto sí requeriría una migración de Room — planear junto con otras migraciones pendientes, no una por una).

---

### v1.7.5 — Reducir descargas del catálogo offline (ahorro de datos móviles)

**Archivo modificado:** `MainActivity.kt`

**Problema:** `syncOfflineCatalog()` se ejecutaba en tres momentos innecesarios:
1. Cada vez que la app volvía del fondo (`onResume` con `autoSync = true`)
2. Después de cada reporte guardado (`refreshCatalogAfterSync = true` en post-save)
3. Después de cada sincronización manual (`refreshCatalogAfterSync = true` en manual sync)

**Nota sobre el consumo de datos estimado:** la proyección de ~4.3 GB/día asumía un peso de catálogo de ~145 MB por descarga, cifra confirmada como real para al menos un cliente/grupo concreto (ver evidencia de crash log en v1.7.4). Sin embargo, una validación sobre el backend (2026-06-19) midió solo ~130 KB para el grupo más grande *probado* en ese momento (`group_id=2`), lo que sugiere que el tamaño del catálogo varía enormemente entre clientes/grupos — desde cientos de KB hasta decenas de MB — y aún no está identificado qué cliente/grupo corresponde al caso de ~145 MB. El problema de descargas redundantes que motivó este fix es real independientemente de la magnitud exacta del ahorro; falta reconciliar el rango real de tamaños de catálogo por cliente para dimensionar el ahorro de datos móviles con precisión.

**Aclaración:** el avance de los compañeros (badges DONE/PENDING por elemento y diagnóstico) **no depende del catálogo**. Se actualiza mediante endpoints independientes (`getWeeklyDiagnosticStatus`, `getWeeklyElementsStatus`) que siguen llamándose igual.

**Fix aplicado:**
- Se eliminó `syncOfflineCatalog()` del bloque `if (autoSync)` en `onResume()`. Se conserva `SyncWorkManager.start()` para que el scheduler de background siga activo.
- Se cambió `refreshCatalogAfterSync = true` → `false` en el post-guardado y en el sync manual.

**Cuándo se descarga el catálogo ahora:**
- Al hacer login (siempre).
- Si la DB local está vacía al restaurar la sesión (proceso matado por MIUI).

---

### v1.7.4 — Fix OutOfMemoryError por HttpLoggingInterceptor con respuesta grande

**Archivo modificado:** `RetrofitClient.kt`

**Problema:** La app se cerraba con `java.lang.OutOfMemoryError` al descargar el catálogo offline. El error ocurría exactamente en `HttpLoggingInterceptor.intercept`, que intentaba leer todo el cuerpo de la respuesta de `/api/inspector/offline-catalog` como String para loguearlo. En dispositivos con poca RAM (gama baja Xiaomi/MIUI) con solo ~24 MB libres, la alocación fallaba y la app se cerraba.

**Evidencia real del crash (reporte de usuario vía Play Store):**
```
java.lang.OutOfMemoryError: Failed to allocate a 151889552 byte allocation
with 25165824 free bytes and 98MB until OOM, target footprint 190411968,
growth limit 268435456
    at java.lang.StringFactory.newStringFromUtf8Bytes
    at okio.Buffer.readString(Buffer.kt:313)
    at okhttp3.logging.HttpLoggingInterceptor.intercept(HttpLoggingInterceptor.kt:209)
    at com.example.mantec_ins.data.remote.AuthInterceptor.intercept(AuthInterceptor.kt:22)
    ...
```
`151889552` bytes ≈ **144.86 MB**, y `25165824` bytes ≈ **24 MB libres** — de aquí salen las cifras "~145 MB" y "~24 MB libres" citadas en el documento. Como el fallo ocurre en `okio.Buffer.readString()`, que decodifica directamente los bytes ya bufferizados del cuerpo de la respuesta (sin duplicación por crecimiento de buffer en ese punto), esta cifra **sí corresponde al tamaño real** del cuerpo de esa respuesta HTTP puntual, no a un artefacto del mensaje de OOM.

**Causa raíz:** `HttpLoggingInterceptor.Level.BODY` bufferiza la respuesta HTTP completa en memoria como String antes de loguearla. Con una respuesta de ese tamaño, esto excede el heap disponible del proceso Android.

**Fix:** Se reemplaza `Level.BODY` por lógica condicional sobre `BuildConfig.DEBUG`:
- En **debug**: `Level.HEADERS` — registra método, URL, status y cabeceras, suficiente para depurar.
- En **release**: `Level.NONE` — sin logging HTTP, sin riesgo de OOM ni exposición de datos.

**Pendiente de reconciliar:** una investigación sobre el backend (2026-06-19, `InspectorOfflineCatalogController@show`) midió, con datos reales de producción, que el catálogo del grupo más grande *conocido en ese momento* (`group_id=2`, 109 elementos) pesa solo ~130 KB sin comprimir — muy por debajo de los ~145 MB de este crash real. Esto indica que el cliente/grupo que sufrió este crash específico **no es el mismo** que se probó, y probablemente tiene un volumen de activos varios órdenes de magnitud mayor. Falta identificar (vía Play Console: fecha, dispositivo, usuario asociado al reporte) qué cliente/grupo generó este catálogo de ~145 MB y repetir la medición del backend sobre ese grupo puntual, en vez de asumir que el mayor grupo conocido representa el peor caso real.

---

### v1.7.3 — Fix crash al restaurar la app desde background (dispositivos MIUI/Xiaomi)

**Archivos modificados:** `DashboardViewModel.kt`, `MainActivity.kt`

**Problema:** La app se cerraba de manera forzosa en dispositivos Xiaomi/MIUI cuando el inspector minimizaba la app y luego intentaba regresar a ella. El crash ocurría al llegar a la HomeScreen: a veces de inmediato, a veces ~2 segundos después.

**Causa raíz:** Cuatro bloques `viewModelScope.launch {}` y `lifecycleScope.launch {}` no tenían `try-catch`. En Android, una excepción no capturada dentro de estas corrutinas cierra la app de forma inmediata (el sistema no la puede recuperar). MIUI es especialmente sensible porque mata el proceso en background de forma agresiva; cuando Android lo recrea, la base de datos Room puede estar en un estado de recuperación WAL por unos milisegundos, y cualquier query de DAO en esa ventana puede fallar.

**Cambios en `DashboardViewModel.kt`:**
- `loadPendingDiagnosticsForElement()`: se envuelve todo el cuerpo del `viewModelScope.launch` en `try/catch`. Un fallo al leer el caché de diagnósticos pendientes ya no cierra la app.
- `loadWeeklyElementsStatus()`: ídem. Un fallo al leer el caché de estado semanal por área/tipo ya no cierra la app.
- `loadWeeklyElementsStatusForElements()`: ídem. Un fallo al leer el estado semanal para múltiples elementos ya no cierra la app.
- `loadRecentReports24h()`: ídem. Un fallo al cargar reportes + entidades de catálogo (elemento, componente, diagnóstico, condición) ya no cierra la app.

**Cambios en `MainActivity.kt`:**
- Callback `onFinished` del auto-sync en HomeScreen (línea del `LaunchedEffect(currentScreen)`): se agrega `try/catch` al `lifecycleScope.launch` que ejecuta `syncAllPendingDrafts()` y actualiza el mensaje de éxito. Un fallo post-sync ya no cierra la app.
- Callback `onFinished` del sync manual (botón "Sincronizar"): ídem. Además se asegura que `isManualSyncRunning = false` en el `catch`, para que el botón no quede bloqueado si falla.

**Por qué el timing era distinto:**
- *Crash inmediato*: `loadRecentReports24h()` se llama en `onCreate()` antes de que la HomeScreen aparezca; si la DB aún recupera el WAL, falla de inmediato.
- *Crash ~2 segundos*: el sync de red tarda ~2s; al completarse, el callback sin `try/catch` ejecutaba `syncAllPendingDrafts()` y si fallaba, cerraba la app en ese momento.

---

### v1.7.2 — Ocultar icono de micrófono en campo Recomendación

**Archivo modificado:** `ReportFormScreen.kt`

**Cambio:** Se deja de pasar el callback `onVoiceInputClick` al composable `ReportTextArea`, por lo que el botón 🎤 no se renderiza. La implementación completa de dictado de voz (permisos, launchers, lógica en `MainActivity`) permanece intacta para habilitarse en una versión futura.

---

### v1.7.1 — Mejoras de UX en módulo de mediciones

**Archivos modificados:** `MeasurementThicknessScreen.kt`, `AndroidManifest.xml`

**Cambios:**

1. **Teclado numérico decimal en campos de medición** — Se agrega `KeyboardOptions(keyboardType = KeyboardType.Decimal)` al `NumberField`. El teclado ya no cambia a letras al enfocar un campo de espesor o dureza.

2. **El campo no auto-formatea el número del usuario** — Se reemplaza `remember(value)` (con clave `Double`) por `rememberSaveable` sin clave + `LaunchedEffect`. Antes, al escribir "12" el ViewModel almacenaba `12.0` y el campo se reseteaba a `"12.0"`. Ahora el texto que escribe el inspector es el que se muestra, sin interferencia.

3. **Punto decimal ya no borra el valor anterior** — El comportamiento roto era: escribir "12.", el `toDoubleOrNull("12.")` devolvía `null`, el campo se vaciaba. Con el desacoplamiento del paso anterior, el inspector puede escribir "12.5" de forma natural.

4. **Filtro de doble punto decimal** — La sanitización ahora descarta cualquier segundo punto: `"5.2.8"` → `"5.28"`.

5. **Scroll con teclado abierto** — Se agrega `android:windowSoftInputMode="adjustResize"` en `AndroidManifest.xml`. Antes el teclado cubría la pantalla sin posibilidad de hacer scroll; ahora Android reduce el área de la app al espacio sobre el teclado y el scroll existente funciona normalmente.

---

### v1.7.0 — Dictado de voz en campo Recomendación

**Archivos modificados:** `AndroidManifest.xml`, `app/build.gradle.kts`, `MainActivity.kt`, `MainScreenHost.kt`, `ReportFormScreen.kt`

**Funcionalidad:** El inspector puede dictar la recomendación usando el micrófono del dispositivo en lugar de escribirla manualmente. Un botón 🎤 naranja aparece junto al label "Recomendación" en el formulario de nuevo reporte.

**Implementación:**
- `AndroidManifest.xml`: se declara el permiso `RECORD_AUDIO`.
- `build.gradle.kts`: se agrega la dependencia `material-icons-extended` para el ícono `Icons.Filled.Mic`.
- `MainActivity.kt`: se registra `speechRecognizerLauncher` (`StartActivityForResult` con `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) y `requestAudioPermissionLauncher` (`RequestPermission`). Al pulsar el botón, se verifica si el permiso ya fue concedido; si no, se solicita antes de lanzar el reconocedor. El texto reconocido se agrega al final del texto ya escrito (no lo reemplaza).
- `MainScreenHost.kt` y `ReportFormScreen.kt`: propagación del callback `onVoiceInputClick` por la cadena de composables.
- `ReportTextArea`: el botón de micrófono se ubica en la misma fila que el label, alineado a la derecha.

**Comportamiento offline:** usa el reconocedor de voz del sistema Android (`RecognizerIntent`). Si el dispositivo tiene paquetes de idioma offline instalados (común en Android moderno con Google Speech Services), funciona sin conexión a internet.

---

### v1.6.3 — Fix recomendación duplicada por reenvío del mismo reporte al servidor

**Archivo modificado:** `SyncRepository.kt`

**Problema:** Cuando la subida de evidencias fallaba (red cortada, URI inaccesible, etc.), `ReportEntity` quedaba con `status = PENDING_SYNC` aunque `ReportDetailEntity.syncStatus` ya fuera `SYNCED`. En el siguiente intento de sync, el mismo reporte se reenviaba al servidor. El servidor tiene lógica de negocio que concatena la recomendación cuando recibe múltiples reportes del mismo elemento+componente+diagnóstico en el mismo día, produciendo texto repetido N veces (tantas como intentos fallidos hubo).

**Causa raíz:** `reportDao.updateStatus(SYNCED)` solo se ejecuta si TODAS las evidencias suben correctamente. Si alguna falla, `ReportEntity` nunca se marca como `SYNCED` y vuelve a ser procesado en cada sync.

**Fix:** En `doSyncPendingReports()`, antes de llamar `api.syncReport()`, se verifica `detail.syncStatus`:
- Si es `SYNCED` y `detail.serverId != null` → el reporte ya fue aceptado por el servidor. Se omite el `api.syncReport()` y se reintenta solo la subida de evidencias pendientes usando el `serverId` almacenado.
- Si es `PENDING_SYNC` → flujo normal: se envía al servidor, se guarda el `serverId`.

**La concatenación legítima no se afecta:** cuando el inspector crea un segundo reporte distinto (nuevo `localId`) para el mismo activo/componente/diagnóstico el mismo día, ese reporte llega al servidor por primera vez con `syncStatus = PENDING_SYNC` y el servidor lo concatena correctamente.

---

### v1.6.2 — Galería múltiple y preservación de recomendación

**Archivos modificados:** `MainActivity.kt`, `MainScreenHost.kt`, `ReportFormScreen.kt`, `InspectionViewModel.kt`

**Cambios:**

1. **Selector de galería con selección múltiple** — Se agregó el botón "Galería" en la sección de evidencias del formulario. Usa `PickMultipleVisualMedia` (Activity 1.10.1+), que abre el selector nativo del SO y permite elegir varias fotos y/o videos a la vez. El tipo de cada archivo (imagen/video) se detecta automáticamente por MIME type. No requiere permisos adicionales en el manifest.

2. **Recomendación no se borra al cambiar componente o diagnóstico** — En `InspectionViewModel`, `setSelectedComponent()` y `setSelectedDiagnostic()` ya no limpian el campo `recommendation`. El texto se preserva si el inspector corrige su selección. Solo se limpia al cambiar el elemento completo (`setSelectedElement()`) o al guardar el reporte exitosamente.

---

### v1.6.1 — Corrección crash al tomar foto/video

**Archivo modificado:** `MainActivity.kt`

**Problema:** La app se cerraba con "force closed due to an internal error" al intentar tomar una foto o grabar un video.

**Causa raíz:** El `AndroidManifest.xml` declara `android.permission.CAMERA` y el `targetSdk` es 35. En Android 11+ (API 30+), si una app declara ese permiso en el manifest, el sistema exige que esté concedido en tiempo de ejecución antes de lanzar `ActivityResultContracts.TakePicture()` o `CaptureVideo()`. No existía ninguna solicitud de permiso en runtime, lo que provocaba una `SecurityException` al intentar abrir la cámara.

**Cambios:**
- Se agregaron dos variables de estado (`pendingTakePhoto`, `pendingRecordVideo`) para rastrear qué acción quedó pendiente mientras se espera la respuesta del permiso.
- Se agregó `requestCameraPermissionLauncher` (después de `createVideoUri()`) que solicita el permiso CAMERA y, si se concede, ejecuta la acción pendiente.
- Se actualizaron `onTakePhotoClick` y `onRecordVideoClick` para verificar el permiso antes de lanzar la cámara; si no está concedido, solicitan el permiso en lugar de crashear.
