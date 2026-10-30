package com.gameads.app

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlin.random.Random

/**
 * Tela principal do Game Ads.
 *
 * Fluxo do aplicativo:
 * - Ao abrir, o primeiro intersticial carregado é exibido automaticamente.
 * - O botão central inicia um loop de anúncios aleatórios de jogos.
 * - Cada anúncio fechado (ou concluído) abre automaticamente o próximo,
 *   com uma contagem entre um anúncio e outro e um botão para sair do loop.
 * - O menu de categorias troca o formato dos anúncios exibidos.
 */
class MainActivity : AppCompatActivity() {

    private enum class AdCategory(val label: String) {
        ALL("Todos os anúncios"),
        VIDEO_GAMEPLAY("Vídeos com gameplay"),
        INTERSTITIALS("Intersticiais (pop-up)")
    }

    companion object {
        private const val INTERVALO_POPUP_MS = 120_000L
        private const val TEMPO_ENTRE_ANUNCIOS_MS = 4_000L
    }

    private lateinit var banner: AdView
    private lateinit var overlay: RelativeLayout
    private lateinit var txtProximo: TextView
    private lateinit var btnVerAnuncio: Button
    private lateinit var btnCategorias: Button
    private lateinit var btnSair: Button
    private lateinit var swPopups: SwitchCompat

    private var selectedCategory = AdCategory.ALL
    private var loopActive = false
    private var launchAdShown = false
    private var autoPopups = true

    private var interstitialAd: InterstitialAd? = null
    private var loadingInterstitial = false
    private var rewardedAd: RewardedAd? = null
    private var loadingRewarded = false

    private var countdownTimer: CountDownTimer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val popupHandler = Handler(Looper.getMainLooper())

    /** Tenta exibir o próximo anúncio até que algum esteja pronto. */
    private val retryRunnable = object : Runnable {
        override fun run() {
            if (!loopActive) return
            if (!tryShowAd()) {
                txtProximo.setText(R.string.carregando)
                mainHandler.postDelayed(this, 1500)
            }
        }
    }

    /** Pop-ups automáticos periódicos. */
    private val popupRunnable = object : Runnable {
        override fun run() {
            if (autoPopups && !loopActive) mostrarIntersticial()
            popupHandler.postDelayed(this, INTERVALO_POPUP_MS)
        }
    }

    private val interstitialCallback = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() {
            interstitialAd = null
            loadInterstitial()
            anuncioFechado()
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
            interstitialAd = null
            loadInterstitial()
            anuncioFechado()
        }
    }

    private val rewardedCallback = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() {
            rewardedAd = null
            loadRewarded()
            anuncioFechado()
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
            rewardedAd = null
            loadRewarded()
            anuncioFechado()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        banner = findViewById(R.id.banner)
        overlay = findViewById(R.id.overlay)
        txtProximo = findViewById(R.id.txtProximo)
        btnVerAnuncio = findViewById(R.id.btnVerAnuncio)
        btnCategorias = findViewById(R.id.btnCategorias)
        btnSair = findViewById(R.id.btnSair)
        swPopups = findViewById(R.id.swPopups)

        btnVerAnuncio.setOnClickListener { iniciarLoopDeAnuncios() }
        btnCategorias.setOnClickListener { mostrarDialogoDeCategorias() }
        btnSair.setOnClickListener { pararLoopDeAnuncios() }
        swPopups.setOnCheckedChangeListener { _, isChecked -> autoPopups = isChecked }

        atualizarBotaoCategorias()
        loadInterstitial()
        loadRewarded()
        banner.loadAd(AdRequest.Builder().build())
    }

    // ---------------------------------------------------------------------
    // Loop de anúncios
    // ---------------------------------------------------------------------

    private fun iniciarLoopDeAnuncios() {
        if (loopActive) return
        loopActive = true
        iniciarContagemParaProximo()
    }

    private fun pararLoopDeAnuncios() {
        loopActive = false
        countdownTimer?.cancel()
        mainHandler.removeCallbacks(retryRunnable)
        overlay.visibility = View.GONE
    }

    private fun iniciarContagemParaProximo() {
        overlay.visibility = View.VISIBLE
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(TEMPO_ENTRE_ANUNCIOS_MS, 250) {
            override fun onTick(millisUntilFinished: Long) {
                val segundos = ((millisUntilFinished + 999) / 1000).toInt()
                txtProximo.text = getString(R.string.proximo_anuncio, segundos)
            }

            override fun onFinish() {
                if (!tryShowAd()) {
                    txtProximo.setText(R.string.carregando)
                    mainHandler.postDelayed(retryRunnable, 1500)
                }
            }
        }.start()
    }

    private fun anuncioFechado() {
        if (loopActive) iniciarContagemParaProximo()
    }

    private fun tryShowAd(): Boolean {
        return when (selectedCategory) {
            AdCategory.ALL ->
                if (Random.nextBoolean()) mostrarRewarded() || mostrarIntersticial()
                else mostrarIntersticial() || mostrarRewarded()
            AdCategory.VIDEO_GAMEPLAY -> mostrarRewarded() || mostrarIntersticial()
            AdCategory.INTERSTITIALS -> mostrarIntersticial() || mostrarRewarded()
        }
    }

    private fun mostrarIntersticial(): Boolean {
        val ad = interstitialAd ?: return false
        interstitialAd = null
        ad.show(this)
        return true
    }

    private fun mostrarRewarded(): Boolean {
        val ad = rewardedAd ?: return false
        rewardedAd = null
        ad.show(this) { }
        return true
    }

    // ---------------------------------------------------------------------
    // Carregamento dos anúncios
    // ---------------------------------------------------------------------

    private fun loadInterstitial() {
        if (interstitialAd != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(
            this,
            AdConfig.INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingInterstitial = false
                    ad.fullScreenContentCallback = interstitialCallback
                    interstitialAd = ad
                    // Exibe um anúncio assim que o aplicativo é aberto.
                    if (!launchAdShown) {
                        launchAdShown = true
                        interstitialAd = null
                        ad.show(this@MainActivity)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingInterstitial = false
                    interstitialAd = null
                }
            }
        )
    }

    private fun loadRewarded() {
        if (rewardedAd != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(
            this,
            AdConfig.REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingRewarded = false
                    ad.fullScreenContentCallback = rewardedCallback
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingRewarded = false
                    rewardedAd = null
                }
            }
        )
    }

    // ---------------------------------------------------------------------
    // Menu de categorias
    // ---------------------------------------------------------------------

    private fun mostrarDialogoDeCategorias() {
        val labels = AdCategory.values().map { it.label }.toTypedArray()
        val selecionado = AdCategory.values().indexOf(selectedCategory)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.categoria_titulo))
            .setSingleChoiceItems(labels, selecionado) { dialog, which ->
                selectedCategory = AdCategory.values()[which]
                atualizarBotaoCategorias()
                dialog.dismiss()
            }
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun atualizarBotaoCategorias() {
        btnCategorias.text = getString(R.string.btn_categorias) + "\n" + selectedCategory.label
    }

    // ---------------------------------------------------------------------
    // Ciclo de vida e tela cheia
    // ---------------------------------------------------------------------

    override fun onResume() {
        super.onResume()
        popupHandler.postDelayed(popupRunnable, INTERVALO_POPUP_MS)
    }

    override fun onPause() {
        super.onPause()
        popupHandler.removeCallbacks(popupRunnable)
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        mainHandler.removeCallbacks(retryRunnable)
        popupHandler.removeCallbacks(popupRunnable)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) esconderBarrasDoSistema()
    }

    private fun esconderBarrasDoSistema() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
