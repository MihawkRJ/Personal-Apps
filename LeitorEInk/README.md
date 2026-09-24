# Leitor E-Ink

App de leitura para Android, no estilo Kindle: leve, simples, com tema "papel"
imitando uma tela de tinta eletrônica, que reconhece pastas e arquivos de
livro (PDF, EPUB e TXT) no seu celular.

## O que ele faz

- **Biblioteca**: você adiciona uma ou mais pastas (ex: a pasta onde estão
  seus e-books) e o app varre tudo recursivamente (inclusive subpastas) e lista os PDFs/EPUBs/TXTs encontrados. Também dá para adicionar
  um arquivo avulso, sem precisar que ele esteja em uma pasta monitorada.
- **Leitura de PDF**: renderiza cada página com a API nativa do Android
  (`PdfRenderer`, sem bibliotecas externas) e aplica um filtro de tons de
  cinza com contraste realçado para lembrar uma tela e-ink.
- **Leitura de EPUB**: um parser de EPUB escrito do zero (extrai o .zip,
  lê o `container.xml` e o `.opf` para montar a ordem dos capítulos) exibe
  cada capítulo numa WebView, com um CSS injetado que força fundo "papel",
  fonte serifada e imagens em tons de cinza — não usa nenhuma biblioteca de
  terceiros.
- **Leitura de TXT**: carrega o texto e divide em "páginas" (cortando em
  espaços, nunca no meio de uma palavra), com botões A-/A+ para ajustar o
  tamanho da fonte.
- **Continua de onde parou**: a página/capítulo atual de cada livro é salva
  automaticamente (banco local Room) e retomada da próxima vez que você abrir
  o livro.
- **Tema e-ink**: fundo bege/branco "papel", texto quase preto, tipografia
  serifada, sem cores vivas.
- **Tema claro ou escuro**: menu **⋮** → **Tema (claro/escuro)** na estante —
  você escolhe entre "papel" (claro) e um modo noturno (fundo quase preto,
  texto claro), independente do modo escuro do sistema — quem decide é você,
  dentro do app, e a escolha vale tanto para a estante quanto para a leitura
  (PDF, EPUB e TXT).
- **Aumentar/diminuir o texto**: os botões **A-**/**A+** no topo da tela de
  leitura funcionam nos três formatos, e o tamanho escolhido fica salvo para
  os próximos livros. Útil para quem tem dificuldade para ler letras pequenas.
- **PDF em "modo Texto" (o texto se ajusta sozinho)**: no PDF existe um botão
  **Texto / Página** no topo. No **modo Texto**, o app extrai o texto do PDF e
  mostra como texto corrido: ao aumentar a letra, as linhas se reorganizam
  sozinhas e nada fica para fora da tela (é o modo indicado para quem quer
  letra grande). No **modo Página**, você vê a página original do PDF como
  imagem, com as margens em branco recortadas automaticamente (o texto já
  aparece maior) e zoom pelos botões A-/A+ quando quiser ver um detalhe.
  Obs.: livros digitalizados (páginas que são só imagem) não têm texto para
  extrair — nesses o app avisa e continua no modo Página.
- **Virar página com o dedo**: arraste o dedo para a esquerda/direita na
  área de leitura para passar para a próxima/página anterior (ou capítulo,
  no EPUB), com uma pequena animação de deslize lembrando virar a página de
  um livro — além dos botões **‹ ›** e do toque nas bordas (só no PDF), que
  continuam funcionando. No PDF, o arrasto vira página só quando a página
  não está ampliada (com zoom, o arrasto serve para navegar dentro da
  página).

## Como abrir e rodar

1. Instale o **Android Studio** (versão Koala/2024.1 ou mais recente,
   recomendado — ele já vem com o JDK 17 necessário).
2. Abra o Android Studio → **Open** → selecione a pasta `LeitorEInk` (a pasta
   que contém este README e o arquivo `settings.gradle.kts`).
3. Aguarde o **Gradle Sync** (a primeira vez demora um pouco: o Android
   Studio baixa sozinho o Gradle, o Android SDK das versões necessárias e
   todas as bibliotecas do projeto). Se pedir para instalar algum componente
   do SDK (ex: "Android SDK Platform 34"), aceite.
4. Conecte seu celular Android por USB com a **depuração USB** ativada
   (Configurações → Sobre o telefone → toque 7x em "Número da versão" para
   ativar Opções do desenvolvedor → ative "Depuração USB"), ou use um
   emulador.
5. Clique no botão verde ▶ **Run 'app'** no Android Studio.

O app vai instalar como "Leitor E-Ink" no celular.

### Gerar um APK para instalar manualmente (sem cabo)

No Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**. O
arquivo `.apk` gerado fica em `app/build/outputs/apk/debug/app-debug.apk` —
transfira esse arquivo para o celular (por cabo, Telegram, etc.) e instale
diretamente (talvez seja necessário permitir "instalar de fontes
desconhecidas" nas configurações do Android).

## Como usar

1. Ao abrir o app pela primeira vez, toque no menu **⋮** no canto superior
   direito → **Adicionar pasta** → escolha a pasta onde estão seus livros
   (ex: a pasta de e-books que você organizou). O Android vai pedir
   permissão — aceite.
2. O app varre a pasta (e subpastas) e mostra os livros encontrados na
   estante.
3. Toque em um livro para abrir. Use os botões **‹ ›** embaixo para passar
   de página/capítulo (no PDF, também dá para tocar nas bordas esquerda/
   direita da tela). Em EPUB e TXT, os botões **A-**/**A+** no topo ajustam
   o tamanho da fonte.
