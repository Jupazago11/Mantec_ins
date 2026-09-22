# Documentación general del proyecto

**Proyecto:** Mantec Inspector (Mantec_ins)
**Versión actual:** 1.9.0 (versionCode 26)
**Lenguaje:** Kotlin 100%
**Plataforma:** Android nativo
**Fecha de análisis inicial:** Mayo 2026
**Última actualización:** 2026-09-21 (ver v1.9.0 en la sección 16 para el detalle del release)

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

### v1.9.0 (release, versionCode 26) — Primer APK de release instalable, apuntando a producción

**Fecha:** 2026-09-21

> **Nota sobre el número de versión**: esta entrada se llamó primero solo "v1.9" (`versionName = "1.9"`); el 2026-09-21 se estandarizó a `"1.9.0"` (versionCode sin cambios, sigue siendo 26) para que el `versionName` sea siempre de 3 partes. Esto **no es la misma versión** que la entrada más abajo "v1.9.0 — Offline-first para el rol Supervisor" (2026-09-20, versionCode 12) — esa fue una build intermedia de esta misma sesión de trabajo, nunca llegó a ser el release firmado; el historial se deja tal cual quedó escrito en su momento, sin reescribirlo. Esta entrada (versionCode 26) es la que corresponde al APK real que se entregó para instalar y probar.

