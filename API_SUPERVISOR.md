# API Supervisor — login Employee + actividades + evidencia a R2

**Proyecto:** Mantec Inspector (Mantec_ins)
**Fecha de implementación:** 2026-09-20
**Última actualización:** 2026-09-22 — comentario del responsable por trabajador/actividad (ver sección 8). Anterior: 2026-09-21, login unificado con Inspector (ver sección 1, y el detalle completo en `LOGIN_Y_ROLES.md`).
**Motivo:** agregar un rol nuevo "Supervisor" a la app (además del Inspector ya existente) para que, desde el celular, un supervisor registre comentarios + horas trabajadas por actividad/persona y suba evidencia (foto/video) a Cloudflare R2 — repitiendo desde la app real lo que hasta ahora solo se probaba vía la pantalla web "Ver como" del panel admin Laravel (repo backend, `NUEVA_FUNCIONALIDAD_PERSONAL_Y_PROGRAMACION.md`, secciones 14.18/14.20/14.21/14.22).

Este documento complementa `LOGIN_Y_ROLES.md` (que ya anticipaba esta pregunta de diseño sin resolverla, sección "¿Nuevo rol = nueva pantalla dedicada, o variantes dentro de HomeScreen?" — ese documento tiene ahora el detalle línea a línea del login unificado, actualizado el 2026-09-21) y se refleja en la sección 16 (Historial de versiones) de `DOCUMENTACION_PROYECTO.md` como v1.8.0.

---

## 1. Decisión de autenticación

El supervisor se autentica como **`Employee`** del backend Laravel, no como `User` (el modelo que ya usa el login del Inspector).

> **Actualizado 2026-09-21 — login unificado.** Esta sección describía originalmente (2026-09-20) dos sistemas de login separados: `POST api/login` solo para `User`, `POST api/personal/login` solo para `Employee`, con un selector "Inspector"/"Supervisor" en `LoginScreen.kt` decidiendo cuál llamar antes de enviar las credenciales. Eso se reemplazó por un **login único**: la app llama siempre a `POST api/login`; el backend (`AuthApiController::login`) prueba primero contra `User` y, si no matchea, contra `Employee`, antes de rechazar con 401. Ya no existe el selector de modo en `LoginScreen.kt`, y las clases que quedaron redundantes (`PersonalAuthRepository.kt`, `SupervisorLoginViewModel.kt` + Factory) se borraron. El endpoint `POST api/personal/login` (`PersonalAuthApiController`) **sigue existiendo y funcionando en el backend** (conserva sus tests, `tests/Feature/Api/Personal/AuthApiControllerTest.php`), pero la app Android ya no lo llama — ver el detalle completo, con diagramas de la bifurcación por rol en `MainActivity`, en `LOGIN_Y_ROLES.md`.
>
> Motivo del cambio: un solo formulario de login es más simple para el usuario final (no tiene que saber de antemano si su cuenta es "Inspector" o "Supervisor" antes de intentar entrar) y elimina una fuente de error (elegir el modo equivocado). Ambos endpoints seguían siendo válidos técnicamente — la unificación fue una decisión de UX, no una corrección de un bug.

Son igual dos modelos separados en el backend (`User` y `Employee`), ambos terminan siendo tokens Sanctum válidos contra el mismo `auth:sanctum` — eso no cambió, solo el punto de entrada HTTP se unificó. La alternativa de fondo (reusar `User` para supervisores) se descartó desde el diseño original por duplicar datos entre `User` y `Employee` sin necesidad, ya que `Employee` ya tiene su propio `username`/`password` (usados hoy por el login web `/personal/login` del panel admin).

Un dispositivo está logueado como Inspector **o** Supervisor, nunca ambos a la vez — reutiliza el mismo `SessionManager`/`UserSession` ya existentes (`roleKey = "supervisor"` en vez de `"inspector"`; `clientId`/`elementTypeId` quedan `null` porque no aplican a `Employee`).

## 2. Endpoints consumidos (backend Laravel, sección 14.24 de `NUEVA_FUNCIONALIDAD_PERSONAL_Y_PROGRAMACION.md`)

