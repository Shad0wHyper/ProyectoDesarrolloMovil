# Idiomas de PanApp

## Comportamiento

El selector común está disponible encima del contenido antes de iniciar sesión y para los tres roles. Ofrece Español (México), English, Português (Portugal) y Usar idioma del sistema, sin añadir destinos de navegación.

MainActivity y PaymentActivity usan AppCompatActivity. En Android 13+ AppCompat delega la selección al LocaleManager del sistema; Android 11–12 usa autoStoreLocales. La lista vacía elimina la preferencia propia y vuelve a seguir la configuración del sistema. No se fija un idioma al arrancar ni se conserva una copia del idioma del dispositivo.

locale_config.xml declara es-MX, en y pt-PT. Los recursos predeterminados son ingleses. Las copias values-en garantizan coincidencia explícita para English con una lista de idiomas del sistema que también contenga español o portugués.

## Extracción de textos

Se añadieron 234 claves compartidas en core, con inglés, español mexicano y portugués de Portugal. Incluyen accesibilidad, formularios, avisos, etiquetas y plantillas con parámetros posicionales. Las cadenas y placeholders existentes se conservan.

Los valores internos de estados, categorías y pagos mantienen sus códigos originales. TextLabels traduce su presentación para los códigos conocidos; conserva valores desconocidos. No traduce nombres de productos, clientes, direcciones, errores remotos ni contenido de notificaciones recibido del servidor. El servicio de notificaciones solo cambia la resolución del nombre y descripción locales del canal.

TextResources resuelve textos fuera de composición con la configuración de la app, también en Android 11–12 donde el contexto de Application no recibe el override de AppCompat. Se inicializa en PanAppApplication. Los textos locales de nombre/correo en ViewModels se resuelven al leerlos; los valores reales se conservan. Los tests que invoquen directamente este helper deben inicializarlo con su contexto de prueba.

## Decisiones para revisar

- Migración de las dos actividades a AppCompatActivity y del tema principal a Theme.AppCompat.Light.NoActionBar; el tema de pago ya era AppCompat.
- Ubicación del selector en una franja común a todos los roles. Reserva espacio y evita superponer controles o cambiar navegación.
- Almacenamiento estándar autoStoreLocales en Android 11–12, sin una segunda preferencia propia que pudiera divergir de los ajustes de Android 13+.
- Recursos compartidos extraídos en core; los textos originales de cada módulo permanecen en sus archivos.
- Duplicación explícita de los recursos ingleses en values-en: al añadir claves futuras habrá que mantener su correspondencia con values.
- Inicialización central de TextResources para callbacks y servicios; la actividad se recrea al cambiar el idioma.

