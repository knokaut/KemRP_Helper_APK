package com.kemrp.helper

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ArticlesActivity : AppCompatActivity() {

    private lateinit var repo: PhraseRepository
    private lateinit var theme: FactionTheme
    private lateinit var listContainer: LinearLayout
    private lateinit var tabsContainer: LinearLayout
    private var currentTab: String = AppData.ARTICLES.keys.first()
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = PhraseRepository(this)
        theme = AppData.themeFor(repo.getSelectedFaction())
        window.statusBarColor = theme.bg
        window.navigationBarColor = theme.bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.bg)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        // Top bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val backBtn = Button(this).apply {
            text = "← Назад"
            setTextColor(Color.WHITE)
            background = roundRect(theme.btn, 10)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { finish() }
        }
        val title = TextView(this).apply {
            text = "📖 Статьи УК и КоАП"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(12), 0, 0, 0)
        }
        topBar.addView(backBtn)
        topBar.addView(title)
        root.addView(topBar)

        // Search input
        val searchInput = EditText(this).apply {
            hint = "Поиск статьи (например: 12.15 или угон)..."
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            background = roundRect(theme.panel, 10, theme.border, 1)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                    renderArticles()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        root.addView(searchInput)

        // Tabs
        val tabsScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        tabsContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabsScroll.addView(tabsContainer)
        root.addView(tabsScroll)

        // Articles list
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply { topMargin = dp(10) }
        }
        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(listContainer)
        root.addView(scroll)

        setContentView(root)
        renderTabs()
        renderArticles()
    }

    private fun renderTabs() {
        tabsContainer.removeAllViews()
        for (tabName in AppData.ARTICLES.keys) {
            val active = tabName == currentTab
            val btn = Button(this).apply {
                text = tabName
                isAllCaps = false
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                background = roundRect(if (active) theme.btn else theme.row, 10)
                setPadding(dp(14), dp(8), dp(14), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = dp(8) }
                setOnClickListener {
                    currentTab = tabName
                    renderTabs()
                    renderArticles()
                }
            }
            tabsContainer.addView(btn)
        }
    }

    private fun renderArticles() {
        listContainer.removeAllViews()
        val items = if (searchQuery.isNotEmpty()) {
            AppData.ARTICLES.values.flatten().filter { (code, desc) ->
                code.lowercase().contains(searchQuery) || desc.lowercase().contains(searchQuery)
            }
        } else {
            AppData.ARTICLES[currentTab].orEmpty()
        }

        if (items.isEmpty()) {
            listContainer.addView(TextView(this).apply {
                text = "Ничего не найдено"
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(dp(8), dp(20), dp(8), dp(8))
            })
            return
        }

        for ((code, desc) in items) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = roundRect(theme.row, 12)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
                setOnClickListener {
                    repo.copyRaw("$code — $desc")
                }
            }
            card.addView(TextView(this).apply {
                text = code
                setTextColor(AppData.GOLD)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            })
            card.addView(TextView(this).apply {
                text = desc
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, dp(4), 0, 0)
            })
            listContainer.addView(card)
        }
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