```
POST   api/login                                              (unificado 2026-09-21 — ver sección 1; sin token, prueba User y luego Employee)
POST   api/personal/login                                     (sigue activo en el backend, pero la app YA NO lo llama — ver sección 1)
POST   api/personal/logout
GET    api/personal/actividades                               (hoy + turno Nocturno de ayer)
POST   api/personal/actividades/{id}                          (comentarios + horas)
POST   api/personal/actividades/{id}/evidencias                (multipart, files[], max 6, max 50MB c/u)
GET    api/personal/actividades/{id}/evidencias/{evidenceId}   (JSON con URL firmada de R2, vigente 10 min)
DELETE api/personal/actividades/{id}/evidencias/{evidenceId}
```

Todas salvo login exigen `Authorization: Bearer <token>` + pasan por un middleware (`EnsureTokenableIsEmployee` del lado Laravel) que rechaza con 403 un token de `User` (Inspector) que intente usarlas — Sanctum es polimórfico en ese backend, así que sin ese chequeo un token del rol equivocado sería aceptado igual. Esto no cambió con la unificación del login: da igual si el token de un supervisor se emitió desde `api/login` o desde `api/personal/login`, en ambos casos es un token de `Employee` y el middleware lo valida igual.

## 3. Archivos nuevos (Android)

| Archivo | Propósito |
|---|---|
| `data/remote/personal/PersonalAuthDtos.kt` | `PersonalEmployeeDto`/`PersonalRoleDto` — hoy usados desde el campo `employee` de `LoginResponse` (login unificado), y también por lo que le sigue quedando a `PersonalApiService` (logout) |
| `data/remote/personal/PersonalActivityDtos.kt` | DTOs de actividades/evidencia — mismo shape que sirve `Api\Personal\ActivityController::serialize()` del backend |
| `data/remote/personal/PersonalApiService.kt` | Interfaz Retrofit — `RetrofitClient.createPersonalApiService()`; hoy solo se usa para `logout()` y para actividades/evidencia, no para login (ver sección 1) |
| `data/repository/PersonalActivityRepository.kt` | Actividades + evidencia; la subida reutiliza el mismo patrón de multipart en streaming que `SyncRepository.buildMultipartFromUri()` (no se extrajo a un util compartido, para no tocar `SyncRepository` ya probado) |
| `presentation/viewmodel/SupervisorActivityViewModel.kt` (+ Factory) | Estado de "Mis actividades de hoy": cargar, guardar registro, subir/ver/borrar evidencia |
| `presentation/ui/SupervisorHomeScreen.kt` | Pantalla nueva — equivalente Compose de `ver-como/index.blade.php` (backend): tarjetas de actividad, expandir para comentarios + horas por persona + evidencia |

**Ya no existen (borrados 2026-09-21, ver sección 1)**: `data/repository/PersonalAuthRepository.kt`, `presentation/viewmodel/SupervisorLoginViewModel.kt` (+ Factory) — el login del supervisor se unificó dentro de `AuthRepository`/`LoginViewModel`, mismos que ya usaba el Inspector.

## 4. Cambios en archivos existentes

- **`app/build.gradle.kts`**: `buildConfigField("String", "BASE_URL", ...)` por `buildType` (`debug` → `http://10.0.2.2:8000/`, `release` → `https://mantecsas.com/`). Antes `BASE_URL` era una constante fija en `RetrofitClient.kt`, editada a mano para probar en local — riesgo ya documentado (commit `6cb0d5c`, quedó apuntando al emulador en un commit real). Con esto, cambiar de variante (debug/release) ya decide la URL, sin edición manual ni riesgo de commitearla mal.
- **`RetrofitClient.kt`**: `BASE_URL` ahora lee `BuildConfig.BASE_URL`; nueva factory `createPersonalApiService()`.
- **`presentation/navigation/AppScreen.kt`**: nuevo `data object SupervisorHome`.
- **`presentation/viewmodel/AppNavigationViewModel.kt`**: nuevo `goToSupervisorHome()`.
- **`MainActivity.kt`**: instancia los repos/ViewModels nuevos; agrega la rama `roleKey == "supervisor"` en los dos puntos donde ya existía `roleKey != "inspector"` (restauración de sesión al abrir la app, y justo después de un login exitoso) para rutear a `SupervisorHome` en vez de `UnsupportedRole`; agrega el `case AppScreen.SupervisorHome ->` en el `when(currentScreen)` que renderiza `SupervisorHomeScreen`.