Referencia: [Idiomas por aplicación en Android](https://developer.android.com/guide/topics/resources/app-languages).

## Cadenas pendientes de confirmar

Estas cadenas preexistentes se conservaron; no impiden seleccionar idiomas:

- app: register_phone_placeholder de es-MX usa +34, aunque México corresponde a +52. Confirmar antes de cambiar el ejemplo.
- app: terms_footer_info usa soporte@panapp.com en español y support@panapp.com en inglés/portugués. Confirmar si ambas direcciones son intencionales.
- Mensaje a proveedores: la cantidad literal X se mantiene en pan_supplier_message.
- Dashboard: se mantienen los valores fijos de “Actualizado 10:30 AM” y “+12% vs ayer”. Confirmar su contenido editorial.
- Revisar regionalmente las traducciones portuguesas anteriores (por ejemplo, você y estoque); las cadenas nuevas usan portugués de Portugal.
- Las afirmaciones legales y de seguridad de los textos originales no se han reformulado.

## Validación

- Compilación Kotlin de admin, client y employee: correcta.
- Pruebas unitarias existentes de core y app: correctas.
- Ensamblado debug y lintDebug de app: correctos con un init script temporal, ignorado por Git, en build/localization-validation.init.gradle. Cambia a WARN la ausencia de google-services.json solo para esa ejecución. No cambia los Gradle del proyecto ni genera credenciales ficticias.
- El build normal requiere el google-services.json real del proyecto, ausente en este entorno. El APK de validación no acredita el funcionamiento de Firebase.
- LocalizationResourcesTest compila y comprueba selección con múltiples locales, fallback inglés, placeholders con valores externos y las cuatro opciones.
- No se ejecutaron pruebas instrumentadas: adb devices no muestra dispositivos conectados.
- Comparación de claves/formatos: sin discrepancias en las 234 claves nuevas; las variantes existentes conservan sus placeholders. default_web_client_id sigue solo en values porque es un identificador técnico.
- git diff --check: correcto.

Para aceptación en un dispositivo, probar Android 11/12 y 13+: elegir los tres idiomas y reiniciar el proceso; volver al modo sistema y cambiar el idioma del dispositivo; seleccionar English con sistema español; revisar login, cliente, administrador, empleado y pago; comprobar que los ajustes de idioma de Android 13+ y el selector coincidan. Requiere la configuración real de Firebase para recorrer el flujo de producción.

Comandos de validación ejecutados:

- gradlew.bat :admin:compileDebugKotlin :client:compileDebugKotlin :employee:compileDebugKotlin :core:testDebugUnitTest
- gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --init-script build/localization-validation.init.gradle
- gradlew.bat :core:assembleDebugAndroidTest

## Archivos modificados o creados

### app

- [app/build.gradle.kts](../app/build.gradle.kts)
- [app/src/main/AndroidManifest.xml](../app/src/main/AndroidManifest.xml)
- [app/src/main/java/com/developers/panapp/AppNavigation.kt](../app/src/main/java/com/developers/panapp/AppNavigation.kt)
- [app/src/main/java/com/developers/panapp/LoginScreen.kt](../app/src/main/java/com/developers/panapp/LoginScreen.kt)
- [app/src/main/java/com/developers/panapp/MainActivity.kt](../app/src/main/java/com/developers/panapp/MainActivity.kt)
- [app/src/main/java/com/developers/panapp/SplashScreen.kt](../app/src/main/java/com/developers/panapp/SplashScreen.kt)
- [app/src/main/res/values/themes.xml](../app/src/main/res/values/themes.xml)
- [app/src/main/java/com/developers/panapp/LanguageSelector.kt](../app/src/main/java/com/developers/panapp/LanguageSelector.kt)
- [app/src/main/java/com/developers/panapp/PanAppApplication.kt](../app/src/main/java/com/developers/panapp/PanAppApplication.kt)
- [app/src/main/res/values-en/strings.xml](../app/src/main/res/values-en/strings.xml)
- [app/src/main/res/xml/locale_config.xml](../app/src/main/res/xml/locale_config.xml)

### admin

- [admin/src/main/java/com/developers/admin/AIReportScreen.kt](../admin/src/main/java/com/developers/admin/AIReportScreen.kt)
- [admin/src/main/java/com/developers/admin/AddProductScreen.kt](../admin/src/main/java/com/developers/admin/AddProductScreen.kt)
- [admin/src/main/java/com/developers/admin/AdminProfileScreen.kt](../admin/src/main/java/com/developers/admin/AdminProfileScreen.kt)
- [admin/src/main/java/com/developers/admin/AdminViewModel.kt](../admin/src/main/java/com/developers/admin/AdminViewModel.kt)
- [admin/src/main/java/com/developers/admin/AlmacenScreen.kt](../admin/src/main/java/com/developers/admin/AlmacenScreen.kt)
- [admin/src/main/java/com/developers/admin/AlmacenStockScreen.kt](../admin/src/main/java/com/developers/admin/AlmacenStockScreen.kt)
- [admin/src/main/java/com/developers/admin/AttendanceHistoryScreen.kt](../admin/src/main/java/com/developers/admin/AttendanceHistoryScreen.kt)
- [admin/src/main/java/com/developers/admin/DashboardAdminScreen.kt](../admin/src/main/java/com/developers/admin/DashboardAdminScreen.kt)
- [admin/src/main/java/com/developers/admin/PedidosScreen.kt](../admin/src/main/java/com/developers/admin/PedidosScreen.kt)
- [admin/src/main/java/com/developers/admin/ProduccionScreen.kt](../admin/src/main/java/com/developers/admin/ProduccionScreen.kt)
- [admin/src/main/java/com/developers/admin/QrGeneratorScreen.kt](../admin/src/main/java/com/developers/admin/QrGeneratorScreen.kt)
- [admin/src/main/res/values-en/strings_admin.xml](../admin/src/main/res/values-en/strings_admin.xml)

### client

- [client/build.gradle.kts](../client/build.gradle.kts)
- [client/src/main/java/com/developers/client/AddCardScreen.kt](../client/src/main/java/com/developers/client/AddCardScreen.kt)
- [client/src/main/java/com/developers/client/AppState.kt](../client/src/main/java/com/developers/client/AppState.kt)
- [client/src/main/java/com/developers/client/CartScreen.kt](../client/src/main/java/com/developers/client/CartScreen.kt)
- [client/src/main/java/com/developers/client/HomeScreen.kt](../client/src/main/java/com/developers/client/HomeScreen.kt)
- [client/src/main/java/com/developers/client/ManageAddressesScreen.kt](../client/src/main/java/com/developers/client/ManageAddressesScreen.kt)
- [client/src/main/java/com/developers/client/OrdersScreen.kt](../client/src/main/java/com/developers/client/OrdersScreen.kt)
- [client/src/main/java/com/developers/client/PanAppMessagingService.kt](../client/src/main/java/com/developers/client/PanAppMessagingService.kt)
- [client/src/main/java/com/developers/client/PaymentActivity.kt](../client/src/main/java/com/developers/client/PaymentActivity.kt)
- [client/src/main/java/com/developers/client/PaymentMethodsScreen.kt](../client/src/main/java/com/developers/client/PaymentMethodsScreen.kt)
- [client/src/main/java/com/developers/client/ProfileScreen.kt](../client/src/main/java/com/developers/client/ProfileScreen.kt)
- [client/src/main/java/com/developers/client/SettingsScreen.kt](../client/src/main/java/com/developers/client/SettingsScreen.kt)
- [client/src/main/java/com/developers/client/SuccessScreen.kt](../client/src/main/java/com/developers/client/SuccessScreen.kt)
- [client/src/main/res/values-en/strings_client.xml](../client/src/main/res/values-en/strings_client.xml)

### core

- [core/src/androidTest/java/com/developers/core/LocalizationResourcesTest.kt](../core/src/androidTest/java/com/developers/core/LocalizationResourcesTest.kt)
- [core/src/main/java/com/developers/core/text/TextLabels.kt](../core/src/main/java/com/developers/core/text/TextLabels.kt)
- [core/src/main/java/com/developers/core/text/TextResources.kt](../core/src/main/java/com/developers/core/text/TextResources.kt)
- [core/src/main/res/values-en/strings_localization.xml](../core/src/main/res/values-en/strings_localization.xml)
- [core/src/main/res/values-es-rMX/strings_localization.xml](../core/src/main/res/values-es-rMX/strings_localization.xml)
- [core/src/main/res/values-pt-rPT/strings_localization.xml](../core/src/main/res/values-pt-rPT/strings_localization.xml)
- [core/src/main/res/values/strings_localization.xml](../core/src/main/res/values/strings_localization.xml)

### employee

- [employee/src/main/java/com/developers/employee/AttendanceScreen.kt](../employee/src/main/java/com/developers/employee/AttendanceScreen.kt)
- [employee/src/main/java/com/developers/employee/EmpleadoAlmacenScreen.kt](../employee/src/main/java/com/developers/employee/EmpleadoAlmacenScreen.kt)
- [employee/src/main/java/com/developers/employee/EmployeeDashboardScreen.kt](../employee/src/main/java/com/developers/employee/EmployeeDashboardScreen.kt)
- [employee/src/main/java/com/developers/employee/EmployeeScreens.kt](../employee/src/main/java/com/developers/employee/EmployeeScreens.kt)
- [employee/src/main/java/com/developers/employee/EmployeeViewModel.kt](../employee/src/main/java/com/developers/employee/EmployeeViewModel.kt)
- [employee/src/main/res/values-en/strings_employee.xml](../employee/src/main/res/values-en/strings_employee.xml)

### Documentación

- [docs/localization.md](localization.md)
