# 🇨🇷 Presupuesto Inteligente CR - Finanzas Personales Avanzadas

Aplicación móvil nativa para Android desarrollada en **Kotlin** y **Jetpack Compose** con arquitectura **Clean Architecture + MVVM**, diseñada específicamente para las dinámicas financieras y salariales de Costa Rica (ciclos quincenales, moneda ₡ / CRC, integración Banco de Costa Rica BCR, y respaldo seguro en Google Drive).

---

## 🏛️ Arquitectura del Sistema

```
app/
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/presupuesto/cr/
│   │   ├── PresupuestoApp.kt                  # Application class + SQLCipher init
│   │   ├── MainActivity.kt                    # Entry point + AndroidX BiometricPrompt
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── entity/                    # AccountEntity, CategoryEntity, TransactionEntity...
│   │   │   │   ├── dao/                       # Room DAOs con soporte reactivo Flow
│   │   │   │   ├── Converters.kt              # Room TypeConverters
│   │   │   │   └── AppDatabase.kt             # Room DB + SQLCipher AES-256 + Seed CR Data
│   │   │   └── repository/
│   │   │       ├── FinanceRepository.kt       # Interface de abstracción
│   │   │       └── FinanceRepositoryImpl.kt   # Implementación atómica de saldos
│   │   ├── domain/
│   │   │   ├── model/                         # Domain Models (SafeToSpendResult, ForecastResult...)
│   │   │   └── usecase/
│   │   │       ├── CalculateSafeToSpendUseCase.kt # Algoritmo Dinero Libre Real
│   │   │       ├── ForecastEngineUseCase.kt   # Proyección predictiva 90d/180d
│   │   │       ├── ProcessGeminiVoiceUseCase.kt # Google Gemini AI NLP
│   │   │       ├── ParseBcrNotificationUseCase.kt # Parser BCR y SINPE Móvil
│   │   │       ├── ExportPdfReportUseCase.kt  # Generador de reportes PDF ejecutivos
│   │   │       └── GoogleDriveBackupUseCase.kt# Respaldo/Restauración Google Drive
│   │   ├── service/
│   │   │   └── BcrNotificationListenerService.kt # Lector de Notificaciones BCR
│   │   ├── ui/
│   │   │   ├── auth/BiometricAuthHelper.kt    # Autenticación biométrica con fallback
│   │   │   ├── ocr/ReceiptScannerHelper.kt    # Escáner de facturas con Google ML Kit
│   │   │   ├── theme/                         # Material Design 3 (Esmeralda & BCR Navy)
│   │   │   ├── navigation/                    # Bottom Navigation (4 secciones)
│   │   │   ├── home/                          # Pantalla Inicio + Safe-to-Spend + Gemini Hold-to-Talk
│   │   │   ├── transactions/                  # Pantalla Transacciones + Bandeja BCR + OCR
│   │   │   ├── reports/                       # Pantalla Reportes + Gráficos Canvas + PDF
│   │   │   └── settings/                      # Configuración, Biometría y Google Drive
│   │   └── util/
│   │       ├── CurrencyFormatter.kt           # Formateo oficial costarricense (₡ / CRC)
│   │       └── DateUtils.kt                   # Manejo de quincenas (15 y 30) y fechas CR
```

---

## 💡 Características Principales

### 1. Algoritmo "Dinero Libre Real" (Safe-to-Spend)
En lugar de mostrar el saldo bruto total, la pantalla de inicio calcula en tiempo real:
$$\text{Dinero Libre Hoy} = \frac{\text{Saldo Líquido Total} - \text{Gastos Fijos Pendientes} - \text{Aportes Metas Ahorro} - \text{Cuotas BCR}}{\text{Días restantes en la quincena}}$$
- **Semáforo Financiero**:
  - 🟢 **Excelente**: $\ge$ ₡18.000 / día.
  - 🔵 **Estable**: ₡7.000 - ₡17.999 / día.
  - 🟡 **Precaución**: ₡2.000 - ₡6.999 / día.
  - 🔴 **Riesgo Crítico**: $<$ ₡2.000 / día.

### 2. Registro por Voz con Gemini AI (Hold-to-Talk)
- Mantén presionado el botón central del micrófono y habla naturalmente:
  > *"Gasté 15 mil colones en carnes en el súper con la tarjeta BCR"*
