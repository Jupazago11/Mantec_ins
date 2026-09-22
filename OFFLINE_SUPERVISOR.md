# Offline-first para el rol Supervisor

**Proyecto:** Mantec Inspector (Mantec_ins)
**Fecha de implementación:** 2026-09-20
**Motivo:** el módulo Supervisor (`API_SUPERVISOR.md`, v1.8.0) se construyó 100% online — sin señal, cada acción fallaba directo, sin caché ni cola. El usuario confirmó que el offline es importante y pidió construirlo completo ahora, antes de seguir con otras mejoras (toggle Hoy/Ayer y el bug de "HTTP 401 Unauthorized" quedaron pausados para una ronda posterior).

Este documento complementa `API_SUPERVISOR.md` (diseño original online-only) y `PATRONES_ASINCRONISMO_OFFLINE.md` (los patrones del Inspector que este trabajo replica en vez de inventar uno nuevo). Se refleja en la sección 16 de `DOCUMENTACION_PROYECTO.md` como v1.9.0.

---

## 1. Decisión de alcance

Se replica tal cual el patrón ya usado por el Inspector, documentado en `PATRONES_ASINCRONISMO_OFFLINE.md`:

- **Local-first real** (patrón 1): la UI siempre lee de Room, nunca espera a la red para pintar.
- **`PENDING_SYNC` → `SYNCED`**, con un tercer estado terminal **`ERROR`** (patrón 2) para fallos permanentes (403/422/413) que no deben reintentarse indefinidamente — un fallo transitorio (sin red, 5xx) se deja en `PENDING_SYNC` y se reintenta en el próximo ciclo.
- **Multipart en streaming** (patrón 3): se reutiliza tal cual `PersonalActivityRepository`'s construcción de multipart ya existente, ahora leyendo desde el archivo copiado en `filesDir` en vez de una `Uri` de `MediaStore`.
- **Migración de Room explícita, no destructiva** (patrón 5): no negociable — subir la versión global de `AppDatabase` sin una migración real habría borrado cualquier reporte `PENDING_SYNC` de un Inspector real en producción.
- **Sin manejo de conflictos**: igual que el Inspector (que no implementa nada tipo 409/`CONFLICT`), gana el local. Si hace falta más adelante, es una mejora aparte sobre esta base.

## 2. Esquema Room nuevo (bump v19 → v20)

Tres tablas nuevas en `data/local/`:

| Entidad | PK | Notas |
|---|---|---|
| `SupervisorActivityEntity` (`supervisor_activities`) | `id` (Long, el ID real del servidor — la actividad la crea Programación, no la app, así que no hace falta un UUID local) | Espejo de `PersonalActivityDto` + campos editables (`comments`, `allWorkedScheduledHours`, `reportedHours`, `registrado`) + `registrationSyncStatus` (`SYNCED`/`PENDING_SYNC`/`ERROR`) + `lastError` |
| `SupervisorPersonaEntity` (`supervisor_personas`) | compuesta `(activityId, employeeId)` | `nombre`, `nickname`, `workedHours` |
| `SupervisorEvidenceEntity` (`supervisor_evidences`) | `id` autoGenerate | `localPath`, `originalName`, `fileType`, `syncStatus`, `serverId: Long?` (null hasta subir), `lastError` |

**Mejora consciente sobre el patrón del Inspector:** el Inspector guarda evidencia como URI pública de `MediaStore` sin copiarla, lo que puede perderla silenciosamente si el sistema limpia esa carpeta antes de sincronizar. Para el Supervisor, `PersonalActivityLocalRepository.encolarEvidenciaDesdeUri()` copia el archivo a almacenamiento **privado** de la app (`context.filesDir/supervisor_evidence/`) antes de insertar la fila en Room — `localPath` apunta a esa copia propia, no a la `Uri` original.

DAOs (`SupervisorActivityDao`, `SupervisorPersonaDao`, `SupervisorEvidenceDao`) — mismo estilo que `ReportDao`/`EvidenceDao`: insert/getAll/getByStatus/update/delete. `SupervisorPersonaDao.deleteByActivity()` y `SupervisorEvidenceDao.getById()` se agregaron durante la implementación para soportar la limpieza de filas huérfanas en el merge y el borrado físico del archivo local, respectivamente.

### Migración 19 → 20

