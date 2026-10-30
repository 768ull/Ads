package com.gameads.app

object AdConfig {
    // =========================================================================
    // CHAVES DE ANÚNCIO (IDs de unidade de anúncio do AdMob)
    // -------------------------------------------------------------------------
    // IMPORTANTE: os IDs abaixo são os IDs DE TESTE oficiais do Google.
    // Eles exibem anúncios de demonstração reais, marcados como "Test Ad".
    //
    // Para usar os SEUS PRÓPRIOS anúncios:
    //   1. Crie uma conta gratuita em https://admob.google.com
    //   2. Adicione o aplicativo e crie unidades de anúncio:
    //      Banner, Intersticial e "Recompensado" (vídeo)
    //   3. Substitua os IDs abaixo pelos seus (começam com ca-app-pub-...)
    //   4. Substitua também:
    //      - O ID do aplicativo no arquivo AndroidManifest.xml
    //      - O ID do banner no arquivo res/layout/activity_main.xml
    // =========================================================================

    // Usado no layout res/layout/activity_main.xml (mantido aqui como referência)
    const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"

    // Anúncio de tela cheia (pop-up), usado no loop e nos pop-ups automáticos
    const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    // Anúncio em vídeo com gameplay (formato "recompensado")
    const val REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
}
