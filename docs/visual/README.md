# YewPlay — sistema visual

Fonte: `YewPlay_Proposta_de_Interface_1.pdf` (novembro de 2026). Sem Material 3: os componentes
Material Components 1.14 continuam por baixo (tema M2), mas cor, forma e tipo são do YewPlay.

## Cores (`res/values{,-night}/colors_mobile.xml`)

| Papel | Escuro | Claro | Token |
|---|---|---|---|
| Fundo | Grafite `#18141A` | Papel `#F6F2EE` | `mobile_color_background` / `?attr/newtubeColorBackground` |
| Blocos | Grafite claro `#221D24` | Papel escuro `#ECE6E1` | `mobile_color_surface` |
| Secundário | Fumaça `#2C262F` | Areia `#E3DCD7` | `mobile_color_raised` |
| Texto e ação | Marfim `#F3EEE9` | Tinta `#1C171E` | `mobile_color_on_surface`, `mobile_color_inverse_surface` |
| Apoio | Cinza quente `#9C9399` | `#6B6268` | `mobile_color_on_surface_secondary` |
| Estado | Rubi `#E0364F` | Rubi escuro `#C8243C` | `mobile_color_accent` |

- A ação principal (Inscrever-se, chave ligada) é **marfim/tinta**, nunca rubi.
- Rubi só marca estado: ponto da aba ativa, indicador da navegação, progresso, ao vivo, frutos
  da gravura, barra do player.
- Layouts e drawables leem as cores de página por `?attr/newtubeColor*` (MobileThemeTest);
  o preto AMOLED opcional troca só esses atributos.

## Tipo (`res/font/`, OFL em `docs/licenses/`)

- Os arquivos da Bricolage são os cortes de título (tamanho óptico 24pt SemiBold, 36pt Bold,
  48pt ExtraBold), os mesmos do YewPlay em Compose; os nomes de recurso continuam
  `bricolage_{semibold,bold,extrabold}`. Backup dos cortes anteriores: branch
  `yewplay/backup-fontes-2026-11-07`.
- **Atenção:** o tema define `android:fontFamily` (Instrument Sans) e isso vence a fonte de um
  `textAppearance` declarado no XML. Todo título em Bricolage declara
  `android:fontFamily="@font/bricolage_bold"` na própria tag (`setTextAppearance` em código não
  tem o problema).

- **Bricolage Grotesque** (títulos): tela 32sp `-0.025` (`TextAppearance.Yew.ScreenTitle`),
  seção 19sp (`.SectionTitle`), vídeo em destaque 17sp, título do vídeo no player 20sp.
- **Instrument Sans** (interface, fonte do tema): título de item SemiBold 14.5sp, corpo 13.5–15sp,
  metadados 12.5–13sp.
- O tema define só `android:fontFamily`; não pôr `app:fontFamily` no tema (troca o peso de todo
  MaterialButton e Chip).

## Ícones

Phosphor (MIT), traço regular; o preenchido só no item ativo da navegação e em controles do
player. Conversão: `phosphor2vd.py` manteve tamanho, cor e tint de cada vetor.

## Formas e medidas (`dimens_mobile.xml`, prefixo `yew_`)

- Miniatura de lista 144x81, raio 10dp; miniatura em destaque sem raio, de ponta a ponta.
- Avatar de canal: quadrado arredondado a 32% do lado (`YewAvatar`).
- Botões 12dp de raio, 44dp de altura; folhas e diálogos 22dp.
- Barra flutuante: raio 26dp, 12dp das bordas, 60dp de navegação; mini-player dentro dela.
- Margem lateral 20dp; listas sem cartões; alvos de toque de 48dp.

## Movimento

- Ponto rubi entre abas da navegação: 180ms (`BrowseNavCapsule`).
- Player abre da barra / minimiza para ela: 250ms, alvo do vídeo dado pelo host
  (`MiniHost.getMiniVideoTargetOnScreen`).

## Telas

- Início: título "Início", abas de filtro de texto com ponto rubi (cada aba é uma seção do
  Browse: Tudo, Música, Jogos, Ao vivo...).
