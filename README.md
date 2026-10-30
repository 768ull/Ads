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
- Botão "DIAGNÓSTICO": mostra na tela o estado da conexão de internet, do Google Play Services e os códigos de erro reais do carregamento dos anúncios.
- Detecção automática de falhas de rede e de erro interno, com orientação na própria tela.
- Bloqueio de conteúdo adulto e de jogos de azar (veja a seção "Bloqueio de conteúdo").

## Gerar o instalador (APK) automaticamente pelo GitHub

O repositório inclui uma GitHub Action (arquivo .github/workflows/android-apk.yml) que compila o APK automaticamente nos servidores do GitHub. Não é preciso instalar nada no computador.

1. Abra o repositório no GitHub e acesse a aba Actions.
2. A compilação inicia sozinha a cada novo envio para a branch main. Para executar manualmente, selecione o fluxo de trabalho "Construir APK do Game Ads" e clique em "Run workflow".
3. Aguarde a conclusão (normalmente de 5 a 10 minutos no primeiro envio).
4. Clique na execução concluída e baixe o artefato "GameAds-APK".
5. O arquivo baixado é um ZIP; extraia o APK (app-debug.apk) e instale-o no celular (será necessário permitir a instalação de aplicativos de fontes desconhecidas).

O APK gerado é a variante debug, que já vem assinada e pode ser instalada diretamente em qualquer Android 8.0 ou superior.

## Como compilar no computador (opcional)

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

## Solução de problemas: os anúncios não aparecem

O botão "DIAGNÓSTICO" do aplicativo mostra na tela o estado da conexão de internet, do Google Play Services e os códigos de erro reais do carregamento dos anúncios. Ele é a primeira ferramenta para descobrir o motivo. As causas mais comuns são:

1. Google Play Services ausente ou desatualizado: é o componente do Android responsável por exibir anúncios, e sem ele nenhum anúncio carrega em aplicativo algum. Verifique em Configurações > Aplicativos e atualize-o na Google Play Store.
2. Aplicativo bloqueador de anúncios ou VPN ativo (AdGuard, Blokada e semelhantes): desativar o DNS privado não desativa esses aplicativos. Encerre-os para testar.
3. DNS privado bloqueando anúncios (veja a seção abaixo).
4. Data e hora incorretas no celular: impedem as conexões seguras dos servidores de anúncios.
5. Restrição de dados por aplicativo (comum em celulares Xiaomi, Redmi e POCO): em Configurações > Aplicativos > Game Ads, permita o uso de Wi-Fi e de dados móveis.

Códigos de erro do AdMob exibidos no diagnóstico:

| Código | Significado | Causa provável |
| --- | --- | --- |
| 0 | Erro interno | Google Play Services ausente ou desatualizado |
| 1 | Solicitação inválida | Problema de configuração dos IDs (não é o caso dos IDs de teste) |
| 2 | Erro de rede | Bloqueio de DNS, VPN bloqueadora ou restrição de dados do aplicativo |
| 3 | Sem preenchimento | Nenhum anúncio disponível no momento (normal e temporário) |

## DNS privado e bloqueadores de anúncios do sistema

O recurso "DNS privado" do Android (Configurações > Rede e Internet > DNS privado), quando configurado com um serviço que bloqueia anúncios (como AdGuard ou NextDNS), impede que os anúncios carreguem em qualquer aplicativo. Esse bloqueio é aplicado pelo próprio sistema operacional a todas as conexões e nenhum aplicativo pode desativá-lo ou contorná-lo; não é possível "bloquear o DNS privado por dentro do aplicativo".

Para que os anúncios apareçam sem desativar o DNS privado, utilize uma das opções:

1. Permita os domínios de anúncios do Google no painel do seu serviço de DNS (por exemplo, no painel da NextDNS ou do AdGuard): doubleclick.net, pagead2.googleapis.com, googleadservices.com e adservice.google.com.
2. Troque o hostname do DNS privado por um que não bloqueia anúncios, por exemplo dns.google (Configurações > Rede e Internet > DNS privado > Nome do host do provedor).
3. Ou defina o DNS privado como "Automático" ou "Desativado".

O aplicativo detecta automaticamente quando os anúncios não carregam por provável bloqueio de DNS e exibe na própria tela um aviso com essas instruções.

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
├── .github/
│   └── workflows/
│       └── android-apk.yml    (compilação automática do APK no GitHub)
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/gameads/app/
│       │   ├── AdConfig.kt        (IDs das unidades de anúncio)
│       │   ├── GameAdsApp.kt      (inicialização e bloqueio de conteúdo "G")
│       │   └── MainActivity.kt    (tela principal, loop de anúncios e diagnóstico)
│       └── res/
│           ├── drawable/          (ícone vetorial e fundos dos botões)
│           ├── layout/            (tela principal)
│           ├── mipmap-anydpi-v26/ (ícone adaptativo)
│           └── values/            (cores, textos e tema)
├── build.gradle
├── settings.gradle
└── gradle.properties
```