4. Menu **⋮** → **Atualizar biblioteca**: reescaneia as pastas monitoradas
   (pega livros novos e remove da lista os que você apagou do celular).
5. Menu **⋮** → **Gerenciar pastas**: veja quais pastas estão sendo
   monitoradas e remova alguma se quiser.

O app nunca copia nem move os arquivos originais — ele só guarda a
"permissão de leitura" para cada pasta/arquivo escolhido (é assim que o
Android moderno funciona: Storage Access Framework).

## Estrutura do projeto

```
LeitorEInk/
├── app/src/main/java/com/widney/leitoreink/
│   ├── data/       → banco de dados local (Room): Book, ScannedFolder, DAOs
│   ├── scan/        → varredura de pastas/arquivos (SAF)
│   ├── reader/      → os três "motores" de leitura: PDF, EPUB, TXT
│   ├── ui/          → as duas telas: MainActivity (estante) e ReaderActivity (leitor)
│   └── util/        → filtro de cor "e-ink" para o PDF
└── app/src/main/res/ → layouts, cores, textos, ícones
```

## Limitações conhecidas (é um MVP simples, de propósito)

- PDF: não tem zoom/pinça ainda, a página é sempre ajustada à largura da
  tela.
- EPUB: capítulos são navegados inteiros (não corta "página" dentro de um
  capítulo longo — você rola com o dedo dentro do capítulo).
- TXT: a paginação é por quantidade de caracteres, não por quanto cabe
  exatamente na tela (é uma aproximação simples, de propósito).
- Sem suporte a MOBI/AZW (formato proprietário da Amazon, mais complexo).
- Sem estante organizada por categoria/pasta na tela principal — por
  enquanto é uma lista única, ordenada por "lido recentemente".

Todas essas são melhorias possíveis de pedir depois, se quiser evoluir o
app — a base (banco de dados, leitores por formato, tema) já está pronta
para isso.

## Baixar

O APK pronto de cada versão fica em **[Releases](../../releases)**.

## Sobre o código

Nenhuma biblioteca de leitura de e-book de terceiros foi usada (nem para
PDF nem para EPUB) — de propósito, para manter o app leve e o `.apk`
pequeno. PDF usa a API `android.graphics.pdf.PdfRenderer` (nativa do
Android); EPUB usa um parser próprio, bem pequeno, escrito só com
`java.util.zip` e `XmlPullParser` (também nativos).

A única biblioteca externa é a [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android)
(Licença Apache 2.0), usada só para extrair o texto dos PDFs no modo Texto.

## Licença

[MIT](../LICENSE) © 2026 Widney Silva.
