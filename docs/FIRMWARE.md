# Firmware

Código en [`firmware/`](../firmware): `main.cpp` (protocolo Bluetooth, OTA), `proxy.cpp`
(reverse proxy), `config.cpp` (EEPROM/flash).

## Funcionamiento

- Escucha en `listen` (por defecto 80) y por cada conexión abre otra hacia `host:port`,
  copiando datos en ambos sentidos. Es agnóstico al protocolo (HTTP, WebSocket, SSH, MQTT…).
- Hasta 4 sesiones simultáneas; cierra sesiones inactivas tras 5 min.
- Configuración persistente (SSID, contraseña, host, puerto, puerto de escucha).
  Sin destino configurado rechaza las conexiones.
- Reconecta el WiFi automáticamente; el proxy arranca cuando hay WiFi.

## Compilar

Con [PlatformIO](https://platformio.org):

```bash
pip install platformio
pio run -d firmware                  # binario: firmware/.pio/build/esp32dev/firmware.bin
```

## Primer flasheo (por USB)

```bash
pio run -d firmware -t upload
```

El `.bin` de Actions es solo la imagen de la aplicación (para OTA desde la app); el primer flasheo de un chip virgen
necesita además bootloader y tabla de particiones, que PlatformIO instala.

Las siguientes actualizaciones se hacen desde la app Android (el `.bin` de Actions es la imagen de app).

## Actualizaciones OTA

Partición `min_spiffs` (≈1.9 MB por app, dos slots). El ESP32 valida el MD5 antes de aplicar y,
si falla (timeout, MD5), el firmware anterior sigue funcionando. Durante la OTA se detiene el proxy.
La subida por Bluetooth tarda del orden de 1–2 minutos. También existe `OTA_URL`
(el ESP descarga el `.bin` por WiFi, ver [PROTOCOL.md](PROTOCOL.md)).
