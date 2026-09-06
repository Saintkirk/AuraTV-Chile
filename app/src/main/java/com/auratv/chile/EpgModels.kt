package com.auratv.chile

data class EpgProgram(
    val channelId: String,
    val start: Long,
    val stop: Long,
    val title: String,
    val description: String? = null
) {
    fun isNow(now: Long = System.currentTimeMillis()): Boolean =
        start <= now && stop > now
}

data class ChannelWithEpg(
    val channel: Channel,
    val programs: List<EpgProgram>
) {
    fun nowPlaying(now: Long = System.currentTimeMillis()): EpgProgram? =
        programs.firstOrNull { it.isNow(now) }
}
