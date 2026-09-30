#include "config.h"
#include <EEPROM.h>

static const uint32_t MAGIC = 0x52505831;  // "RPX1"

static uint32_t crc32(const uint8_t *d, size_t n) {
  uint32_t crc = 0xFFFFFFFF;
  while (n--) {
    crc ^= *d++;
    for (int i = 0; i < 8; i++) crc = (crc >> 1) ^ (0xEDB88320 & -(crc & 1));
  }
  return ~crc;
}

static uint32_t calc(const Config &c) {
  return crc32((const uint8_t *)&c, offsetof(Config, crc));
}

void configDefaults(Config &c) {
  memset(&c, 0, sizeof(c));
  c.magic = MAGIC;
  c.port = 80;
  c.listenPort = 80;
}

void configLoad(Config &c) {
  EEPROM.begin(sizeof(Config));
  EEPROM.get(0, c);
  if (c.magic != MAGIC || c.crc != calc(c)) configDefaults(c);
  // Garantiza cadenas terminadas en \0 aunque la flash este corrupta.
  c.ssid[sizeof(c.ssid) - 1] = 0;
  c.pass[sizeof(c.pass) - 1] = 0;
  c.host[sizeof(c.host) - 1] = 0;
}

bool configSave(Config &c) {
  c.magic = MAGIC;
  c.crc = calc(c);
  EEPROM.put(0, c);
  return EEPROM.commit();
}
