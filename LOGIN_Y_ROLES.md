# Flujo de login y manejo de roles

**Proyecto:** Mantec Inspector (Mantec_ins)
**Fecha de análisis:** 2026-09-16
**Última actualización:** 2026-09-21 — login unificado (ver nota abajo) y rol `"supervisor"` con pantalla propia (`SupervisorHome`), ambos ya implementados. Las secciones de este documento se actualizaron en el lugar para reflejar el estado actual, no solo el análisis original.
**Motivo:** documentar el estado actual del login y la bifurcación por rol antes de ampliar la app a nuevos roles/pantallas.

Este documento complementa a `DOCUMENTACION_PROYECTO.md` (sección 11, Seguridad y autenticación) con el detalle línea a línea de cómo funciona hoy el login y qué ve cada rol al entrar.

> **Nota 2026-09-21 — Login unificado.** Hasta esta fecha, Inspector (`User`) y Supervisor (`Employee`) se autenticaban contra dos endpoints separados (`POST /api/login` vs `POST /api/personal/login`), con un selector "Inspector"/"Supervisor" en `LoginScreen.kt` que decidía cuál llamar (así quedó documentado originalmente en `API_SUPERVISOR.md` sección 1, y así analizaba este documento el login en su versión anterior). Se reemplazó por un **login único**: un solo formulario, un solo endpoint (`POST /api/login`), que en el backend prueba primero contra `User` y, si no matchea, contra `Employee`, antes de rechazar con 401. El selector se eliminó de `LoginScreen.kt`; las clases que quedaron redundantes (`PersonalAuthRepository.kt`, `SupervisorLoginViewModel.kt` + su Factory) se borraron. `POST /api/personal/login` sigue existiendo y funcionando en el backend (tiene tests propios, `tests/Feature/Api/Personal/AuthApiControllerTest.php` del repo Laravel) pero **la app Android ya no lo llama**.

---

## 1. Estado actual: tres destinos posibles

Tras un login exitoso, la app bifurca según `UserSession.roleKey`:

| `roleKey` | Pantalla destino | Qué puede hacer |
|---|---|---|
| `"inspector"` | `AppScreen.Home` | Todo el flujo funcional: reportes, mediciones, sync, catálogo offline |
| `"supervisor"` | `AppScreen.SupervisorHome` | Ver "mis actividades" del día (y turno nocturno de ayer), registrar comentarios/horas por persona, subir evidencia (foto/video) — offline-first, ver `OFFLINE_SUPERVISOR.md` |
| Cualquier otro valor (o `null`) | `AppScreen.UnsupportedRole` | Nada — pantalla informativa con un único botón "Cerrar sesión" |

`roleKey` no viene del backend como texto libre elegido por el usuario: lo determina la app según **cuál campo vino en la respuesta de `/api/login`** — `user` → `"inspector"` (en realidad toma `user.role?.key`, que hoy en la práctica siempre es `"inspector"` para los que pueden loguearse por este medio), `employee` → siempre `"supervisor"` (fijo, `Employee` no tiene un campo de rol de app propio — ver sección 4). Cualquier otro `role.key` de `User` que no sea `"inspector"` (ej. un admin web) cae en `UnsupportedRole`, igual que antes.

---

## 2. Pantalla de Login (`LoginScreen.kt`)

Composable "tonto" sin lógica propia, recibe todo por parámetros desde `MainActivity`:

- Campos: usuario, contraseña (con `PasswordVisualTransformation`).
- Botón "Ingresar", deshabilitado mientras `isLoading = true`.
- Muestra `errorMessage` en un banner rojo si existe.
- Mientras `isLoading = true`, `MainActivity` además superpone un `AlertDialog` **no descartable** ("Bloqueado intencionalmente mientras termina la sincronización").

Estado: `LoginUiState(isLoading, errorMessage, loginSuccess)` — expuesto por `LoginViewModel` como `StateFlow`.

