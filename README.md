# ESP8266 Reverse Proxy + App Android

Reverse proxy TCP para **ESP8266** que se configura por **Bluetooth** desde una app Android:
a qué red WiFi conectarse y a qué destino reenviar el tráfico. La misma app puede **subir
firmware nuevo** al ESP8266 por Bluetooth (OTA).

```
 Cliente ──WiFi──► [ESP8266 :listen] ──WiFi──► destino host:port
                        ▲
                        │ UART (HC-05/HC-06)
                  App Android (Bluetooth)
```

> ⚠️ El ESP8266 **no tiene Bluetooth integrado**. Necesitas un módulo **HC-05 o HC-06**
> conectado por UART. Ver [docs/HARDWARE.md](docs/HARDWARE.md).

## Contenido

| Carpeta | Qué es |
|---|---|
| [`firmware/`](firmware) | Firmware (PlatformIO / Arduino core) |
| [`android/`](android) | App Android (Kotlin) |
| [`docs/`](docs) | Documentación |
| [`main/`](main) | Proyecto original ESP8266 RTOS SDK (sin uso; se conserva por historial) |

## Descargas (GitHub Actions)

Cada push a `main` compila ambos programas. En la pestaña **Actions** → run → *Artifacts*
descargas `esp8266-reverse-proxy-*.bin` y `esp-proxy-config.apk`. Al crear un tag `vX.Y.Z`
se publican además en **Releases**.

## Inicio rápido

1. Cablea el HC-05/HC-06 ([docs/HARDWARE.md](docs/HARDWARE.md)).
2. Flashea el `.bin` por USB la primera vez ([docs/FIRMWARE.md](docs/FIRMWARE.md)).
3. Vincula el módulo en Android, instala el APK y abre la app ([docs/ANDROID.md](docs/ANDROID.md)).
4. Conecta, pon SSID/contraseña, destino y puertos → **Guardar**.
5. Apunta tus clientes a `IP_del_ESP:puerto_escucha`.

Más: [Protocolo Bluetooth](docs/PROTOCOL.md) · [CI/CD](docs/CI.md) · [Seguridad](docs/SECURITY.md)

## Limitaciones

- Proxy TCP transparente (no reescribe cabeceras HTTP ni termina TLS); máx. 4 conexiones simultáneas.
- OTA por Bluetooth es lento a 9600 baudios (HC-06 por defecto); ver docs.
- Sin autenticación propia: la seguridad depende del PIN Bluetooth y de la red WiFi.

Licencia: GPL-3.0 (ver [LICENSE](LICENSE)).
