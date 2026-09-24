/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.widney.leitoreink.EInkReaderApp
import com.widney.leitoreink.R
import com.widney.leitoreink.data.Book
import com.widney.leitoreink.data.BookFormat
import com.widney.leitoreink.data.LibraryRepository
import com.widney.leitoreink.databinding.ActivityReaderBinding
import com.widney.leitoreink.reader.EpubBookReader
import com.widney.leitoreink.reader.EpubEInkScript
import com.widney.leitoreink.reader.PdfBookReader
import com.widney.leitoreink.reader.PdfTextExtractor
import com.widney.leitoreink.reader.TxtBookReader
import com.widney.leitoreink.util.EInkColorMatrix
import com.widney.leitoreink.util.ReaderPrefs
import com.widney.leitoreink.util.ThemePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Uma única Activity de leitura para os três formatos. A UI (barra superior/inferior)
 * é sempre a mesma; só a área de conteúdo no meio muda de acordo com o formato do livro.
 */
class ReaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReaderBinding
    private lateinit var repository: LibraryRepository

    private var book: Book? = null
    private var currentIndex: Int = 0

    private var pdfReader: PdfBookReader? = null
    private var epubReader: EpubBookReader? = null
    private var txtReader: TxtBookReader? = null

    private var epubFontPercent: Int = 100
    private var txtFontSizeSp: Float = 17f

    /** Zoom da página de PDF (modo página), em %. 100 = página inteira na tela. */
    private var pdfZoomPercent: Int = 100

    /** Modo texto do PDF: exibe o texto extraído, que refluxa conforme a fonte. */
    private var pdfTextMode: Boolean = false
    private var pdfTextExtractor: PdfTextExtractor? = null
    private var pdfTextFontSizeSp: Float = 18f

    private var isTurningPage: Boolean = false

    /**
     * Contador de "geração" da página: se o usuário virar a página antes de a
     * anterior terminar de carregar, o resultado antigo é descartado em vez de
     * aparecer na tela por engano.
     */
    private var renderGeneration: Int = 0

    /** PdfRenderer só aceita uma página aberta por vez: serializa as renderizações. */
    private val pdfRenderMutex = Mutex()
    private val cropCache = HashMap<Int, PdfBookReader.CropBox?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as EInkReaderApp
        repository = LibraryRepository(this, app.database)

        epubFontPercent = ReaderPrefs.epubFontPercent(this)
        txtFontSizeSp = ReaderPrefs.txtFontSp(this)
        pdfTextFontSizeSp = ReaderPrefs.pdfTextFontSp(this)
        pdfTextMode = ReaderPrefs.isPdfTextMode(this)

        val bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L)
        if (bookId == -1L) {
            finish()
            return
        }

        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonPrev.setOnClickListener { attemptPrev() }
        binding.buttonNext.setOnClickListener { attemptNext() }
        binding.buttonFontDecrease.setOnClickListener { changeFontSize(-1) }
        binding.buttonFontIncrease.setOnClickListener { changeFontSize(1) }
        binding.buttonToggleMode.setOnClickListener { setPdfTextMode(!pdfTextMode, fromUser = true) }

        // Virar a página arrastando o dedo, como num livro. Desligado só quando o
        // PDF está com zoom no modo página (aí o arrasto serve para navegar dentro
        // da página ampliada).
        binding.contentContainer.onSwipeLeft = { attemptNext() }
        binding.contentContainer.onSwipeRight = { attemptPrev() }
        binding.contentContainer.swipeEnabled = {
            book?.format != BookFormat.PDF || pdfTextMode || pdfZoomPercent == 100
        }

        loadBook(bookId)
    }

    private fun loadBook(bookId: Long) {
        showLoading(true)
        lifecycleScope.launch {
            val loadedBook = repository.getBook(bookId)
            if (loadedBook == null) {
                showError()
                return@launch
            }
            book = loadedBook
            currentIndex = loadedBook.lastPositionIndex
            binding.textReaderTitle.text = loadedBook.displayName

            val opened = withContext(Dispatchers.IO) { openReaderFor(loadedBook) }
            showLoading(false)
            if (!opened) {
                showError()
                return@launch
            }

            when (loadedBook.format) {
                BookFormat.PDF -> setupPdfUi()
                BookFormat.EPUB -> setupEpubUi()
                BookFormat.TXT -> setupTxtUi()
            }
            goTo(currentIndex)
        }
    }

    private fun openReaderFor(b: Book): Boolean {
        val uri = Uri.parse(b.uriString)
        return when (b.format) {
            BookFormat.PDF -> {
                val reader = PdfBookReader(this, uri)
                pdfReader = reader
                reader.isValid && reader.pageCount > 0
            }
            BookFormat.EPUB -> {
                val reader = EpubBookReader.open(this, uri, b.id)
                epubReader = reader
                reader != null && reader.chapterCount > 0
            }
            BookFormat.TXT -> {
                val reader = TxtBookReader.open(this, uri)
                txtReader = reader
                reader != null && reader.pageCount > 0
            }
        }
    }

    private fun setupPdfUi() {
        binding.imagePage.colorFilter = EInkColorMatrix.createFilter(invert = ThemePrefs.isDark(this))
        binding.buttonFontDecrease.visibility = View.VISIBLE
        binding.buttonFontIncrease.visibility = View.VISIBLE
        binding.buttonToggleMode.visibility = View.VISIBLE
        binding.textTxtPage.textSize = pdfTextFontSizeSp
        setupTapZones(binding.imagePage)
        applyPdfModeViews()
    }

    /** Mostra a view certa (imagem da página ou texto refluído) para o modo atual. */
    private fun applyPdfModeViews() {
        binding.buttonToggleMode.text =
            getString(if (pdfTextMode) R.string.mode_page else R.string.mode_text)
        if (pdfTextMode) {
            binding.scrollPdfVertical.visibility = View.GONE
            binding.scrollTxt.visibility = View.VISIBLE
            binding.textTxtPage.textSize = pdfTextFontSizeSp
        } else {
            binding.scrollTxt.visibility = View.GONE
            binding.scrollPdfVertical.visibility = View.VISIBLE
        }
    }

    private fun setPdfTextMode(enabled: Boolean, fromUser: Boolean) {
        if (book?.format != BookFormat.PDF) return
        pdfTextMode = enabled
        ReaderPrefs.setPdfTextMode(this, enabled)
        if (!enabled) pdfZoomPercent = 100
        applyPdfModeViews()
        if (enabled && fromUser) {
            Toast.makeText(this, R.string.text_mode_hint, Toast.LENGTH_SHORT).show()
        }
        goTo(currentIndex)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupEpubUi() {
        binding.webViewEpub.visibility = View.VISIBLE
        binding.buttonFontDecrease.visibility = View.VISIBLE
        binding.buttonFontIncrease.visibility = View.VISIBLE
        binding.webViewEpub.settings.javaScriptEnabled = true
        binding.webViewEpub.settings.allowFileAccess = true
        binding.webViewEpub.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val script = EpubEInkScript.build(
                    epubFontPercent,
                    ThemePrefs.isDark(this@ReaderActivity)
                )
                view?.evaluateJavascript(script, null)
            }
        }
    }

    private fun setupTxtUi() {
        binding.scrollTxt.visibility = View.VISIBLE
        binding.buttonFontDecrease.visibility = View.VISIBLE
        binding.buttonFontIncrease.visibility = View.VISIBLE
        binding.textTxtPage.textSize = txtFontSizeSp
    }

    /**
     * Zonas de toque do PDF no modo página: tocar no terço esquerdo/direito vira a
     * página. Desligado quando há zoom, para não atrapalhar o arrasto de leitura.
     */
    private fun setupTapZones(target: View) {
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (pdfTextMode || pdfZoomPercent != 100) return false
                val width = target.width
                if (width <= 0) return false
                when {
                    e.x < width * 0.35f -> attemptPrev()
                    e.x > width * 0.65f -> attemptNext()
                }
                return true
            }
        })
        target.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
    }

    private fun totalCount(): Int = when (book?.format) {
        BookFormat.PDF -> pdfReader?.pageCount ?: 0
        BookFormat.EPUB -> epubReader?.chapterCount ?: 0
        BookFormat.TXT -> txtReader?.pageCount ?: 0
        null -> 0
    }

    private fun attemptNext() {
        if (currentIndex + 1 < totalCount()) animateAndGo(1)
    }

    private fun attemptPrev() {
        if (currentIndex - 1 >= 0) animateAndGo(-1)
    }

    private fun currentContentView(): View? = when (book?.format) {
        BookFormat.PDF -> if (pdfTextMode) binding.scrollTxt else binding.scrollPdfVertical
        BookFormat.EPUB -> binding.webViewEpub
        BookFormat.TXT -> binding.scrollTxt
        null -> null
    }

    /** Troca de página deslizando a página atual para fora e a nova para dentro. */
    private fun animateAndGo(direction: Int) {
        if (isTurningPage) return
        val contentView = currentContentView() ?: return
        isTurningPage = true

        val width = binding.contentContainer.width.toFloat().let { if (it > 0f) it else 720f }
        val outX = if (direction > 0) -width else width

        contentView.animate()
            .translationX(outX)
            .setDuration(160)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                goTo(currentIndex + direction)
                contentView.translationX = -outX
                contentView.animate()
                    .translationX(0f)
                    .setDuration(200)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        contentView.translationX = 0f
                        isTurningPage = false
                    }
                    .start()
            }
            .start()
    }

    private fun goTo(index: Int) {
        when (book?.format) {
            BookFormat.PDF -> if (pdfTextMode) showPdfTextPage(index) else showPdfPage(index)
            BookFormat.EPUB -> showEpubChapter(index)
            BookFormat.TXT -> showTxtPage(index)
            null -> {}
        }
    }

    // ---------------------------------------------------------------- PDF (página)

    private fun showPdfPage(index: Int) {
        val reader = pdfReader ?: return
        val safeIndex = index.coerceIn(0, reader.pageCount - 1)
        val baseWidth = resources.displayMetrics.widthPixels
        val containerHeight = binding.scrollPdfVertical.height.takeIf { it > 0 }
            ?: resources.displayMetrics.heightPixels
        val zoom = pdfZoomPercent

        currentIndex = safeIndex
        updateIndicator(safeIndex + 1, reader.pageCount, isChapter = false)
        persistProgress()

        val generation = ++renderGeneration
        lifecycleScope.launch {
            val bitmap = pdfRenderMutex.withLock {
                withContext(Dispatchers.Default) {
                    renderFittedPage(reader, safeIndex, baseWidth, containerHeight, zoom)
                }
            }
            if (generation != renderGeneration) return@launch
            if (bitmap != null) {
                binding.imagePage.setImageBitmap(bitmap)
                binding.scrollPdfVertical.scrollTo(0, 0)
                binding.scrollPdfHorizontal.scrollTo(0, 0)
            }
        }
    }

    /**
     * Recorta as margens em branco da página (assim o texto já sai maior sem cortar
     * nada) e ajusta para caber na tela. Com zoom (>100%) renderiza maior de
     * propósito, permitindo arrastar para ver o restante.
     */
    private fun renderFittedPage(
        reader: PdfBookReader,
        index: Int,
        baseWidth: Int,
        containerHeight: Int,
        zoomPercent: Int
    ): Bitmap? {
        val crop = cropFor(reader, index)

        if (zoomPercent > 100) {
            val zoomedWidth = (baseWidth * zoomPercent / 100f).toInt().coerceAtLeast(1)
            return reader.renderPage(index, zoomedWidth, crop)
        }

        val fitWidthBitmap = reader.renderPage(index, baseWidth, crop) ?: return null
        if (containerHeight > 0 && fitWidthBitmap.height > containerHeight) {
            val adjustedWidth = (baseWidth * (containerHeight.toFloat() / fitWidthBitmap.height))
                .toInt()
                .coerceAtLeast(1)
            return reader.renderPage(index, adjustedWidth, crop) ?: fitWidthBitmap
        }
        return fitWidthBitmap
    }

    private fun cropFor(reader: PdfBookReader, index: Int): PdfBookReader.CropBox? {
        synchronized(cropCache) {
            if (cropCache.containsKey(index)) return cropCache[index]
        }
        val crop = reader.detectContentBox(index)
        synchronized(cropCache) {
            cropCache[index] = crop
        }
        return crop
    }

    // ----------------------------------------------------------------- PDF (texto)

    private fun showPdfTextPage(index: Int) {
        val reader = pdfReader ?: return
        val safeIndex = index.coerceIn(0, reader.pageCount - 1)
        currentIndex = safeIndex
        updateIndicator(safeIndex + 1, reader.pageCount, isChapter = false)
        persistProgress()

        val uriString = book?.uriString ?: return
        val generation = ++renderGeneration

        lifecycleScope.launch {
            val existing = pdfTextExtractor
            val extractor: PdfTextExtractor
            if (existing != null) {
                extractor = existing
            } else {
                showLoading(true)
                val opened = withContext(Dispatchers.IO) {
                    PdfTextExtractor.open(this@ReaderActivity, Uri.parse(uriString))
                }
                showLoading(false)
                if (opened == null) {
                    Toast.makeText(
                        this@ReaderActivity,
                        R.string.text_mode_unavailable,
                        Toast.LENGTH_LONG
                    ).show()
                    setPdfTextMode(enabled = false, fromUser = false)
                    return@launch
                }
                pdfTextExtractor = opened
                extractor = opened
            }

            val text = withContext(Dispatchers.IO) { extractor.textForPage(safeIndex) }
            if (generation != renderGeneration) return@launch
            binding.textTxtPage.text = if (text.isNullOrBlank()) {
                getString(R.string.text_mode_empty_page)
            } else {
                text
            }
            binding.scrollTxt.scrollTo(0, 0)
        }
    }

    // ------------------------------------------------------------------ EPUB / TXT

    private fun showEpubChapter(index: Int) {
        val reader = epubReader ?: return
        val safeIndex = index.coerceIn(0, reader.chapterCount - 1)
        val file = reader.chapterFile(safeIndex) ?: return
        binding.webViewEpub.loadUrl("file://${file.absolutePath}")
        currentIndex = safeIndex
        updateIndicator(safeIndex + 1, reader.chapterCount, isChapter = true)
        persistProgress()
    }

    private fun showTxtPage(index: Int) {
        val reader = txtReader ?: return
        val safeIndex = index.coerceIn(0, reader.pageCount - 1)
        binding.textTxtPage.text = reader.page(safeIndex)
        binding.scrollTxt.scrollTo(0, 0)
        currentIndex = safeIndex
        updateIndicator(safeIndex + 1, reader.pageCount, isChapter = false)
        persistProgress()
    }

    // ------------------------------------------------------------------------ UI

    private fun updateIndicator(current: Int, total: Int, isChapter: Boolean) {
        binding.textPageIndicator.text = if (isChapter) {
            getString(R.string.chapter_indicator, current, total)
        } else {
            getString(R.string.page_indicator, current, total)
        }
        binding.buttonPrev.isEnabled = current > 1
        binding.buttonNext.isEnabled = current < total
        binding.buttonPrev.alpha = if (binding.buttonPrev.isEnabled) 1f else 0.3f
        binding.buttonNext.alpha = if (binding.buttonNext.isEnabled) 1f else 0.3f
    }

    private fun changeFontSize(direction: Int) {
        when (book?.format) {
            BookFormat.PDF -> {
                if (pdfTextMode) {
                    // Modo texto: aumenta a letra e o texto se reorganiza sozinho.
                    pdfTextFontSizeSp = (pdfTextFontSizeSp + direction * 2f).coerceIn(12f, 34f)
                    ReaderPrefs.setPdfTextFontSp(this, pdfTextFontSizeSp)
                    binding.textTxtPage.textSize = pdfTextFontSizeSp
                } else {
                    pdfZoomPercent = (pdfZoomPercent + direction * 20).coerceIn(100, 220)
                    showPdfPage(currentIndex)
                }
            }
            BookFormat.EPUB -> {
                epubFontPercent = (epubFontPercent + direction * 15).coerceIn(70, 200)
                ReaderPrefs.setEpubFontPercent(this, epubFontPercent)
                showEpubChapter(currentIndex)
            }
            BookFormat.TXT -> {
                val reader = txtReader ?: return
                txtFontSizeSp = (txtFontSizeSp + direction * 2f).coerceIn(12f, 30f)
                ReaderPrefs.setTxtFontSp(this, txtFontSizeSp)
                binding.textTxtPage.textSize = txtFontSizeSp
                val newCharsPerPage = estimateCharsPerPage(txtFontSizeSp)
                currentIndex = reader.repaginate(newCharsPerPage, currentIndex)
                showTxtPage(currentIndex)
            }
            else -> {}
        }
    }

    private fun estimateCharsPerPage(fontSp: Float): Int {
        val base = TxtBookReader.DEFAULT_CHARS_PER_PAGE
        val ratio = 17f / fontSp
        return (base * ratio).toInt().coerceAtLeast(300)
    }

    private fun persistProgress() {
        val b = book ?: return
        val indexToSave = currentIndex
        lifecycleScope.launch { repository.saveProgress(b.id, indexToSave) }
    }

    private fun showLoading(loading: Boolean) {
        binding.progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError() {
        showLoading(false)
        binding.textError.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfReader?.close()
        pdfTextExtractor?.close()
    }

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
    }
}
