# Seguridad

- **Bluetooth**: quien pueda emparejar el módulo controla el ESP32 (config y firmware).
  Cambia `BT_NAME` y considera añadir un PIN (`BT.setPin`) en el firmware.
- **Proxy**: no autentica ni cifra; expón solo servicios que ya se protejan solos (TLS, SSH…).
- **`OTA_URL` con https** no verifica el certificado (`setInsecure`) por falta de RAM/almacén
  de certificados; el MD5 solo se comprueba en la OTA por Bluetooth. Usa `http` en LAN de confianza
  o la OTA por Bluetooth.
- La contraseña WiFi se guarda en claro en la memoria flash del ESP32.
- El APK se firma con la clave debug; para distribuir configura tu propia keystore.
