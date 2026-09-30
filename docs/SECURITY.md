# Seguridad

- **Bluetooth**: quien pueda emparejar el módulo controla el ESP8266 (config y firmware).
  Cambia el PIN y el nombre del HC-05/HC-06 (`AT+PSWD`, `AT+NAME`).
- **Proxy**: no autentica ni cifra; expón solo servicios que ya se protejan solos (TLS, SSH…).
- **`OTA_URL` con https** no verifica el certificado (`setInsecure`) por falta de RAM/almacén
  de certificados; el MD5 solo se comprueba en la OTA por Bluetooth. Usa `http` en LAN de confianza
  o la OTA por Bluetooth.
- La contraseña WiFi se guarda en claro en la EEPROM del ESP8266.
- El APK se firma con la clave debug; para distribuir configura tu propia keystore.
