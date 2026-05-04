# 📝 Plano de Implementação - Campilot (S25 Ultra)

Este documento detalha as etapas técnicas para o desenvolvimento do sistema de streaming e controle remoto via S25 Ultra.

## 🛠️ Stack Tecnológica
- **Linguagem:** Kotlin 2.0+
- **UI:** Jetpack Compose
- **Camera:** Android CameraX (Jetpack)
- **Streaming:** WebRTC (Google/WebRTC Android SDK)
- **API Local:** Ktor Server (Netty engine)
- **Service:** Android Foreground Service
- **Persistência:** Room Database (para Presets)
- **DI:** Hilt (Dependency Injection)

---

## 📅 Fases de Desenvolvimento

### Fase 1: Fundação e Streaming Base
- [ ] **Setup do Projeto:** Configurar Gradle, permissões (Camera, Mic, Network) e Hilt.
- [ ] **Módulo de Câmera (CameraX):**
  - Implementar `PreviewView`.
  - Mapear IDs das câmeras do S25 Ultra (Ultra-wide, Wide, Tele 3x, Tele 10x).
- [ ] **Módulo WebRTC:**
  - Configurar `PeerConnectionFactory`.
  - Implementar captura de vídeo da CameraX para o WebRTC.
  - Criar "Signaling Minimalista" via HTTP (handshake local).
- [ ] **Servidor Ktor Base:**
  - Endpoint `/status` básico.
  - Endpoint para handshake WebRTC (Offer/Answer).

### Fase 2: Sistema de Controle e API
- [ ] **Controle de Hardware:**
  - Implementar `setZoomRatio()` dinâmico.
  - Implementar troca de câmera a quente (switch lens).
  - Controle de Foco/Exposição via `CameraControl`.
- [ ] **Sistema de Presets (Room):**
  - Schema do Banco de Dados conforme `README.md`.
  - CRUD de Presets na UI e via API.
- [ ] **API Endpoints:**
  - `POST /api/presets/apply/{id}`: Lógica de transição suave.
  - `POST /api/camera/zoom`: Controle granular.
- [ ] **Foreground Service:**
  - Garantir que a câmera e o servidor não morram em background.
  - Notificação persistente com controles rápidos.

### Fase 3: Integração OBS e Polimento
- [ ] **Interface de Usuário (Compose):**
  - Painel de monitoramento (Temp, Bateria, Bitrate).
  - Preview de baixa latência.
- [ ] **OBS Scripting:**
  - Criar script Python/Lua para o OBS que detecta mudança de cena e chama a API do celular.
- [ ] **Otimização S25 Ultra:**
  - Configurar perfis de bitrate específicos para Wi-Fi 6/6E.
  - Testar limites de temperatura durante streaming 1080p.

---

## 📐 Detalhes Técnicos Críticos

### 1. Mapeamento de Lentes (S25 Ultra)
O S25 Ultra possui IDs de câmera específicos. Usaremos `CameraManager` para listar e filtrar:
- **Traseiras:** Ultra-wide, Wide (Principal), Tele 3x, Tele 10x (Periscópio).
- **Frontal:** Selfie de alta resolução.
- Utilizaremos `LENS_FACING_BACK` e `LENS_FACING_FRONT` + `REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA`.

### 2. Transição de Presets
Ao aplicar um preset, a ordem de execução deve ser:
1. `Switch Camera` (se necessário) -> Aguardar `onActive`.
2. `Set Zoom` (linear ou imediato).
3. `Focus/Exposure Lock`.

### 3. Latência WebRTC
Para ambiente LAN:
- Forçar codec H.264 (hardware accelerated no S25).
- Configurar `iceServers: []` (vazio para conexão direta na rede local).

---

## ✅ Definição de Pronto (DoP)
- O app inicia o servidor e a câmera automaticamente.
- O OBS recebe o vídeo com <300ms de latência.
- Ao mudar de cena no OBS, o S25 Ultra troca de lente/zoom em <1s.
