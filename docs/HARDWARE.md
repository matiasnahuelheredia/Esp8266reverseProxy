# Hardware

El ESP8266 no incluye Bluetooth, así que se usa un módulo serie **HC-05** o **HC-06**
(Bluetooth Classic, perfil SPP). Los módulos **BLE** (HM-10, etc.) **no** son compatibles con la app.

## Conexiones

UART0 se remapea con `Serial.swap()` para dejar el USB libre para logs.

| HC-05/HC-06 | ESP8266 (NodeMCU) | GPIO |
|---|---|---|
| TXD | D7 | 13 (RX) |
| RXD | D8 | 15 (TX) |
| VCC | 5 V (VIN) o 3.3 V según módulo | |
| GND | GND | |

- Logs de depuración: salida `Serial1` en **D4 (GPIO2)**, 115200 baudios.
- Los pines RXD de muchos HC-05 son de 3.3 V; el TXD del módulo es 3.3 V: compatible con ESP8266.
  Si alimentas el módulo a 5 V, verifica que su regulador lo permita.
- GPIO15 debe estar a nivel bajo durante el arranque; si tu módulo lo fuerza a alto, añade una
  resistencia de 10 kΩ a GND en GPIO15.

## Velocidad (baudios)

`BT_BAUD` en `firmware/platformio.ini` (por defecto **115200**) debe coincidir con el módulo.

| Módulo | Por defecto | Cambiar |
|---|---|---|
| HC-06 | 9600 | `AT+BAUD8` (115200) |
| HC-05 | 9600 (modo datos) | en modo AT: `AT+UART=115200,0,0` |

Con 9600 baudios todo funciona pero subir un firmware de ~300 KB tarda varios minutos;
con 115200 tarda menos de un minuto. Vincula el módulo (PIN típico `1234` o `0000`)
desde los ajustes Bluetooth de Android antes de usar la app.