- Biblioteca: linhas Histórico, Downloads (com contagem), Playlists, Canais...; engrenagem de
  Configurações no topo.
- Player: título Bricolage; ações rotuladas Curtir (contagem), Baixar, Ouvir, Salvar, Janela,
  Não curti, Compartilhar; Inscrever-se marfim -> Inscrito em Fumaça; "A seguir" com a chave
  Automático (modo Todos x Pausar); barra de progresso rubi.
- Configurações: título grande, títulos de grupo pequenos em cinza quente, linhas com ícone e
  descrição de uma linha.
- Estados vazios: gravura do ramo de teixo (`ic_mobile_empty`).
- Início: o primeiro vídeo de "Tudo" é o destaque editorial (`item_video_card_hero`): de ponta a
  ponta, mais alto, título Bricolage 22sp e metadados em branco sobre um degradê grafite.
- Biblioteca: "Continue de onde parou" no topo, cartões 176x99 com a linha rubi do quanto já foi
  visto, a partir do estado local de reprodução (`VideoStateService`, entre 3% e 95%).
- Downloads: abas Tudo, Vídeos e Áudios na fileira de filtros; excluir mostra "Desfazer" por 5 s e
  só então apaga os arquivos (`DownloadMenu.deleteWithUndo`).
- Sobre numa tela só: topo com ícone, nome, versão, origem (NewTube e SmartTube), licença MIT em
  uma frase e o significado do nome; linhas em grupos nomeados.
- Esqueleto de carregamento no formato dos itens: grande no Início, compacto nas listas e na Busca.

## Voz (pt-BR)

Ouvir (não "segundo plano"), Janela (não "Popup"/"Picture-in-picture"), "Sem conexão." +
"Tentar de novo", Inscrever-se -> Inscrito.

## Fase 4 (fidelidade à proposta)

- Gravuras: os quatro traçados da proposta (`yew_gravura_{boas_vindas,inscricoes,downloads,sobre}`),
  convertidos dos SVG do YewPlay em Compose por `tools/visual/gravuras.py` (skia-pathops; o traço
  é cortado em ladrilhos porque o aapt2 não aceita um path com mais de 32 mil caracteres).
- Estado vazio (p. 17): gravura, título em Bricolage, frase e ação em marfim.
- Mini-player: tocar/pausar e Próximo (p. 14); fechar é arrastar a barra para o lado.
- Player: só as cinco ações; Não curti e Compartilhar no menu Mais.
- Busca: campo grande com lupa, X e Cancelar; abas Tudo, Vídeos, Canais, Playlists, Filmes.
- Configurações: lupa no topo no lugar do campo.
- Downloads: espaço usado, Baixando (velocidade, Pausar/Retomar no item e Pausar tudo),
  Concluídos (formato e data). A pausa guarda as partes; Retomar continua delas.
- Inscrições: fileira de canais com ponto rubi e feed por Hoje, Ontem, Esta semana e Antes (o
  dia sai do texto relativo do YouTube, `FeedDays`).
- Ouvir: tela própria (`MobileListenActivity`), controla o player da sessão mini.
- Playlist: capa em mosaico, Reproduzir, Aleatório, Ouvir e itens numerados.
- Canal: topo com avatar e nome em Bricolage; abas com ponto rubi.
- Início: sino das notificações; transmitir só aparece com uma sessão ativa.

## Ainda diferente da proposta

- Início: sem "serviço e região" acima do título (o app tem um serviço só).
- Busca: a aba "Músicas" vira "Filmes" (o YouTube não tem filtro de música nessa busca).
- Inscrições: sem abas de grupo nem "atualizado há"; sem ícones de ordenar.
- Canal: sem imagem de banner, número de inscritos, Inscrever-se, avisos, RSS e "Mais
  recentes" (a tela de canal não recebe esses dados hoje).
- Biblioteca: "Suas playlists" com capa quadrada.
- Ouvir: a fonte aparece como "Tocando agora", não o nome da playlist.

## Verificação

`YewPlay telas` (`.github/workflows/yewplay-telas.yml`) roda o emulador API 34 em pt-BR,
percorre `tools/screens/capture.sh` e imprime as capturas em base64 no log
(`tools/screens/print_screens.py`).
