# Personal Apps

Apps Android de código aberto feitos por **Widney Silva**. Pode baixar, usar, estudar e adaptar à vontade (Licença MIT).

| App | Pasta | O que faz | Baixar |
|---|---|---|---|
| **Leitor E-Ink** | [`LeitorEInk/`](LeitorEInk/) | Leitor de livros estilo Kindle (PDF, EPUB e TXT): tema "papel" claro ou escuro, letra ajustável, modo texto do PDF e virar página com o dedo. | [Releases](../../releases) |

## Instalar no celular

1. Abra **[Releases](../../releases)** e baixe o `.apk` mais recente do app.
2. Abra o arquivo no celular. Se o Android pedir, permita **instalar apps de fontes desconhecidas**.

## Compilar você mesmo

Abra a pasta do app (ex.: `LeitorEInk/`) no **Android Studio** e clique em **Run ▶**, ou rode `gradlew assembleDebug` dentro dela.

## Como as versões são publicadas

A cada alteração enviada para a `main`, o GitHub Actions (`.github/workflows/release.yml`) compila o app. Se o `versionName` do `app/build.gradle.kts` ainda não tiver uma Release, ele cria uma com o nome `leitor-eink-vX.Y` e o APK anexado. Para lançar uma versão nova, basta aumentar `versionName` e `versionCode`.

## Licença

[MIT](LICENSE) © 2026 Widney Silva. Você pode usar, copiar, modificar e distribuir, desde que mantenha o aviso de copyright.
