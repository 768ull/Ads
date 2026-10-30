package com.gameads.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

/**
 * Inicialização do SDK de anúncios do Google (AdMob).
 *
 * A classificação máxima de conteúdo é definida como "G" (público geral),
 * o que bloqueia anúncios impróprios, incluindo a maioria dos anúncios
 * de jogos de azar e apostas.
 */
class GameAdsApp : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build()
        )

        MobileAds.initialize(this)
    }
}
