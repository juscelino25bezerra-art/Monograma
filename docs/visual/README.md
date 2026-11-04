# Monograma — sistema visual

A proposta visual do Monograma (fase 2) aplicada ao app. O código marca cada ponto com
`MONOGRAMA(visual)` ou `MONOGRAMA(icon)`.

## Paleta

Cores em `smarttubetv/src/stmobile/res/values-night/colors_mobile.xml` (escuro) e
`values/colors_mobile.xml` (claro). Os valores do escuro foram medidos nos mockups.

| Papel | Escuro | Claro | Token |
|---|---|---|---|
| Página | `#0E0E10` | `#F6F5F7` | `mobile_color_background` (lido por `?attr/newtubeColorBackground`) |
| Cartão, folha, diálogo | `#1C1B20` | `#FFFFFF` | `mobile_color_card` / `mobile_color_surface` |
| Um degrau acima do cartão | `#2E2D33` | `#EDECEF` | `mobile_color_raised` (círculo dos ícones, selo "Desativado", trilho do segmentado) |
| Texto | `#F4F4F6` | `#141316` | `mobile_color_on_surface` |
| Texto secundário | `#A1A1A9` | `#66656C` | `mobile_color_on_surface_secondary` |
| **Coral** (destaque) | `#FE5A4E` | `#E5483C` | `mobile_color_accent` |
| Texto sobre coral | `#140F0E` | `#140F0E` | `mobile_color_on_accent` (quase preto, nunca branco) |
| Coral fraco | `#3A1B1B` | `#FDE3DF` | `mobile_color_accent_container` (pílula da aba ativa, selo "Ativado") |

O coral é o único destaque: aba ativa, interruptores e segmentos ligados, títulos de grupo,
o botão principal (Inscrever-se, Fazer login) e a parte assistida de toda barra de progresso
(`PlayerTimeBar.PLAYED_COLOR`). O preto puro (Configurações > Aparência) continua trocando só a
página e as superfícies (`ThemeOverlay.NewTube.PureBlack`).

Regra herdada do NewTube que continua valendo: layouts e drawables leem as cores da página por
`?attr/newtubeColor*`, nunca por `@color/mobile_color_surface|card|field|...` direto
(`MobileThemeTest.layoutsReadThePageColoursThroughTheTheme`).

## Tipografia

Manrope (SIL OFL 1.1, `res/font/`, aviso em `THIRD_PARTY_NOTICES.md`), pesos 400 a 800.

- `@font/manrope` (família) é o padrão do tema; `textStyle="bold"` pega o 700.
- `@font/manrope_semibold` substitui o antigo `sans-serif-medium`; títulos usam `manrope_bold`
  ou `manrope_extrabold`.
- Texto desenhado em código usa `AppFonts.semibold()` / `AppFonts.semiboldSpan()`.
- O tema define só `android:fontFamily`. Um `app:fontFamily` no tema vira o padrão que o AppCompat
  aplica por cima da fonte de cada MaterialButton e Chip, que então saem em peso normal.
- Android 7 (API 24–25) não carrega `@font` em TextView de plataforma e fica com a fonte do sistema.

## Componentes

- **Cartões**: `bg_monograma_card` (20 dp, cor do cartão, ripple). Miniaturas com 16 dp
  (`mobile_card_corner_radius`).
- **Grupos de configurações**: as linhas entre dois cabeçalhos formam um cartão;
  `SettingsAdapter.bindGroup` escolhe `bg_settings_group_{single,top,middle,bottom}` e o separador.
  Ícone num disco de 40 dp, seta nas linhas que abrem algo, selo Ativado/Desativado
  (`SettingsRow.status`), etiqueta "Experimental" (`SettingsRow.tag`).
- **Segmentado**: `SettingsRow.Choice.segmented()` / `segmentedUnderHeader()`; continua sendo uma
  `KIND_CHOICE` (busca, aplicar e reiniciar iguais), só desenhada no lugar.
- **Interruptor**: `Widget.Monograma.Switch` (`SwitchMaterial` com drawables próprios). O
  `MaterialSwitch` do Material 3 lê cores de tema M3 que este tema M2 não tem.
- **Chips**: `Widget.Monograma.Chip` (tema `chipStyle`): pílula de 40 dp, cor do cartão, invertido
  quando selecionado.
- **Barra inferior**: indicador ativo em pílula coral (`Widget.Monograma.BottomNav.ActiveIndicator`),
  ícones em contorno nos dois estados.
- **Títulos de grupo**: coral, ExtraBold, caixa alta, espaçamento 0,08 (`item_mobile_settings_header`,
  reaproveitado na aba Você e em "A SEGUIR").

## Ícone

`res/drawable/ic_launcher_*` (vetor 108 dp) e PNGs em `mipmap-*` para Android 7. Duas
`activity-alias` da Splash no manifesto do stmobile: `LauncherGrafite` (padrão) e `LauncherCoral`;
`AppIcons` liga uma e desliga a outra (Configurações > Aparência e idioma > Ícone do app). A camada
monocromática serve para os ícones temáticos do Android 13+. `ic_monograma_logo` /
`ic_monograma_logo_coral` são as versões em quadrado arredondado (topo do Início e seletor).

## Telas

- **Início**: logo e "Monograma" no topo; nas outras abas, o nome da aba como título grande
  (`BrowseTopBar`).
- **Downloads**: cartão de armazenamento e chips Recentes/Tamanho/Nome (`DownloadsHeader`,
  `DownloadSort`), vídeos em linhas (`item_video_card_row`, `VideoCardAdapter.setRowLayout`),
  lista vazia "Assista sem internet".
- **Você**: cartão do perfil (iniciais em `InitialsAvatar` até a foto chegar), Sua biblioteca em
  blocos, Fixados e Explorar em chips, Configurações num cartão.
- **Vídeo**: título em ExtraBold, ações em pílulas, Inscrever-se coral, cartão de comentários,
  "A SEGUIR" com o interruptor de reprodução automática (Próximo vídeo ↔ Pausar, a mesma
  preferência do player).
- **Configurações**: como acima; Aparência e idioma nos grupos do mockup (Ícone do app, Tema,
  Idioma e região, Interface); Avançado com o buffer segmentado.

Ficou de fora porque o app não tem a função (e a proposta só desenha): os chips de categoria do
Início ("Tudo, Tecnologia, Música…") e as contagens nos blocos da biblioteca ("309 vídeos").

## Conferir

O workflow **Monograma telas** (`.github/workflows/monograma-telas.yml`) roda em todo push para
`monograma/**`: emulador Android 14 em pt-BR, temas escuro e claro, capturas das telas acima
impressas no log em base64 (`tools/screens/print_screens.py`) e guardadas como artefato.
