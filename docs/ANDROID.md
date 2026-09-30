# App Android

Código en [`android/`](../android) (Kotlin, minSdk 26). Se conecta por **Bluetooth Classic (SPP)**.

## Instalar

Descarga `esp-proxy-config.apk` de Actions/Releases e instálalo (permite "orígenes desconocidos").
El APK se firma con la clave *debug* de Android, válida para uso personal.

## Uso

1. Vincula el HC-05/HC-06 en *Ajustes → Bluetooth* (PIN `1234`/`0000`).
2. En la app pulsa **Actualizar**, elige el dispositivo y **Conectar**. Se muestra el estado
   (WiFi, IP, sesiones) y se lee la configuración actual.
3. **Buscar redes** lista las redes que ve el ESP8266; elige o escribe el SSID y la contraseña.
   Si ya hay contraseña guardada y dejas el campo vacío, se conserva.
4. Indica **destino** (IP/dominio + puerto) y **puerto de escucha**, pulsa **Guardar**:
   se persiste y se aplica sin reiniciar.
5. **Firmware**: *Elegir .bin* → *Subir*. Se envía por Bluetooth con confirmación por bloque y
   verificación MD5; al terminar el ESP8266 se reinicia. No apagues el ESP durante la subida.

## Compilar localmente

Requiere JDK 17, Android SDK 34 y Gradle 8.7:

```bash
cd android && gradle assembleRelease   # app/build/outputs/apk/release/app-release.apk
```
