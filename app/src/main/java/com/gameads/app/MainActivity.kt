package com.gameads.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
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
 * - O botão DIAGNÓSTICO mostra o estado da conexão, do Google Play Services
 *   e os códigos de erro reais do carregamento dos anúncios.
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
    private lateinit var btnDiagnostico: Button
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

    private var falhasDeRede = 0
    private var falhasInternas = 0
    private var avisoDnsExibido = false
    private var avisoGmsExibido = false
    private var ultimoErroInterstitial = ""
    private var ultimoErroRecompensado = ""

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
        btnDiagnostico = findViewById(R.id.btnDiagnostico)
        btnSair = findViewById(R.id.btnSair)
        swPopups = findViewById(R.id.swPopups)

        btnVerAnuncio.setOnClickListener { iniciarLoopDeAnuncios() }
        btnCategorias.setOnClickListener { mostrarDialogoDeCategorias() }
        btnDiagnostico.setOnClickListener { mostrarDiagnostico() }
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
    // Carregamento dos anúncios e falhas
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
                    tratarFalhaDeCarregamento("Intersticial", error)
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
                    tratarFalhaDeCarregamento("Recompensado", error)
                }
            }
        )
    }

    /**
     * Registra a falha e orienta conforme o tipo de erro:
     * - código 2 (rede): provável bloqueio de DNS privado, VPN bloqueadora
     *   de anúncios ou restrição de dados do aplicativo;
     * - código 0 (erro interno): provável problema com o Google Play Services.
     * Em ambos os casos o bloqueio é aplicado fora do aplicativo; nenhum
     * aplicativo pode desativá-lo, por isso o aviso apenas orienta o usuário.
     */
    private fun tratarFalhaDeCarregamento(formato: String, error: LoadAdError) {
        val detalhe = formato + ": código " + error.code + " (" + error.message + ")"
        if (formato == "Intersticial") ultimoErroInterstitial = detalhe
        else ultimoErroRecompensado = detalhe

        when (error.code) {
            0 -> {
                falhasInternas++
                if (falhasInternas >= 2 && !avisoGmsExibido) {
                    avisoGmsExibido = true
                    mostrarAvisoGms()
                }
            }
            2 -> {
                falhasDeRede++
                if (falhasDeRede >= 3 && !avisoDnsExibido) {
                    avisoDnsExibido = true
                    mostrarAvisoDeDns()
                }
            }
        }
    }

    private fun mostrarAvisoDeDns() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dns_titulo))
            .setMessage(getString(R.string.dns_mensagem))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun mostrarAvisoGms() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.gms_titulo))
            .setMessage(getString(R.string.gms_mensagem))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    // ---------------------------------------------------------------------
    // Diagnóstico
    // ---------------------------------------------------------------------

    private fun mostrarDiagnostico() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val statusInternet =
            if (caps != null &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ) getString(R.string.diag_ok)
            else getString(R.string.diag_sem_conexao)

        val statusGms = when (GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(this)) {
            ConnectionResult.SUCCESS -> getString(R.string.diag_ok)
            ConnectionResult.SERVICE_MISSING -> getString(R.string.gms_ausente)
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> getString(R.string.gms_desatualizado)
            ConnectionResult.SERVICE_DISABLED -> getString(R.string.gms_desativado)
            else -> getString(R.string.gms_invalido)
        }

        val erros = mutableListOf<String>()
        if (ultimoErroInterstitial.isNotEmpty()) erros.add(ultimoErroInterstitial)
        if (ultimoErroRecompensado.isNotEmpty()) erros.add(ultimoErroRecompensado)
        val textoErros =
            if (erros.isEmpty()) getString(R.string.diag_nenhum_erro)
            else erros.joinToString("\n")

        val msg = getString(R.string.diag_internet, statusInternet) + "\n\n" +
            getString(R.string.diag_gms, statusGms) + "\n\n" +
            getString(R.string.diag_erros, textoErros) + "\n\n" +
            getString(R.string.diag_dica)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.diag_titulo))
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok, null)
            .show()
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
