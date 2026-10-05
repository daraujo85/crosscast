# Prompt para redesign profissional do app de câmera/live para OBS

Você é um designer UI/UX sênior especializado em apps mobile com estética premium inspirada nos apps nativos da Apple: limpo, elegante, minimalista, fluido e com alta atenção a detalhes visuais.

Preciso redesenhar a interface de um app mobile usado para transmissão de vídeo ao vivo para OBS. O app funciona como uma câmera remota: ele captura a imagem do celular e disponibiliza um endereço local para ser usado no OBS ou navegador, por exemplo:

`http://192.168.31.87:8080`

O app atual já possui:
- Preview da câmera em tela cheia.
- Informação de IP no topo.
- Nome do dispositivo abaixo do IP.
- Botão de flash.
- Indicador de gravação/transmissão.
- Controles de zoom: `.6x`, `1x`, `2x`, `5x`, `10x`.
- Slider de zoom.
- Botão principal vermelho para iniciar/parar transmissão.
- Botão de QR Code para conexão Campilot/OBS.
- Botão para alternar câmera frontal/traseira.
- Modal com QR Code e endereço HTTP.

Quero manter a funcionalidade, mas transformar completamente o visual para parecer um app profissional, moderno e bonito, no estilo Apple Camera / Blackmagic Camera / apps premium de live streaming.

## Objetivo visual

Criar uma interface:
- Minimalista, sofisticada e limpa.
- Com aparência premium.
- Com boa hierarquia visual.
- Com menos poluição na tela.
- Com controles translúcidos sobre o vídeo.
- Com uso de blur, glassmorphism e cantos arredondados.
- Com animações suaves.
- Com visual coerente para uso em igreja, eventos, lives e produção de vídeo.

A interface deve transmitir sensação de:
- Qualidade profissional.
- Confiabilidade para transmissão ao vivo.
- Simplicidade para operadores não técnicos.
- Estética semelhante aos apps da Apple.

## Direção de estilo

Use como referência visual:
- Apple Camera app.
- Apple VisionOS glass panels.
- Blackmagic Camera app, mas com menos poluição.
- Halide Camera, pela elegância dos controles.
- Apps profissionais de vídeo, porém com visual mais simples.

Paleta sugerida:
- Fundo principal: o próprio preview da câmera.
- Overlays: preto translúcido com blur.
- Textos principais: branco.
- Destaques: amarelo suave ou verde neon discreto para conexão ativa.
- Estado de transmissão: vermelho vivo.
- Botões inativos: branco translúcido.
- Botões ativos: preenchimento claro ou destaque colorido.

Não usar aparência genérica de Material Design. Evitar botões quadrados comuns, sombras exageradas, cores aleatórias e textos grandes demais.

## Layout desejado

### 1. Tela principal

A tela deve ser fullscreen, com o preview da câmera ocupando 100% da área.

Adicionar safe areas corretamente para iPhone e Android.

A interface deve ser dividida em 3 zonas:

---

## Zona superior: status e conexão

Criar uma barra superior flutuante, discreta e translúcida, com blur.

Ela deve conter:

À esquerda:
- Um pequeno indicador de conexão.
- Texto: `Live Camera`
- Abaixo ou ao lado: IP local `192.168.31.87`
- Nome do dispositivo: `SM-S938B`

Exemplo visual:

`● Online   Live Camera`
`192.168.31.87 · SM-S938B`

À direita:
- Ícone de flash.
- Ícone de configurações.
- Opcional: ícone de qualidade/resolução.

Regras visuais:
- A barra não deve ocupar muito espaço.
- O IP deve ser legível, mas não parecer um debug técnico.
- Usar fonte pequena, elegante e com peso médio.
- Indicador verde quando o servidor estiver ativo.
- Indicador vermelho ou cinza quando estiver parado.

---

## Zona central: foco e feedback visual

Manter a grade de câmera, mas torná-la mais sutil:
- Linhas finas.
- Opacidade baixa.
- Não deve competir com o vídeo.

