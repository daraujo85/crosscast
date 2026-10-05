# Modo OBS - Explicação do que foi implementado

## Resumo

Implementei um novo **Modo OBS** para o Campilot que permite cenas automatizadas e controle via HTTP.

---

## O que foi criado:

### 1. Tela de Seleção de Modo (após splash)
- **Modo Câmera** → Transmite a câmera com controles de zoom/flash
- **Modo OBS** → Cenas automatizadas com detecção de projeção

### 2. Modo OBS - Funcionalidades

#### Cenas Automatizadas
- **Câmera Principal** (1x)
- **Câmera Wide** (ultra-wide)
- **Câmera Tele** (telephoto)
- **Câmera + Projeção** (PIP - câmera em cima, Holyrics embaixo)
- **Somente Projeção**

#### Automações
- **Timer Switch**: Alterna cenas a cada X segundos (configurável)
- **Detecção Holyrics**: Detecta quando a projeção inicia/termina e alterna cena automaticamente
- **Auto-Switch por sinal**: Cada fonte pode detectar entrada de sinal e mudar de cena

#### Fontes Configuráveis
Cada fonte (câmera, Holyrics) pode ter:
- URL para detecção de sinal
- Modo split (dividir tela)
- Posição do split (top, bottom, left, right)
- Auto-switch ao detectar sinal

### 3. API HTTP para Controle

Controle via tablet/celular externo:

```
GET  /api/obs/status              → Status completo
POST /api/obs/scene/{id}         → Trocar cena
POST /api/obs/scene/next         → Próxima cena
POST /api/obs/auto-switch        → Toggle auto-switch
POST /api/obs/timer-switch?interval=10  → Timer switch
POST /api/obs/detection?enable=true     → Detecção Holyrics
GET  /api/obs/sources           → Listar fontes
POST /api/obs/sources/{id}      → Editar fonte
```

---

## Como usar:

1. **Instalar o APK** no celular
2. **Conectar o celular** na mesma rede WiFi
3. **Abrir o app** → selecionar "Modo OBS"
4. **Configurar**:
   - URL do Holyrics (ex: http://192.168.1.100:4000/live)
   - Ativar detecção
   - Ativar auto-switch ou timer
5. **Controlar** via HTTP do tablet

---

## Arquivos Criados:

- `SplashActivity.kt` - Tela de splash
- `ModeSelectionActivity.kt` - Seleção de modo
- `ObsModeActivity.kt` - Activity do modo OBS
- `obs/ObsManager.kt` - Gerenciador de cenas e automações
- `obs/ObsApiRoutes.kt` - Rotas HTTP
- `ui/ObsModeScreen.kt` - Interface visual

---

## Para compilar:

1. Abrir projeto no Android Studio
2. Build > Make Project
3. APK sairá em `app/build/outputs/apk/debug/app-debug.apk`
