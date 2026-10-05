# 🎬 CrossCast

> **Stream. Switch. Worship.**
> Transmissão inteligente para a sua igreja.

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84.svg)](https://www.android.com)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

CrossCast transforma qualquer celular Android em uma estação de produção
de vídeo completa para igrejas. Com dois modos integrados — **Camera**
e **Studio** — qualquer voluntário consegue operar uma transmissão
ao vivo com qualidade profissional, sem precisar entender OBS, URLs
ou protocolos de streaming.

---

## ✨ Visão Geral

A complexidade fica **dentro do software**. O operador vê apenas:
**câmera, cena, projeção e transmissão.**

| | |
|---|---|
| 🎥 **Camera** | Transforma o celular em uma câmera de transmissão remota |
| 🎬 **Studio** | Mesa de produção com cenas, fontes, Holyrics e automações |
| 🌐 **Remote** | Painel web aberto por tablet ou notebook na mesma rede |
| ✨ **Auto Switch** | Detecta a projeção do Holyrics e troca as cenas automaticamente |

---

## 🎨 Design System

CrossCast segue a regra visual **60/30/10** para garantir consistência
e profissionalismo em uso prolongado:

```
60% ████████  charcoal/black #080B0E  (fundo, neutralidade)
30% ███       AppleGreen  #22E27D  (Camera, conectado, ativo)
10% █         PurpleOBS  #A855F7  (Studio, automação, projeção)
            +  Red  #FF4545  (apenas: on-air, destrutivo, LIVE)
```

- **Verde** → Câmera, conectado, "estou no ar"
- **Roxo** → Studio, automação, projeção
- **Vermelho** → Está no ar, ação destrutiva, LIVE

### Branding bicolor

O logo "CrossCast" sempre é renderizado em duas cores:
- **Cross** → AppleGreen (light weight)
- **Cast** → branco (light weight)

---

## 📱 Telas

| Tela | Descrição |
|------|-----------|
| **Splash** | Logo oficial + "STREAM · SWITCH · WORSHIP" |
| **Home** | "Como você quer usar o CrossCast?" + cards Camera/Studio + IP/QR |
| **Camera** | Back à esquerda, header "Live Camera", zoom pills, GO LIVE |
| **Studio** | Back à esquerda, título bicolor, PROGRAM, scenes, TAKE, Auto Switch, Sources |

---

## 🏗️ Arquitetura

```
┌────────────────────────────────────────────┐
│            CrossCast Android App             │
├────────────────────────────────────────────┤
│                                              │
│  SplashActivity ──► ModeSelectionActivity   │
│                            │                 │
│                ┌───────────┴───────────┐     │
│                ▼                       ▼     │
│         MainActivity            StudioActivity
│           (Camera)                (Studio)    │
│              │                       │       │
│              └───────────┬───────────┘       │
│                          ▼                   │
│                   CameraService              │
│              (foreground Ktor server)        │
│                          │                   │
│              ┌───────────┴───────────┐       │
│              ▼                       ▼       │
│         CameraManager         StudioManager   │
│         (CameraX)             (compositor)   │
│                                              │
├──────────────────────────────────────────────┤
│  HTTP API:                                   │
│  GET  /api/stream  (MJPEG)                   │
│  GET  /api/studio/status                      │
│  POST /api/studio/scene/{id}                 │
│  POST /api/studio/timer-switch?interval=N    │
│  GET  /api/auto-switch                        │
└──────────────────────────────────────────────┘
                  │
                  ▼
       📱 Tablet / Notebook
       (painel de controle web)
```

---

## 🚀 Tecnologias

- **Linguagem:** Kotlin 2.0
- **UI:** Jetpack Compose + Material 3
- **Câmera:** CameraX (preview, ImageAnalysis, ImageCapture)
- **Servidor:** Ktor (Netty engine, content negotiation, JSON)
- **Streaming:** MJPEG + REST API
- **Concorrência:** Kotlin Coroutines
- **Async:** StateFlow + SharedFlow
- **Persistência:** Room (preparado para próximas versões)

---

## 🛠️ Como Compilar e Rodar

### Requisitos
- Android Studio Hedgehog ou superior
- JDK 17+
- Android SDK 26+ (Android 8.0 Oreo)
- Celular físico recomendado (emulador tem limitações de câmera)

### Build
```bash
# Build
cd /mnt/c/repo/campilot
"/mnt/c/Program Files/Eclipse Adoptium/jdk-17.0.17.10-hotspot/bin/java.exe" \
  -Dorg.gradle.appname=gradlew \
  -classpath gradle/wrapper/gradle-wrapper.jar \
  org.gradle.wrapper.GradleWrapperMain assembleDebug

# Install
"/mnt/c/Users/usuario/AppData/Local/Android/Sdk/platform-tools/adb.exe" \
  install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
"/mnt/c/Users/usuario/AppData/Local/Android/Sdk/platform-tools/adb.exe" \
  shell am start -n com.campilot/.SplashActivity
```

### Capturas de tela via ADB
```bash
# Screenshot
"/mnt/c/Users/usuario/AppData/Local/Android/Sdk/platform-tools/adb.exe" \
  exec-out screencap -p > /tmp/screen.png

# Listar devices
"/mnt/c/Users/usuario/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices
```

---

## 🌐 API HTTP

O CrossCast expõe uma API REST no servidor Ktor (porta 8080):

### Camera
```http
GET  /api/stream?id={cameraId}&zoom={zoom}    # MJPEG stream
GET  /status                                   # Status do dispositivo
POST /api/camera/select?id={id}&zoom={zoom}    # Trocar lente + zoom
POST /api/presets/apply/{preset}              # Aplicar preset salvo
```

### Studio
```http
GET  /api/studio/status                       # Estado completo
POST /api/studio/scene/{sceneId}              # Trocar cena
POST /api/studio/timer-switch?interval=10     # Auto-switch por tempo
POST /api/studio/auto-switch                  # Toggle detecção Holyrics
GET  /api/studio/sources                      # Estado das fontes
```

---

## 📂 Estrutura do Projeto

```
campilot/
├── app/
│   └── src/main/
│       ├── java/com/campilot/
│       │   ├── SplashActivity.kt          # Logo + entrada
│       │   ├── ModeSelectionActivity.kt   # Home (Camera/Studio)
│       │   ├── MainActivity.kt            # Camera mode
│       │   ├── StudioActivity.kt          # Studio mode
│       │   ├── CameraService.kt           # Foreground Ktor server
│       │   ├── camera/                    # CameraX wrapper
│       │   ├── studio/                    # Compositor de cenas
│       │   │   └── auto/                  # Auto-switch + Holyrics
│       │   └── ui/                        # Componentes Compose
│       ├── res/
│       │   ├── drawable/
│       │   │   ├── logo_crosscast.png     # Logo oficial
│       │   │   └── icon_camera.png
│       │   ├── mipmap-*/                  # Ícones adaptativos
│       │   └── values/                    # Strings, temas
│       └── AndroidManifest.xml
├── gradle/
│   └── libs.versions.toml                 # Version catalog
├── app/build.gradle.kts
├── settings.gradle.kts
└── build.gradle.kts
```

---

## 🤖 Automação Inteligente

O **Auto Switch** detecta quando a projeção do Holyrics inicia e
automaticamente alterna para a cena "Câmera + Projeção" (PIP). Quando
a projeção termina, volta para a câmera principal.

```kotlin
// Holyrics URL detection
val holyricsUrl = "http://192.168.31.100:4000/live"

// Detecção automática de mudança de imagem
if (frameContentChanged) activateScene("camera_pip_holyrics")
```

---

## 🎯 Casos de Uso Reais

- **Pregador no púlpito** → câmera principal
- **Mostra versículo** → câmera + projeção
- **Apresentação de slides** → tela cheia da projeção
- **Oferta** → câmera no ofertante
- **Coro** → câmera do coral

Tudo isso sem o operador tocar no celular — o Auto Switch resolve.

---

## 📜 Licença

MIT License - veja [LICENSE](LICENSE) para detalhes.

---

## 🤝 Contribuindo

Contribuições são bem-vindas! Por favor:

1. Fork o projeto
2. Crie uma feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add: AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

---

## 📞 Suporte

- 🐛 Issues: [GitHub Issues](https://github.com/daraujo85/campilot/issues)
- 📧 Email: diego.araujo@pratadigital.com.br

---

<p align="center">
  Feito com 💚 para igrejas que querem transmissão profissional sem complicação<br>
  <strong>CrossCast</strong> · <em>Stream. Switch. Worship.</em>
</p>
