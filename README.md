# AuraTV Chile

App **Android TV / Google TV** para ver canales free-to-air de Chile.

## Características

- Lista base: [iptv-org Chile](https://iptv-org.github.io/iptv/countries/cl.m3u)
- EPG: [epg.lat Chile](https://epg.lat/files/cl.xml.gz) (zona America/Santiago)
- Reproductor: **Media3 ExoPlayer** (HLS, ABR, 4K cuando el hardware lo permita)
- Interfaz optimizada para mando a distancia
- Actualización automática de EPG (WorkManager)

## Generar APK

### Automático (GitHub Actions)
1. Ve a la pestaña **Actions**
2. Ejecuta el workflow **Build APK**
3. Descarga el artefacto `app-debug.apk`

### Local
```bash
./gradlew assembleDebug
# APK en: app/build/outputs/apk/debug/app-debug.apk
```

## Instalar en Android TV

1. Activa **Orígenes desconocidos** / depuración USB
2. Usa `adb install app-debug.apk` o un gestor de archivos

## Legal

Solo canales públicamente disponibles (free-to-air). No incluye contenido con copyright no autorizado.

## Licencia

MIT
