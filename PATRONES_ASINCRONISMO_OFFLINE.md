# Patrones de asincronismo y offline-first — lecciones reutilizables

Este documento es **agnóstico de Mantec Inspector**. Reúne los patrones de manejo asincrónico
(red, caché local, coroutines, timeouts) que fuimos aprendiendo a los golpes en este proyecto,
para no repetir los mismos errores en la próxima app. Cada patrón trae: la regla general, por qué
importa, cómo se manifestó acá (como caso de estudio concreto) y cómo aplicarlo desde cero en un
proyecto nuevo.

No asume ningún framework específico — aplica a cualquier app (Android/Kotlin, pero los principios
son igual de válidos en iOS, web con service workers, etc.) que necesite funcionar con conectividad
intermitente.

---

## 1. Local-first, no servidor-first-con-fallback

**Regla:** cuando una pantalla puede mostrar datos que ya tenés cacheados localmente, mostralos
**primero**, siempre. La red es una *mejora en segundo plano*, nunca el camino que hay que esperar
para pintar algo.

**Por qué importa:** el orden inverso (intentar el servidor, y solo si falla usar la caché) parece
razonable en papel, pero en la práctica significa que **cada interacción del usuario paga el timeout
completo de red** antes de mostrar nada, incluso cuando ya tenías el dato disponible localmente. Con
un timeout de conexión típico de 15–30s, y un usuario interactuando decenas de veces por turno en
una zona de señal mala, esto se siente como que la app "no funciona", aunque técnicamente sí
funciona (solo que después de medio minuto).

**Cómo se manifestó acá:** un selector de "Condición" en un formulario de inspección intentaba
`GET /elements/{id}/conditions` primero, y solo caía a Room si la llamada fallaba. Con
`connectTimeout = 20s`, cada tap sin señal se colgaba 20 segundos antes de mostrar el fallback local
que ya estaba disponible instantáneamente.

**Cómo aplicarlo en un proyecto nuevo:**
- Toda función `loadX()` que tenga tanto una fuente local como remota debe: `emit(local)` primero
  (síncrono o casi), y lanzar el refresh remoto aparte, actualizando el estado solo si el remoto
  responde. Nunca `await(remoto) → fallback(local) en catch`.
- Si necesitás refrescar varias fuentes en paralelo (ej. N pares área/tipo), usá
  `coroutineScope { ...map { async {...} }.awaitAll() }` en vez de un loop secuencial — evita que
  el tiempo total sea "suma de N requests" en lugar de "el más lento de los N".
- Bajá los timeouts de conexión a algo agresivo (5–10s) si el patrón ya es local-first — ya no hay
  excusa para que un timeout largo bloquee la UI, porque la UI no depende de que la llamada
  responda.

---

## 2. El patrón PENDING_SYNC → SYNCED (o cómo no perder nada)

**Regla:** toda acción del usuario que deba llegar a un servidor se escribe primero en almacenamiento
local con un estado explícito (`PENDING_SYNC`), y solo se marca `SYNCED` cuando el servidor confirma.
La sincronización es un proceso aparte, reintentable, que nunca es la única copia del dato.

**Por qué importa:** en un entorno con conectividad intermitente, si el "guardar" del usuario
*es* la llamada de red, cualquier corte a mitad de camino pierde el trabajo. Separar "guardar
localmente" de "sincronizar" hace que perder la señal a mitad de una acción sea, en el peor caso,
una demora — nunca una pérdida de datos.

**Cómo se manifestó acá:** cada reporte de inspección se graba en Room con `syncStatus =
PENDING_SYNC` antes de intentar `POST /reports/sync`. Distintos triggers (WorkManager en
background, entrada a la pantalla principal, botón manual) intentan sincronizar los pendientes; si
fallan, el registro se queda tal cual, listo para el próximo intento.

