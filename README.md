# AuraTV Chile

App **Android TV / Google TV** para canales free-to-air de Chile.

## Características

- Lista: [iptv-org Chile](https://iptv-org.github.io/iptv/countries/cl.m3u)
- **EPG interactivo** (XMLTV Chile)
  - Fuentes: `epg.lat/files/cl.xml.gz` → fallback `iptv-epg.org`
  - Zona horaria: `America/Santiago`
  - Matching por `tvg-id` / nombre
  - Pantalla Guía TV navegable con mando (↑↓ + OK para reproducir)
- Reproductor: **Media3 ExoPlayer** (HLS, ABR, reintentos)

## Uso en TV

1. Abre la app → grid de canales
2. Botón **Guía TV** → guía con programa actual y siguiente
3. OK en una fila → reproduce ese canal

## Generar APK

### GitHub Actions
1. [Actions](https://github.com/Saintkirk/AuraTV-Chile/actions) → **Build APK** → Run workflow
2. Descarga el artefacto `AuraTV-Chile-debug`

### Local
```bash
./gradlew assembleDebug
```

## Legal

Solo streams públicamente listados como free-to-air. Sin contenido no autorizado.

MIT
