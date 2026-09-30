#pragma once
#include <WiFi.h>

// Reverse proxy TCP transparente: acepta conexiones en `listenPort` y las
// reenvia a host:port. Funciona con HTTP, WebSocket, SSH, MQTT, etc.
class ReverseProxy {
 public:
  static const int MAX_SESSIONS = 4;

  void begin(uint16_t listenPort, const char *host, uint16_t port);
  void stop();
  void loop();
  int activeSessions() const;

 private:
  struct Session {
    WiFiClient down;  // cliente que se conecto al ESP
    WiFiClient up;    // conexion hacia el destino
    bool active = false;
    uint32_t lastActivity = 0;
  };
  static void pump(WiFiClient &from, WiFiClient &to, bool &moved);
  void closeSession(Session &s);

  WiFiServer *server_ = nullptr;
  Session sessions_[MAX_SESSIONS];
  String host_;
  uint16_t port_ = 0;
};
