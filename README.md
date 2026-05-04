# 📡 Local Camera Streaming System (S25 Ultra + OBS)

## 🚀 Status do Projeto
O projeto está funcional com suporte a troca de lentes via rede e streaming MJPEG de baixa latência.

### 📱 Informações do Dispositivo (S25 Ultra)
- **IP Local:** `192.168.31.91` (Porta 8080)
- **Mapeamento de Lentes (IDs Técnicos):**
  - **ID 0:** Lente Principal (Wide) - B
  - **ID 1:** Câmera Frontal (Selfie) - F
  - **ID 2:** Lente Ultra-wide - B
  - **ID 3:** Telefoto 3x - B
  - **ID 4:** Telefoto 10x (Periscópio) - B

---

## 🛠️ Detalhes Técnicos (Backend Android)

### Endpoints da API Local
- **Status:** `GET http://<device-ip>:8080/status`
- **Stream de Vídeo:** `GET http://<device-ip>:8080/api/stream` (MJPEG)
- **Aplicar Preset:** `POST http://<device-ip>:8080/api/presets/apply/{id}`
- **Controle Direto:** `POST http://<device-ip>:8080/api/camera/select?id={id}&zoom={zoom}`

### Presets Pré-configurados
| ID | Nome | Câmera | Zoom |
|---|---|---|---|
| `wide` | Wide (Main) | 0 | 1.0x |
| `ultrawide` | Ultra-wide | 2 | 1.0x |
| `tele3x` | Tele 3x | 3 | 1.0x |
| `tele10x` | Tele 10x | 4 | 1.0x |
| `selfie` | Selfie | 1 | 1.0x |
| `close-up` | Main Close-up | 0 | 2.0x |

---

## 💻 Integração com OBS

### 1. Visualização (Vídeo)
- Adicione uma fonte de **Navegador (Browser Source)**.
- URL: `http://192.168.31.91:8080/api/stream`
- Largura/Altura: 1920x1080 (ou conforme o sensor).

### 2. Automação (Troca de Lentes)
O arquivo `obs_campilot_controller.py` na raiz do projeto deve ser carregado no OBS:
1. Vá em `Ferramentas -> Scripts`.
2. Adicione o arquivo Python.
3. Configure o IP do celular na interface do script.
4. Nomeie suas cenas conforme o dicionário `scene_mapping` dentro do script.

---

## 📂 Estrutura de Código
- **`MainActivity.kt`**: Interface Compose, gerenciamento de permissões e ponte de frames da câmera.
- **`CameraService.kt`**: Serviço de primeiro plano que hospeda o servidor Ktor e gerencia o banco de dados.
- **`CameraManager.kt`**: Lógica CameraX para troca de lentes, zoom e correção de rotação de frames.
- **`AppDatabase.kt`**: Persistência Room para salvar as configurações de presets.

---

## ⏭️ Próximos Passos Sugeridos
1. **Implementar WebRTC:** Substituir o MJPEG por WebRTC para reduzir o consumo de banda e aumentar a nitidez.
2. **Controle de Foco:** Adicionar endpoint para `tap-to-focus` via API.
3. **Interface de Presets:** Criar uma tela no app para editar os valores de zoom e IDs de cada preset visualmente.

---

## ⚙️ Como Compilar
1. Abra a pasta raiz no **Android Studio**.
2. Aguarde o Sync do Gradle (Gradle 8.7 / AGP 8.5.0).
3. Certifique-se de que o `gradle.properties` contém `android.useAndroidX=true`.
4. Clique em **Run** apontando para o S25 Ultra.