**Contexto:** el usuario pidió generar el APK de release (`BASE_URL = https://mantecsas.com/`) para instalarlo en un celular real y probar contra producción. Al correr `gradlew assembleRelease` por primera vez esta sesión, `apksigner verify` confirmó que el APK resultante **no estaba firmado** ("DOES NOT VERIFY", falta `META-INF/MANIFEST.MF") — el proyecto nunca tuvo un `signingConfig` para `release`, así que Android no permite instalarlo en ningún dispositivo, ni para pruebas.

**Fix:** se agregó `signingConfig = signingConfigs.getByName("debug")` al bloque `release` — firma temporalmente con la keystore de debug (autogenerada por Android) solo para poder instalar y probar. **Esto no es apto para una publicación real** (Play Store u otra distribución fuera del equipo) — antes de eso hace falta una keystore de release propia, que este proyecto todavía no tiene.

**Verificación de esta sesión:** `apksigner verify --print-certs` confirmó la firma tras el fix (`CN=Android Debug`). Se instaló el APK en un emulador limpio (arrancado desde cero para esta prueba) y abrió sin errores hasta la pantalla de login. No se completó un intento de login real contra producción por automatización de UI (el teclado en pantalla interfirió repetidamente con las coordenadas de los campos) — la garantía de que apunta a producción es directa por lectura de código: `BASE_URL` para `release` es una constante de compilación (`"https://mantecsas.com/"`), no hay ninguna lógica en tiempo de ejecución que pueda desviarla.

**Archivos modificados:** `app/build.gradle.kts` (signingConfig de release, versionCode 26).

**Pendiente:** keystore de release real antes de cualquier distribución fuera del equipo. Login real contra producción queda para cuando el usuario lo pruebe en su celular.

---

### v1.9.13 — Quitado el banner "Mostrando actividades de AYER"

**Fecha:** 2026-09-21

**Contexto:** el toggle Ayer/Hoy (naranja, con estilo segmentado) ya deja claro cuál día se está viendo — el banner ámbar adicional "Mostrando actividades de AYER (turno nocturno pendiente)" debajo era redundante. Se pidió quitarlo.

**Fix:** eliminado el bloque `Surface`/`Text` que mostraba ese mensaje en `SupervisorHomeScreen.kt`. Al seleccionar "Ayer" ahora se va directo a la lista de actividades, sin mensaje intermedio.

**Archivos modificados:** `presentation/ui/SupervisorHomeScreen.kt`, `app/build.gradle.kts` (versionCode 25, versionName 1.9.13).

**Validación de esta sesión:** `gradlew assembleDebug`/`testDebugUnitTest` — `BUILD SUCCESSFUL`. Verificado visualmente en el emulador.

---

### v1.9.12 — Fix: valores decimales invisibles en el stepper de horas

**Fecha:** 2026-09-21

**Contexto:** tras achicar el stepper en v1.9.11, un entero ("5", "6") se veía bien pero un decimal ("5.5") no se veía nada — solo un puntito suelto entre los íconos +/-. Causa raíz: `OutlinedTextField` trae un padding interno fijo (parte del spec de Material3, no configurable de forma simple) que con un ancho de 48dp le dejaba a "5.5" prácticamente cero espacio real para el texto — un solo dígito alcanzaba a asomar, tres caracteres no.

**Fix:** `OutlinedTextField` → `BasicTextField` en `HorasStepper` — sin chrome ni padding propio, todo el ancho disponible (52dp) es para el texto. De paso se agregó `KeyboardOptions(keyboardType = KeyboardType.Decimal)` para que el teclado que aparece al tocar el campo ya muestre el punto decimal a mano, sin cambiar de teclado.

**Archivos modificados:** `presentation/ui/SupervisorHomeScreen.kt`, `app/build.gradle.kts` (versionCode 24, versionName 1.9.12).

**Validación de esta sesión:** `gradlew assembleDebug`/`testDebugUnitTest` — `BUILD SUCCESSFUL`. Reproducido el bug exacto en el emulador (tocar "+" hasta llegar a "5.5") y confirmado que ahora se ve completo.

---

### v1.9.11 — Stepper de horas: estilo más minimalista

**Fecha:** 2026-09-21

**Contexto:** el rediseño del stepper en v1.9.9 (círculos blancos + contenedor en pastilla con fondo suave) no le gustó al usuario — pidió algo más minimalista.

**Fix:** `HorasStepper` perdió el contenedor (`background(SoftBackground, RoundedCornerShape(50))`) y los círculos blancos detrás de cada ícono (`background(CardBackground, CircleShape)`) — ahora es solo el ícono "−", el número (campo editable sin borde visible salvo al enfocar, igual que antes) y el ícono "+", uno al lado del otro sin ningún fondo ni contorno. Íconos más chicos (28dp de toque, 18dp visual) para acompañar el look más liviano.

**Archivos modificados:** `presentation/ui/SupervisorHomeScreen.kt`, `app/build.gradle.kts` (versionCode 23, versionName 1.9.11).

**Validación de esta sesión:** `gradlew assembleDebug`/`testDebugUnitTest` — `BUILD SUCCESSFUL`. Verificado visualmente en el emulador.

---

### v1.9.10 — Miniatura local inmediata de evidencia (antes solo se veía tras sincronizar)

**Fecha:** 2026-09-21

**Contexto:** el usuario notó que, al tomar una foto o video, la miniatura real se ve en los reportes de activos (Inspector) de inmediato, pero en Supervisor se quedaba en el ícono genérico hasta sincronizar. Causa raíz: la miniatura de v1.9.1 solo se pedía para evidencia ya `SYNCED` (URL firmada de R2) — una evidencia recién tomada, todavía `PENDING_SYNC`, nunca tenía de dónde sacar una miniatura. `ReportFormScreen.kt` (Inspector) no tiene este problema porque genera la miniatura desde el **archivo local** (`loadEvidenceThumbnail()`: `ImageDecoder`/`MediaStore.Images.Media.getBitmap` para fotos, `MediaMetadataRetriever.getFrameAtTime(0)` para el primer frame de un video), sin depender de que ya haya subido a ningún lado.

**Fix:** se replicó ese mismo patrón para Supervisor. `SupervisorEvidenceUi` ganó el campo `localPath` (ya existía en la entidad de Room, solo no estaba expuesto a la UI). Nueva función `loadLocalEvidenceThumbnail(path, fileType)` en `SupervisorHomeScreen.kt` — mismo patrón que `loadEvidenceThumbnail()` del Inspector, adaptado a un path de archivo plano en vez de una `content://` Uri (porque `encolarEvidenciaDesdeUri()` ya copia la evidencia a `filesDir` antes de guardarla, ver v1.9.0) — usa `BitmapFactory.decodeFile()` para fotos y `MediaMetadataRetriever.setDataSource(path)` para el frame de video. El tile de evidencia ahora prioriza la miniatura local (offline, instantánea); solo si ya no queda copia local (se subió y se borró, ver `SupervisorSyncRepository`) cae al comportamiento anterior de pedir la URL firmada — y solo para fotos, un video sincronizado sigue mostrando su ícono genérico (no se puede previsualizar un video por URL con `AsyncImage`).

**Archivos modificados:** `presentation/viewmodel/SupervisorActivityViewModel.kt` (`SupervisorEvidenceUi.localPath`), `presentation/ui/SupervisorHomeScreen.kt`, `app/build.gradle.kts` (versionCode 22, versionName 1.9.10).

**Validación de esta sesión:** `gradlew assembleDebug`/`testDebugUnitTest` — `BUILD SUCCESSFUL`. Verificado en el emulador: las dos evidencias tomadas en la ronda anterior (v1.9.9, una foto real + un video grabado) ahora muestran su miniatura real de inmediato en la grilla, con el punto ámbar de "pendiente de sincronizar" superpuesto — antes se veían como ícono genérico hasta sincronizar. Las evidencias antiguas ya sincronizadas (sin copia local, archivo borrado tras subir) siguen mostrando el ícono genérico como antes, comportamiento esperado.

---

### v1.9.9 — Stepper de horas rediseñado + botones de cámara (foto/video) iguales a los reportes de activos

**Fecha:** 2026-09-21

**Contexto:** dos mejoras pedidas sobre `SupervisorHomeScreen.kt`. (1) El stepper de horas por persona (+/-) se veía "muy simple" — 3 elementos sueltos (ícono, caja con borde, ícono) sin relación visual entre sí. (2) "Agregar foto o video" era un único botón que abría la galería del sistema — se pidió que use los mismos botones que ya existen en los reportes de activos (`ReportFormScreen.kt`/`EvidenceSection`): "Tomar foto", "Grabar video" y "Galería" por separado, con captura directa de cámara.

**Fix 1 — stepper de horas:** nuevo `HorasStepper` — un contenedor en pastilla (`SoftBackground`, `RoundedCornerShape(50)`) con los botones +/- como círculos blancos flotando adentro (`CardBackground`, `CircleShape`, íconos más pequeños) y el campo de texto sin borde propio en el medio (`OutlinedTextFieldDefaults.colors` con bordes/contenedor transparentes salvo al enfocar, que se pone naranja) — mantiene la edición manual del valor, solo cambia el estilo.

**Fix 2 — captura de foto/video:** se replicó el patrón exacto que ya usa el Inspector en sus reportes (`MainActivity.kt`: `ActivityResultContracts.TakePicture()`/`CaptureVideo()` sobre una `Uri` insertada en `MediaStore`, con el permiso de cámara pedido en el momento si hace falta) — pero ahora local a `ActividadCard` (no hoisted a `MainActivity`, ya que este módulo no sigue ese patrón para el picker de galería tampoco) y en su propia carpeta de `MediaStore` (`Pictures/ManTecSupervisor`/`Movies/ManTecSupervisor`) para no mezclar con la que ya usa el Inspector. La `Uri` resultante (de cámara o de galería) entra por el mismo `onSubirEvidencias()` de siempre — `encolarEvidenciaDesdeUri()` la copia a almacenamiento privado igual, sin importar el origen. Layout: "Tomar foto"/"Grabar video" como botones sólidos naranja lado a lado, "Galería" como botón de contorno naranja debajo (mismos colores que "Guardar registro" y "Galería" del Inspector).

**Archivos modificados:** `presentation/ui/SupervisorHomeScreen.kt`, `app/build.gradle.kts` (versionCode 21, versionName 1.9.9).

**Validación de esta sesión:** `gradlew assembleDebug`/`testDebugUnitTest` — `BUILD SUCCESSFUL`. Verificado en el emulador de punta a punta, no solo visualmente: se tomó una foto real con la cámara virtual del AVD (`adb shell uiautomator dump` para ubicar los botones exactos del flujo nativo de cámara — "Tomar foto" → captura → "Done"), se confirmó el toast "Evidencia guardada, se subirá cuando haya conexión." y una tercera miniatura con ícono de imagen (distinto de los íconos de video ya existentes) y punto ámbar de pendiente-de-sincronizar, igual que cualquier evidencia de galería. No se probó "Grabar video" de punta a punta en esta sesión (mismo contrato de Android que "Tomar foto", ya verificado funcionando) — queda como confirmación pendiente si se quiere.

---

### v1.9.8 — Visor de evidencia en modal (sin navegador) + fix de borde en botones Sí/No

**Fecha:** 2026-09-21

**Contexto:** dos correcciones sobre `SupervisorHomeScreen.kt`. (1) Tocar una evidencia abría el navegador del sistema (`Intent.ACTION_VIEW`) para mostrar la foto/video — sacaba al usuario de la app innecesariamente; se pidió un visor en modal, dentro de la app, cerrable. (2) Los botones "Sí"/"No" de "¿Todos trabajaron las horas programadas?" mostraban un trazo oscuro alrededor cuando estaban seleccionados (naranja), a diferencia de "Guardar registro" (limpio, sin trazo) — inconsistencia visual detectada por el usuario en captura de pantalla.

**Fix 1 — visor de evidencia:** nuevo `EvidenciaVisorDialog` (`Dialog` de Compose, `usePlatformDefaultWidth = false` para ocupar toda la pantalla) — fondo oscuro semitransparente, botón de cerrar (X) arriba a la derecha, se cierra tocando la X o fuera del contenido (`onDismissRequest`). Fotos con `AsyncImage` (Coil, ya usado para miniaturas desde v1.9.1); video con `VideoView` nativo de Android (vía `AndroidView`) con sus propios controles de reproducción (`MediaController`) — no se agregó ExoPlayer, `VideoView` alcanza para reproducir el video en modal sin dependencia nueva. El tap en una evidencia ahora guarda `EvidenciaVisorState(url, fileType, originalName)` en estado local en vez de lanzar un `Intent`.

**Fix 2 — borde de Sí/No:** `OutlinedButton` de Compose dibuja su borde por defecto encima del `containerColor`, incluso cuando está relleno de naranja — a diferencia de un `Button` normal (como "Guardar registro"), que no tiene borde. Se agregó `border = null` explícito cuando el botón está seleccionado (y se mantiene el borde gris normal cuando no lo está).

**Archivos modificados:** `presentation/ui/SupervisorHomeScreen.kt`, `app/build.gradle.kts` (versionCode 20, versionName 1.9.8).

**Validación de esta sesión:** `gradlew assembleDebug`/`testDebugUnitTest` — `BUILD SUCCESSFUL`. Verificado en el emulador con capturas: botón "No" seleccionado ya sin trazo oscuro; visor de evidencia abre en modal (fondo oscuro, X arriba a la derecha), el video se reproduce en vivo desde R2 dentro del modal, y se cierra correctamente al tocar la X, volviendo a la pantalla normal.

---

### v1.9.7 — Título simplificado + toggle Ayer/Hoy con estilo segmentado

**Fecha:** 2026-09-21

**Contexto:** dos ajustes de UX pedidos sobre "Mis actividades de hoy" (rebautizada). (1) El título ya no debe decir "de hoy" porque ahora la pantalla puede mostrar el día de ayer también (desde el toggle de v1.9.3) — queda solo "Mis actividades". (2) El toggle Hoy/Ayer usaba dos `OutlinedButton` sueltos con espacio entre ellos (parecían dos botones independientes); se pidió el mismo look que ya usa el selector Inspector/Supervisor de `LoginScreen.kt` (`LoginModeButton`): un solo contenedor con fondo suave y esquinas redondeadas, con el segmento activo como una pastilla naranja adentro. También se invirtió el orden: "Ayer" a la izquierda, "Hoy" a la derecha (antes era al revés).

**Cambio:** en `SupervisorHomeScreen.kt`, `DiaToggleButton` pasó de `OutlinedButton` a un `Surface` clickeable (mismo patrón exacto que `LoginModeButton`: `color = MantecOrange` si está seleccionado, `Color.Transparent` si no), envuelto en un `Row` con `.background(SoftBackground, RoundedCornerShape(14.dp)).padding(4.dp)` — el contenedor compartido que le da el look de segmented control. Título cambiado de "Mis actividades de hoy" a "Mis actividades".

**Archivos modificados:** `presentation/ui/SupervisorHomeScreen.kt`, `app/build.gradle.kts` (versionCode 19, versionName 1.9.7).

**Validación de esta sesión:** `gradlew assembleDebug` — `BUILD SUCCESSFUL`. Verificado visualmente en el emulador con captura de pantalla: título correcto, "Ayer" a la izquierda y "Hoy" a la derecha (seleccionado, pastilla naranja) dentro de un único contenedor — coincide con la referencia visual que se pidió replicar.

---

### v1.9.6 — Fix: Hoy/Ayer debe clasificarse en hora de Colombia, no la del dispositivo

**Fecha:** 2026-09-21

**Contexto:** el usuario recordó explícitamente "la hora es la de Colombia" justo después de que en la verificación visual de v1.9.5 se observara que el reloj del emulador de pruebas había cruzado medianoche — y al revisar ese detalle salió a la luz que el emulador corría con su reloj en **GMT**, no en hora de Colombia. Esto expuso un bug real, no solo un detalle de la prueba: `todayDateString()`/`yesterdayDateString()` (agregadas en v1.9.3 para el toggle Hoy/Ayer) usaban `Calendar.getInstance()`, basado en la zona horaria configurada en el **dispositivo**, no en Colombia. El backend Laravel, en cambio, fija `'timezone' => 'America/Bogota'` en `config/app.php` — todo "hoy"/"ayer" del lado servidor está calculado ahí, sin importar dónde corra el servidor. Si el teléfono de un supervisor estuviera mal configurado (o, como en este caso, un emulador de prueba en GMT), la clasificación Hoy/Ayer del cliente divergería de la del servidor durante las ~5 horas al día donde Bogotá (UTC-5) y la otra zona no coinciden en la fecha — siempre alrededor de la medianoche.

**Fix:** `todayDateString()`/`yesterdayDateString()` ahora anclan explícitamente a `TimeZone.getTimeZone("America/Bogota")` tanto para el cálculo (`Calendar.getInstance(bogotaZone)`) como para el formato (`SimpleDateFormat.timeZone`), sin importar la zona horaria del dispositivo. Además, ambas funciones ganaron un parámetro opcional `nowMillis: Long = System.currentTimeMillis()` (no rompe ningún call site existente) específicamente para poder fijar un instante exacto en tests.

**Tests nuevos:** `SupervisorActivityDateTest` (2 tests) — el caso central fija el instante `2026-09-21 02:00:00 UTC` (que es `2026-09-20 21:00:00` en Bogotá: ya "mañana" en UTC/GMT pero todavía "hoy" en Colombia) y confirma que `todayDateString()` devuelve `"2026-09-20"` — este test habría fallado con el código de antes del fix en cualquier máquina/CI configurada en UTC, exactamente el escenario que expuso el bug.

**Archivos modificados:** `presentation/viewmodel/SupervisorActivityViewModel.kt`, `app/build.gradle.kts` (versionCode 18, versionName 1.9.6).

**Archivos nuevos:** `app/src/test/java/.../presentation/viewmodel/SupervisorActivityDateTest.kt`.

**Validación de esta sesión:** `gradlew testDebugUnitTest` — **19/19 tests pasando** (16 previos + 2 nuevos de zona horaria + el stub por defecto), confirmado leyendo los XML de resultado. `gradlew assembleDebug` — `BUILD SUCCESSFUL`.

---

### v1.9.5 — Refresco periódico de la URL firmada de evidencia

**Fecha:** 2026-09-20/21

**Contexto:** última limitación conocida de las 6 que quedaban pendientes para el módulo Supervisor (numeral 5): la miniatura real de evidencia (v1.9.1) pedía la URL firmada de R2 una sola vez al mostrar el tile; esa URL vence a los 10 minutos (`Api\Personal\ActivityEvidenceController::show()`), así que si la pantalla quedaba abierta más tiempo, la miniatura podía dejar de cargar sin que nada la refrescara. Los numerales 4 (prueba en dispositivo físico real / build de release) y 6 (commit del fix v1.7.8 de Mediciones) quedan a criterio del usuario, no son cambios de código.

**Fix:** en `SupervisorHomeScreen.kt`, el `LaunchedEffect` que pide la URL de miniatura pasó de una sola petición a un bucle (`while (true) { ...; delay(8 min) }`) que se repite cada 8 minutos — por debajo del TTL real de 10 — mientras el tile de evidencia siga en pantalla. Se cancela solo cuando Compose saca el tile de composición (se cierra la actividad, se hace scroll fuera de la lista, etc.), sin necesidad de limpieza manual. Sigue siendo "best effort": si no hay red en ese ciclo, simplemente no se refresca esa vuelta y se reintenta en el próximo.

**Archivos modificados:** `presentation/ui/SupervisorHomeScreen.kt`, `app/build.gradle.kts` (versionCode 17, versionName 1.9.5).

**Validación de esta sesión:** `gradlew assembleDebug` — `BUILD SUCCESSFUL`. Reinstalado en el emulador y verificado que la app sigue abriendo, navegando y expandiendo tarjetas sin error tras el cambio (smoke test visual, no se esperó los 8 minutos para confirmar el segundo ciclo de refresco en esta sesión). De paso, este smoke test confirmó algo útil: el reloj real del emulador cruzó medianoche durante la sesión (pasó a 2026-09-21), y la clasificación Hoy/Ayer se ajustó sola correctamente (la actividad de "hoy" pasó a mostrarse bajo "Ayer", y el toggle desapareció porque ya solo quedaba un día con datos) — confirma en vivo, sin proponérselo, que la lógica de fecha real programada funciona en el cruce de día real, no solo en pruebas con fecha fija.

**Pendiente:** de los 6 puntos originales para "100%", cierran 1, 2, 3 y 5 en esta sesión. Quedan 4 (prueba en dispositivo físico/release) y 6 (commit de v1.7.8) del lado del usuario.

---

### v1.9.4 — Prueba en modo avión, fix del logout global por 401 en background, y primeros tests automatizados

**Fecha:** 2026-09-20

**Contexto:** el usuario pidió cerrar 3 de los 6 puntos pendientes que quedaban para poder decir que el módulo Supervisor está "100%": (1) la prueba real en modo avión del flujo offline, nunca ejecutada; (2) el bug de "HTTP 401 Unauthorized" reportado semanas atrás y nunca root-causeado; (3) la ausencia total de pruebas automatizadas en el módulo.

**1. Prueba en modo avión — ejecutada y verificada de punta a punta.** En el emulador: `adb shell svc wifi disable` (único transporte de red del AVD; `dumpsys connectivity` confirmó 0 redes activas), se abrió la app (lectura local-first sin error), se escribió un comentario de prueba y se guardó un registro, se agregaron 2 evidencias (una foto y un video) — todo mostró el toast correcto ("...guardado"/"...se subirá cuando haya conexión") y el badge ámbar "Sin sincronizar" en la actividad y en cada evidencia, sin ningún error visible. Se reactivó la red (`svc wifi enable`), se tocó el botón de sincronizar manual, y los badges de "sin sincronizar" desaparecieron. Se verificó directamente contra la base de datos Postgres del backend (no solo la UI) que los datos realmente llegaron: `comments = "PRUEBA_OFFLINE_20260920"`, `all_worked_scheduled_hours = true`, y las 2 evidencias con su path real en R2 bajo `personal-actividades/argos-1/2026/actividad-31/...`. Cierra el único punto que nunca se había probado del diseño offline original.

**2. Bug de "HTTP 401 Unauthorized" — causa raíz encontrada y corregida.** Investigando `AuthInterceptor.kt` se encontró que **cualquier** respuesta 401 de **cualquier** llamada a la API (Inspector o Supervisor) dispara `TokenExpirationEvent.emit()`, que en `MainActivity.kt` fuerza un logout global inmediato (`authRepository.logout()` + navegación a Login) — sin distinguir si la llamada vino de una acción en primer plano (el usuario mirando la pantalla) o de un sync silencioso en segundo plano disparado por `SyncWorker`/`SupervisorSyncWorker` (WorkManager periódico, cada 15 min, puede correr con la pantalla apagada). Esto significa que un 401 transitorio durante un sync en background podía sacar al usuario a la pantalla de Login sin ningún aviso, en medio de lo que estuviera haciendo — encaja con el síntoma original reportado ("cree una actividad para hoy, y mira... necesitamos un botón de refresh?"). Nota honesta: no fue posible reproducir el 401 original exacto (el disparador más probable, ya cerrado estructuralmente en v1.8.0, era el `BASE_URL` hardcodeado que había que editar a mano para probar en local — un login contra un backend y una acción posterior contra otro habría producido justo este síntoma; con `BuildConfig.BASE_URL` por `buildType` ese escenario específico ya no puede repetirse). Independientemente de la causa original, el gap de arquitectura sí es real y se corrigió: `AuthInterceptor` ahora acepta `emitExpirationEvent: Boolean`, y `RetrofitClient.createSyncApiService()`/`createMeasurementApiService()`/`createPersonalApiService()` ganan un parámetro `background: Boolean = false` que lo desactiva. `SyncWorker.kt` y `SupervisorSyncWorker.kt` (ambos, mismo riesgo compartido) ahora piden sus servicios de API con `background = true` — un 401 en un sync silencioso ya no fuerza un logout global; solo lo sigue haciendo un 401 en una acción explícita en primer plano (login, guardar, sincronizar manual), donde sí es correcto avisarle al usuario de inmediato.

**3. Primeros tests automatizados del proyecto.** El proyecto no tenía ningún test real (solo los stubs por defecto de Android Studio). Se agregó MockK + `kotlinx-coroutines-test` (`testImplementation`, sin tocar dependencias de producción) y se escribieron 16 tests JVM nuevos, enfocados en la lógica más crítica de esta sesión:
- `SupervisorSyncRepositoryTest` (11 tests) — cubre la máquina de estados `PENDING_SYNC → SYNCED/ERROR` completa: push de registro exitoso/403/422/500/excepción de red, push de evidencia exitoso (incluye que borre el archivo local)/422/archivo-local-perdido, y el merge de refresh (actividad nueva se inserta `SYNCED`; una con registro `PENDING_SYNC` no se pisa; una ya sincronizada sí se actualiza). Se prueba a través de la API pública (`sync()`/`refreshDesdeServidor()`) verificando las llamadas a los DAOs con MockK, sin abrir visibilidad de métodos privados solo para testear.
- `SupervisorHomeScreenUtilsTest` (5 tests) — cubre `formatHoras()`: enteros sin decimales, medios puntos, redondeo de ruido de punto flotante tras sumas repetidas de 0.5 (el caso real del stepper +/-), precisión arbitraria, valores negativos.
- Se encontró y corrigió en el camino un gotcha estándar de Android: los unit tests JVM no tienen `android.util.Log` real y lanzaban `RuntimeException` en cualquier log — se agregó `testOptions { unitTests.isReturnDefaultValues = true }`, configuración de solo-test que no afecta producción.

**Archivos nuevos:** `app/src/test/java/.../data/repository/SupervisorSyncRepositoryTest.kt`, `app/src/test/java/.../presentation/ui/SupervisorHomeScreenUtilsTest.kt`.

**Archivos modificados:** `AuthInterceptor.kt`, `RetrofitClient.kt`, `sync/SyncWorker.kt`, `sync/SupervisorSyncWorker.kt`, `SupervisorHomeScreen.kt` (`formatHoras` de `private` a `internal` para poder testearla), `gradle/libs.versions.toml` + `app/build.gradle.kts` (MockK, coroutines-test, `testOptions`, versionCode 16, versionName 1.9.4).

**Validación de esta sesión:** `gradlew testDebugUnitTest` — **16/16 tests pasando, 0 fallos** (confirmado leyendo los XML de resultado, no solo el resumen de consola). `gradlew assembleDebug` — `BUILD SUCCESSFUL`. Prueba en modo avión ejecutada en emulador real contra backend y Postgres reales (detalle arriba). El fix del 401 se probó por compilación y por revisión de código; **no se reprodujo un 401 real post-fix** porque no se pudo forzar uno de forma controlada en esta sesión — la corrección está BIEN fundamentada en el código leído, pero queda como una mejora de resiliencia verificada por análisis, no por reproducción directa del bug original.

**Pendiente:** de los 6 puntos que quedaban para decir "100%", cierran 1, 2 y 3 en esta ronda. Siguen abiertos: (4) prueba en dispositivo físico real y en build de release — todo lo de hoy se probó solo en emulador AVD, en debug; (5) la URL firmada de evidencia sigue venciendo a los 10 min sin refresco automático si la pantalla queda abierta más tiempo; (6) el fix de v1.7.8 en Mediciones de Espesor sigue sin commitear.

---

### v1.9.3 — Toggle Hoy/Ayer para actividades nocturnas

**Fecha:** 2026-09-20

**Contexto:** feature pendiente desde antes de la ronda offline (pausada a pedido explícito del usuario: "Offline completo ya, antes de seguir con otras mejoras" — ver `API_SUPERVISOR.md` y `OFFLINE_SUPERVISOR.md` sección 12). El pedido original: mostrar un botón arriba para elegir entre actividades de "Ayer" y "Hoy"; si no hay de hoy, mostrar solo las de ayer y que sea evidente; si no hay de ayer, mostrar solo las de hoy. Precisión del usuario en esta ronda: "una nocturna cuenta con la fecha real de programada" — la clasificación Hoy/Ayer se hace comparando `actividad.date` (la fecha real programada de cada actividad, ya presente en el modelo) contra la fecha real de hoy/ayer del dispositivo, nunca infiriendo nada a partir del turno.

**Cambio:** no hizo falta tocar el backend ni la sincronización — `GET api/personal/actividades` ya devuelve, en una sola respuesta, las actividades de hoy más cualquier turno Nocturno de ayer (`Api\Personal\ActivityController::index()`, ver `API_SUPERVISOR.md` sección 2), y esa unión ya se cachea completa en Room. Todo el trabajo fue de UI/estado:

- `SupervisorActivityViewModel.kt`: nuevo enum `DiaActividad` (`HOY`/`AYER`). `SupervisorHomeUiState` gana `actividadesHoy`, `actividadesAyer` (ambas derivadas de `actividades` comparando el campo `date` de cada una contra la fecha real de hoy/ayer calculada con `Calendar`/`SimpleDateFormat`, mismo formato `yyyy-MM-dd` que usa el backend) y `diaSeleccionado`. `recargarDesdeRoom()` recalcula ambas listas en cada refresh y decide el día por defecto: mantiene la selección actual si todavía tiene datos, si no cae al otro día que sí tenga, y si ninguno tiene, por defecto `HOY`. Nuevo método `seleccionarDia()` para el toggle manual.
- `SupervisorHomeScreen.kt`: nuevo `DiaToggleButton` (mismo criterio visual que el "Sí/No" de horas — relleno naranja si seleccionado, contorno si no), en una fila debajo del encabezado que **solo aparece si hay actividades en ambos días**. Cuando `diaSeleccionado == AYER`, un banner ámbar fijo dice "Mostrando actividades de AYER (turno nocturno pendiente)" — visible incluso cuando no hay toggle porque hoy no tenía actividades, para que nunca sea ambiguo qué día se está viendo.
- `MainActivity.kt`: pasa los 3 campos nuevos de `uiState` y el callback `onSeleccionarDia` a `SupervisorHomeScreen`.

**Bug encontrado y corregido durante la verificación visual en emulador** (no solo compilado — probado de verdad): el primer intento anidó por error la fila del toggle dentro del mismo `Surface` que el encabezado sin un `Column` contenedor, y como el slot de contenido de `Surface` apila a los hijos como un `Box` en vez de uno debajo del otro, el toggle quedó dibujado *encima* del encabezado (título y botones de sincronizar/salir superpuestos e ilegibles). Se corrigió envolviendo encabezado + toggle en un `Column` dentro del `Surface`. Se aprovechó la misma sesión de prueba para corregir otro problema menor de la ronda anterior (v1.9.1): la miniatura real de evidencia dejaba el tile en blanco mientras la imagen cargaba o si fallaba la carga (el ícono genérico y la miniatura eran mutuamente excluyentes); ahora el ícono se pinta siempre como fondo y la miniatura se superpone encima solo si termina de cargar, así nunca se ve un tile vacío.

**Archivos modificados:** `presentation/viewmodel/SupervisorActivityViewModel.kt`, `presentation/ui/SupervisorHomeScreen.kt`, `MainActivity.kt`, `app/build.gradle.kts` (versionCode 15, versionName 1.9.3).

**Validación de esta sesión:** `gradlew assembleDebug` — `BUILD SUCCESSFUL`. **Probado en el emulador contra datos reales** (backend local + R2 real, empleado de prueba `lfdo` con una actividad de hoy Diurno y una actividad Nocturno fechada ayer): capturas de pantalla confirmaron el toggle cambiando de apariencia, el banner de "ayer" apareciendo, el filtrado correcto por día, las miniaturas reales de evidencia cargando desde R2, y el stepper de horas mostrando "12" / "12.5" correctamente. Encontrado y corregido en el momento un bug de layout real (ver arriba) que no se habría detectado solo compilando.

**Pendiente:** con esto, el toggle Hoy/Ayer queda resuelto — ya no está en la lista de pendientes. Sigue abierto el bug de "HTTP 401 Unauthorized" (pausado, causa raíz del lado cliente aún sin identificar) y la prueba manual en modo avión del flujo offline completo.

---

### v1.9.2 — Acordeón: solo una actividad expandida a la vez

**Fecha:** 2026-09-20

**Contexto:** en "Mis actividades de hoy", cada tarjeta manejaba su propio estado de expandido/contraído de forma independiente — se podían tener varias actividades abiertas a la vez, lo que confundía sobre en cuál se estaba editando el registro (comentarios/horas/evidencia). Pedido explícito: al abrir una actividad, cualquier otra que estuviera abierta debe contraerse sola.

**Cambio:** el estado de "cuál actividad está expandida" se subió de `ActividadCard` (estado local `var abierta by remember { ... }`) a `SupervisorHomeScreen` (`var actividadExpandidaId by remember { mutableStateOf<Long?>(null) }`, un solo id o `null`). Cada tarjeta ahora recibe `abierta: Boolean` y `onToggleAbierta: () -> Unit` desde el padre; el toggle en el padre simplemente asigna `actividadExpandidaId` al id tocado (o `null` si ya era ese mismo, para poder volver a cerrarla) — como solo hay una variable de estado para las N tarjetas, abrir una dejando `actividadExpandidaId` distinto del id de cualquier otra automáticamente las contrae a todas.

**Archivos modificados:** `presentation/ui/SupervisorHomeScreen.kt` (`app/build.gradle.kts` versionCode 14, versionName 1.9.2).

**Validación de esta sesión:** `gradlew assembleDebug` — `BUILD SUCCESSFUL`. No probado todavía manualmente en emulador/dispositivo.

---

### v1.9.1 — Miniaturas reales de evidencia + stepper de horas por 0.5

**Fecha:** 2026-09-20

**Contexto:** dos mejoras de UX pedidas sobre la pantalla "Mis actividades de hoy" (`SupervisorHomeScreen.kt`) del rol Supervisor.

1. **Miniatura real de evidencia**: la grilla de evidencias mostraba solo un ícono genérico de imagen/video (ver pendiente ya señalado en `API_SUPERVISOR.md` sección 7). Ahora, para evidencia de tipo foto que ya está `SYNCED` (tiene `serverId`) y solo si el dispositivo tiene conexión (`NetworkUtils.hasInternet`), se pide la URL firmada de R2 (misma que ya se usaba para abrir la evidencia al tocarla) y se muestra como miniatura real con Coil (`AsyncImage`). Es deliberadamente "best effort": si no hay red, si la evidencia todavía no se sincronizó, o si la carga falla, se mantiene el ícono genérico de siempre — no bloquea ni rompe nada. Video sigue mostrando siempre su ícono. Se agregó `io.coil-kt:coil-compose:2.7.0` como dependencia nueva (antes el proyecto no tenía ninguna librería de carga de imágenes).
   - Limitación conocida, no resuelta en esta ronda: la URL firmada vence a los 10 minutos (ver `API_SUPERVISOR.md` sección 2); si la pantalla queda abierta más tiempo, esa miniatura puntual podría dejar de cargar hasta la próxima recomposición de esa fila. No se implementó refresco periódico de la URL por ser un caso extremo de bajo impacto.
2. **Stepper de horas por persona**: el campo de horas trabajadas por persona ganó dos botones (+/-) que suman/restan 0.5 horas, además de seguir siendo editable a mano. El valor se formatea con una función nueva `formatHoras()`: si es un número entero se muestra sin decimales ("12" en vez de "12.0"); si no, se muestra con su parte decimal ("12.5"). Se redondea a 2 decimales en cada paso para no arrastrar ruido de punto flotante tras varios +/- seguidos.

**Archivos modificados:** `presentation/ui/SupervisorHomeScreen.kt` (ambas mejoras), `gradle/libs.versions.toml` y `app/build.gradle.kts` (dependencia de Coil), `app/build.gradle.kts` (versionCode 13, versionName 1.9.1). `API_SUPERVISOR.md` actualizado para marcar el pendiente de miniaturas como resuelto.

**Validación de esta sesión:** `gradlew assembleDebug` — `BUILD SUCCESSFUL`, sin errores (solo warnings preexistentes no relacionados: íconos deprecados en otras pantallas, falta de índice en dos junction entities ya existentes). No se probó todavía manualmente en emulador/dispositivo — queda pendiente confirmar visualmente la miniatura real contra R2 real y el comportamiento del stepper en pantalla.

---

### v1.9.0 — Offline-first para el rol Supervisor

**Fecha:** 2026-09-20

**Contexto:** ver `OFFLINE_SUPERVISOR.md` (documento de diseño completo). El módulo Supervisor de v1.8.0 era 100% online; el usuario pidió construir el soporte offline completo antes de seguir con otras mejoras. Se replicó tal cual el patrón `PENDING_SYNC → SYNCED` (+ `ERROR`) que ya usa el Inspector, documentado en `PATRONES_ASINCRONISMO_OFFLINE.md`, en vez de inventar uno nuevo.

**Archivos nuevos:** `data/local/SupervisorActivityEntity.kt`, `SupervisorPersonaEntity.kt`, `SupervisorEvidenceEntity.kt`, `SupervisorActivityDao.kt`, `SupervisorPersonaDao.kt`, `SupervisorEvidenceDao.kt`, `SupervisorMigrations.kt`, `data/repository/SupervisorSyncRepository.kt`, `sync/SupervisorSyncWorker.kt`, `sync/SupervisorSyncWorkManager.kt`.

**Archivos modificados:** `AppDatabase.kt` (entidades nuevas, `version = 20`, `exportSchema = true`), `DatabaseProvider.kt` (`MIGRATION_19_20`), `PersonalActivityLocalRepository.kt` (rewrite — ahora Room-only, copia evidencia a almacenamiento privado antes de encolarla), `PersonalActivityRepository.kt` (recortado a solo acciones online: URL firmada y borrado remoto), `PersonalApiService.kt` (`saveActividad`/`uploadEvidencias` devuelven `Response<T>` para poder distinguir por código HTTP), `SupervisorActivityViewModel.kt` (+ Factory, rewrite completo), `SupervisorHomeScreen.kt` (badges de estado de sincronización), `MainActivity.kt` (instancias nuevas + mismos puntos de disparo de sync que el Inspector, rama paralela por `roleKey == "supervisor"`), `app/build.gradle.kts` (`ksp room.schemaLocation`; versionCode 12, versionName 1.9.0).

**Validación de esta sesión:** `gradlew assembleDebug` — `BUILD SUCCESSFUL`. La migración `19 → 20` se verificó contra un dispositivo real con datos preexistentes del Inspector (no solo revisada en código): se extrajo la base de datos real de un emulador (`PRAGMA user_version` 19, `integrity_check` ok, 7 reports/7 report_details/10 measurement_thickness_drafts/53 elements/171 cross-refs), se instaló el APK actualizado **sobre** la instalación existente, se abrió la app sin crash ni excepción de migración, y se volvió a extraer la base de datos: `user_version` 20, `integrity_check` ok, las 3 tablas `supervisor_*` nuevas presentes, y **los mismos conteos exactos** en todas las tablas preexistentes del Inspector — confirmando que la migración no destruye datos reales. Detalle completo, incluyendo una nota de tooling sobre corrupción de datos binarios al extraer la DB vía PowerShell (resuelto usando Git Bash), en `OFFLINE_SUPERVISOR.md` sección 2.

**Pendiente:** prueba manual en modo avión (guardar + subir evidencia sin conexión, confirmar reconexión y sync) — no ejecutada en esta sesión, ver `OFFLINE_SUPERVISOR.md` sección 12. El toggle Hoy/Ayer y el bug de "HTTP 401 Unauthorized" siguen pausados, a pedido explícito del usuario, para la siguiente ronda.

---

### v1.8.0 — Rol nuevo "Supervisor": login Employee + actividades + evidencia a R2

**Fecha:** 2026-09-20

**Contexto:** ver `API_SUPERVISOR.md` (documento de diseño completo) y, del lado backend, `NUEVA_FUNCIONALIDAD_PERSONAL_Y_PROGRAMACION.md` sección 14.24 (repo Laravel). En resumen: la app gana un segundo rol, "Supervisor" (modelo `Employee` del backend, login separado del Inspector vía `api/personal/login`), para registrar desde el celular comentarios + horas trabajadas por actividad/persona, y subir evidencia foto/video a Cloudflare R2 — repitiendo lo que hasta ahora solo se probaba en la pantalla web "Ver como" del panel admin.

**Archivos nuevos:** `data/remote/personal/PersonalAuthDtos.kt`, `data/remote/personal/PersonalActivityDtos.kt`, `data/remote/personal/PersonalApiService.kt`, `data/repository/PersonalAuthRepository.kt`, `data/repository/PersonalActivityRepository.kt`, `presentation/viewmodel/SupervisorLoginViewModel.kt` (+ Factory), `presentation/viewmodel/SupervisorActivityViewModel.kt` (+ Factory), `presentation/ui/SupervisorHomeScreen.kt`.

**Archivos modificados:** `app/build.gradle.kts` (BASE_URL ahora por `buildType` vía `buildConfigField`, ya no una constante editada a mano — evita el riesgo ya vivido en v1.6.3 de dejarla apuntando al emulador en un commit real), `RetrofitClient.kt`, `AppScreen.kt`, `AppNavigationViewModel.kt`, `LoginScreen.kt` (selector Inspector/Supervisor), `MainActivity.kt` (rama nueva `roleKey == "supervisor"` en los puntos donde ya existía `roleKey != "inspector"`, sin refactorizar esa duplicación ya señalada en `LOGIN_Y_ROLES.md`).

**Validación de esta sesión:** `gradlew assembleDebug` — `BUILD SUCCESSFUL`, sin errores ni warnings. El backend (login, actividades, evidencia a R2) ya estaba probado end-to-end con `curl` real y 20 tests automatizados antes de tocar Android (ver `NUEVA_FUNCIONALIDAD_PERSONAL_Y_PROGRAMACION.md` 14.24). **No probado todavía en emulador/dispositivo real** — ver `API_SUPERVISOR.md` sección 6/7 para el detalle de qué queda pendiente.

---

### v1.7.8 — Fix valores de medición "heredados" del activo anterior en Mediciones de Espesor

**Fecha:** 2026-08-27

**Archivos modificados:** `MeasurementThicknessScreen.kt`, `MeasurementThicknessViewModel.kt`

**Contexto:** el equipo de planta reportó que, al terminar de diligenciar un activo en el módulo de Mediciones y pasar al siguiente, un activo **nuevo** (sin ningún borrador previo, ni local ni remoto) aparecía con los valores de espesor/dureza del activo anterior en vez de en blanco. Esto obligaba a borrar manualmente los campos antes de poder registrar el nuevo activo, retrasando la revisión en campo. Se reportó puntualmente en el área de Apilado de Aditivos.

**Investigación:** se revisó toda la cadena de datos antes de tocar código, para descartar que fuera un problema de sincronización o de catálogo desactualizado (como el caso de v1.7.7):
- `MeasurementThicknessRepository` y `MeasurementThicknessDao` filtran y aíslan correctamente por `elementId` en cada consulta (`WHERE elementId = :elementId`), y el borrador tiene `elementId` como `@PrimaryKey`. Un activo nuevo sin borrador efectivamente llega en blanco (`newEmptyLine()`, todos los campos `null`) una vez que termina de cargar.
- **Conclusión inicial (parcial):** no era un bug del backend ni de Room — el dato final correcto (en blanco) sí llegaba. No era exclusivo de ninguna área en particular.

**Causa raíz — dos causas combinadas, no una sola:**
1. **UI (Compose), en `NumberField`:** el `forEach` que dibuja las cubiertas no asignaba una `key` de Compose ni por cubierta ni por activo. Compose identifica cada composable por su posición en el árbol, no por el activo al que pertenecen sus datos — así que al cambiar de activo, el campo de texto de "Cubierta 1 → Izq." seguía siendo *el mismo* composable físico que en el activo anterior, y su `rememberSaveable var text` no se reiniciaba. Arrastrado desde v1.7.1 (cambio deliberado de `remember(value)` a `rememberSaveable` sin key para otro bug, nunca probado con la secuencia de cambiar de activo).
2. **ViewModel, en `MeasurementThicknessViewModel.selectElement()` — la causa determinante:** a diferencia de `selectArea()` y `selectElementType()` (que sí limpian `draft = null, lines = emptyList()` de inmediato al iniciar la selección), `selectElement()` actualizaba `selectedElementId` de forma síncrona pero dejaba `lines`/`draft` intactos hasta que terminara la llamada de red (`refreshThicknessState()`). Esto generaba un fotograma intermedio real, no solo teórico, donde el `elementId` ya era el del activo nuevo pero `lines` todavía tenía los valores del activo anterior. Verificado en vivo: aplicar *solo* el fix de `key()` (causa 1) no bastó — al probar en el emulador, cambiar de activo seguía mostrando los valores del anterior, porque ese fotograma intermedio alcanzaba a "sembrar" el valor viejo en el composable recién creado antes de que llegaran los datos reales en blanco.

**Fix aplicado (ambas partes, la combinación fue necesaria):**
- `MeasurementThicknessScreen.kt`: cada `ThicknessLineCard` se envuelve en `key(elementId, line.coverNumber)` dentro de `ThicknessLinesSection`, para que Compose trate cada combinación activo+cubierta como una identidad distinta.
- `MeasurementThicknessViewModel.kt`: `selectElement()` ahora limpia `draft = null, lines = emptyList()` en la misma actualización síncrona donde fija `selectedElementId`, igual que ya hacían `selectArea()`/`selectElementType()`. Así el primer fotograma renderizado para el activo nuevo ya no tiene datos del activo anterior que filtrar.

**Alcance del cambio — por qué no afecta el trabajo offline:** ambos archivos tocados son de presentación/orquestación de estado en el cliente. No se modificó `MeasurementThicknessRepository`, `MeasurementThicknessDao` ni ningún endpoint de `MeasurementApiService` — el patrón local-first (Room primero, sincronización en segundo plano) y el guardado de borradores `PENDING_SYNC` sin conexión siguen funcionando exactamente igual. Verificado explícitamente en el emulador con WiFi y datos móviles deshabilitados (ver Verificación).

**Verificación (2026-08-27), en emulador real (Android 15, AVD `Medium_Phone_API_35`), sesión y catálogo reales del cliente CORONA:**
- `gradlew compileDebugKotlin --rerun-tasks` y `assembleDebug` — `BUILD SUCCESSFUL`, sin errores nuevos.
- **Con red (WiFi):** en el área Apilado de Aditivos, se cargó `K11BT02`/`K11BT03` (activos con borrador remoto real) y se confirmó que cada uno muestra sus propios valores de Cubierta 1 (no los de otro). Se detectó en este paso que el fix de `key()` por sí solo *no* resolvía el problema (`K11BT01` heredaba los valores exactos de `K11BT03`), lo que llevó a encontrar la causa raíz real en el ViewModel (arriba).
- **Después del fix del ViewModel, con red:** `K11BT01` cargó sus propios valores reales (6.75/6.4/6.8...), distintos a los de `K11BT02`/`K11BT03` — confirma que el fotograma intermedio ya no contamina el estado.
- **Sin red (`adb shell svc wifi disable` + `svc data disable`), reproduciendo el escenario exacto reportado:** con `K11BT01` cargado (con sus valores reales visibles), se cambió a `K21BT01` — un activo del mismo área nunca antes visitado, sin borrador local ni remoto. Los nueve campos de Cubierta 1 (superior, inferior, dureza) aparecieron completamente en blanco, sin rastro de los valores de `K11BT01`, junto con el mensaje correcto "Sin conexión. Puedes crear un borrador local para este activo." No se guardó ningún borrador durante la prueba.

---

### v1.7.7 — Fix Condición offline + auditoría de completitud de catálogo + simplificación de íconos de Área

**Fecha:** 2026-08-05

**Archivos modificados:** `CatalogViewModel.kt`, `CatalogLocalRepository.kt`, `CatalogCompletenessResult.kt` (nuevo), `DashboardUiState.kt`, `DashboardViewModel.kt`, `MainActivity.kt`, `HomeScreen.kt`, `ReportFormScreen.kt`, `app/build.gradle.kts`

**Contexto:** un inspector en campo (cliente CORONA) reportó no poder ver las opciones de **Condición** para un componente puntual estando sin conexión, aunque Área/Elemento/Componente/Diagnóstico sí se veían con normalidad (ver foto del reporte de campo). Investigación conjunta con el equipo de backend descartó un bug de query en `offline-catalog` — las tres fuentes comparadas (`offline-catalog`, `GET /elements/{id}/conditions`, `GET /components/{id}/diagnostics`) coincidían exactamente para el caso reportado. La causa más probable fue un catálogo local **desactualizado**: el dispositivo bajó su `offline-catalog` antes de que esa relación Componente↔Condición existiera en el backend, y nunca volvió a refrescarlo con éxito por trabajar casi siempre en zona de señal baja (planta industrial), con WiFi solo disponible al inicio de turno.

**Cambio 1 — Condición local-first (`CatalogViewModel.kt`):**
- `loadConditionsForComponent()` intentaba el servidor **primero** y solo caía al catálogo local de Room si esa llamada fallaba (por ejemplo, sin red). El `connectTimeout` de Retrofit es de 20s (`RetrofitClient.kt`), así que cada vez que un inspector tocaba un Componente sin señal, la UI se colgaba hasta ese timeout antes de mostrar Condición.
- Se invirtió el orden: ahora pinta primero desde Room (instantáneo, funciona sin red) y refresca del servidor en segundo plano solo como mejora — si el refresco falla, se queda con el catálogo local sin bloquear ni mostrar error. Mismo patrón *local-first* ya usado en `DashboardViewModel` desde v1.7.6 para los íconos de estado semanal.

**Cambio 2 — Auditoría de completitud de catálogo + banner en HomeScreen:**
- `CatalogLocalRepository.auditCatalogCompleteness(groupId)` (nuevo): recorre localmente, sin red, todos los elementos del grupo del inspector y detecta componentes sin diagnósticos o sin condiciones asociadas en Room. Devuelve `CatalogCompletenessResult` (nuevo archivo `CatalogCompletenessResult.kt`).
- `DashboardViewModel.checkCatalogCompleteness(groupId, tryRepairIfIncomplete)` (nuevo): corre la auditoría; si encuentra huecos, intenta repararlos llamando `RemoteCatalogRepository.syncOfflineCatalog()` (solo si hay red — si falla, se captura sin bloquear) y vuelve a auditar. Expone el resultado vía `CatalogCompletenessUi`/`CatalogCompletenessStatus` en `DashboardUiState`.
- Se dispara automáticamente al entrar a Home (`MainActivity.kt`, nuevo `LaunchedEffect(currentScreen, profile.groupId)`), sin requerir un nuevo login — usa la sesión ya guardada, igual que el resto del auto-sync existente.
- `HomeScreen.kt` muestra un banner: verde "Catálogo completo y actualizado. Podés trabajar sin conexión." cuando todo está OK, rojo con el conteo de componentes afectados si encuentra huecos que no pudo reparar (por ejemplo, sin WiFi en ese momento), y un estado "verificando" mientras corre.
- **Objetivo:** que el inspector vea, con el WiFi de inicio de turno en la fábrica, si su catálogo está completo *antes* de salir a la zona de señal baja, en vez de descubrir un hueco recién frente al activo.
- **Alcance conocido de la auditoría:** cubre específicamente relaciones Componente↔Diagnóstico y Componente↔Condición. No audita otros eslabones de la cadena (por ejemplo, un Área sin Elementos, o un Elemento sin Componentes). Tampoco puede reparar huecos cuyo origen sea el propio backend (dato que nunca existió del lado servidor) — en ese caso el banner rojo queda persistente hasta que se corrija la fuente, que es el comportamiento esperado (avisar, no inventar datos).

**Cambio 3 — Simplificación del ícono de estado por Área (`ReportFormScreen.kt`):**
- Se eliminó el badge naranja "P" (pendiente/parcial) del picker de **Área**, agregado en la adenda de v1.7.6. Los inspectores pidieron que ya no se muestre ese indicador de avance parcial a nivel Área — ahora solo se ve el ✅ verde cuando el área está **100% completa** (todos sus activos con expectativa esta semana en `DONE`), y ningún ícono en cualquier otro caso (vacía o parcialmente diligenciada). El badge "P" se mantiene sin cambios en los niveles Activo, Componente y Diagnóstico.
- Se eliminó `localPendingAreaIds` (cálculo que ya no se usa) y se dejó de pasar `pendingSyncIds` al `ProgressiveDropdownField` del picker de Área.

**Hallazgo de backend relacionado, no corregido en esta versión (pendiente para el equipo backend):** `InspectorOfflineCatalogController::show()` exige exactamente una agrupación activa por inspector (422 si tiene 0 o ≥2), mientras que los endpoints puntuales validan contra todas las agrupaciones del inspector. Hoy ningún inspector tiene 2+ agrupaciones activas, así que no genera síntomas actualmente, pero es una asimetría real — si algún inspector llega a tener 2+ agrupaciones, `offline-catalog` fallaría con 422 mientras el resto de la app seguiría funcionando. Recomendado como mejora preventiva, no bloqueante.

**Verificación:** `gradlew compileDebugKotlin --rerun-tasks` — `BUILD SUCCESSFUL` sin errores nuevos. Los tres cambios se verificaron en vivo sobre un emulador real (Android 15, AVD `Medium_Phone_API_35`) con sesión y catálogo reales del cliente CORONA:
- **Cambio 1:** banner verde confirmado en Home con red; log `CATALOG_VM: No se pudo refrescar condiciones remotas... Se mantiene el catálogo local.` confirmado con modo avión activado, sin colgarse.
- **Cambio 2 — ciclo completo detección→reparación:** se borró manualmente en la base de datos local del emulador la relación Componente↔Condición del componente id=4 ("Guardilla", el mismo caso investigado con backend), simulando el hueco real. Sin red, el banner mostró correctamente "Catálogo incompleto: 1 componente sin diagnóstico o condición..." sin bloquear la navegación. Al reactivar WiFi y reabrir la app, `syncOfflineCatalog()` reparó la relación sola y el banner volvió a verde — confirma el ciclo completo, no solo el camino feliz.
- **Cambio 3 (badge de Área):** confirmado visualmente en el picker de Área — ya no aparece el badge "P" en ningún área (antes lo mostraban "Molino Cemento", "Molino Crudo" y "Reclamador Aditivos").
- Estabilidad general: app verificada estable tras `force-stop` + reapertura sin red, sin pedir login.

**Sin probar:** build *release* (solo se probó *debug*) y dispositivo físico (solo emulador).

---

### v1.7.6 — Fix OutOfMemoryError al subir evidencia de video + soporte de archivos de hasta 1 GB + íconos de estado por Área

**Fecha:** 2026-08-04

**Archivos modificados:** `SyncRepository.kt`, `EvidenceDao.kt`, `RetrofitClient.kt`, `HomeScreen.kt`, `app/build.gradle.kts`, `ReportFormScreen.kt`, `DashboardViewModel.kt`

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

#### Adenda a v1.7.6 (mismo día, mismo release): íconos de estado por Área + fix de lentitud al cargar el estado semanal

Este cambio se compactó dentro de v1.7.6 en vez de abrir una versión nueva porque es una extensión directa del mismo flujo de estado semanal (`weeklyElementStatuses`) que ya se tocaba en esta versión, sin cambios de esquema ni de API.

**Contexto:** el formulario de nuevo reporte (`ReportFormScreen.kt`) ya marcaba con ✅ verde y badge naranja "P" los activos, componentes y diagnósticos (ver sección 10). Faltaba ese mismo indicador un nivel más arriba, en el picker de **Área**, para que el inspector vea de un vistazo qué áreas ya están completas, cuáles tienen avance parcial y cuáles no tienen ningún registro esta semana.

**Cambio 1 — Íconos de estado en el picker de Área (`ReportFormScreen.kt`):**
- Se agregaron `elementIdsByArea`, `elementIdsWithExpectation`, `completedAreaIds` y `localPendingAreaIds` (calculados 100% en cliente a partir de `weeklyElementStatuses` y `localPendingDiagnosticItems`, que ya se descargaban para todas las áreas del grupo — no hizo falta ningún endpoint nuevo).
- **Verde (✅):** el área tiene *todos* sus activos con expectativa esta semana confirmados `DONE` por el servidor.
- **Badge "P" naranja:** el área tiene *al menos un* activo con avance (confirmado por servidor o guardado local pendiente de sync) pero no todos. Esto reutiliza deliberadamente el mismo badge "P" que ya significaba "pendiente por sincronizar" a nivel activo/componente/diagnóstico — a nivel Área su alcance es un poco más amplio (cubre tanto "área parcialmente revisada" como "área completa pero sin subir"), decisión tomada con el usuario para no introducir un tercer ícono nuevo.
- **Sin ícono:** ningún activo del área tiene registro esta semana.
- Se extendió `ProgressiveDropdownField` para aceptar `completedIds`/`pendingSyncIds` opcionales (default vacío), sin afectar el picker de "Tipo de activo" que reutiliza el mismo composable.

**Cambio 2 — Fix de lentitud (~2s) al pintar los íconos (`DashboardViewModel.kt`):**
- **Causa raíz:** `loadWeeklyElementsStatusForElements()` refrescaba el estado semanal desde el servidor de forma **secuencial**, una llamada HTTP por cada combinación (área, tipo de activo) — con 8 áreas eso son hasta 8 requests uno detrás del otro antes de que la UI mostrara cualquier ícono.
- **Fix:** se aplicó el mismo patrón *local-first* que ya usa `RemoteCatalogViewModel` para el catálogo — se pinta primero lo que ya haya en caché de Room (instantáneo), y las llamadas de refresco al servidor ahora corren en **paralelo** (`coroutineScope` + `async`/`awaitAll()`) en vez de en secuencia, así el tiempo total pasa de "suma de N requests" a "el más lento de los N". Verificado por el usuario en dispositivo real tras el cambio: la carga se sintió notablemente más rápida.

**Verificación de compilación:** se corrió `gradlew compileDebugKotlin --rerun-tasks` dos veces (una por cada cambio) sobre el JDK embebido de Android Studio. `BUILD SUCCESSFUL` ambas veces, sin errores ni warnings nuevos en `ReportFormScreen.kt` ni `DashboardViewModel.kt`.

**Pendiente:** no se probó en emulador/dispositivo desde este entorno de desarrollo (WSL sin GUI); la verificación visual del picker de Área y de la mejora de velocidad la hizo el usuario en su propio dispositivo.

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