- La app envía la transcripción al modelo **Gemini 1.5 Flash**, que reconoce modismos ticos (*rojo*, *teja*, *tucán*, *quincena*) y devuelve un JSON estructurado con monto, categoría, cuenta y nota.
- Presenta una tarjeta modal previa para guardar en 1 toque.
- Incluye **parser heurístico local offline** que funciona sin internet si el dispositivo pierde conectividad.

### 3. Automatización Banco de Costa Rica (BCR)
- Servicio `BcrNotificationListenerService` que detecta en segundo plano compras de tarjetas de débito/crédito BCR y transferencias SINPE Móvil.
- Las coloca en la **"Bandeja BCR / Por Clasificar"** con sugerencias automáticas de categorías (AutoMercado/Walmart/Palí $\to$ Comida, Carnicerías $\to$ Carnes, CNFL/ICE/Kolbi $\to$ Servicios).

### 4. Motor Predictivo de Flujo de Caja (Forecast a 3 y 6 meses)
- Algoritmo en Kotlin que simula día a día la velocidad de gasto variable y los eventos futuros (ingresos quincenales los días 15 y 30, vencimiento de servicios públicos, diezmo y cuotas de tarjetas).
- Detecta **"Valles Críticos"** donde el saldo cae por debajo de ₡25.000 y genera alertas preventivas.
- Gráfico interactivo en **Jetpack Compose Canvas** con curva suavizada y relleno degradado.

### 5. Escáner de Comprobantes (OCR) con Google ML Kit
- Toma una foto a tu tiquete de caja en AutoMercado, Walmart, Palí o carnicerías.
- Extrae el monto total, comercio y fecha de forma instantánea.

### 6. Exportador de Reportes PDF Ejecutivo
- Genera un reporte imprimible con `android.graphics.pdf.PdfDocument`.
- Incluye métricas de Dinero Libre, patrimonio neto consolidado, desglose por categorías y últimas transacciones. Listo para compartir por WhatsApp, correo o guardar.

### 7. Seguridad y Respaldo en la Nube
- Base de datos local cifrada grado militar con **SQLCipher (AES-256)**.
- Bloqueo biométrico con **AndroidX Biometric** (Huella dactilar, Face Unlock o PIN).
- Respaldo y restauración en Google Drive asociado a **`tavo03079999@gmail.com`**.

---

## 🚀 Guía de Configuración y Ejecución

### Requisitos Previos
1. **Android Studio Iguana | Jellyfish (2024+)** o superior.
2. **JDK 17** configurado en Android Studio (`Settings > Build > Build Tools > Gradle > Gradle JDK`).
3. Dispositivo físico Android con biometría o emulador con API 26 o superior (API 34 recomendada).

### Paso 1: Configurar la API Key de Google Gemini
1. Obtén tu API key gratuita en [Google AI Studio](https://aistudio.google.com/).
2. En la raíz del proyecto, crea un archivo llamado `local.properties` (puedes basarte en `local.properties.example`):
   ```properties
   sdk.dir=C\:\\Users\\gustagox\\AppData\\Local\\Android\\Sdk
   GEMINI_API_KEY=AIzaSy...TuClaveAqui
   ```
3. El archivo `app/build.gradle.kts` inyectará automáticamente esta clave en `BuildConfig.GEMINI_API_KEY`.

### Paso 2: Configurar Google Sign-In para Google Drive
1. Ve a [Google Cloud Console](https://console.cloud.google.com/).
2. Crea un proyecto y habilita la **Google Drive API**.
3. En la pantalla de consentimiento OAuth, agrega el correo `tavo03079999@gmail.com` como usuario de prueba.
4. Genera un ID de cliente OAuth de Android con el paquete `com.presupuesto.cr` y el hash SHA-1 de tu keystore de depuración.

### Paso 3: Abrir y Compilar en Android Studio
1. Abre Android Studio y selecciona **Open**, eligiendo la carpeta `c:\Users\gustagox\Desktop\presupuestos`.
2. Espera a que Gradle sincronice las dependencias del catálogo `libs.versions.toml`.
3. Conecta tu teléfono Android o inicia un emulador.
4. Haz clic en el botón verde **Run 'app'** (o presiona `Shift + F10`).

### Paso 4: Habilitar el Lector de Notificaciones BCR en el Teléfono
1. En la app, dirígete a la pestaña **Ajustes**.
2. En la sección **Automatización Banco de Costa Rica (BCR)**, toca **Configurar Permiso de Notificaciones**.
3. Android te llevará a los ajustes del sistema: activa el interruptor para **Presupuesto Inteligente CR**.
4. ¡Listo! Cada compra que hagas con el BCR o SINPE Móvil se capturará automáticamente.