**Deliberadamente NO se tocó**: la duplicación de `roleKey != "inspector"`/`roleKey == "inspector"` en 6+ puntos de `MainActivity.kt` (ya señalada en `LOGIN_Y_ROLES.md` punto 3 como mejora pendiente) — se agregó la rama nueva de la misma forma que ya existía, sin refactorizar esa duplicación, para no arriesgar el flujo del Inspector ya en producción con un cambio no pedido.

> **Nota 2026-09-21**: el bullet que estaba aquí sobre `presentation/ui/LoginScreen.kt` ("selector Inspector/Supervisor") ya no aplica — ese selector se eliminó al unificar el login (sección 1). `LoginScreen.kt` volvió a ser el mismo formulario simple que ya tenía el Inspector, sin ningún cambio visible relacionado con Supervisor.

## 5. Probar en local

1. Backend Laravel corriendo en WSL (`php artisan serve`, puerto 8000) — ver `ARRANCAR_LOCAL.txt` del repo backend.
2. Compilar la variante **debug** (`gradlew.bat assembleDebug` o desde Android Studio) — ya apunta a `http://10.0.2.2:8000/` automáticamente, sin editar nada.
3. En el emulador AVD, `10.0.2.2` alcanza el `127.0.0.1:8000` del host Windows sin configuración adicional — confirmado que Windows ya puede alcanzar el Laravel de WSL en `127.0.0.1:8000` directamente (verificado con `curl` y con Chrome/Playwright durante el desarrollo del backend), así que el mismo alias debería funcionar desde el emulador sin necesitar `php artisan serve --host=0.0.0.0`. Si no llega, ese es el primer punto a revisar (host de bind del `artisan serve` en WSL).
4. Para dispositivo físico (no emulador): `10.0.2.2` no aplica — usar la IP LAN real del host Windows. El manifest ya tiene `usesCleartextTraffic="true"` global y `network_security_config.xml` ya permite cleartext a `10.0.2.2`; para una IP LAN distinta puede hacer falta agregarla ahí también si el flag global no alcanza.
5. Un `Employee` necesita `has_login=true`, `activo=true` y `username`/`password` seteados para poder loguearse — crear uno de prueba desde el panel admin (`/personal/empleados`) o vía `php artisan tinker` en el backend.

## 6. Verificado antes de esta ronda de Android

El backend Laravel (login, listar actividades, guardar horas, subir/ver/borrar evidencia) se probó end-to-end con `curl` contra R2 real y contra la suite de tests (`tests/Feature/Api/Personal/`, 20 pruebas) — documentado en el repo backend, sección 14.24. Del lado Android, esta sesión llegó hasta `gradlew.bat assembleDebug` exitoso (compila sin errores ni warnings) — **no se probó todavía en un emulador o dispositivo real** (login real, ver actividades, guardar horas, subir una foto desde la app). Esa prueba queda pendiente para el usuario en su máquina.

## 7. Pendiente / no incluido en esta ronda

