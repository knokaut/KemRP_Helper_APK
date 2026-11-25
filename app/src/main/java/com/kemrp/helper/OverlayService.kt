package com.kemrp.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class OverlayService : Service() {

    companion object {
        const val ACTION_REFRESH = "com.kemrp.helper.ACTION_REFRESH"
        const val ACTION_STOP = "com.kemrp.helper.ACTION_STOP"
        private const val CHANNEL_ID = "kemrp_overlay_channel"
        private const val NOTIF_ID = 1001
        @Volatile
        var isRunning: Boolean = false
    }

    private lateinit var wm: WindowManager
    private lateinit var repo: PhraseRepository

    private var bubbleView: View? = null
    private var panelView: View? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams

    private var isExpanded = false
    private var showArticlesMode = false
    private var currentSectionKey: String = ""
    private var currentArticleTab: String = AppData.ARTICLES.keys.first()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        repo = PhraseRepository(this)
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        startForegroundNotification()
        initBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH -> {
                if (isExpanded) {
                    rebuildPanel()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        removeViewSafe(bubbleView)
        removeViewSafe(panelView)
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "KemRPhelper Оверлей",
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(channel)

        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KemRPhelper активен")
            .setContentText("Нажми на иконку RP поверх игры для открытия фраз")
            .setSmallIcon(R.drawable.ic_rp_logo)
            .setContentIntent(openAppIntent)
            .addAction(0, "Закрыть оверлей", stopIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun initBubble() {
        val size = dp(56)
        val container = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E293B"))
                setStroke(dp(2), AppData.GOLD)
            }
            elevation = dp(6).toFloat()
        }
        val img = ImageView(this).apply {
            setImageResource(R.drawable.ic_rp_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        container.addView(img, FrameLayout.LayoutParams(size, size))

        bubbleParams = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(140)
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = bubbleParams.x
                    startY = bubbleParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    bubbleParams.x = startX + dx
                    bubbleParams.y = startY + dy
                    wm.updateViewLayout(container, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        expandPanel()
                    }
                    true
                }
                else -> false
            }
        }

        bubbleView = container
        wm.addView(container, bubbleParams)
    }

    private fun expandPanel() {
        if (isExpanded) return
        isExpanded = true
        bubbleView?.visibility = View.GONE
        rebuildPanel()
    }

    private fun collapsePanel() {
        isExpanded = false
        removeViewSafe(panelView)
        panelView = null
        bubbleView?.visibility = View.VISIBLE
    }

    private fun rebuildPanel() {
        val prevX = if (::panelParams.isInitialized) panelParams.x else dp(12)
        val prevY = if (::panelParams.isInitialized) panelParams.y else dp(80)
        removeViewSafe(panelView)

        val fid = repo.getSelectedFaction()
        val faction = AppData.factionById(fid)
        val theme = AppData.themeFor(fid)
        if (currentSectionKey !in faction.sectionKeys) {
            currentSectionKey = faction.sectionKeys.first()
        }

        val metrics = resources.displayMetrics
        val panelW = (metrics.widthPixels * 0.86f).toInt().coerceAtMost(dp(360))
        val panelH = (metrics.heightPixels * 0.56f).toInt().coerceAtMost(dp(460))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            alpha = repo.getOverlayAlpha()
            background = roundRect(theme.bg, 16, theme.border, 2)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        // Draggable Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundRect(theme.panel, 10)
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }

        val factionBtn = Button(this).apply {
            text = "${faction.name} ▾"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, Typeface.BOLD)
            background = roundRect(theme.btn, 8)
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener {
                val idx = AppData.FACTIONS.indexOfFirst { it.id == fid }
                val next = AppData.FACTIONS[(idx + 1) % AppData.FACTIONS.size]
                repo.setSelectedFaction(next.id)
                currentSectionKey = next.sectionKeys.first()
                rebuildPanel()
            }
        }
        header.addView(factionBtn)

        if (faction.megaphoneKey != null) {
            val megaBtn = Button(this).apply {
                text = "🔊"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                background = roundRect(theme.row, 8)
                minWidth = 0
                minimumWidth = 0
                minHeight = 0
                minimumHeight = 0
                setPadding(dp(8), dp(5), dp(8), dp(5))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { leftMargin = dp(6) }
                setOnClickListener {
                    val txt = repo.getMegaphone(faction.megaphoneKey)
                    if (txt.isNotBlank()) {
                        repo.copyPhrase(txt, fid)
                        if (repo.getAutoMinimize()) collapsePanel()
                    }
                }
            }
            header.addView(megaBtn)
        }

        val articlesBtn = Button(this).apply {
            text = if (showArticlesMode) "💬 Фразы" else "📖 Статьи"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            background = roundRect(if (showArticlesMode) theme.btn else theme.row, 8)
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(8), dp(6), dp(8), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(6) }
            setOnClickListener {
                showArticlesMode = !showArticlesMode
                rebuildPanel()
            }
        }
        header.addView(articlesBtn)

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        header.addView(spacer)

        val closeBtn = Button(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            background = roundRect(Color.parseColor("#991B1B"), 8)
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(10), dp(5), dp(10), dp(5))
            setOnClickListener { collapsePanel() }
        }
        header.addView(closeBtn)

        root.addView(header)

        // Category Tabs
        val tabsScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        val tabsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        if (!showArticlesMode) {
            for (secKey in faction.sectionKeys) {
                val active = secKey == currentSectionKey
                tabsRow.addView(Button(this).apply {
                    text = AppData.SECTION_TITLES[secKey] ?: secKey
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    background = roundRect(if (active) theme.btn else theme.row, 8)
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { rightMargin = dp(6) }
                    setOnClickListener {
                        currentSectionKey = secKey
                        rebuildPanel()
                    }
                })
            }
        } else {
            for (tabName in AppData.ARTICLES.keys) {
                val active = tabName == currentArticleTab
                tabsRow.addView(Button(this).apply {
                    text = tabName
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    background = roundRect(if (active) theme.btn else theme.row, 8)
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { rightMargin = dp(6) }
                    setOnClickListener {
                        currentArticleTab = tabName
                        rebuildPanel()
                    }
                })
            }
        }
        tabsScroll.addView(tabsRow)
        root.addView(tabsScroll)

        // Content List
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply { topMargin = dp(8) }
        }
        val listLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        if (!showArticlesMode) {
            val phrases = repo.getPhrases(currentSectionKey)
            for (item in phrases) {
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = roundRect(theme.row, 10)
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(6) }
                    setOnClickListener {
                        repo.copyPhrase(item.text, fid)
                        if (repo.getAutoMinimize()) {
                            collapsePanel()
                        }
                    }
                }
                card.addView(TextView(this).apply {
                    text = item.label
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTypeface(null, Typeface.BOLD)
                })
                card.addView(TextView(this).apply {
                    text = repo.formatPhrase(item.text, fid)
                    setTextColor(Color.parseColor("#CBD5E1"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    maxLines = 2
                })
                listLayout.addView(card)
            }
        } else {
            val articles = AppData.ARTICLES[currentArticleTab].orEmpty()
            for ((code, desc) in articles) {
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = roundRect(theme.row, 10)
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(6) }
                    setOnClickListener {
                        repo.copyRaw("$code — $desc")
                    }
                }
                card.addView(TextView(this).apply {
                    text = code
                    setTextColor(AppData.GOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
                })
                card.addView(TextView(this).apply {
                    text = desc
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                })
                listLayout.addView(card)
            }
        }

        scroll.addView(listLayout)
        root.addView(scroll)

        panelParams = WindowManager.LayoutParams(
            panelW,
            panelH,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prevX
            y = prevY
        }

        // Drag panel by header
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = panelParams.x
                    startY = panelParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    panelParams.x = startX + (event.rawX - touchX).toInt()
                    panelParams.y = startY + (event.rawY - touchY).toInt()
                    wm.updateViewLayout(root, panelParams)
                    true
                }
                else -> false
            }
        }

        panelView = root
        wm.addView(root, panelParams)
    }

    private fun removeViewSafe(view: View?) {
        if (view != null) {
            try {
                wm.removeView(view)
            } catch (_: Exception) {}
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
