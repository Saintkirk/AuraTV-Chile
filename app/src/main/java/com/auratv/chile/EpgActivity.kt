package com.auratv.chile

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EpgActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHANNELS_JSON = "channels_json" // no usado; cargamos de nuevo
    }

    private lateinit var list: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private lateinit var headerTime: TextView

    private var rows: List<ChannelWithEpg> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epg)

        list = findViewById(R.id.epgList)
        progress = findViewById(R.id.epgProgress)
        status = findViewById(R.id.epgStatus)
        headerTime = findViewById(R.id.epgHeaderTime)

        list.layoutManager = LinearLayoutManager(this)
        headerTime.text = SimpleDateFormat("EEE d MMM  •  HH:mm", Locale("es", "CL"))
            .format(Date())

        loadEpg()
    }

    private fun loadEpg() {
        progress.visibility = View.VISIBLE
        status.visibility = View.VISIBLE
        status.text = "Cargando guía de Chile…"

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                // Reutilizar misma lista Chile
                val playlistUrl = getString(R.string.playlist_url)
                val channels = fetchChannels(playlistUrl)
                val programs = EpgRepository.loadChileEpg()
                EpgRepository.matchChannels(channels, programs)
            }

            progress.visibility = View.GONE
            rows = result.filter { it.programs.isNotEmpty() || true }

            if (rows.isEmpty()) {
                status.text = "Sin datos de guía. Revisa la conexión."
                status.visibility = View.VISIBLE
            } else {
                status.visibility = View.GONE
                list.adapter = EpgRowAdapter(rows) { channel ->
                    openPlayer(channel)
                }
                list.post { list.getChildAt(0)?.requestFocus() }
            }
        }
    }

    private fun fetchChannels(url: String): List<Channel> {
        return try {
            val client = okhttp3.OkHttpClient()
            val req = okhttp3.Request.Builder().url(url).header("User-Agent", "AuraTV/1.0").build()
            val body = client.newCall(req).execute().body?.string() ?: return emptyList()
            parseM3u(body).take(300)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseM3u(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        var name = ""
        var logo: String? = null
        var group: String? = null
        var tvgId: String? = null
        content.lineSequence().forEach { line ->
            when {
                line.startsWith("#EXTINF:") -> {
                    tvgId = Regex("""tvg-id=\"([^\"]*)\"""").find(line)?.groupValues?.getOrNull(1)
                    logo = Regex("""tvg-logo=\"([^\"]*)\"""").find(line)?.groupValues?.getOrNull(1)
                    group = Regex("""group-title=\"([^\"]*)\"""").find(line)?.groupValues?.getOrNull(1)
                    name = line.substringAfterLast(",").trim()
                }
                line.isNotBlank() && !line.startsWith("#") -> {
                    if (name.isNotBlank()) {
                        channels.add(Channel(name, line.trim(), logo, group, tvgId))
                    }
                }
            }
        }
        return channels
    }

    private fun openPlayer(channel: Channel) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URL, channel.url)
            putExtra(PlayerActivity.EXTRA_NAME, channel.name)
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

class EpgRowAdapter(
    private val items: List<ChannelWithEpg>,
    private val onPlay: (Channel) -> Unit
) : RecyclerView.Adapter<EpgRowAdapter.VH>() {

    private val timeFmt = SimpleDateFormat("HH:mm", Locale("es", "CL"))
    private val now = System.currentTimeMillis()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val channelName: TextView = view.findViewById(R.id.epgChannelName)
        val programTitle: TextView = view.findViewById(R.id.epgProgramTitle)
        val programTime: TextView = view.findViewById(R.id.epgProgramTime)
        val nextTitle: TextView = view.findViewById(R.id.epgNextTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_epg_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        val nowProg = row.nowPlaying(now)
        val next = row.programs.firstOrNull { it.start > now }

        holder.channelName.text = row.channel.name
        if (nowProg != null) {
            holder.programTitle.text = nowProg.title
            holder.programTime.text =
                "${timeFmt.format(Date(nowProg.start))} – ${timeFmt.format(Date(nowProg.stop))}  •  EN VIVO"
        } else {
            holder.programTitle.text = "Sin información de programa"
            holder.programTime.text = "—"
        }
        holder.nextTitle.text = next?.let {
            "Luego: ${it.title} (${timeFmt.format(Date(it.start))})"
        } ?: ""

        holder.itemView.setOnClickListener { onPlay(row.channel) }
        holder.itemView.isFocusable = true
        holder.itemView.isClickable = true
    }

    override fun getItemCount() = items.size
}
