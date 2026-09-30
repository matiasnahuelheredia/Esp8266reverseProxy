#pragma once
#include <Arduino.h>

// Configuracion persistente (EEPROM). Se edita por Bluetooth con SET/SAVE.
struct Config {
  uint32_t magic;
  char ssid[33];
  char pass[65];
  char host[64];      // destino del reverse proxy (IP o dominio)
  uint16_t port;      // puerto destino
  uint16_t listenPort;// puerto en el que escucha el ESP32
  uint32_t crc;
};

void configLoad(Config &c);
bool configSave(Config &c);
void configDefaults(Config &c);