`AppDatabase.kt`: `version = 20`, entidades agregadas al arreglo. `SupervisorMigrations.kt` define `MIGRATION_19_20` con `CREATE TABLE IF NOT EXISTS` para las 3 tablas nuevas — DDL copiado verbatim del schema JSON exportado por Room (`app/schemas/.../20.json`, generado habilitando `exportSchema = true` temporalmente para esta tarea). `DatabaseProvider.kt` la agrega a `.addMigrations(...)`, y mantiene `fallbackToDestructiveMigration()` solo como red de seguridad en debug (no aplica si `MIGRATION_19_20` cubre el salto, que es el caso).

**Verificado en esta sesión contra un dispositivo real** (no solo revisado en el código):

1. Se extrajo la base de datos real de un emulador con la app ya instalada (`adb exec-out run-as com.example.mantec_ins cat .../mantec_inspector_db`, 241.664 bytes) — `PRAGMA user_version` = 19, `PRAGMA integrity_check` = `ok`. Estado real antes de migrar: `reports` (7 filas, todas `SYNCED`), `report_details` (7, `SYNCED`), `measurement_thickness_drafts` (10, `SYNCED`), `elements` (53), `component_condition_cross_ref` (171) — sin datos `PENDING_SYNC` reales en este dispositivo de prueba en este momento, pero sirve igual como prueba de no-destructividad: si la migración fuera destructiva, estas tablas habrían quedado en 0 filas.
2. Se instaló el APK ya compilado con este cambio **sobre la instalación existente** (`adb install -r`, no un `-d`/reinstalación limpia) y se abrió la app. Sin crash, sin `FATAL`/`AndroidRuntime` en Logcat, sin excepción de validación de migración de Room.
3. Se volvió a extraer la base de datos post-arranque: `PRAGMA user_version` = **20**, `integrity_check` = `ok`. Las 3 tablas `supervisor_*` existen con las columnas esperadas (vacías, como corresponde — no se ejecutó ningún login de Supervisor en esta prueba). **Los datos previos del Inspector quedaron exactamente iguales**: `reports` sigue en 7 filas `SYNCED`, `report_details` 7, `measurement_thickness_drafts` 10, `elements` 53, `component_condition_cross_ref` 171 — mismos conteos, ningún dato perdido.

Con esto, `MIGRATION_19_20` queda confirmada como no-destructiva en un caso real, no solo revisada por lectura de código. Nota honesta: este dispositivo de prueba no tenía filas `PENDING_SYNC` reales al momento de la prueba, así que no se demostró específicamente que un reporte a medio sincronizar sobreviva byte a byte — pero dado que la migración usa `CREATE TABLE IF NOT EXISTS` (nunca toca las tablas existentes) y los conteos de todas las tablas preexistentes no cambiaron, el riesgo que motivaba este chequeo (pérdida de datos por bump de versión) queda cerrado.

### Migración 20 → 21 (2026-09-22 — comentario por trabajador)

Mismo patrón: `AppDatabase.kt` a `version = 21`, `MIGRATION_20_21` en `SupervisorMigrations.kt` agrega la columna `comentario` (`ALTER TABLE supervisor_personas ADD COLUMN comentario TEXT`, nullable, sin `DEFAULT` explícito para calzar exacto con `app/schemas/.../21.json`). Verificado en esta sesión instalando el APK nuevo **sobre** una instalación existente en emulador (schema v20 real, no una base recién creada) — la app abrió sin crash ni excepción de validación de Room, confirmando la migración no-destructiva contra un caso real, igual que 19→20.

**Nota lateral de tooling** (para quien repita esta verificación en el futuro): un primer intento de extraer la base de datos vía PowerShell (`adb exec-out ... > archivo`) corrompió el archivo — PowerShell trata la redirección `>` de un proceso externo como texto, no como bytes, e insertó un BOM UTF-8 al inicio más caracteres de reemplazo (`U+FFFD`) donde la salida binaria no era UTF-8 válido. La extracción binaria-segura se logró desde Git Bash con `MSYS_NO_PATHCONV=1` (para que no reescriba la ruta POSIX `/data/data/...` a una ruta de Windows) y redirección `>` nativa de shell POSIX.

## 3. Repositorio local (fuente de verdad de la UI)

`data/repository/PersonalActivityLocalRepository.kt` (rewrite completo, ahora recibe `context: Context, database: AppDatabase`) — calco de `InspectionLocalRepository`, sin llamadas a Retrofit:

- `getActividades()`, `getPersonas(activityId)`, `getEvidencias(activityId)` — lectura pura de Room.
- `guardarRegistroLocal(activityId, comments, allWorkedScheduledHours, personasHoras)` — escribe `registrationSyncStatus = "PENDING_SYNC"` de inmediato.
- `encolarEvidenciaDesdeUri(activityId, uri)` — copia el archivo desde la `Uri` del picker a `filesDir/supervisor_evidence/<uuid>.<ext>` (streaming, `InputStream.copyTo(OutputStream)`) e inserta la fila `PENDING_SYNC`; si falla, borra el archivo parcial.
- `borrarEvidenciaLocal(localId)` — busca la fila, borra el archivo físico (`File(it.localPath).delete()`) y la fila.
- `getPendientesDeRegistro()`, `getEvidenciasPendientes()`, `clearAll()`.

`data/repository/PersonalActivityRepository.kt` se recortó a solo las dos acciones que siguen siendo online-only: `obtenerUrlEvidencia()` (URL firmada de R2, no tiene sentido cachearla) y `borrarEvidenciaRemota()`. Ya no recibe `context` en el constructor.

## 4. Repositorio de sincronización

`data/repository/SupervisorSyncRepository.kt` (nuevo) — calco de `SyncRepository` del Inspector, con `Mutex` para evitar sincronizaciones concurrentes. `sync()` → `doSync()` corre en este orden:

1. **Push de registros pendientes**: por cada actividad con `registrationSyncStatus == "PENDING_SYNC"`, `POST api/personal/actividades/{id}`. Éxito → `SYNCED`. `403`/`422` → `ERROR` con el mensaje del servidor. Otro código o excepción de red → se deja `PENDING_SYNC`, reintenta en el próximo ciclo.
2. **Push de evidencia pendiente**: por cada evidencia `PENDING_SYNC`, lee el archivo de `filesDir`, construye el multipart y sube. Éxito → `SYNCED` + `serverId`, y borra la copia local (ya vive en R2, no hace falta duplicarla en el dispositivo). `422`/`413`/`403`/`404` → `ERROR`.
3. **Refresh/merge desde servidor** (`GET api/personal/actividades`): upsert de actividades nuevas como `SYNCED`. Si una actividad existente tiene `registrationSyncStatus == "PENDING_SYNC"`, el refresh **no pisa** `comments`/`allWorkedScheduledHours`/personas (hay un cambio local esperando subir) — solo actualiza los campos de solo-lectura (`description`, `estimatedHours`, `shift`, `closed`). Si no está pendiente, hace `personaDao.deleteByActivity()` + reinserta (evita filas huérfanas si una persona fue quitada del lado servidor). La evidencia del servidor se upsertea por `serverId`; las filas locales `PENDING_SYNC`/`ERROR` (que el servidor todavía no conoce) nunca se tocan.

## 5. Contrato de `PersonalApiService`

`saveActividad()` y `uploadEvidencias()` pasaron de devolver el DTO directo a `Response<DTO>` (mismo patrón que `SyncApiService` del Inspector) — necesario para que `SupervisorSyncRepository` distinga por código HTTP (403/422/413 vs. transitorio). `login()`/`getActividades()`/`show`/`delete` se quedaron igual — no participan del push-sync granular.

## 6. WorkManager

`sync/SupervisorSyncWorker.kt` + `sync/SupervisorSyncWorkManager.kt` (nuevos) — mismo intervalo (15 min) y constraint (`NetworkType.CONNECTED`) que el `SyncWorker` del Inspector, sin el gate de `group.autoSync` (ese flag es de `GroupEntity`, no aplica a `Employee`). `WORK_NAME = "supervisor_sync_worker"` — nombre distinto, no compite con el work del Inspector.

## 7. ViewModel y UI

`SupervisorActivityViewModel` (rewrite completo): constructor ahora recibe `(localRepository, syncRepository, remoteRepository)`. `cargarActividades()` lee Room de inmediato sin esperar red. `sincronizar()` llama a `syncRepository.sync()` y vuelve a pintar desde Room. `guardarRegistro()`/`subirEvidencias()` escriben local primero (`PENDING_SYNC`) y disparan `sincronizar()` a continuación — si no hay red, la UI ya mostró el estado pendiente y el intento de red falla en silencio (patrón 7: un sync que falla no debe asustar).

`SupervisorHomeScreen`: cada tarjeta de actividad gana un indicador de estado de sincronización (mismo criterio visual que Mediciones/Reportes) — `"Sin sincronizar"` (ámbar, ícono `CloudOff`) si `registrationSyncStatus/syncStatus == PENDING_SYNC`, `"Error al enviar"` (rojo) si `ERROR`. Las miniaturas de evidencia muestran un punto de color (ámbar/rojo) para las que no están `SYNCED`. La barra superior gana un botón manual de sincronización (`IconButton` con spinner mientras `sincronizando == true`).