**Refinamiento importante — no todo fallo es igual:** un fallo *transitorio* (sin red, timeout, 5xx)
debe reintentarse indefinidamente. Un fallo *permanente* (422 de validación, archivo rechazado por
tamaño) debe marcarse con un estado terminal (ej. `ERROR`) y dejar de reintentarse — si no,
un ítem irrecuperable queda consumiendo ciclos de sync para siempre sin nunca poder tener éxito.
Distinguí ambos casos por el código de respuesta, no trates todo fallo igual.

**Cómo aplicarlo en un proyecto nuevo:**
- Definí el estado de sync como parte del modelo de datos local desde el día uno, no como
  algo que se agrega después.
- Separá claramente "persistir" (siempre local, siempre inmediato) de "sincronizar" (best-effort,
  reintentable, en background).
- Al diseñar el enum de estados, contemplá al menos: pendiente, sincronizado, error-permanente. Un
  solo booleano `isSynced` no alcanza para distinguir "todavía no se pudo" de "nunca se va a poder".

---

## 3. No bufferices en memoria lo que puede ser arbitrariamente grande

**Regla:** cualquier cosa que lea un cuerpo de red completo, un archivo completo, o una respuesta
completa a memoria (`String`, `ByteArray`, `List` completo) antes de procesarla es una bomba de
tiempo si ese payload puede crecer sin límite conocido. Usá streaming.

**Por qué importa:** funciona perfecto en desarrollo con payloads chicos y dispositivos con RAM de
sobra, y explota en producción con datos reales grandes en dispositivos de gama baja — que es
exactamente donde termina la app cuando el usuario real la usa en el mundo real.

**Cómo se manifestó acá dos veces, con la misma causa raíz en dos lugares distintos:**
1. Un interceptor de logging HTTP en modo verbose (`Level.BODY`) leía el cuerpo completo de la
   respuesta a `String` para loguearlo, sin importar el tamaño — un catálogo de ~145 MB tiraba
   `OutOfMemoryError` en un dispositivo con ~24 MB libres.
2. Subida de evidencia de video: el archivo se leía completo a memoria antes de armar el
   `multipart/form-data`, en vez de transmitirlo en streaming desde disco.

**Cómo aplicarlo en un proyecto nuevo:**
- Nunca actives logging verbose (`BODY`) de interceptores HTTP en builds de release; en debug,
  preferí `HEADERS` salvo que estés depurando algo puntual.
- Para uploads de archivos, usá siempre streaming (leer y escribir en chunks) — la librería HTTP que
  uses casi seguro ya soporta esto (`RequestBody.create(file, ...)` en OkHttp, por ejemplo), no lo
  reinventes leyendo todo a un `ByteArray` primero.
- Cualquier endpoint que devuelva "todo el catálogo" o "todos los registros" sin paginar es un
  riesgo de crecimiento sin límite — al menos medí cuál es el tamaño real en el peor cliente antes
  de asumir que "siempre va a ser chico".

---

## 4. Auditoría de completitud de caché + reparación condicionada a conectividad

**Regla:** si tu app depende de que una caché local esté *completa* (no solo "algo" cacheado, sino
"todo lo necesario"), no asumas que la descarga inicial salió bien. Auditá la caché contra su propio
criterio de completitud cuando haya oportunidad (usuario en una pantalla clave, con conectividad
disponible), y repará automáticamente si podés.

**Por qué importa:** una descarga puede fallar a mitad de camino, quedar desactualizada respecto a
cambios del servidor posteriores, o simplemente nunca haber incluido algo por un bug puntual. Sin
una auditoría activa, el usuario se entera del hueco recién cuando lo choca en el peor momento
posible — típicamente sin conectividad para arreglarlo ahí mismo.

**Cómo se manifestó acá:** un catálogo completo se descarga al login y queda cacheado. Un inspector
se encontró, semanas después y sin señal, con que un componente puntual no tenía sus opciones de
"condición" disponibles — el dato nunca se había actualizado localmente desde que se creó en el
servidor. La solución no fue "arreglar la descarga" (la descarga estaba bien, era un problema de
timing/staleness) sino agregar una verificación activa: al entrar a la pantalla principal, auditar
localmente qué falta, y si hay red, reparar solo; si no hay red, avisar en vez de fallar en
silencio.

