# Firmware

Código en [`firmware/`](../firmware): `main.cpp` (protocolo Bluetooth, OTA), `proxy.cpp`
(reverse proxy), `config.cpp` (EEPROM).

## Funcionamiento

- Escucha en `listen` (por defecto 80) y por cada conexión abre otra hacia `host:port`,
  copiando datos en ambos sentidos. Es agnóstico al protocolo (HTTP, WebSocket, SSH, MQTT…).
- Hasta 4 sesiones simultáneas; cierra sesiones inactivas tras 5 min.
- Configuración guardada en EEPROM (SSID, contraseña, host, puerto, puerto de escucha).
  Sin destino configurado rechaza las conexiones.
- Reconecta el WiFi automáticamente.

## Compilar

Con [PlatformIO](https://platformio.org):

```bash
pip install platformio
pio run -d firmware -e nodemcuv2     # NodeMCU / Wemos D1 mini (4 MB)
pio run -d firmware -e esp01_1m      # ESP-01 (1 MB)
# binario: firmware/.pio/build/<env>/firmware.bin
```

## Primer flasheo (por USB)

```bash
pio run -d firmware -e nodemcuv2 -t upload
# o con esptool: esptool.py --port /dev/ttyUSB0 write_flash 0x0 firmware.bin
```

Las siguientes actualizaciones se pueden hacer desde la app Android.

## Actualizaciones OTA

El `.bin` debe ocupar menos que el espacio libre para sketch (con `nodemcuv2` ≈ 1 MB;
con `esp01_1m` ≈ 470 KB). El ESP8266 valida el MD5 antes de aplicar. Durante la OTA se detiene
el proxy. Si falla (timeout, MD5), el firmware anterior sigue funcionando.
Además existe el comando `OTA_URL` (el ESP descarga el `.bin` por WiFi, ver [PROTOCOL.md](PROTOCOL.md)).
