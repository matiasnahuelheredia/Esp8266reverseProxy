#include "proxy.h"

static const uint32_t IDLE_TIMEOUT_MS = 5UL * 60UL * 1000UL;
static const uint16_t CONNECT_TIMEOUT_MS = 3000;

void ReverseProxy::begin(uint16_t listenPort, const char *host, uint16_t port) {
  stop();
  host_ = host;
  port_ = port;
  server_ = new WiFiServer(listenPort);
  server_->begin();
  server_->setNoDelay(true);
}

void ReverseProxy::stop() {
  for (auto &s : sessions_) closeSession(s);
  if (server_) {
    server_->stop();
    delete server_;
    server_ = nullptr;
  }
}

int ReverseProxy::activeSessions() const {
  int n = 0;
  for (auto &s : sessions_) n += s.active;
  return n;
}

void ReverseProxy::closeSession(Session &s) {
  s.down.stop();
  s.up.stop();
  s.active = false;
}

void ReverseProxy::pump(WiFiClient &from, WiFiClient &to, bool &moved) {
  static uint8_t buf[1024];
  int n = from.available();
  if (n <= 0) return;
  int room = to.availableForWrite();
  if (room <= 0) return;  // backpressure: el destino aun no puede recibir
  n = min(min(n, room), (int)sizeof(buf));
  n = from.read(buf, n);
  if (n > 0) {
    to.write(buf, n);
    moved = true;
  }
}

void ReverseProxy::loop() {
  if (!server_) return;

  // Nuevas conexiones entrantes
  if (server_->hasClient()) {
    WiFiClient c = server_->accept();
    Session *free = nullptr;
    for (auto &s : sessions_)
      if (!s.active) { free = &s; break; }

    if (!free || host_.length() == 0) {
      c.stop();  // sin cupo o sin destino configurado
    } else {
      free->up = WiFiClient();
      if (free->up.connect(host_.c_str(), port_, CONNECT_TIMEOUT_MS)) {
        c.setNoDelay(true);
        free->up.setNoDelay(true);
        free->down = c;
        free->active = true;
        free->lastActivity = millis();
      } else {
        c.stop();
      }
    }
  }

  // Copia de datos en ambos sentidos
  for (auto &s : sessions_) {
    if (!s.active) continue;
    bool moved = false;
    pump(s.down, s.up, moved);
    pump(s.up, s.down, moved);
    if (moved) s.lastActivity = millis();

    bool downGone = !s.down.connected() && s.down.available() == 0;
    bool upGone = !s.up.connected() && s.up.available() == 0;
    if (downGone || upGone || millis() - s.lastActivity > IDLE_TIMEOUT_MS)
      closeSession(s);
  }
}
