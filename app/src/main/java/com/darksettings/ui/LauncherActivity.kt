package com.darksettings.ui

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darksettings.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LauncherActivity : AppCompatActivity() {

    private lateinit var tvClock: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvFocusMode: TextView
    private lateinit var tvFocusTimer: TextView
    private lateinit var musicPlayer: LinearLayout
    private lateinit var tvTrackTitle: TextView
    private lateinit var tvTrackArtist: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var pinnedApps: LinearLayout
    private lateinit var allAppsScreen: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var rvAllApps: RecyclerView
    private lateinit var focusScreen: LinearLayout
    private lateinit var tvFocusCountdown: TextView
    private lateinit var btnStartFocus: TextView
    private lateinit var btnPomodoro: TextView
    private lateinit var btnShortBreak: TextView
    private lateinit var btnLongBreak: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var focusTimer: CountDownTimer? = null
    private var isFocusRunning = false
    private var focusDuration = 25 * 60 * 1000L
    private var focusTimeLeft = focusDuration

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var currentController: MediaController? = null

    private val clockUpdateRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000)
        }
    }

    private val pinnedAppPackages = listOf(
        "com.whatsapp",
        "com.instagram.lite",
        "org.schabi.newpipe",
        "com.shazam.android",
        "com.dnsnet",
        "com.android.settings"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_launcher)

        initViews()
        setupClock()
        setupMusicPlayer()
        setupFocusMode()
        setupAllApps()
        setupPinnedApps()
        setupBottomActions()
    }

    private fun initViews() {
        tvClock = findViewById(R.id.tvClock)
        tvDate = findViewById(R.id.tvDate)
        tvFocusMode = findViewById(R.id.tvFocusMode)
        tvFocusTimer = findViewById(R.id.tvFocusTimer)
        musicPlayer = findViewById(R.id.musicPlayer)
        tvTrackTitle = findViewById(R.id.tvTrackTitle)
        tvTrackArtist = findViewById(R.id.tvTrackArtist)
        seekBar = findViewById(R.id.seekBar)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        pinnedApps = findViewById(R.id.pinnedApps)
        allAppsScreen = findViewById(R.id.allAppsScreen)
        etSearch = findViewById(R.id.etSearch)
        rvAllApps = findViewById(R.id.rvAllApps)
        focusScreen = findViewById(R.id.focusScreen)
        tvFocusCountdown = findViewById(R.id.tvFocusCountdown)
        btnStartFocus = findViewById(R.id.btnStartFocus)
        btnPomodoro = findViewById(R.id.btnPomodoro)
        btnShortBreak = findViewById(R.id.btnShortBreak)
        btnLongBreak = findViewById(R.id.btnLongBreak)
    }

    private fun setupClock() {
        handler.post(clockUpdateRunnable)
    }

    private fun updateClock() {
        val now = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es"))
        tvClock.text = timeFormat.format(now.time)
        tvDate.text = dateFormat.format(now.time)
    }

    private fun setupMusicPlayer() {
        try {
            mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            checkActiveSession()
        } catch (e: Exception) {
            musicPlayer.visibility = View.GONE
        }

        btnPlayPause.setOnClickListener {
            currentController?.transportControls?.let { controls ->
                if (isPlaying()) {
                    controls.pause()
                    btnPlayPause.setImageResource(R.drawable.ic_play)
                } else {
                    controls.play()
                    btnPlayPause.setImageResource(R.drawable.ic_pause)
                }
            }
        }

        btnPrevious.setOnClickListener {
            currentController?.transportControls?.skipToPrevious()
        }

        btnNext.setOnClickListener {
            currentController?.transportControls?.skipToNext()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentController?.transportControls?.seekTo(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        handler.postDelayed(object : Runnable {
            override fun run() {
                updateMusicInfo()
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun checkActiveSession() {
        try {
            currentController = MediaNotificationListenerService.getActiveController(this)
            if (currentController != null) {
                musicPlayer.visibility = View.VISIBLE
                updateMusicInfo()
            } else {
                musicPlayer.visibility = View.GONE
            }
        } catch (e: Exception) {
            musicPlayer.visibility = View.GONE
        }
    }

    private fun updateMusicInfo() {
        val controller = currentController ?: return
        val metadata = controller.metadata
        if (metadata != null) {
            musicPlayer.visibility = View.VISIBLE
            tvTrackTitle.text = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
            tvTrackArtist.text = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
            seekBar.max = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).toInt()
            seekBar.progress = controller.playbackState?.position?.toInt() ?: 0

            if (isPlaying()) {
                btnPlayPause.setImageResource(R.drawable.ic_pause)
            } else {
                btnPlayPause.setImageResource(R.drawable.ic_play)
            }
        } else {
            musicPlayer.visibility = View.GONE
        }
    }

    private fun isPlaying(): Boolean {
        return currentController?.playbackState?.state == PlaybackState.STATE_PLAYING
    }

    private fun setupFocusMode() {
        btnPomodoro.setOnClickListener { setFocusDuration(25 * 60 * 1000L, btnPomodoro) }
        btnShortBreak.setOnClickListener { setFocusDuration(5 * 60 * 1000L, btnShortBreak) }
        btnLongBreak.setOnClickListener { setFocusDuration(15 * 60 * 1000L, btnLongBreak) }

        btnStartFocus.setOnClickListener {
            if (isFocusRunning) {
                pauseFocus()
                btnStartFocus.text = "resume"
            } else {
                startFocus()
                btnStartFocus.text = "pause"
            }
        }

        findViewById<TextView>(R.id.btnResetFocus).setOnClickListener {
            resetFocus()
            btnStartFocus.text = "start"
        }

        findViewById<TextView>(R.id.btnCloseFocus).setOnClickListener {
            focusScreen.visibility = View.GONE
            findViewById<View>(R.id.homeScreen).visibility = View.VISIBLE
        }

        findViewById<TextView>(R.id.btnFocus).setOnClickListener {
            findViewById<View>(R.id.homeScreen).visibility = View.GONE
            focusScreen.visibility = View.VISIBLE
        }
    }

    private fun setFocusDuration(duration: Long, selectedTab: TextView) {
        focusDuration = duration
        focusTimeLeft = duration
        updateFocusDisplay()

        btnPomodoro.setTextColor(0x40FFFFFF.toInt())
        btnShortBreak.setTextColor(0x40FFFFFF.toInt())
        btnLongBreak.setTextColor(0x40FFFFFF.toInt())
        selectedTab.setTextColor(0xFFFFFFFF.toInt())
    }

    private fun startFocus() {
        isFocusRunning = true
        tvFocusMode.visibility = View.VISIBLE
        tvFocusTimer.visibility = View.VISIBLE

        focusTimer = object : CountDownTimer(focusTimeLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                focusTimeLeft = millisUntilFinished
                updateFocusDisplay()
            }
            override fun onFinish() {
                isFocusRunning = false
                tvFocusMode.visibility = View.GONE
                tvFocusTimer.visibility = View.GONE
                Toast.makeText(this@LauncherActivity, "Focus session complete", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun pauseFocus() {
        isFocusRunning = false
        focusTimer?.cancel()
    }

    private fun resetFocus() {
        isFocusRunning = false
        focusTimer?.cancel()
        focusTimeLeft = focusDuration
        updateFocusDisplay()
        tvFocusMode.visibility = View.GONE
        tvFocusTimer.visibility = View.GONE
    }

    private fun updateFocusDisplay() {
        val minutes = (focusTimeLeft / 1000) / 60
        val seconds = (focusTimeLeft / 1000) % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)
        tvFocusCountdown.text = timeString
        tvFocusTimer.text = timeString
    }

    private fun setupAllApps() {
        val allApps = getAllApps()
        val adapter = AllAppsAdapter(allApps) { appInfo ->
            val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        }

        rvAllApps.layoutManager = LinearLayoutManager(this)
        rvAllApps.adapter = adapter

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString().lowercase()
                adapter.filter(query)
                true
            }
            false
        }

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s?.toString()?.lowercase() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        findViewById<TextView>(R.id.btnCloseAllApps).setOnClickListener {
            allAppsScreen.visibility = View.GONE
            findViewById<View>(R.id.homeScreen).visibility = View.VISIBLE
            etSearch.setText("")
        }
    }

    private fun setupPinnedApps() {
        pinnedApps.removeAllViews()
        pinnedAppPackages.forEach { pkg ->
            try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                val label = packageManager.getApplicationLabel(appInfo).toString()

                val textView = TextView(this).apply {
                    text = label.lowercase()
                    setTextColor(0xB0FFFFFF.toInt())
                    textSize = 16f
                    setPadding(0, 12, 0, 12)
                    setOnClickListener {
                        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        }
                    }
                }
                pinnedApps.addView(textView)
            } catch (e: PackageManager.NameNotFoundException) {
                // App not installed, skip
            }
        }
    }

    private fun setupBottomActions() {
        findViewById<TextView>(R.id.btnAllApps).setOnClickListener {
            findViewById<View>(R.id.homeScreen).visibility = View.GONE
            allAppsScreen.visibility = View.VISIBLE
            etSearch.requestFocus()
        }

        findViewById<TextView>(R.id.btnSettings).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getAllApps(): List<AppInfo> {
        val pm = packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        return packages
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                val appInfo = pm.getApplicationInfo(it.packageName, 0)
                AppInfo(
                    packageName = it.packageName,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo)
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    override fun onResume() {
        super.onResume()
        handler.post(clockUpdateRunnable)
        checkActiveSession()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockUpdateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clockUpdateRunnable)
        focusTimer?.cancel()
    }

    override fun onBackPressed() {
        when {
            focusScreen.visibility == View.VISIBLE -> {
                focusScreen.visibility = View.GONE
                findViewById<View>(R.id.homeScreen).visibility = View.VISIBLE
            }
            allAppsScreen.visibility == View.VISIBLE -> {
                allAppsScreen.visibility = View.GONE
                findViewById<View>(R.id.homeScreen).visibility = View.VISIBLE
                etSearch.setText("")
            }
            else -> {
                // Stay on launcher
            }
        }
    }

    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable
    )

    class AllAppsAdapter(
        private var apps: List<AppInfo>,
        private val onClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<AllAppsAdapter.ViewHolder>() {

        private var filteredApps = apps.toList()

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view as TextView
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val textView = TextView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 14, 0, 14)
                setTextColor(0xB0FFFFFF.toInt())
                textSize = 16f
            }
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = filteredApps[position]
            holder.textView.text = app.label.lowercase()
            holder.textView.setOnClickListener { onClick(app) }
        }

        override fun getItemCount() = filteredApps.size

        fun filter(query: String) {
            filteredApps = if (query.isEmpty()) {
                apps
            } else {
                apps.filter { it.label.lowercase().contains(query) }
            }
            notifyDataSetChanged()
        }
    }
}
