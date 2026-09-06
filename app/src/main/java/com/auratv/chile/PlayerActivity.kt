package com.auratv.chile

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_NAME = "name"
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var infoBar: LinearLayout
    private lateinit var titleView: TextView
    private lateinit var channelView: TextView

    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { infoBar.visibility = View.GONE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)
        infoBar = findViewById(R.id.infoBar)
        titleView = findViewById(R.id.nowPlayingTitle)
        channelView = findViewById(R.id.nowPlayingChannel)

        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            finish()
            return
        }
        val name = intent.getStringExtra(EXTRA_NAME) ?: "Canal"

        titleView.text = name
        channelView.text = getString(R.string.now_playing)

        initPlayer(url)
        showInfoBarTemporarily()
    }

    private fun initPlayer(url: String) {
        val trackSelector = DefaultTrackSelector(this).apply {
            parameters = buildUponParameters()
                .setMaxVideoSize(3840, 2160)
                .setForceHighestSupportedBitrate(true)
                .setPreferredVideoMimeTypes(
                    MimeTypes.VIDEO_H265,
                    MimeTypes.VIDEO_AV1,
                    MimeTypes.VIDEO_VP9,
                    MimeTypes.VIDEO_H264
                )
                .setTunnelingEnabled(true)
                .build()
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 50_000, 2_500, 5_000)
            .build()

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .also { exo ->
                playerView.player = exo
                playerView.useController = false

                exo.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        when (error.errorCode) {
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                                hideHandler.postDelayed({
                                    exo.prepare()
                                    exo.play()
                                }, 2500)
                            }
                            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                                exo.seekToDefaultPosition()
                                exo.prepare()
                            }
                        }
                    }
                })

                val mediaItem = MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()

                exo.setMediaItem(mediaItem)
                exo.prepare()
                exo.playWhenReady = true
            }
    }

    private fun showInfoBarTemporarily() {
        infoBar.visibility = View.VISIBLE
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 4000)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                player?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
                showInfoBarTemporarily()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_INFO -> {
                showInfoBarTemporarily()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        hideHandler.removeCallbacks(hideRunnable)
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