---

## 3. `LoginViewModel.login()`

```
1. Marca isLoading = true.
2. Llama AuthRepository.login(username, password).
3. onFailure → mapLoginError(error) según tipo:
   - SocketTimeoutException → "La conexión tardó demasiado..."
   - IOException           → "Debes estar conectado a internet..."
   - HttpException(code)   → mensaje específico por 400/401/403/404/422/500/502-504
   - otro (por texto crudo)→ host no resuelto / failed to connect / timeout / genérico
4. onSuccess(session):
   a. Si session.roleKey != "inspector":
      - loguea advertencia, NO descarga catálogo
      - loginSuccess = true directamente
   b. Si session.roleKey == "inspector":
      - llama RemoteCatalogRepository.syncOfflineCatalog() (descarga completa del catálogo)
      - si tiene éxito → loginSuccess = true
      - si falla (try/catch) → loginSuccess = false + mapCatalogError(e)
        (mensaje distinto, tipo "se inició sesión, pero...")
```

**Punto importante:** para el rol inspector, el login **no se considera terminado** hasta que el catálogo offline completo se descargó con éxito. Si la descarga falla, el usuario queda técnicamente autenticado en el backend (el token ya se guardó, ver más abajo) pero la UI no avanza y muestra error — quedaría "logueado" a medias si reabre la app (ver sección 6).

---

## 4. `AuthRepository.login()`

- `POST /api/login` → `LoginResponse(success, message, token, user, employee)`. El backend (`AuthApiController::login`, repo Laravel) prueba primero `User::where('username', ...)->where('status', true)` + `Hash::check`; si no matchea, prueba `Employee::where('username', ...)->where('has_login', true)->where('activo', true)` + `Hash::check`, antes de rechazar con `401`. La respuesta trae `user` **o** `employee`, nunca ambos.
- Si `!success || token.isNullOrBlank()` → `Result.failure`.
- Si `user != null`, arma `UserSession` (caso Inspector, sin cambios respecto a antes de la unificación):
  - `roleKey = user.role?.key`
  - `clientId/clientName` = **el primer** elemento de `user.clients`
  - `elementTypeId/elementTypeName` = **el primer** elemento de `user.allowed_element_types`
- Si `employee != null` (Supervisor, agregado 2026-09-21), arma `UserSession` distinto:
  - `roleKey = "supervisor"` — fijo en el cliente, no viene del backend (`Employee` no tiene un campo de rol de app propio).
  - `userId/userName/username` = `employee.id`/`employee.nombre`/`employee.nickname`.
  - `clientId/clientName/elementTypeId/elementTypeName` = `null` (no aplican a `Employee`).
- Si ni `user` ni `employee` vinieron (no debería pasar con `success = true`, pero el código lo cubre igual) → `Result.failure(Exception(response.message))`.
- Guarda la sesión en `SessionManager` (SharedPreferences, **sin cifrar** — riesgo ya documentado) inmediatamente, antes de que el ViewModel decida si el login "cuenta" como exitoso (ver punto anterior) — igual para ambos roles.

**Limitación relevante para ampliar roles:** el modelo asume **un solo cliente y un solo tipo de elemento** por usuario (toma `firstOrNull()`) — y esto solo aplica al caso `User`/Inspector; `Employee`/Supervisor no tiene ningún concepto de cliente/tipo de elemento hoy (van `null` directamente). Si en el futuro un rol necesita operar sobre varios clientes/tipos, esto debe cambiar tanto en `UserSession` como en el selector de perfil.

---

## 5. Qué pasa en `MainActivity` tras un login exitoso

En `LaunchedEffect(loginState.loginSuccess)` (dentro del `when (currentScreen) { AppScreen.Login -> ... }`), la bifurcación es ahora de **tres** caminos, en este orden (`when (session.roleKey)`):