Adicionar feedback visual discreto no centro quando o usuário tocar para focar:
- Pequeno quadrado ou círculo de foco com animação.
- Fade out após alguns segundos.

Indicador de transmissão:
- Quando estiver transmitindo, mostrar no topo central um pequeno pill:
  `● LIVE`
- O pill deve ser vermelho, com blur e texto branco.
- Deve ter animação sutil de pulso no ponto vermelho.
- Não usar apenas uma bolinha solta no topo; precisa parecer um estado profissional.

---

## Zona inferior: controles principais

Criar uma área inferior flutuante com visual de painel de vidro.

Ela deve conter:

### Linha 1: controle de zoom

Substituir os botões atuais `.6x`, `1x`, `2x` por um seletor elegante em formato segmented control / pill.

Exemplo:

`0.6x    1x    2x`

Regras:
- O zoom ativo deve ter fundo branco ou amarelo suave.
- Os outros devem ser translúcidos.
- O seletor deve ficar centralizado.
- Os botões devem ser arredondados.
- Usar animação suave ao trocar zoom.

Abaixo do seletor, manter um slider de zoom:
- Minimalista.
- Linha fina.
- Thumb circular pequeno.
- Destaque amarelo suave.
- Sem aparência pesada.
- O valor do zoom atual pode aparecer acima do slider como `2.8x`, mas de forma discreta e elegante.

### Linha 2: botões de ação

Na parte inferior, criar três ações principais:

Esquerda:
- Botão de QR Code / conexão OBS.
- Ícone simples.
- Label pequeno opcional: `OBS`

Centro:
- Botão principal de iniciar/parar transmissão.
- Grande, circular.
- Vermelho quando pronto para iniciar.
- Quando transmitindo, pode virar botão de stop com vermelho escuro ou ícone de stop.
- Deve ter anel externo branco/translúcido.
- Deve parecer botão de gravação profissional, não um botão comum.

Direita:
- Alternar câmera.
- Ícone de câmera reversa.
- Label pequeno opcional: `Camera`

Regras:
- Os botões laterais devem ficar em círculos translúcidos com blur.
- O botão central deve ser visualmente dominante.
- A área inferior deve respeitar o polegar e ser confortável para uso rápido.
- Evitar excesso de texto.

---

# Modal de conexão OBS / Campilot

Redesenhar o modal atual para parecer um sheet moderno da Apple.

Quando o usuário tocar no botão de QR Code, abrir um bottom sheet ou card central com blur no fundo.

Conteúdo do modal:

Título:
`Conectar ao OBS`

Subtítulo:
`Use o endereço abaixo como fonte de navegador ou escaneie o QR Code.`

Card do QR Code:
- QR Code grande.
- Fundo branco.
- Cantos arredondados.
- Padding generoso.

Endereço:
`http://192.168.31.87:8080`

Ações:
- Botão primário: `Copiar endereço`
- Botão secundário: `Fechar`
- Opcional: botão `Compartilhar`

Regras visuais:
- O modal deve ter cantos arredondados grandes.
- Usar fundo preto translúcido com blur.
- O endereço HTTP deve ficar em um pequeno card/pill com botão de copiar.
- O botão “Fechar” não deve parecer pesado.
- O QR Code precisa ser centralizado e com boa margem.
- Não usar caixa escura simples sem refinamento visual.

---

# Estados da interface

Criar visual para os seguintes estados:

## Servidor parado
- Indicador cinza: `Offline`
- Botão principal vermelho com label/ícone de iniciar.
- IP pode aparecer como indisponível ou oculto.

## Servidor ativo, sem OBS conectado
- Indicador amarelo ou verde: `Pronto`
- Mostrar IP.
- Botão principal pronto para transmitir.

## OBS conectado
- Indicador verde: `OBS conectado`
- Pequeno badge no topo ou na barra superior.
- Pode mostrar número de viewers/conexões se existir.

