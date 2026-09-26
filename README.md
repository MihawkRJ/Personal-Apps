# Personal Apps

Apps de código aberto (Android e web) feitos por **Widney Silva**. Pode baixar, usar, estudar e adaptar à vontade (Licença MIT).

| App | Pasta | O que faz | Baixar |
|---|---|---|---|
| **Leitor E-Ink** | [`LeitorEInk/`](LeitorEInk/) | Leitor de livros estilo Kindle (PDF, EPUB e TXT): tema "papel" claro ou escuro, letra ajustável, modo texto do PDF e virar página com o dedo. | [Releases](../../releases) |
| **Revelador** | [`revelador/`](revelador/) | Editor de fotos por predefinições, sem prompts: iluminação da cena (golden hour, noturna, fim de tarde…), color grading e perfis de câmeras de cinema. Funciona no iPhone como app da Tela de Início. | [Abrir](https://mihawkrj.github.io/Personal-Apps/revelador/) · [versão PC](https://mihawkrj.github.io/Personal-Apps/revelador/pc.html) |

## Instalar no celular

1. Abra **[Releases](../../releases)** e baixe o `.apk` mais recente do app.
2. Abra o arquivo no celular. Se o Android pedir, permita **instalar apps de fontes desconhecidas**.

## Revelador no iPhone

1. Abra **https://mihawkrj.github.io/Personal-Apps/revelador/** no **Safari**.
2. Toque em **Compartilhar** → **Adicionar à Tela de Início**.
3. Pronto: abre em tela cheia, com ícone próprio, e funciona offline. Para salvar a foto editada, use **Salvar** → **Salvar no Fotos**.

O site é publicado automaticamente pelo GitHub Actions (`.github/workflows/pages.yml`) a cada alteração na pasta `revelador/`.

## Compilar você mesmo

Abra a pasta do app (ex.: `LeitorEInk/`) no **Android Studio** e clique em **Run ▶**, ou rode `gradlew assembleDebug` dentro dela.

## Como as versões são publicadas

A cada alteração enviada para a `main`, o GitHub Actions (`.github/workflows/release.yml`) compila o app. Se o `versionName` do `app/build.gradle.kts` ainda não tiver uma Release, ele cria uma com o nome `leitor-eink-vX.Y` e o APK anexado. Para lançar uma versão nova, basta aumentar `versionName` e `versionCode`.

## Licença

[MIT](LICENSE) © 2026 Widney Silva. Você pode usar, copiar, modificar e distribuir, desde que mantenha o aviso de copyright.
