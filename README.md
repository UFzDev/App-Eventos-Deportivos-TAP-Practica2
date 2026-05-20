# Eventos Deportivos ⚽🏆

Una sofisticada aplicación móvil nativa para **Android** desarrollada en **Kotlin** bajo los más altos estándares de ingeniería y diseño de interfaces. La aplicación ofrece a los usuarios un dashboard deportivo premium para consultar noticias de fútbol de última hora (Currents API), buscar clubes de fútbol mundiales y ver sus plantillas oficiales, consultar marcadores e historial de partidos (API-Football v3), y gestionar un sistema de favoritos persistente en tiempo real respaldado por la nube de **Firebase**.

---

## ✨ Características Principales

*   **📰 Feed de Noticias Interactivo:**
    *   Integración directa con **Currents API** para consumir noticias de fútbol en tiempo real en español.
    *   Diseño visual premium con tarjetas dinámicas y una cabecera destacada estilo *Hero Card* animada ("EN VIVO").
    *   Navegación integrada para abrir los artículos completos en el navegador web móvil de forma fluida.
*   **🛡️ Buscador de Equipos y Plantillas:**
    *   Buscador interactivo en tiempo real con la base de datos global de **API-Football**.
    *   Visualización de información detallada (año de fundación, ciudad, estadio y escudo oficial).
    *   **Plantillas Oficiales:** Al pulsar sobre cualquier equipo, se despliega un `BottomSheetDialog` deslizable que consulta la plantilla de jugadores oficial en tiempo real, detallando su nombre, edad, posición e imagen.
*   **📊 Historial y Marcadores de Partidos:**
    *   Sección dedicada a consultar resultados históricos y marcadores de partidos con escudos oficiales optimizados.
*   **❤️ Sistema de Favoritos Sincronizado en Tiempo Real (Firebase):**
    *   **Soporte de Invitados (UX Premium):** Los usuarios anónimos experimentan una interfaz de bloqueo estética con un candado vectorizado y una invitación elegante a iniciar sesión. Los botones de favorito alertan interactivamente al usuario con Toasts premium explicativos.
    *   **Persistencia Reactiva en Firestore:** Para usuarios registrados, sus favoritos se sincronizan de forma bidireccional instantánea bajo la ruta `/users/{uid}/favorites/{itemId}`.
    *   **Desmarca Animado:** Permite eliminar elementos directamente desde el dashboard de favoritos, actualizando la interfaz de manera reactiva con transiciones fluidas.

---

## 🛠️ Stack Tecnológico

*   **Lenguaje:** Kotlin 100% nativo bajo prácticas de clean code y modularización limpia.
*   **UI/UX:** Material Design Components, ConstraintLayout, NestedScrollView, dialogs BottomSheet premium y celdas RecyclerView adaptativas (horizontales y verticales).
*   **Firebase Suite:**
    *   **Firebase Authentication:** Login seguro por correo electrónico y contraseñas, registro de nuevos usuarios y soporte de autenticación anónima para invitados.
    *   **Cloud Firestore:** Base de datos NoSQL en tiempo real para albergar favoritos.
*   **Red y Serialización:**
    *   **Retrofit 2:** Cliente HTTP optimizado para consumos asíncronos concurrentes.
    *   **OkHttp 3:** Interceptores de red y cabeceras de seguridad.
    *   **Gson:** Deserialización y serialización instantánea de payloads JSON en memoria.
*   **Procesamiento de Imágenes:**
    *   **Glide:** Carga asíncrona de imágenes y escudos con placeholder de carga y `LazyHeaders` para simular un User-Agent seguro, evitando denegaciones de acceso (HTTP 403) de servidores deportivos estrictos.

---

## 📐 Arquitectura de Persistencia de Favoritos

Diseñamos una arquitectura **polimórfica** extremadamente eficiente y tolerante a fallos para la gestión de favoritos:

### 1. Modelo de Datos Unificado (`FavoriteItem`)
Para optimizar las conexiones de red y simplificar la base de datos a un solo listener en tiempo real, definimos la clase de datos:
```kotlin
data class FavoriteItem(
    val id: String = "",
    val type: String = "", // "news", "team", "match"
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val dataJson: String = "", // Payload original en JSON
    val timestamp: Long = System.currentTimeMillis()
)
```

