/*
 * ESP32 Reverse Proxy configurable por Bluetooth (Bluetooth Classic SPP
 * integrado). Logs por USB (Serial). Protocolo en docs/PROTOCOL.md.
 */
#include <Arduino.h>
#include <BluetoothSerial.h>
#include <HTTPUpdate.h>
#include <Update.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>

#include "config.h"
#include "proxy.h"

static BluetoothSerial BT;
#define LOG Serial

static const size_t OTA_BLOCK = 1024;
static const uint32_t OTA_TIMEOUT_MS = 15000;
static const size_t LINE_MAX = 200;

static Config cfg;       // configuracion guardada (la que esta en uso)
static Config pending;   // configuracion editada con SET, aun sin SAVE
static ReverseProxy proxy;

static char line[LINE_MAX + 1];
static size_t lineLen = 0;

// --- Estado OTA por Bluetooth -------------------------------------------
static bool otaActive = false;
static size_t otaTotal = 0, otaDone = 0, otaFill = 0;
static uint32_t otaLastByte = 0;
static uint8_t otaBuf[OTA_BLOCK];

static bool wifiStarted = false;
static bool proxyRunning = false;

// --- Utilidades ----------------------------------------------------------
static void reply(const String &s) { BT.print(s); BT.print("\n"); }
static void ok() { reply("OK"); }
static void err(const char *m) { reply(String("ERR ") + m); }

static void applyConfig() {
  proxy.stop();
  proxyRunning = false;
  WiFi.persistent(false);
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.disconnect();
  wifiStarted = false;
  if (cfg.ssid[0]) {
    WiFi.begin(cfg.ssid, cfg.pass);
    wifiStarted = true;
  }
  LOG.printf("Config aplicada: ssid='%s' destino=%s:%u escucha=%u\n", cfg.ssid,
             cfg.host, cfg.port, cfg.listenPort);
}

static const char *wifiState() {
  if (WiFi.status() == WL_CONNECTED) return "connected";
  return wifiStarted ? "connecting" : "idle";
}

static void sendStatus() {
  String s = "STATUS wifi=";
  s += wifiState();
  s += " ip=" + WiFi.localIP().toString();
  s += " rssi=" + String(WiFi.status() == WL_CONNECTED ? WiFi.RSSI() : 0);
  s += " listen=" + String(cfg.listenPort);
  s += " upstream=" + String(cfg.host) + ":" + String(cfg.port);
  s += " sessions=" + String(proxy.activeSessions());
  s += " heap=" + String(ESP.getFreeHeap());
  s += " fw=" FW_VERSION;
  reply(s);
}

static void sendConfig() {
  // Una linea por clave: el SSID puede contener espacios.
  reply("CFG ssid=" + String(pending.ssid));
  reply("CFG pass_set=" + String(pending.pass[0] ? 1 : 0));
  reply("CFG host=" + String(pending.host));
  reply("CFG port=" + String(pending.port));
  reply("CFG listen=" + String(pending.listenPort));
}

static bool copyStr(char *dst, size_t cap, const String &v) {
  if (v.length() >= cap) return false;
  strcpy(dst, v.c_str());
  return true;
}

static void cmdSet(const String &args) {
  int sp = args.indexOf(' ');
  String key = sp < 0 ? args : args.substring(0, sp);
  String val = sp < 0 ? "" : args.substring(sp + 1);
  key.toLowerCase();
  long n = val.toInt();
  if (key == "ssid") {
    if (!copyStr(pending.ssid, sizeof(pending.ssid), val)) return err("ssid too long");
  } else if (key == "pass") {
    if (!copyStr(pending.pass, sizeof(pending.pass), val)) return err("pass too long");
  } else if (key == "host") {
    if (!copyStr(pending.host, sizeof(pending.host), val)) return err("host too long");
  } else if (key == "port") {
    if (n < 1 || n > 65535) return err("bad port");
    pending.port = n;
  } else if (key == "listen") {
    if (n < 1 || n > 65535) return err("bad port");
    pending.listenPort = n;
  } else {
    return err("unknown key");
  }
  ok();
}

static void cmdScan() {
  int n = WiFi.scanNetworks();
  for (int i = 0; i < n; i++) {
    reply("NET " + String(WiFi.RSSI(i)) + " " +
          (WiFi.encryptionType(i) == WIFI_AUTH_OPEN ? "open" : "secure") + " " +
          WiFi.SSID(i));
  }
  WiFi.scanDelete();
  ok();
}

// --- OTA ---------------------------------------------------------------
static void otaAbort(const char *why) {
  Update.end(false);
  otaActive = false;
  err(why);
  LOG.printf("OTA abortada: %s\n", why);
  applyConfig();  // reanuda WiFi y proxy
}

