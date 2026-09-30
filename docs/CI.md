# CI/CD (GitHub Actions)

| Workflow | Disparador | Resultado |
|---|---|---|
| `.github/workflows/firmware.yml` | cambios en `firmware/`, tags `v*` | `esp8266-reverse-proxy-nodemcuv2.bin`, `...-esp01_1m.bin` |
| `.github/workflows/android.yml` | cambios en `android/`, tags `v*` | `esp-proxy-config.apk` |

- Los binarios quedan como *artifacts* del run.
- Para publicar un release: `git tag v1.0.0 && git push origin v1.0.0`; ambos workflows adjuntan sus
  archivos al Release de GitHub.
- La versión del firmware se define en `firmware/platformio.ini` (`FW_VERSION`); la de la app usa
  el número de run como `versionCode`.
