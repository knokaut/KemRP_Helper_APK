package com.kemrp.helper

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var repo: PhraseRepository
    private var currentFid: String = "dps"
    private var currentSectionKey: String = "dps_proc"

    private lateinit var rootLayout: LinearLayout
    private lateinit var overlayCard: LinearLayout
    private lateinit var overlayBtn: Button
    private lateinit var alphaLabel: TextView
    private lateinit var factionRow: LinearLayout
    private lateinit var profileCard: LinearLayout
    private lateinit var rankLabel: TextView
    private lateinit var nameLabel: TextView
    private lateinit var rankInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var megaBtn: Button
    private lateinit var sectionRow: LinearLayout
    private lateinit var phrasesList: LinearLayout
    private lateinit var counterText: TextView
    private lateinit var articlesBtn: Button

    private var updatingInputs = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = PhraseRepository(this)
        currentFid = repo.getSelectedFaction()
        currentSectionKey = AppData.factionById(currentFid).sectionKeys.first()

        buildUi()
        renderAll()
    }

    override fun onResume() {
        super.onResume()
        updateOverlayButton()
        updateCounter()
    }

    private fun buildUi() {
        val mainScroll = ScrollView(this)
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        mainScroll.addView(rootLayout)

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_rp_logo)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
        }
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(this).apply {
            text = "KemRPhelper v${AppData.APP_VERSION}"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(null, Typeface.BOLD)
        })
        titleCol.addView(TextView(this).apply {
            text = "Android · Копирование в 1 тап"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })
        articlesBtn = Button(this).apply {
            text = "📖 Статьи"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ArticlesActivity::class.java))
            }
        }
        header.addView(logo)
        header.addView(titleCol)
        header.addView(articlesBtn)
        rootLayout.addView(header)

        // Overlay Control Card
        overlayCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
        }
        overlayBtn = Button(this).apply {
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setOnClickListener { toggleOverlay() }
        }
        overlayCard.addView(overlayBtn)

        val alphaRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        alphaLabel = TextView(this).apply {
            text = "Прозрачность: ${(repo.getOverlayAlpha() * 100).toInt()}%"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        }
        val seekBar = SeekBar(this).apply {
            max = 70
            progress = ((repo.getOverlayAlpha() - 0.3f) * 100).toInt().coerceIn(0, 70)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(10)
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val alpha = (30 + progress) / 100f
                    alphaLabel.text = "Прозрачность: ${30 + progress}%"
                    repo.setOverlayAlpha(alpha)
                    refreshOverlayIfRunning()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        alphaRow.addView(alphaLabel)
        alphaRow.addView(seekBar)
        overlayCard.addView(alphaRow)

        val autoMinSwitch = Switch(this).apply {
            text = "Сворачивать панель после копирования"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            isChecked = repo.getAutoMinimize()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
            setOnCheckedChangeListener { _, isChecked ->
                repo.setAutoMinimize(isChecked)
            }
        }
        overlayCard.addView(autoMinSwitch)
        rootLayout.addView(overlayCard)

        // Factions horizontal bar
        val factionScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
        }
        factionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        factionScroll.addView(factionRow)
        rootLayout.addView(factionScroll)

        // Profile Card (Rank + Name)
        profileCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }
        val col1 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(8)
            }
        }
        rankLabel = TextView(this).apply {
            setTextColor(Color.parseColor("#CBD5E1"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        rankInput = EditText(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setSingleLine(true)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        col1.addView(rankLabel)
        col1.addView(rankInput)

        val col2 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        nameLabel = TextView(this).apply {
            setTextColor(Color.parseColor("#CBD5E1"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        nameInput = EditText(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setSingleLine(true)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        col2.addView(nameLabel)
        col2.addView(nameInput)

        val profileWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!updatingInputs) {
                    repo.saveProfile(currentFid, rankInput.text.toString(), nameInput.text.toString())
                    renderPhrases()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        rankInput.addTextChangedListener(profileWatcher)
        nameInput.addTextChangedListener(profileWatcher)

        profileCard.addView(col1)
        profileCard.addView(col2)
        rootLayout.addView(profileCard)

        // Megaphone quick button
        megaBtn = Button(this).apply {
            text = "🔊 Скопировать МЕГАФОН"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            setOnClickListener {
                val key = AppData.factionById(currentFid).megaphoneKey ?: return@setOnClickListener
                val txt = repo.getMegaphone(key)
                if (txt.isNotBlank()) {
                    repo.copyPhrase(txt, currentFid)
                    updateCounter()
                }
            }
        }
        rootLayout.addView(megaBtn)

        // Section tabs
        val secScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }
        sectionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        secScroll.addView(sectionRow)
        rootLayout.addView(secScroll)

        // Phrases list
        phrasesList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        rootLayout.addView(phrasesList)

        // Footer
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        }
        counterText = TextView(this).apply {
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val resetBtn = Button(this).apply {
            text = "Сбросить фразы"
            isAllCaps = false
            setTextColor(Color.parseColor("#FCA5A5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = roundRect(Color.parseColor("#3F1D2B"), 8)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Сброс фраз")
                    .setMessage("Вернуть все фразы и профили к стандартным?")
                    .setPositiveButton("Сбросить") { _, _ ->
                        repo.resetToDefaults()
                        renderAll()
                        Toast.makeText(this@MainActivity, "Фразы сброшены", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        }
        footer.addView(counterText)
        footer.addView(resetBtn)
        rootLayout.addView(footer)

        setContentView(mainScroll)
    }

    private fun renderAll() {
        val faction = AppData.factionById(currentFid)
        val theme = AppData.themeFor(currentFid)
        if (currentSectionKey !in faction.sectionKeys) {
            currentSectionKey = faction.sectionKeys.first()
        }

        window.statusBarColor = theme.bg
        window.navigationBarColor = theme.bg
        rootLayout.setBackgroundColor(theme.bg)
        (rootLayout.parent as? View)?.setBackgroundColor(theme.bg)

        articlesBtn.background = roundRect(theme.btn, 10)
        overlayCard.background = roundRect(theme.panel, 14, theme.border, 1)
        profileCard.background = roundRect(theme.panel, 14)
        rankInput.background = roundRect(theme.entry, 8, theme.border, 1)
        nameInput.background = roundRect(theme.entry, 8, theme.border, 1)
        megaBtn.background = roundRect(theme.btn, 10)
        megaBtn.visibility = if (faction.megaphoneKey != null) View.VISIBLE else View.GONE

        rankLabel.text = faction.rankLabel
        nameLabel.text = faction.nameLabel
        val (r, n) = repo.getProfile(currentFid)
        updatingInputs = true
        rankInput.setText(r)
        nameInput.setText(n)
        updatingInputs = false

        renderFactions(theme)
        renderSections(faction, theme)
        renderPhrases()
        updateOverlayButton()
        updateCounter()
    }

    private fun renderFactions(theme: FactionTheme) {
        factionRow.removeAllViews()
        for (f in AppData.FACTIONS) {
            val active = f.id == currentFid
            factionRow.addView(Button(this).apply {
                text = f.name
                isAllCaps = false
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
                background = roundRect(if (active) theme.btn else theme.row, 10)
                setPadding(dp(14), dp(8), dp(14), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = dp(8) }
                setOnClickListener {
                    currentFid = f.id
                    repo.setSelectedFaction(currentFid)
                    currentSectionKey = f.sectionKeys.first()
                    renderAll()
                    refreshOverlayIfRunning()
                }
            })
        }
    }

    private fun renderSections(faction: FactionDef, theme: FactionTheme) {
        sectionRow.removeAllViews()
        for (secKey in faction.sectionKeys) {
            val active = secKey == currentSectionKey
            sectionRow.addView(Button(this).apply {
                text = AppData.SECTION_TITLES[secKey] ?: secKey
                isAllCaps = false
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                background = roundRect(if (active) theme.btn else theme.panel, 10)
                setPadding(dp(12), dp(7), dp(12), dp(7))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = dp(8) }
                setOnClickListener {
                    currentSectionKey = secKey
                    renderSections(faction, theme)
                    renderPhrases()
                }
            })
        }
    }

    private fun renderPhrases() {
        phrasesList.removeAllViews()
        val theme = AppData.themeFor(currentFid)
        val phrases = repo.getPhrases(currentSectionKey)

        phrases.forEachIndexed { idx, item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = roundRect(theme.row, 12)
                setPadding(dp(12), dp(10), dp(10), dp(10))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
                setOnClickListener {
                    repo.copyPhrase(item.text, currentFid)
                    updateCounter()
                }
            }

            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(this).apply {
                text = item.label
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(null, Typeface.BOLD)
            })
            textCol.addView(TextView(this).apply {
                text = repo.formatPhrase(item.text, currentFid)
                setTextColor(Color.parseColor("#CBD5E1"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(3), 0, 0)
            })

            val editBtn = Button(this).apply {
                text = "✏️"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                background = roundRect(theme.panel, 8)
                minWidth = 0
                minimumWidth = 0
                setPadding(dp(10), dp(6), dp(10), dp(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { leftMargin = dp(8) }
                setOnClickListener { showEditDialog(idx, item) }
            }

            card.addView(textCol)
            card.addView(editBtn)
            phrasesList.addView(card)
        }
    }

    private fun showEditDialog(index: Int, item: PhraseItem) {
        val input = EditText(this).apply {
            setText(item.text)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        AlertDialog.Builder(this)
            .setTitle(item.label)
            .setMessage("Можно использовать {rank} и {name}")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                repo.updatePhrase(currentSectionKey, index, input.text.toString())
                renderPhrases()
                refreshOverlayIfRunning()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun toggleOverlay() {
        if (OverlayService.isRunning) {
            startService(Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_STOP))
            overlayBtn.postDelayed({ updateOverlayButton() }, 200)
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Разреши показ поверх других приложений для KemRPhelper", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        startForegroundService(Intent(this, OverlayService::class.java))
        overlayBtn.postDelayed({ updateOverlayButton() }, 250)
    }

    private fun refreshOverlayIfRunning() {
        if (OverlayService.isRunning) {
            startService(Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_REFRESH))
        }
    }

    private fun updateOverlayButton() {
        val theme = AppData.themeFor(currentFid)
        if (OverlayService.isRunning) {
            overlayBtn.text = "⏹ ОСТАНОВИТЬ ОВЕРЛЕЙ"
            overlayBtn.background = roundRect(Color.parseColor("#DC2626"), 12)
        } else {
            overlayBtn.text = "▶ ЗАПУСТИТЬ ОВЕРЛЕЙ ПОВЕРХ ИГРЫ"
            overlayBtn.background = roundRect(theme.btn, 12)
        }
    }

    private fun updateCounter() {
        counterText.text = "Скопировано фраз: ${repo.getSentCount()}"
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun roundRect(fill: Int, radiusDp: Int, strokeColor: Int = 0, strokeDp: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeDp > 0) setStroke(dp(strokeDp), strokeColor)
        }
    }
}