## Transmitindo
- Badge vermelho `LIVE`.
- Botão central muda para estado de parar.
- Barra superior pode ganhar destaque vermelho discreto.
- Feedback visual deve ser claro, mas sem poluir.

## Erro de conexão
- Mostrar toast elegante:
  `Não foi possível iniciar o servidor`
- Toast com fundo escuro translúcido, ícone e mensagem curta.

---

# Componentes visuais esperados

Criar ou ajustar os seguintes componentes:

1. `GlassPanel`
    - Container translúcido com blur.
    - Cantos arredondados.
    - Borda fina branca com opacidade baixa.
    - Sombra suave.

2. `StatusPill`
    - Indicador de estado: Online, Offline, Live, OBS conectado.
    - Pequeno, elegante e legível.

3. `ZoomSelector`
    - Segmented control moderno.
    - Estados ativo/inativo.
    - Animação de seleção.

4. `ZoomSlider`
    - Slider minimalista.
    - Valor atual opcional.

5. `RecordButton`
    - Botão circular profissional.
    - Estados: idle, starting, live, stopping.
    - Anel externo e animações suaves.

6. `IconActionButton`
    - Botões laterais com blur.
    - Ícones simples.
    - Tamanho confortável para toque.

7. `ConnectionSheet`
    - Modal/bottom sheet moderno para QR Code e link.

8. `Toast`
    - Mensagens temporárias elegantes.

---

# Animações

Adicionar animações sutis:
- Fade/slide na entrada dos painéis.
- Pulso suave no indicador LIVE.
- Transição suave no zoom selecionado.
- Feedback tátil/visual ao tocar botões.
- Bottom sheet abrindo de baixo com spring animation.
- Toast entrando e saindo suavemente.

As animações devem ser rápidas e discretas, sem parecerem chamativas demais.

---

# Tipografia

Usar tipografia semelhante à Apple:
- iOS: San Francisco / system font.
- Android: system font, com pesos bem definidos.

Regras:
- Textos pequenos, limpos e legíveis.
- Evitar textos grandes demais.
- Usar peso semibold para títulos e medium para informações técnicas.
- IP deve ter aparência monoespaçada ou técnica discreta, mas sem parecer debug feio.

---

# Ícones

Usar ícones consistentes, finos e modernos.

Sugestões:
- Flash: `bolt.slash` ou equivalente.
- QR Code: `qrcode`
- Alternar câmera: `camera.rotate`
- Configurações: `gearshape`
- Copiar: `doc.on.doc`
- Link: `link`
- Live: círculo vermelho/ponto.

Ícones devem ser:
- Brancos.
- Finos.
- Centralizados.
- Com tamanho consistente.

---

# Responsividade e usabilidade

A interface deve funcionar bem em:
- Celulares pequenos.
- Celulares grandes.
- Modo retrato.
- Futuramente modo paisagem.

Os controles não podem ficar muito próximos das bordas.

Garantir:
- Área de toque mínima de 44px.
- Bom contraste.
- Legibilidade sobre fundos claros e escuros.
- Elementos importantes sempre dentro da safe area.

---

# Regras importantes

- Não remover funcionalidades existentes.
- Não esconder o endereço do OBS.
- Não deixar o IP com aparência de texto de debug.
- Não usar Material Design genérico.
- Não usar cards quadrados sem personalidade.
- Não poluir a tela com muitos textos.
- Não colocar botões demais na tela principal.
- Não prejudicar a visualização da câmera.
- Priorizar experiência visual premium e simples.

---

# Resultado esperado

Entregar uma nova interface com aparência profissional, parecida com apps nativos da Apple, mantendo todas as funcionalidades atuais do app de live para OBS.

A tela principal deve parecer um app de câmera profissional, e o modal de conexão OBS deve parecer um bottom sheet moderno, elegante e fácil de usar.

O resultado final precisa passar a sensação de que o app é confiável para uso em lives reais, eventos, igrejas, gravações e transmissões profissionais.