1. Relee la sesión guardada con `authRepository.getSavedSession()`.
2. **`"supervisor"`** (agregado 2026-09-21):
   - `profileVM.setProfile(...)` con datos mínimos, igual que el caso "no soportado" de antes: `groupId = null`, `groupName = ""`, `groupAutoSync = false`, `specialtyId = null`, `availableElementTypes = emptyList()`.
   - `SupervisorSyncWorkManager.start(this@MainActivity)` — arranca el worker de sincronización periódica del módulo Supervisor (ver `OFFLINE_SUPERVISOR.md`).
   - `navigationVM.goToSupervisorHome()`.
3. **`"inspector"`** (y `clientId != null`):
   - Consulta en Room: `catalogRepository.getElementTypesByClient(clientId)` y `catalogRepository.getAssignedGroup()`.
   - Si hay un solo `elementType`, lo fija como `specialty` (evita un paso de selección en el flujo de reportes).
   - `profileVM.setProfile(...)` con perfil completo (grupo, auto-sync, tipos de elemento disponibles).
   - Limpia selección de área/catálogo/inspección.
   - `navigationVM.goToHome()`.
4. **Cualquier otro valor** (`else`):
   - `profileVM.setProfile(...)` con los mismos datos mínimos que el caso `"supervisor"`.
   - Limpia selección de área/catálogo/inspección.
   - `navigationVM.goToUnsupportedRole()`.

### Duplicación de lógica a tener en cuenta

Este mismo bloque (bifurcación por rol + armado de perfil) está **repetido casi textual** en `onCreate()` (~líneas 270-340 de `MainActivity.kt`), usado para restaurar sesión al abrir la app en frío con una sesión ya guardada. Hoy son dos copias independientes, y **cada una** ya tiene la rama `"supervisor"` agregada por separado (no se unificó al agregar el rol — ver `API_SUPERVISOR.md` sección 4, "deliberadamente no se tocó" la duplicación):

- `onCreate()` → restauración de sesión en frío.
- `LaunchedEffect(loginState.loginSuccess)` → justo después de un login manual exitoso.

**Recomendación, ahora más urgente que antes:** extraer esta lógica a una función única (p. ej. `suspend fun buildProfileAndNavigate(session: UserSession)`). Con dos roles ya bifurcando en dos copias del bloque, un tercer rol (o un cambio de regla para uno de los dos ya existentes) significa tocar 4 lugares en vez de 2 si esto no se unifica pronto.

### Restauración en frío: caso especial de catálogo vacío

En `onCreate()`, si la sesión existe pero `catalogRepository.getAssignedGroup()` devuelve `null` (agrupación local vacía), se re-descarga el catálogo (`remoteCatalogRepository.syncOfflineCatalog()`) antes de armar el perfil. Este caso no está replicado en el flujo de login manual (ahí la descarga ya se hizo en el paso 3.b del `LoginViewModel`).

---

## 6. Pantalla `UnsupportedRoleScreen`

Puramente informativa, sin acceso a ninguna funcionalidad:

- "Hola, {userName}."
- "Tu rol actual ({roleKey}) todavía no tiene una vista móvil habilitada..."
- "Ingresa desde la plataforma web o solicita a ManTec la habilitación correspondiente."
- Único botón: "Cerrar sesión" → **confirmado (2026-09-21) que sí limpia la sesión**: el `onLogout` en `MainActivity.kt` llama `authRepository.logout()` (limpia `SessionManager` y, si el rol fuera `"supervisor"`, también intenta el logout remoto — ver sección 8) antes de `navigationVM.logout()`. El usuario no queda "logueado a medias" al volver a esta pantalla.

---

## 7. `HomeScreen` (solo llega el rol `"inspector"`)

Lo que ve un inspector tras el login:

- Nombre, agrupación (`groupName`), tipo de sincronización (`groupAutoSync` → "automática con cualquier conexión" / "manual con botón"), estado de conexión actual.
- Botón **"Registrar reporte"** — siempre visible para el inspector.
- Botón **"Mediciones de espesores"** — visible solo si `showMeasurementsButton = true`, que viene de `MeasurementPendingViewModel.hasEnabledElementTypes()` → consulta `MeasurementElementTypeAccessEntity` en Room para saber si el tipo de elemento del inspector tiene el módulo de mediciones habilitado. **Esta es la única funcionalidad hoy con gating adicional más allá del rol** (gating por tipo de elemento dentro del rol inspector).
- Banner de completitud de catálogo offline (`CatalogCompletenessStatus`: `CHECKING` / `COMPLETE` / `INCOMPLETE`), disparado solo si `roleKey == "inspector"`.
- Auto-sync de reportes/mediciones pendientes al entrar, solo si `roleKey == "inspector"` y `groupAutoSync = true`.

---

## 8. Logout y expiración de token

`TokenExpirationEvent` (singleton con `SharedFlow<Unit>`) es emitido por `AuthInterceptor` (OkHttp) cuando cualquier request autenticado recibe un `401`. Se colecta globalmente en `MainActivity`:

```kotlin
LaunchedEffect(Unit) {
    TokenExpirationEvent.flow.collect {
        authRepository.logout()       // ver abajo: ya no es solo local
        profileVM.clearProfile()
        selectedAreaId = null
        catalogVM.clearAllSelections()
        inspectionVM.clearSelectionsFromAreaChange()
        navigationVM.logout()          // -> AppScreen.Login
    }
}
```

Esto es independiente del rol: aplica igual para inspector, supervisor, o cualquier otro rol que hubiera llegado a tener una sesión con token expirado.

**`authRepository.logout()` cambió (2026-09-21):** pasó de una función simple a `suspend fun logout()`. Sigue limpiando siempre `SessionManager` (SharedPreferences) al final, pero antes de eso, **solo si `roleKey == "supervisor"`**, intenta primero `POST api/personal/logout` (vía `PersonalApiService`) para invalidar el token también del lado del servidor — envuelto en un `try/catch` silencioso a propósito: si el token ya venció o no hay red, no importa, el usuario solo quiere cerrar sesión en su celular y la limpieza local sigue igual. Para `"inspector"` el comportamiento no cambió: sigue siendo puramente local (no hay logout remoto para `User`, mismo gap que ya señalaba `API_SUPERVISOR.md` sección 7 antes de que existiera este endpoint para Supervisor).

---

## 9. Puntos a definir antes de ampliar roles

Preguntas abiertas para cuando se diseñe el soporte de nuevos roles/vistas:

1. ~~¿Nuevo rol = nueva pantalla dedicada, o variantes dentro de `HomeScreen`?~~ **Resuelto (2026-09-20/21):** pantalla dedicada. El rol `"supervisor"` entra por una rama nueva en el mismo `when (currentScreen)` (`AppScreen.SupervisorHome`, `SupervisorHomeScreen.kt`), no reutiliza `Home`. Ver `API_SUPERVISOR.md` para el detalle completo de esa decisión y `OFFLINE_SUPERVISOR.md` para su arquitectura offline. `UnsupportedRoleScreen` sigue siendo el destino solo para roles que ni son `"inspector"` ni `"supervisor"`.
2. **Multi-cliente / multi-tipo de elemento por usuario:** `AuthRepository.login()` hoy descarta todo salvo el primer cliente y el primer tipo de elemento (`firstOrNull()`). Si un nuevo rol necesita ver varios clientes o tipos a la vez, este es el primer punto a tocar (`UserSession`, `SessionManager`, y el armado de perfil en `MainActivity`).
3. **Duplicación de la lógica de bifurcación por rol** (sección 5) — antes de agregar un tercer camino, conviene unificarla en una sola función para evitar que `onCreate()` y el login manual queden desincronizados.
4. **Descarga de catálogo condicionada al rol:** hoy `syncOfflineCatalog()` solo se dispara para `"inspector"`. Si un nuevo rol necesita datos offline propios (no necesariamente el mismo catálogo), hay que decidir si reusa este mecanismo o necesita uno paralelo.
5. **Persistencia de sesión sin cifrar** (`SessionManager` usa `SharedPreferences` planas) — si los nuevos roles manejan datos más sensibles, es buen momento para migrar a `EncryptedSharedPreferences` (ya estaba anotado como riesgo pendiente en `DOCUMENTACION_PROYECTO.md` sección 11).

