import obspython as obs
import urllib.request
import urllib.parse

# Configurações do Script
phone_ip = "192.168.31.91"
port = "8080"

# Mapeamento de Cenas -> Presets do App
# Edite os nomes das cenas abaixo para bater com o seu OBS
scene_mapping = {
    "Cena Wide": "wide",
    "Cena Aberta": "ultrawide",
    "Cena Zoom": "tele3x",
    "Cena Periscopio": "tele10x",
    "Cena Selfie": "selfie",
    "Cena Close": "close-up"
}

def apply_preset(preset_id):
    url = f"http://{phone_ip}:{port}/api/presets/apply/{preset_id}"
    try:
        req = urllib.request.Request(url, method="POST")
        with urllib.request.urlopen(req, timeout=1) as response:
            print(f"Campilot: Aplicado preset {preset_id}")
    except Exception as e:
        print(f"Campilot: Erro ao conectar no celular: {e}")

def on_event(event):
    if event == obs.OBS_FRONTEND_EVENT_SCENE_CHANGED:
        current_scene = obs.obs_frontend_get_current_scene()
        scene_name = obs.obs_source_get_name(current_scene)
        obs.obs_source_release(current_scene)
        
        print(f"Campilot: Cena mudou para '{scene_name}'")
        
        if scene_name in scene_mapping:
            preset = scene_mapping[scene_name]
            apply_preset(preset)

def script_description():
    return "Controlador Automático Campilot para S25 Ultra\n\nMuda a lente do celular ao trocar de cena no OBS."

def script_load(settings):
    obs.obs_frontend_add_event_callback(on_event)
    print("Campilot: Script carregado com sucesso!")

def script_update(settings):
    global phone_ip
    phone_ip = obs.obs_data_get_string(settings, "phone_ip")

def script_properties():
    props = obs.obs_properties_create()
    obs.obs_properties_add_text(props, "phone_ip", "IP do S25 Ultra:", obs.OBS_TEXT_DEFAULT)
    return props
