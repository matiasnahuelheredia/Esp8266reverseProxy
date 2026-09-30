# Protocolo Bluetooth

Texto por líneas terminadas en `\n` sobre SPP. Cada comando responde con líneas de datos
opcionales y termina en `OK` o `ERR <mensaje>`.

| Comando | Respuesta |
|---|---|
| `PING` | `PONG`, `OK` |
| `STATUS` | `STATUS wifi=connected\|connecting\|idle ip=.. rssi=.. listen=.. upstream=host:port sessions=.. heap=.. fw=..`, `OK` |
| `GET` | líneas `CFG ssid=..`, `CFG pass_set=0\|1`, `CFG host=..`, `CFG port=..`, `CFG listen=..`, `OK` (nunca devuelve la contraseña) |
| `SET ssid <v>` / `pass` / `host` / `port` / `listen` | `OK` — modifica la config pendiente (el valor es el resto de la línea, admite espacios) |
| `SAVE` | guarda en EEPROM y aplica (reconecta WiFi y reinicia el proxy), `OK` |
| `SCAN` | líneas `NET <rssi> <open\|secure> <ssid>`, `OK` |
| `RESET` | restaura valores de fábrica, `OK` |
| `REBOOT` | `OK` y reinicia |
| `OTA_URL <url>` | el ESP descarga el firmware por WiFi (`http://` o `https://` sin verificar cert.), `OTA_OK` y reinicia, o `ERR` |

## OTA por Bluetooth

```
App → OTA <tamaño> <md5 hex>\n
ESP → OK <bloque>\n            (bloque = 1024)   | ERR <motivo>
App → <bloque bytes binarios>
ESP → ACK\n                     (repetir hasta completar)
ESP → OTA_OK\n                  (último bloque; luego reinicia)  | ERR md5 mismatch / timeout / ...
```

Stop-and-wait: la app no envía el bloque siguiente hasta recibir `ACK`. Sin datos durante
15 s el ESP aborta y reanuda el proxy.