---

## 10. Archivos involucrados (referencia rápida)

| Archivo | Rol en el flujo |
|---|---|
| `presentation/ui/LoginScreen.kt` | UI del formulario de login — **un solo formulario, sin selector de rol** (el selector "Inspector"/"Supervisor" que existió brevemente se eliminó el 2026-09-21 al unificar el login) |
| `presentation/viewmodel/LoginViewModel.kt` | Lógica de login + descarga de catálogo (solo Inspector) + mapeo de errores |
| `presentation/viewmodel/LoginUiState.kt` | Estado expuesto a la UI |
| `data/repository/AuthRepository.kt` | Llamada a `/api/login` (única, ambos roles), arma `UserSession` según venga `user` o `employee`, guarda sesión, `logout()` (`suspend`, con logout remoto para Supervisor — ver sección 8) |
| `data/local/SessionManager.kt` | Persistencia de sesión en SharedPreferences |
| `domain/model/UserSession.kt` | Modelo de sesión (un solo cliente/tipo de elemento — solo aplica a Inspector) |
| `data/remote/LoginResponse.kt` | DTOs de la respuesta del backend — `user: LoginUserDto?` **y** `employee: PersonalEmployeeDto? = null` (agregado 2026-09-21) |
| `data/remote/personal/PersonalAuthDtos.kt` | `PersonalEmployeeDto`/`PersonalRoleDto` — forma del campo `employee` de `LoginResponse` |
| `data/remote/personal/PersonalApiService.kt` | `logout()` remoto (`POST api/personal/logout`), usado desde `AuthRepository.logout()` para Supervisor |
| `MainActivity.kt` (`onCreate`, `LaunchedEffect(loginState.loginSuccess)`) | Restauración de sesión en frío + bifurcación por rol (tres caminos) tras login manual |
| `presentation/viewmodel/AppNavigationViewModel.kt` | Estado de navegación (`AppScreen`) |
| `presentation/navigation/AppScreen.kt` | Pantallas posibles (`Loading`, `Login`, `Home`, `Report`, `MeasurementThickness`, `UnsupportedRole`, `SupervisorHome`) |
| `presentation/viewmodel/InspectorProfileViewModel.kt` | Perfil del usuario logueado (`groupId`, `groupAutoSync`, `specialtyId`, etc.) — se reutiliza igual para Supervisor con valores mínimos/`null` |
| `presentation/ui/UnsupportedRoleScreen.kt` | Pantalla para roles que no son ni Inspector ni Supervisor |
| `presentation/ui/HomeScreen.kt` | Pantalla principal del inspector |
| `presentation/ui/SupervisorHomeScreen.kt` | Pantalla principal del supervisor — ver `API_SUPERVISOR.md`/`OFFLINE_SUPERVISOR.md` |
| `presentation/viewmodel/MeasurementPendingViewModel.kt` | Gating de acceso al módulo de mediciones por tipo de elemento |
| `data/remote/TokenExpirationEvent.kt` | Señal de token expirado (401) → fuerza logout global, para cualquier rol |

**Eliminados el 2026-09-21** (existieron brevemente durante la primera versión, separada por endpoints, del login de Supervisor — quedaron redundantes al unificar): `data/repository/PersonalAuthRepository.kt`, `presentation/viewmodel/SupervisorLoginViewModel.kt` y su Factory.
