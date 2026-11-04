<div align="center">

# Monograma

### Um cliente de YouTube para celulares Android, em português.

O Monograma é um fork do [NewTube](https://github.com/aleixrodriala/newtube), que por sua vez é
construído sobre o [SmartTube](https://github.com/yuliskov/SmartTube) de [@yuliskov](https://github.com/yuliskov).
Faça login com um código, ouça em segundo plano, baixe vídeos para assistir offline e transmita
para o SmartTube na TV.

[![Última versão](https://img.shields.io/github/v/release/juscelino25bezerra-art/Monograma?style=flat-square&label=vers%C3%A3o&color=FF5A4E)](https://github.com/juscelino25bezerra-art/Monograma/releases/latest)
[![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B-FF5A4E?style=flat-square)](#download)
[![Licença: MIT](https://img.shields.io/badge/licen%C3%A7a-MIT-FF5A4E?style=flat-square)](LICENSE)

</div>

> [!NOTE]
> **Por baixo, o trabalho é do SmartTube e do NewTube.** O motor que conversa com o YouTube, o
> login e as integrações com SponsorBlock, DeArrow e Return YouTube Dislike vêm do
> [SmartTube](https://github.com/yuliskov/SmartTube). A interface de toque, o player e os
> downloads vêm do [NewTube](https://github.com/aleixrodriala/newtube). O Monograma muda a
> identidade visual, traduz tudo para português do Brasil e ajusta a experiência. É um projeto
> independente, sem endosso dos desenvolvedores do SmartTube ou do NewTube.

## O que muda em relação ao NewTube

- Nome, ícone e identidade visual próprios
- Tradução completa para português do Brasil, com vírgula decimal e relógio de 24 horas
- Pacote próprio (`io.github.juscelino25bezerra.monograma`): instala ao lado do NewTube, do
  SmartTube e do app oficial
- Atualizações vêm das versões deste repositório

## Download

Baixe na página de [versões](https://github.com/juscelino25bezerra-art/Monograma/releases/latest).

| Seu aparelho | Arquivo |
|:--|:--|
| Quase todo celular dos últimos ~8 anos | `Monograma_<versão>_arm64-v8a.apk` |
| Celulares 32 bits mais antigos | `Monograma_<versão>_armeabi-v7a.apk` |
| Não sabe (qualquer celular ARM) | `Monograma_<versão>_universal.apk` (maior) |
| Aparelhos x86 e emuladores | `Monograma_<versão>_x86.apk` |

- **Requer Android 7.0 ou mais recente.**
- **Atualizações:** o app verifica sozinho se há versão nova. Com o
  [Obtainium](https://obtainium.imranr.dev), escolha *Adicionar app* e cole
  `https://github.com/juscelino25bezerra-art/Monograma`.
- **Confira o que você instala:** cada versão lista o SHA-256 de todos os arquivos, e os APKs são
  gerados pelo GitHub Actions a partir da tag:
  `gh attestation verify <arquivo>.apk -R juscelino25bezerra-art/Monograma`.

## Compilar

```
ANDROID_HOME=<sdk> ./gradlew :smarttubetv:assembleStmobileDebug
# -> smarttubetv/build/outputs/renamed_apks/stmobileDebug/Monograma_<versão>_<abi>.apk
```

Instruções completas em [docs/BUILDING.md](docs/BUILDING.md). Contexto técnico para quem for mexer
no código: [CLAUDE.md](CLAUDE.md) e [docs/mobile-port/](docs/mobile-port/).

Para conferir a tradução depois de mudar textos:

```
python3 tools/i18n/check_ptbr.py
```

## Créditos

| Projeto | Por | Usado para |
|:--|:--|:--|
| [NewTube](https://github.com/aleixrodriala/newtube) | [@aleixrodriala](https://github.com/aleixrodriala) e colaboradores | Interface para celular, player, downloads e casting |
| [SmartTube](https://github.com/yuliskov/SmartTube) | [@yuliskov](https://github.com/yuliskov) e colaboradores | O motor, as contas e as integrações |
| [SponsorBlock](https://sponsor.ajay.app) · [DeArrow](https://dearrow.ajay.app) | Ajay Ramachandran e colaboradores | Dados de trechos e títulos (CC BY-NC-SA 4.0) |
| [Return YouTube Dislike](https://returnyoutubedislike.com) | Colaboradores do RYD | Contagem de "Não gostei" |
| [androidx Media3](https://developer.android.com/media/media3) | Google (Apache-2.0) | Reprodução |

Avisos completos em [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Os READMEs originais ficam
em [docs/UPSTREAM_README_NewTube.md](docs/UPSTREAM_README_NewTube.md) e
[docs/UPSTREAM_README_SmartTube.md](docs/UPSTREAM_README_SmartTube.md).

## Licença

[MIT](LICENSE), a mesma do SmartTube e do NewTube. © yuliskov (SmartTube), colaboradores do
NewTube e do Monograma. Os avisos de copyright originais são mantidos.

---

*O Monograma é um projeto independente e não oficial. Não tem afiliação, financiamento,
autorização ou endosso do Google LLC, do YouTube, nem dos desenvolvedores do SmartTube ou do
NewTube, e não hospeda conteúdo. Salve apenas o que a lei e os termos da plataforma permitem.
YouTube, Android e Google são marcas do Google LLC.*