- ~~Prueba real en emulador/dispositivo~~ — hecho parcialmente en la ronda offline (login/actividades/evidencia funcionando en emulador; ver `OFFLINE_SUPERVISOR.md`). Falta la prueba específica en modo avión (ver ese documento, sección 12).
- ~~Arquitectura offline (esta pantalla era 100% online-only)~~ — resuelto en v1.9.0, ver `OFFLINE_SUPERVISOR.md`.
- ~~Miniaturas reales de imagen en la grilla de evidencias~~ — resuelto en v1.9.1 con Coil, solo para evidencia ya sincronizada y con conexión (ver `DOCUMENTACION_PROYECTO.md` v1.9.1). Evidencia pendiente de subir (`PENDING_SYNC`) sigue mostrando el ícono genérico.
- ~~Indicador visual de "turno de ayer" + toggle Hoy/Ayer explícito~~ — resuelto en v1.9.3 (ver `DOCUMENTACION_PROYECTO.md`).
- ~~Bug reportado de "HTTP 401 Unauthorized" al crear una actividad nueva~~ — investigado en v1.9.4, causa raíz probable identificada y un gap de arquitectura real corregido (un 401 en un sync silencioso en background ya no fuerza un logout global). Ver `DOCUMENTACION_PROYECTO.md` v1.9.4.
- ~~`AuthRepository.logout()` es puramente local para ambos roles, sin invalidar el token en el servidor~~ — **resuelto parcialmente (2026-09-21)** para Supervisor: `logout()` pasó a ser `suspend` y, solo si `roleKey == "supervisor"`, intenta primero `POST api/personal/logout` (con `try/catch` silencioso: si falla, igual limpia la sesión local) antes de `SessionManager.clearSession()`. Esto aplica tanto al botón "Cerrar sesión" como al logout automático por `TokenExpirationEvent` (401 en primer plano) — ambos caminos llaman la misma función. **Para Inspector sigue sin cambios**: el logout de `User` sigue siendo puramente local, no hay endpoint de logout remoto para ese rol todavía. Ver `LOGIN_Y_ROLES.md` sección 8 para el detalle completo.
- No se creó una pantalla de "elegir empresa/cliente" — el supervisor ve sus actividades sin importar la empresa (igual que el backend, que no filtra por eso).

## 8. Comentario del responsable por trabajador/actividad (2026-09-22)

Además de comentar/registrar horas para la actividad completa, el supervisor puede comentar sobre las horas reportadas de UN trabajador puntual (ej. "se fue temprano por cita médica"), cuando marcó "No, no todos trabajaron las horas programadas". No hay ruta nueva: reutiliza `POST api/personal/actividades/{id}` — `personas.*.comment` (`nullable|string|max:1000`) viaja junto a `worked_hours`; la respuesta trae `personas[].comment`. Ver la sección 14.31 de `NUEVA_FUNCIONALIDAD_PERSONAL_Y_PROGRAMACION.md` (repo backend) para el diseño completo (tabla `activity_employee_comments`, historial mezclado en Bitácora web).

**Cambios Android**: Room v20→21 (`MIGRATION_20_21` en `SupervisorMigrations.kt`, columna `comentario` en `supervisor_personas`), `PersonalPersonaDto.comment`/`PersonalPersonaHoursRequest.comment`, `SupervisorPersonaDao.updateComentario()`, `PersonalActivityLocalRepository.guardarComentarioPersonaLocal()`, `SupervisorActivityViewModel.guardarComentarioPersona()`, y en `SupervisorHomeScreen.kt` el nombre del trabajador es clickeable (ícono de lápiz si ya tiene comentario) y abre `ComentarioPersonaDialog` (Guardar/Borrar/Cancelar).

**Bug real encontrado y corregido en verificación en vivo**: guardar un comentario ANTES de tocar "Guardar registro" alguna vez dejaba `allWorkedScheduledHours=null` en Room, y `SupervisorSyncRepository` (con su default `?: true`) mandaba `personas=[]` — el comentario se guardaba local pero nunca llegaba al servidor, sin error visible. Fix: el callback de guardar comentario ahora manda también el snapshot actual del formulario (comments/todosTrabajaron/horasPorPersona), persistido primero vía `guardarRegistroLocal()`. Verificado contra un backend real (local, no producción) consultando la tabla `activity_employee_comments` directamente antes y después del fix.

**Pendiente**: el backend de producción (Railway) todavía no tiene esta migración ni este código desplegado — feature completo y verificado localmente (Laravel + Android), pendiente de despliegue explícito.
