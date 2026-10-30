# Game Ads

Aplicativo Android dedicado exclusivamente à exibição de anúncios de jogos da Google Play Store. Ao abrir o aplicativo, um anúncio é exibido imediatamente. Ao tocar no grande botão central, inicia-se uma sequência contínua de anúncios de jogos, escolhidos de forma aleatória.

## Funcionalidades

- Tela cheia (modo imersivo, sem barras de sistema).
- Um anúncio é exibido automaticamente assim que o aplicativo é aberto.
- Botão central "VER ANÚNCIO": inicia a sequência de anúncios aleatórios.
- Sequência contínua: quando um anúncio é fechado (ou o vídeo termina), o próximo anúncio é aberto automaticamente após uma contagem de 4 segundos.
- Botão "SAIR DOS ANÚNCIOS" disponível entre um anúncio e outro para encerrar a sequência.
- Menu de categorias (botão "CATEGORIAS"): "Todos os anúncios", "Vídeos com gameplay" (anúncios em vídeo que normalmente mostram trechos reais da jogabilidade) e "Intersticiais (pop-up)".
- Banner fixo na parte inferior, que se atualiza automaticamente.
- Pop-ups automáticos (intersticiais) a cada 2 minutos, com interruptor para ativar e desativar.
- Bloqueio de conteúdo adulto e de jogos de azar (veja a seção "Bloqueio de conteúdo").

## Como compilar

1. Instale o Android Studio (com JDK 17) em https://developer.android.com/studio
2. Abra esta pasta como projeto (File > Open).
3. Se o Android Studio avisar sobre o Gradle wrapper, aceite a correção sugerida.
4. Aguarde a sincronização das dependências. O SDK de anúncios do Google é baixado automaticamente por meio da dependência play-services-ads.
5. Para gerar o APK: Build > Build App Bundle(s)/APK(s) > Build APK(s).
6. Transfira o APK para o celular e instale (será necessário permitir a instalação de aplicativos de fontes desconhecidas).

Requisitos: Android 8.0 (API 26) ou superior.

## Como os anúncios são exibidos (chave de API)

O Android não possui um "aplicativo de sistema de anúncios" que possa ser copiado para o projeto. O mecanismo oficial do Google é o SDK do Google Mobile Ads (AdMob), que já está incluído no projeto por meio da dependência "play-services-ads" no arquivo app/build.gradle. A identificação do aplicativo perante o AdMob é feita pelo "ID do aplicativo" no AndroidManifest.xml, e cada espaço de anúncio usa um "ID de unidade de anúncios".

O projeto já vem configurado com os IDs de TESTE oficiais do Google, que exibem anúncios de demonstração reais (marcados com "Test Ad"). Com eles, o aplicativo funciona imediatamente, sem necessidade de nenhuma conta.

### Para usar suas próprias chaves (IDs) do AdMob

1. Crie uma conta gratuita em https://admob.google.com
2. Em "Aplicativos", adicione um aplicativo Android e anote o ID do aplicativo.
3. Crie unidades de anúncio: "Banner", "Intersticial" e "Recompensado" (vídeo).
4. Substitua os IDs nos três arquivos abaixo:

| Arquivo | O que substituir |
| --- | --- |
| app/src/main/AndroidManifest.xml | ID do aplicativo (formato ca-app-pub-...~...) |
| app/src/main/java/com/gameads/app/AdConfig.kt | ID do intersticial e do recompensado |
| app/src/main/res/layout/activity_main.xml | ID do banner |

## Bloqueio de conteúdo

- O código já define a classificação máxima de conteúdo como "G" (público geral) no arquivo GameAdsApp.kt. Isso bloqueia anúncios impróprios, incluindo a maioria dos anúncios de jogos de azar e apostas.
- No console do AdMob, em "Controles de bloqueio", é possível bloquear as categorias de apostas e jogos de azar para garantir o bloqueio total.
- Observação: o AdMob não possui um filtro exato de "somente jogos"; não é possível impedir por código que apareça ocasionalmente algum anúncio de aplicativo. No console do AdMob, a categoria de anúncios de aplicativos também pode ser restringida.

## Sobre as categorias de anúncios

Nenhuma rede de anúncios (inclusive o AdMob) permite escolher entre "anúncios enganosos" e "anúncios verdadeiros": as políticas do Google proíbem anúncios enganosos, portanto a rede não os fornece intencionalmente, e não existe esse tipo de filtro. As categorias deste aplicativo são, portanto, baseadas no formato do anúncio:

- "Vídeos com gameplay": anúncios em vídeo (recompensados), que normalmente mostram trechos reais da jogabilidade dos jogos.
- "Intersticiais (pop-up)": anúncios de tela cheia, estáticos ou em vídeo.
- "Todos os anúncios": alterna aleatoriamente entre os dois formatos.

## Ícone

Ícone adaptativo vetorial: fundo vermelho e título "GAME ADS" em amarelo, com um pequeno botão de play, em estilo retrô de jogos (fonte de pixels). Arquivos: app/src/main/res/drawable/ic_launcher_foreground.xml e app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml.

## Avisos

- Os IDs de teste exibem anúncios de demonstração e não geram receita. Para gerar receita, é obrigatório usar seus próprios IDs do AdMob.
- A Google Play Store pode rejeitar aplicativos cuja única função é exibir anúncios (política de "funcionalidade mínima"). O aplicativo é indicado para uso próprio, instalando o APK diretamente no aparelho.

## Estrutura do projeto

```
GameAds/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/gameads/app/
│       │   ├── AdConfig.kt        (IDs das unidades de anúncio)
│       │   ├── GameAdsApp.kt      (inicialização e bloqueio de conteúdo "G")
│       │   └── MainActivity.kt    (tela principal, loop de anúncios e menu)
│       └── res/
│           ├── drawable/          (ícone vetorial e fundos dos botões)
│           ├── layout/            (tela principal)
│           ├── mipmap-anydpi-v26/ (ícone adaptativo)
│           └── values/            (cores, textos e tema)
├── build.gradle
├── settings.gradle
└── gradle.properties
```
