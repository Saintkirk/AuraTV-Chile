package com.auratv.chile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class Channel(
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val tvgId: String? = null
)

class MainActivity : AppCompatActivity() {

    private lateinit var channelGrid: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var btnEpg: Button

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        channelGrid = findViewById(R.id.channelGrid)
        progress = findViewById(R.id.progress)
        statusText = findViewById(R.id.statusText)
        btnEpg = findViewById(R.id.btnEpg)

        channelGrid.layoutManager = GridLayoutManager(this, 4)

        btnEpg.setOnClickListener {
            startActivity(Intent(this, EpgActivity::class.java))
        }

        loadChilePlaylist()
    }

    private fun loadChilePlaylist() {
        progress.visibility = View.VISIBLE
        statusText.visibility = View.VISIBLE
        statusText.text = getString(R.string.loading)

        lifecycleScope.launch {
            val channels = withContext(Dispatchers.IO) {
                fetchAndParseM3u(getString(R.string.playlist_url))
            }

            progress.visibility = View.GONE

            if (channels.isEmpty()) {
                statusText.text = "No se pudieron cargar canales. Revisa la conexión."
                statusText.visibility = View.VISIBLE
            } else {
                statusText.visibility = View.GONE
                channelGrid.adapter = ChannelAdapter(channels) { channel ->
                    openPlayer(channel)
                }
            }
        }
    }

    private fun fetchAndParseM3u(url: String): List<Channel> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AuraTV/1.0")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            parseM3u(body).take(500)
        } catch (e: Exception) {
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
                        channels.add(
                            Channel(
                                name = name,
                                url = line.trim(),
                                logo = logo,
                                group = group,
                                tvgId = tvgId
                            )
                        )
                    }
                }
            }
        }
        return channels
    }

    private fun openPlayer(channel: Channel) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URL, channel.url)
            putExtra(PlayerActivity.EXTRA_NAME, channel.name)
        }
        startActivity(intent)
    }
}

class ChannelAdapter(
    private val items: List<Channel>,
    private val onClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.channelName)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val channel = items[position]
        holder.name.text = channel.name
        holder.itemView.setOnClickListener { onClick(channel) }
    }

    override fun getItemCount() = items.size
}
