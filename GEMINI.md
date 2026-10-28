# Custom Rules — PresupuestoCR (Antigravity)
Guía de buenas prácticas para la generación de código con Antigravity.

## 1. Identidad del Proyecto
Configuración base del proyecto sobre la que deben aplicarse todas las reglas.

| Propiedad | Valor |
| :--- | :--- |
| **Nombre de la app** | PresupuestoCR |
| **Tipo** | Aplicación móvil de registro de ingresos y gastos personales |
| **Stack principal** | Kotlin + Jetpack Compose + Material3 + Room Database |
| **Build** | Gradle 8.6 / Java 17 |
| **OCR** | ML Kit Text Recognition |
| **Backup** | Google Drive API |

---

## 2. Reglas de Código

### 2.1 Estructura de carpetas
- Respetar siempre la estructura actual de carpetas: `data/`, `domain/`, `ui/`, `util/`, `service/`, `worker/`
- No crear archivos sueltos en la raíz del paquete — cada archivo va en su carpeta correspondiente
- Nuevas funcionalidades deben seguir el patrón existente: `Model → DAO → Repository → ViewModel → Screen`

### 2.2 Imports
- NUNCA dejar imports pegados en una sola línea — cada import en su propia línea
- Siempre incluir TODOS los imports necesarios — no asumir que ya existen
- Verificar que cada import corresponda a una dependencia ya presente en `build.gradle.kts`
- Si una nueva dependencia es necesaria, indicarla explícitamente con la versión exacta

### 2.3 Dependencias
- Antes de agregar una dependencia, verificar que la versión exista en Maven/Google Repository
- Usar versiones estables (no beta, no alpha, no RC) salvo que sea estrictamente necesario
- Si el proyecto usa Version Catalog (`libs.versions.toml`), agregar ahí las nuevas versiones

### 2.4 Compilación
- Todo código generado DEBE compilar sin errores con `assembleDebug`
- No usar APIs deprecadas (ejemplo: `AndroidHttp`) — usar siempre la alternativa moderna (`NetHttpTransport`)
- No acceder a propiedades internas de Material3 (ejemplo: `indicatorColor`) — usar las APIs públicas
- Verificar compatibilidad con el `compileSdk` y `targetSdk` del proyecto

---

## 3. Reglas de Diseño UI

| Regla UI | Detalle |
| :--- | :--- |
| **Colores principales** | Verde (`#4CAF50`) para ingresos, Rojo (`#F44336`) para gastos, Azul (`#2196F3`) para el tema general |
| **Consistencia visual** | Mantener el mismo estilo de tarjetas, botones y tipografía en TODAS las pantallas. No mezclar estilos diferentes — si una pantalla usa `Card` con `elevation`, todas deben usarlos. Usar Material3 para todos los componentes — no mezclar Material2 con Material3 |
| **Navegación** | Barra inferior siempre visible con 5 secciones: `Inicio`, `Transacciones`, `Reportes`, `Calendario`, `Configuración`. Toda nueva pantalla debe integrarse al `NavGraph` existente. Cada pantalla debe tener navegación de regreso funcional |
| **Responsive** | Usar `Modifier.fillMaxWidth()` en lugar de anchos fijos. Textos largos deben tener `overflow` controlado (`TextOverflow.Ellipsis` o scroll). Listas largas deben usar `LazyColumn`, nunca `Column` con scroll |

---

## 4. Reglas de Base de Datos

| Área | Reglas |
| :--- | :--- |
| **Entidades** | Toda entidad debe tener un `@PrimaryKey(autoGenerate = true)`. Nombres de tablas en `snake_case` en inglés (`transactions`, `categories`, `payment_reminders`, `accounts`, `budgets`, `bcr_pending_transactions`). Incluir campos de auditoría: `created_at`, `updated_at` como `Long` (timestamp) |
| **DAO** | Toda entidad CRUD debe tener: `insert()`, `update()`, `delete()`, `getAll()`, `getById()`. Queries de filtro deben usar `@Query` con parámetros — nunca concatenar strings. Funciones de escritura/consulta puntual del DAO deben ser `suspend` (coroutines) |
| **Migraciones** | Si se modifica una entidad existente, incluir la `Migration` correspondiente. Nunca usar `fallbackToDestructiveMigration()` en producción — solo en desarrollo |

---

## 5. Reglas de Funcionalidad

| Funcionalidad | Reglas |
| :--- | :--- |
| **Transacciones** | Toda transacción debe tener: fecha, descripción, categoría, monto, tipo (ingreso/gasto). Eliminar siempre pide confirmación con diálogo antes de borrar. Editar abre el mismo formulario con datos pre-cargados |
| **Gráficos** | Los gráficos deben ser interactivos — al tocar una sección, navegar al detalle o mostrar selección. Mostrar tooltip con monto y porcentaje al presionar. Funcionar tanto en modo claro como oscuro |
| **Notificaciones** | Usar `WorkManager` para notificaciones programadas — no `AlarmManager`. El usuario elige cuándo recibir la notificación (1 día antes, mismo día, 1 hora antes). Incluir los permisos necesarios en `AndroidManifest.xml` |
| **Imágenes** | Guardar imágenes como URI (`String`) en Room — no como `ByteArray`. Permitir galería y cámara como fuente. Mostrar miniaturas optimizadas en las listas — cargar imagen completa solo al hacer zoom |

---

## 6. Reglas de Calidad

| Área de calidad | Reglas |
| :--- | :--- |
| **Antes de entregar código** | Verificar que TODOS los imports estén presentes y correctos. Verificar que TODAS las dependencias necesarias estén en `build.gradle.kts` con versión válida. No dejar código comentado, TODOs, ni placeholders sin completar. No dejar funciones vacías o con `TODO("Not yet implemented")` |
| **Manejo de errores** | Toda operación de base de datos debe estar envuelta en `try/catch`. Toda llamada de red (Google Drive, Gemini) debe manejar errores y mostrar mensaje al usuario. Nunca mostrar stacktraces o errores técnicos al usuario — usar mensajes amigables |
| **Performance** | No hacer queries de Room en el hilo principal — siempre usar coroutines. Imágenes grandes deben cargarse con Coil — nunca cargar bitmaps grandes directamente en listas. Listas con más de 20 items deben usar paginación o `LazyColumn` |

---

## 7. Formato de Respuesta
Antigravity debe seguir estas pautas de formato al entregar cualquier resultado:
1. Cuando generes código, indica SIEMPRE el archivo completo con su ruta (ejemplo: `app/src/main/java/com/presupuesto/cr/ui/transactions/TransactionsScreen.kt`)
2. Si un cambio requiere modificar múltiples archivos, listar TODOS los archivos afectados al inicio
3. Si una nueva dependencia es necesaria, indicarla primero antes del código
4. Confirmar que el código compila antes de entregarlo

Estas reglas aplican a todas las sesiones de generación de código para **PresupuestoCR** en Antigravity.