static void cmdOta(const String &args) {
  int sp = args.indexOf(' ');
  if (sp < 0) return err("usage: OTA <size> <md5>");
  size_t size = args.substring(0, sp).toInt();
  String md5 = args.substring(sp + 1);
  md5.trim();
  if (size == 0 || md5.length() != 32) return err("bad args");

  proxy.stop();
  proxyRunning = false;  // libera RAM y evita trafico durante la actualizacion
  if (!Update.setMD5(md5.c_str())) return otaAbort("bad md5");
  if (!Update.begin(size)) {
    String m = "no space (" + String(ESP.getFreeSketchSpace()) + " free)";
    return otaAbort(m.c_str());
  }
  otaActive = true;
  otaTotal = size;
  otaDone = otaFill = 0;
  otaLastByte = millis();
  reply("OK " + String(OTA_BLOCK));
}

static void otaFeed() {
  size_t blockLen = min(OTA_BLOCK, otaTotal - otaDone);
  while (BT.available() && otaFill < blockLen) {
    int n = BT.readBytes(otaBuf + otaFill, min((size_t)BT.available(), blockLen - otaFill));
    otaFill += n;
    otaLastByte = millis();
  }
  if (otaFill == blockLen) {
    if (Update.write(otaBuf, blockLen) != blockLen) return otaAbort("write failed");
    otaDone += blockLen;
    otaFill = 0;
    if (otaDone == otaTotal) {
      if (!Update.end()) return otaAbort("md5 mismatch");
      reply("OTA_OK");
      BT.flush();
      delay(300);
      ESP.restart();
    } else {
      reply("ACK");
    }
  } else if (millis() - otaLastByte > OTA_TIMEOUT_MS) {
    otaAbort("timeout");
  }
}

static void cmdOtaUrl(const String &url) {
  if (WiFi.status() != WL_CONNECTED) return err("wifi not connected");
  proxy.stop();
  proxyRunning = false;
  httpUpdate.rebootOnUpdate(false);
  httpUpdate.setFollowRedirects(HTTPC_FORCE_FOLLOW_REDIRECTS);
  t_httpUpdate_return r;
  if (url.startsWith("https://")) {
    WiFiClientSecure c;
    c.setInsecure();  // sin verificar certificado; ver docs/SECURITY.md
    r = httpUpdate.update(c, url);
  } else {
    WiFiClient c;
    r = httpUpdate.update(c, url);
  }
  if (r == HTTP_UPDATE_OK) {
    reply("OTA_OK");
    BT.flush();
    delay(300);
    ESP.restart();
  } else {
    String m = httpUpdate.getLastErrorString();
    applyConfig();
    err(m.c_str());
  }
}

// --- Interprete de comandos -----------------------------------------------
static void handleLine(String s) {
  s.trim();
  if (s.length() == 0) return;
  int sp = s.indexOf(' ');
  String cmd = sp < 0 ? s : s.substring(0, sp);
  String args = sp < 0 ? "" : s.substring(sp + 1);
  cmd.toUpperCase();
  LOG.println("BT> " + (cmd == "SET" ? String("SET ...") : s));

  if (cmd == "PING") { reply("PONG"); ok(); }
  else if (cmd == "STATUS") { sendStatus(); ok(); }
  else if (cmd == "GET") { sendConfig(); ok(); }
  else if (cmd == "SET") cmdSet(args);
  else if (cmd == "SAVE") {
    cfg = pending;
    if (!configSave(cfg)) return err("flash write failed");
    applyConfig();
    ok();
  } else if (cmd == "SCAN") cmdScan();
  else if (cmd == "RESET") {
    configDefaults(cfg);
    pending = cfg;
    configSave(cfg);
    applyConfig();
    ok();
  } else if (cmd == "REBOOT") {
    ok();
    BT.flush();
    delay(200);
    ESP.restart();
  } else if (cmd == "OTA") cmdOta(args);
  else if (cmd == "OTA_URL") cmdOtaUrl(args);
  else err("unknown command");
}

static void btLoop() {
  if (otaActive) return otaFeed();
  while (BT.available()) {
    char ch = BT.read();
    if (ch == '\n' || ch == '\r') {
      line[lineLen] = 0;
      lineLen = 0;
      handleLine(String(line));
      if (otaActive) return;  // el resto de bytes son datos binarios
    } else if (lineLen < LINE_MAX) {
      line[lineLen++] = ch;
    } else {
      lineLen = 0;  // linea demasiado larga: descartar
    }
  }
}

void setup() {
  LOG.begin(115200);
  BT.begin(BT_NAME);
  LOG.println("\nESP32 Reverse Proxy " FW_VERSION " - Bluetooth: " BT_NAME);

  configLoad(cfg);
  pending = cfg;
  applyConfig();
}

void loop() {
  btLoop();
  if (!otaActive) {
    if (!proxyRunning && WiFi.status() == WL_CONNECTED) {
      proxy.begin(cfg.listenPort, cfg.host, cfg.port);
      proxyRunning = true;
    }
    proxy.loop();
  }
  yield();
}
