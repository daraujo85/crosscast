# Graph Report - campilot  (2026-07-19)

## Corpus Check
- 18 files · ~7,194 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 143 nodes · 179 edges · 17 communities (14 shown, 3 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 18 edges (avg confidence: 0.78)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `bbb28332`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- .onCreate
- CameraService
- CameraManager
- Components.kt
- obs_campilot_controller.py
- CameraControlRegistry.kt
- StaticCameraBridge
- WebRtcManager
- ui.md
- 📡 Local Camera Streaming System (S25 Ultra + OBS)
- Prompt para redesign profissional do app de câmera/live para OBS
- Implementation Plan: Professional UI Redesign for Campilot

## God Nodes (most connected - your core abstractions)
1. `MainActivity` - 14 edges
2. `CameraManager` - 14 edges
3. `CameraService` - 10 edges
4. `Implementation Plan: Professional UI Redesign for Campilot` - 7 edges
5. `📡 Local Camera Streaming System (S25 Ultra + OBS)` - 7 edges
6. `Prompt para redesign profissional do app de câmera/live para OBS` - 7 edges
7. `ConnectionSheet()` - 6 edges
8. `Estados da interface` - 6 edges
9. `start()` - 4 edges
10. `stop()` - 4 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Import Cycles
- None detected.

## Communities (17 total, 3 thin omitted)

### Community 0 - ".onCreate"
Cohesion: 0.15
Nodes (10): CameraIdInfo, Bitmap, MainActivity, QuickLens, FocusIndicator(), CampilotTheme(), Bundle, CampilotCameraManager (+2 more)

### Community 1 - "CameraService"
Cohesion: 0.14
Nodes (12): CameraService, ByteArray, Context, start(), stop(), getBestLocalIpAddress(), Context, IBinder (+4 more)

### Community 2 - "CameraManager"
Cohesion: 0.21
Nodes (7): androidx, CameraManager, ByteArray, ImageProxy, LifecycleOwner, PreviewView, ProcessCameraProvider

### Community 3 - "Components.kt"
Cohesion: 0.16
Nodes (15): ConnectionSheet(), GlassPanel(), IconActionButton(), Bitmap, LivePill(), RecordButton(), StatusPill(), ZoomSelector() (+7 more)

### Community 4 - "obs_campilot_controller.py"
Cohesion: 0.38
Nodes (3): apply_preset(), on_event(), script_load()

### Community 13 - "ui.md"
Cohesion: 0.13
Nodes (14): Animações, Componentes visuais esperados, Erro de conexão, Estados da interface, Modal de conexão OBS / Campilot, OBS conectado, Regras importantes, Responsividade e usabilidade (+6 more)

### Community 14 - "📡 Local Camera Streaming System (S25 Ultra + OBS)"
Cohesion: 0.15
Nodes (12): 1. Visualização (Vídeo), 2. Automação (Troca de Lentes), ⚙️ Como Compilar, 🛠️ Detalhes Técnicos (Backend Android), Endpoints da API Local, 📂 Estrutura de Código, 📱 Informações do Dispositivo (S25 Ultra), 💻 Integração com OBS (+4 more)

### Community 15 - "Prompt para redesign profissional do app de câmera/live para OBS"
Cohesion: 0.20
Nodes (10): 1. Tela principal, Direção de estilo, Layout desejado, Linha 1: controle de zoom, Linha 2: botões de ação, Objetivo visual, Prompt para redesign profissional do app de câmera/live para OBS, Zona central: foco e feedback visual (+2 more)

### Community 16 - "Implementation Plan: Professional UI Redesign for Campilot"
Cohesion: 0.25
Nodes (7): 1. Foundation and Theming, 2. Reusable UI Components, 3. Main Interface Refactoring (`MainActivity.kt`), 4. Connection Modal (OBS / Campilot), 5. Animations and Refinement, 6. Verification and Testing, Implementation Plan: Professional UI Redesign for Campilot

## Knowledge Gaps
- **35 isolated node(s):** `CameraIdInfo`, `1. Foundation and Theming`, `2. Reusable UI Components`, `3. Main Interface Refactoring (`MainActivity.kt`)`, `4. Connection Modal (OBS / Campilot)` (+30 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MainActivity` connect `.onCreate` to `CameraService`?**
  _High betweenness centrality (0.079) - this node is a cross-community bridge._
- **Why does `getBestLocalIpAddress()` connect `CameraService` to `.onCreate`?**
  _High betweenness centrality (0.058) - this node is a cross-community bridge._
- **What connects `CameraIdInfo`, `1. Foundation and Theming`, `2. Reusable UI Components` to the rest of the system?**
  _35 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `.onCreate` be split into smaller, more focused modules?**
  _Cohesion score 0.1471861471861472 - nodes in this community are weakly interconnected._
- **Should `CameraService` be split into smaller, more focused modules?**
  _Cohesion score 0.1380952380952381 - nodes in this community are weakly interconnected._
- **Should `ui.md` be split into smaller, more focused modules?**
  _Cohesion score 0.13333333333333333 - nodes in this community are weakly interconnected._