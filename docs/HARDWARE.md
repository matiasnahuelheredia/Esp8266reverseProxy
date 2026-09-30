# Hardware

Se usa un **ESP32 clásico** (ESP32-WROOM/WROVER, DevKit v1, etc.), que trae WiFi y
**Bluetooth Classic (SPP)** integrados. No necesitas módulos externos ni cableado extra.

> ⚠️ Solo sirven los ESP32 con Bluetooth Classic. **ESP32-S2, S3 y C3/C6 no lo tienen** (solo BLE o nada)
> y no son compatibles con esta app.

- Al arrancar, el ESP32 aparece por Bluetooth como **`ESP-Proxy`** (`BT_NAME` en `firmware/platformio.ini`).
- Vincúlalo desde *Ajustes → Bluetooth* de Android antes de usar la app.
- Logs de depuración por USB (Serial, 115200 baudios).
- Alimentación por USB (5 V) o 3.3 V en el pin 3V3.