## 8. `MainActivity.kt`

Se instancian `PersonalActivityLocalRepository` y `SupervisorSyncRepository` junto a los repos ya existentes del módulo Supervisor. Se replican los mismos puntos de disparo de sync que usa el Inspector, como rama paralela por `roleKey == "supervisor"` (sin refactorizar la duplicación ya existente, mismo criterio usado en v1.8.0):

- Arranque de la app / restauración de sesión.
- Justo después de un login exitoso.
- `onResume()` (rama temprana: si `roleKey == "supervisor"` y hay internet, sincroniza y retorna — no pisa la lógica de `onResume` del Inspector).
- Botón manual en `SupervisorHomeScreen`.
- `onLogout` detiene el `WorkManager` (`SupervisorSyncWorkManager.stop()`).

## 9. Archivos nuevos

```
data/local/SupervisorActivityEntity.kt
data/local/SupervisorPersonaEntity.kt
data/local/SupervisorEvidenceEntity.kt
data/local/SupervisorActivityDao.kt
data/local/SupervisorPersonaDao.kt
data/local/SupervisorEvidenceDao.kt
data/local/SupervisorMigrations.kt
data/repository/SupervisorSyncRepository.kt
sync/SupervisorSyncWorker.kt
sync/SupervisorSyncWorkManager.kt
```

## 10. Archivos modificados

```
data/local/AppDatabase.kt                              (entidades + version 20 + migración registrada + exportSchema = true, ver nota abajo)
data/local/DatabaseProvider.kt                          (.addMigrations(MIGRATION_19_20))
data/repository/PersonalActivityLocalRepository.kt      (rewrite completo — ver sección 3)
data/repository/PersonalActivityRepository.kt           (recortado — ver sección 3)
data/remote/personal/PersonalApiService.kt              (Response<T> en saveActividad/uploadEvidencias)
presentation/viewmodel/SupervisorActivityViewModel.kt          (rewrite completo)
presentation/viewmodel/SupervisorActivityViewModelFactory.kt   (nuevos parámetros)
presentation/ui/SupervisorHomeScreen.kt                        (rewrite — badges de estado de sync)
MainActivity.kt                                                (instancias nuevas + puntos de sync — ver sección 8)
app/build.gradle.kts                                           (ksp room.schemaLocation; versionCode 12, versionName 1.9.0)
```

**Nota sobre `exportSchema`:** estaba en `false`; se cambió a `true` (junto con `ksp { arg("room.schemaLocation", ...) }` en `build.gradle.kts`) para poder generar el JSON real del esquema v20 y escribir `MIGRATION_19_20` con el DDL exacto en vez de adivinarlo. Se dejó en `true` — no afecta el runtime de la app (solo genera archivos de esquema en tiempo de compilación bajo `app/schemas/`), y deja a Room en condiciones de validar migraciones futuras contra el historial real de esquemas, algo que antes no era posible.

## 11. Validación de esta sesión

- `gradlew.bat assembleDebug` — `BUILD SUCCESSFUL`, sin errores.
- Migración `19 → 20` verificada contra un dispositivo real con datos preexistentes (detalle completo en sección 2) — no destructiva, `integrity_check` `ok`, todos los conteos de tablas del Inspector iguales antes y después.

## 12. Pendiente / no incluido en esta ronda

- ~~Prueba manual en modo avión~~ — ejecutada y verificada de punta a punta en v1.9.4 (emulador con wifi apagado, guardado + evidencia offline, reconexión, sync, verificado contra Postgres real). Ver `DOCUMENTACION_PROYECTO.md` v1.9.4.
- ~~El bug de "HTTP 401 Unauthorized"~~ — causa raíz más probable identificada (mismatch histórico de `BASE_URL`, ya cerrado estructuralmente) y un gap de arquitectura real corregido en v1.9.4: un 401 en un sync silencioso en background ya no fuerza un logout global. Ver `DOCUMENTACION_PROYECTO.md` v1.9.4 para el detalle honesto de qué se pudo y no se pudo reproducir.
- ~~Toggle Hoy/Ayer explícito en la UI~~ — resuelto en v1.9.3 (ver `DOCUMENTACION_PROYECTO.md`).
- No se implementó detección de conflicto (tipo 409/`CONFLICT`) — mismo alcance que el Inspector, que tampoco lo tiene.
- Prueba en dispositivo físico real y en build de release (todo lo probado hasta ahora fue en emulador AVD, en debug).
