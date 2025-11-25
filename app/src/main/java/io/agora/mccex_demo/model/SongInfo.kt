package io.agora.mccex_demo.model

data class SongInfo(
    val songId: String,
    val optionJson: String,
    val songName: String,
    val singerName: String,
    var songCode: Long = 0L,
    var lyricPath: String,
    var pitchPath: String,
    var songOffsetBegin: Int = 0,
    var songOffsetEnd: Int = 0,
    var lyricOffset: Int = 0,
)