### 2. Hashing Determinista MD5 (Seguridad de Segmentos)
*   **El Problema:** Firestore prohíbe el uso de diagonales (`/`) en los IDs de los documentos porque los interpreta como delimitadores de subcolecciones, lo que provocaba crashes fatales en versiones previas al intentar guardar URLs de noticias como clave de documento.
*   **La Solución:** Implementamos una función de hashing criptográfica de longitud fija en `FavoritesManager.getSafeId()` que calcula el hash **MD5** de la URL, sanitizando las claves de forma infalible antes de consultar o escribir en Firestore.

### 3. Resiliencia contra Datos Obsoletos y Crashes
*   **Deserialización Segura:** `FavoritesManager.listenToFavorites` recorre y deserializa cada documento de forma individual en bloques `try-catch`. Si un documento almacenado en Firestore pertenece a una estructura previa u obsoleta de la app, el sistema lo captura de forma segura, ejecuta un fallback de parsing manual de campos básicos y continúa la ejecución sin provocar cuelgues o crashes en la app.
*   **Toast Informativo de Diagnóstico:** Los callbacks `onFailure` recuperan la descripción exacta de la excepción de Firebase y la muestran en Toasts explicativos en pantalla. Esto te permite saber de inmediato en tu emulador si una operación falló por falta de permisos de base de datos (`PERMISSION_DENIED`) para corregirlo al instante.

---

## ⚙️ Configuración del Entorno y API Keys

La aplicación utiliza variables de entorno inyectadas de forma segura en tiempo de compilación a través del archivo `build.gradle.kts` mediante **BuildConfig**.

### 1. Agregar tus llaves en `local.properties`
Abre el archivo `local.properties` en la raíz de tu proyecto y define tus llaves del siguiente modo:
```properties
CURRENTS_API_KEY=tu_api_key_de_currents_aquí
API_SPORTS_KEY=tu_api_key_de_api_football_aquí
```

### 2. Propagación en Gradle (`app/build.gradle.kts`)
El archivo Gradle lee estas propiedades y las inyecta de forma segura:
```kotlin
android {
    ...
    buildTypes {
        release {
            ...
        }
        debug {
            val localProperties = java.util.Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localProperties.load(localPropertiesFile.inputStream())
            }

            val currentsKey = localProperties.getProperty("CURRENTS_API_KEY") ?: ""
            val sportsKey = localProperties.getProperty("API_SPORTS_KEY") ?: ""

            buildConfigField("String", "CURRENTS_API_KEY", "\"$currentsKey\"")
            buildConfigField("String", "API_SPORTS_KEY", "\"$sportsKey\"")
        }
    }
}
```

---

## 🚀 Instrucciones para Compilar y Ejecutar

### Requisitos Previos:
*   Android Studio Ladybug (o posterior).
*   Java Development Kit (JDK) 17.
*   Dispositivo virtual Android (Emulador) o físico con API Level 26 (Android 8.0) o superior.

### Paso 1: Clonar e Importar el Proyecto
1.  Clona este repositorio o importa el código fuente en tu espacio de trabajo.
2.  Abre Android Studio y selecciona **Open an Existing Project** apuntando al directorio raíz.
3.  Espera a que finalice la sincronización de Gradle de forma satisfactoria.

### Paso 2: Vincular tu Proyecto de Firebase
1.  Crea un nuevo proyecto en tu [Firebase Console](https://console.firebase.google.com/).
2.  Registra una nueva aplicación Android en el proyecto con el paquete `ufz.dev.eventosdeportivos`.
3.  Descarga el archivo `google-services.json` y colócalo en el directorio del módulo de la app: `/app/google-services.json`.
4.  Habilita **Authentication** en la consola y activa los proveedores de **Correo electrónico/Contraseña** e **Invitado (Anónimo)**.
5.  Habilita **Cloud Firestore Database** en la nube.

### Paso 3: Configurar las Reglas de Firestore
En tu Firebase Console, en la sección de **Firestore Database**, pestaña **Reglas** (Rules), publica la siguiente configuración segura para habilitar la lectura y escritura para usuarios autenticados:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/favorites/{favoriteId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### Paso 4: Ejecutar
1.  Conecta tu emulador o dispositivo móvil en Android Studio.
2.  Asegúrate de haber ingresado tus claves en `local.properties`.
3.  Haz clic en el botón verde de **Run app** (`Shift + F10`).
4.  ¡Listo! Experimenta una navegación fluida, diseño de primera categoría y favoritos en tiempo real.
