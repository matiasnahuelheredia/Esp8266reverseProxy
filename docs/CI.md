# CI/CD (GitHub Actions)

| Workflow | Disparador | Resultado |
|---|---|---|
| `.github/workflows/firmware.yml` | cambios en `firmware/`, tags `v*` | `esp32-reverse-proxy.bin` |
| `.github/workflows/android.yml` | cambios en `android/`, tags `v*` | `esp-proxy-config.apk` |

- Los binarios quedan como *artifacts* del run.
- **Cada push a `main`** actualiza el release **`latest`** (pre-release) con el último `esp-proxy-config.apk` y `esp32-reverse-proxy.bin`; el tag `latest` se mueve al commit actual.
- Para publicar un release: `git tag v1.0.0 && git push origin v1.0.0`; ambos workflows adjuntan sus
  archivos al Release de GitHub.
- La versión del firmware se define en `firmware/platformio.ini` (`FW_VERSION`); la de la app usa
  el número de run como `versionCode`.