**Cómo aplicarlo en un proyecto nuevo:**
- Definí explícitamente qué significa "caché completa" para tu dominio (ej. "todo componente tiene
  al menos un diagnóstico y una condición asociada").
- Enganchá la auditoría a un punto de entrada de bajo costo y alta frecuencia (pantalla principal,
  `onResume`), no a cada acción del usuario.
- La reparación debe ser *condicional* a que haya conectividad, y su fallo debe ser silencioso para
  el flujo (no bloquear, no crashear) pero visible para el usuario (banner, indicador) — la meta es
  que el usuario decida si vale la pena esperar a tener señal antes de irse a un lugar sin ella.
- Si la reparación automática falla persistentemente con conectividad disponible, ese es el momento
  de investigar del lado del servidor — la app ya hizo su parte.

---

## 5. Las migraciones destructivas de esquema son una bomba en producción

**Regla:** en una app offline-first, `fallbackToDestructiveMigration()` (Room) o equivalente en
otras librerías de storage local es aceptable en desarrollo temprano, pero es un riesgo serio en
producción: cualquier cambio de esquema entre versiones borra **todo** el storage local del
usuario, incluyendo datos que todavía no se sincronizaron.

**Por qué importa:** el impacto no es "se rompe la app" (eso se nota y se reporta rápido) — es
"se pierden silenciosamente datos que el usuario ya dio por guardados", que es mucho peor y más
difícil de detectar.

**Cómo se manifestó acá:** documentado como riesgo crítico desde el análisis inicial del proyecto,
nunca disparado en producción hasta ahora porque la versión de esquema no volvió a cambiar — pero
sigue siendo una migración destructiva configurada, esperando a que el día que alguien agregue un
campo a una entidad, se lleve puesto cualquier reporte pendiente de sincronizar en todos los
dispositivos que actualicen.

**Cómo aplicarlo en un proyecto nuevo:**
- Empezá con migraciones explícitas (`addMigrations()` en Room, o el equivalente) desde que la app
  tiene el primer usuario real con datos que importan — no esperes a "cuando el esquema se
  estabilice", porque nunca se estabiliza del todo.
  Si usás migración destructiva temporalmente en desarrollo, hacelo explícito y condicionado a
  build de debug, nunca en el build que se distribuye.

---

## 6. Sesión/credenciales y datos cacheados son capas de persistencia distintas

**Regla:** en general vas a tener al menos dos storages locales con ciclos de vida distintos: uno
para credenciales/sesión (chico, se lee siempre) y otro para datos de dominio cacheados (grande, se
reconstruye). Entendé explícitamente qué sobrevive a qué evento (cerrar la app, que el SO mate el
proceso, actualizar la app, limpiar caché vs. limpiar datos) para cada uno por separado.

**Por qué importa:** un bug que parece "se cerró la sesión" puede en realidad ser "se vació la caché
de datos pero la sesión sigue viva" — son síntomas distintos con causas y fixes distintos, y
confundirlos hace perder tiempo de diagnóstico.

**Cómo se manifestó acá:** la sesión vive en `SharedPreferences` (sobrevive a que el proceso muera
en background); el catálogo vive en Room (mismo storage, pero se reconstruye completo si el
esquema cambia). Un reporte de campo de "no veo el catálogo, pero no me pidió loguearme de nuevo"
fue la pista clave para diferenciar "perdí la sesión" (no pasó) de "el catálogo local está
incompleto o viejo" (sí pasó) — dos causas completamente distintas que un reporte de usuario
ambiguo podía confundir fácilmente.

**Cómo aplicarlo en un proyecto nuevo:**
- Documentá, para cada storage local que uses, qué lo sobrevive y qué lo borra.
- Cuando investigues un bug de "algo desapareció", lo primero es identificar en qué storage vivía
  ese dato — la causa probable cambia completamente según la respuesta.

---

## 7. El proceso puede morir en cualquier momento — el arranque en frío debe ser offline-safe

**Regla:** en móvil, el sistema operativo puede matar tu proceso en background sin avisar (por
memoria, batería, o simplemente tiempo). La ruta de "restaurar sesión al arrancar" tiene que
funcionar sin red si los datos que necesita ya están cacheados localmente — no asumas que arrancar
la app es equivalente a loguearse.

**Por qué importa:** si tu lógica de arranque intenta contactar al servidor "por las dudas" antes
de mostrar la pantalla principal, cualquier usuario que reabra la app sin señal (después de que el
SO mató el proceso en background, algo muy común en dispositivos de gama baja) se encuentra
bloqueado o deslogueado sin motivo real.

**Cómo se manifestó acá:** la restauración de sesión al arrancar (`onCreate`) lee perfil y catálogo
100% de Room cuando los datos ya están completos — sin tocar la red. Solo intenta re-descargar del
servidor si detecta una anomalía real (datos locales vacíos), y ese es el único camino que podría
fallar sin conectividad. El camino normal (datos completos, proceso recreado, sin señal) es
totalmente offline-safe por diseño.

**Cómo aplicarlo en un proyecto nuevo:**
- Separá explícitamente "¿tengo sesión guardada?" (chequeo local) de "¿está todo actualizado?"
  (chequeo que puede necesitar red). Lo primero nunca debería depender de lo segundo.
- Los intentos de refresco en background (`onResume`, auto-sync) deben fallar en silencio si no hay
  red — nunca deslogueado automático ni bloqueo de UI solo porque un refresh opcional no pudo
  completarse.

---

## 8. Los timeouts "razonables" no lo son cuando se repiten todo el día

**Regla:** un timeout que parece aceptable probado una vez en la oficina con buen WiFi (15–20s) se
siente completamente distinto cuando el usuario real lo sufre decenas de veces por turno en una
zona de señal mala. Probá explícitamente en modo avión / red cortada, no solo con buena señal.

**Por qué importa:** el testing normal de desarrollo casi nunca reproduce el entorno real de uso
(fábrica, sótano, zona rural) — es fácil que un timeout problemático pase inadvertido hasta que
llega un reporte de campo.

**Cómo aplicarlo en un proyecto nuevo:**
- Como parte del checklist de pruebas de cualquier feature que toque red, incluí explícitamente:
  "probar con modo avión activado, no solo con buena conexión".
- Si el patrón ya es local-first (punto 1), el timeout de conexión puede bajarse agresivamente sin
  costo para el usuario, porque la UI ya no depende de que la llamada responda.

---

## Checklist rápido para el próximo proyecto

Al arrancar una app nueva que necesite trabajar con conectividad intermitente, definir desde el
diseño inicial:

- [ ] ¿Qué pantallas necesitan datos que deberían venir de caché local primero, servidor como
      mejora en segundo plano?
- [ ] ¿Qué acciones del usuario necesitan el patrón pendiente→sincronizado, y cómo se distingue un
      fallo transitorio de uno permanente?
- [ ] ¿Qué endpoints/archivos pueden crecer sin límite conocido, y están usando streaming en vez de
      bufferizar completo?
- [ ] ¿Qué significa "caché completa" para el dominio de esta app, y quién audita eso, cuándo?
- [ ] ¿Las migraciones de esquema del storage local son explícitas, o hay una destructiva
      configurada "temporalmente" que nadie sacó?
- [ ] ¿Está clara la diferencia entre storage de sesión y storage de datos cacheados, y qué
      sobrevive a qué?
- [ ] ¿El arranque en frío (proceso muerto, reabrir la app) funciona sin red si los datos ya están
      completos localmente?
- [ ] ¿Se probaron los timeouts de red explícitamente en modo avión, no solo con buena señal?
