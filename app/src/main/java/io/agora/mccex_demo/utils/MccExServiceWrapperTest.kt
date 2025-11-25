package io.agora.mccex_demo.utils

import io.agora.mccex.bridge.BridgeUtils
import io.agora.mccex.bridge.NativeMsgBridge
import io.agora.mccex.bridge.TestWrapper
import io.agora.mccex.constants.ChargeMode
import io.agora.mccex.constants.LyricType
import io.agora.mccex.constants.MusicPlayMode
import io.agora.mccex.constants.ScoreHardLevel
import io.agora.mccex.utils.Utils
import io.agora.mccex_demo.agora.MccExManager
import io.agora.mediaplayer.Constants
import io.agora.rtc2.ChannelMediaOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

object MccExServiceWrapperTest : TestWrapper.Callback {

    private var mPlayerId = 0
    private var mSongCode = 0L

    fun testStart() {
        LogUtils.d("MccExServiceWrapperTest test start")
        val config = "{\"appId\":\"${KeyCenter.APP_ID}\"}"
        val rtcNativeEngineHandle = TestWrapper.createRtcEngine(config)
        LogUtils.d("MccExServiceWrapperTest rtcEngine native Handle: $rtcNativeEngineHandle")

        joinChannel()

        TestWrapper.setCallback(this)

        TestWrapper.createMccEx()

        val configurationJson = JSONObject()
        val vendorConfigureJson = JSONObject()
        vendorConfigureJson.put("appId", MccExKeys.ysdAppId)
        vendorConfigureJson.put("appKey", MccExKeys.ysdAppKey)
        vendorConfigureJson.put("token", MccExKeys.ysdToken)
        vendorConfigureJson.put("userId", MccExKeys.ysdUserId)
        vendorConfigureJson.put("deviceId", Utils.getUuid())
        vendorConfigureJson.put("chargeMode", ChargeMode.ONCE.chargeMode)
        vendorConfigureJson.put("urlTokenExpireTime", 60 * 15)
        configurationJson.put("vendorConfigure", vendorConfigureJson)
        configurationJson.put("enableLog", true)
        configurationJson.put("enableSaveLogToFile", true)
        configurationJson.put("eventHandler", 11111)
        configurationJson.put(
            "logFilePath",
            BridgeUtils.getContext().getExternalFilesDir(null)?.path
        )

        val result =
            TestWrapper.callApi("MusicContentCenterEx_initialize", configurationJson.toString())
        LogUtils.d("MccExServiceWrapperTest initialize result: $result")

    }

    fun testEnd() {
        var result =
            TestWrapper.callApi(
                "MusicContentCenterEx_destroyMusicPlayer", "{\n" +
                        "\"playerId\": $mPlayerId" +
                        "}"
            )
        LogUtils.d("MccExServiceWrapperTest destroyMusicPlayer result: $result")

        result =
            TestWrapper.callApi("MusicContentCenterEx_unregisterEventHandler", "{}")
        LogUtils.d("MccExServiceWrapperTest unregisterEventHandler result: $result")

        result =
            TestWrapper.callApi("MusicContentCenterEx_unregisterScoreEventHandler", "{}")
        LogUtils.d("MccExServiceWrapperTest unregisterScoreEventHandler result: $result")

        TestWrapper.destroyMccEx()
        TestWrapper.destroyRtcEngine()
    }

    override fun onCallback(callbackName: String, callbackParam: String) {
        LogUtils.d("MccExServiceWrapperTest onCallback callbackName: $callbackName callbackParam: $callbackParam")
        CoroutineScope(Dispatchers.Main).launch {
            val callbackParamJson = JSONObject(callbackParam)
            when (callbackName) {
                "MusicContentCenterExEventHandler_onInitializeResult" -> {
                    var result =
                        TestWrapper.callApi(
                            "MusicContentCenterEx_renewToken", "{\n" +
                                    "\"newToken\": \"${MccExManager.getToken()}\"" +
                                    "}"
                        )
                    LogUtils.d("MccExServiceWrapperTest renewToken result: $result")

                    result =
                        TestWrapper.callApi("MusicContentCenterEx_registerEventHandler", "{}")
                    LogUtils.d("MccExServiceWrapperTest registerEventHandler result: $result")


                    result =
                        TestWrapper.callApi("MusicContentCenterEx_registerScoreEventHandler", "{}")
                    LogUtils.d("MccExServiceWrapperTest registerScoreEventHandler result: $result")

                    result =
                        TestWrapper.callApi(
                            "MusicContentCenterEx_createMusicPlayer", "{}"
                        )
                    LogUtils.d("MccExServiceWrapperTest createMusicPlayer result: $result")
                    var resultJson = JSONObject(result)
                    mPlayerId = resultJson.getInt("result")
                    LogUtils.d("MccExServiceWrapperTest createMusicPlayer playerId: $mPlayerId")

                    result =
                        TestWrapper.callApi(
                            "MediaPlayer_registerPlayerObserver", "{\n" +
                                    "\"playerId\": $mPlayerId\n" +
                                    "}"
                        )
                    LogUtils.d("MccExServiceWrapperTest MediaPlayer_registerPlayerObserver result: $result")

                    result =
                        TestWrapper.callApi(
                            "MusicContentCenterEx_getInternalSongCode", "{\n" +
                                    "\"songId\": \"89398414\"," +
                                    "\"jsonOption\": \"\"" +
                                    "}"
                        )
                    LogUtils.d("MccExServiceWrapperTest getInternalSongCode result: $result")
                    resultJson = JSONObject(result)
                    mSongCode = resultJson.getLong("result")
                    LogUtils.d("MccExServiceWrapperTest getInternalSongCode songCode: $mSongCode")


                    result =
                        TestWrapper.callApi(
                            "MusicContentCenterEx_isPreloaded", "{\n" +
                                    "\"songCode\": $mSongCode" +
                                    "}"
                        )
                    LogUtils.d("MccExServiceWrapperTest isPreloaded result: $result")

                    result =
                        TestWrapper.callApi(
                            "MusicContentCenterEx_preload", "{\n" +
                                    "\"songCode\": $mSongCode" +
                                    "}"
                        )
                    LogUtils.d("MccExServiceWrapperTest preload result: $result")
                }

                "MusicContentCenterExEventHandler_onPreLoadEvent" -> {
                    val percent = callbackParamJson.optInt("percent")
                    if (percent == 100) {
                        var result =
                            TestWrapper.callApi(
                                "MusicContentCenterEx_startScore", "{\n" +
                                        "\"songCode\": $mSongCode" +
                                        "}"
                            )
                        LogUtils.d("MccExServiceWrapperTest startScore result: $result")


                        result =
                            TestWrapper.callApi(
                                "MusicContentCenterEx_getLyric", "{\n" +
                                        "\"songCode\": $mSongCode," +
                                        "\"lyricType\": ${LyricType.KRC.lyricType}" +
                                        "}"
                            )
                        LogUtils.d("MccExServiceWrapperTest MusicContentCenterEx_getLyric result: $result")

                        result =
                            TestWrapper.callApi(
                                "MusicContentCenterEx_getPitch", "{\n" +
                                        "\"songCode\": $mSongCode" +
                                        "}"
                            )
                        LogUtils.d("MccExServiceWrapperTest MusicContentCenterEx_getPitch result: $result")
                    }
                }

                "MusicContentCenterExEventHandler_onStartScoreResult" -> {
                    var result =
                        TestWrapper.callApi(
                            "MusicContentCenterEx_setScoreLevel", "{\n" +
                                    "\"level\": ${ScoreHardLevel.LEVEL4.level}" +
                                    "}"
                        )
                    LogUtils.d("MccExServiceWrapperTest MusicContentCenterEx_setScoreLevel result: $result")

                    result =
                        TestWrapper.callApi(
                            "MusicPlayer_open", "{\n" +
                                    "\"playerId\": $mPlayerId,\n" +
                                    "\"songCode\": $mSongCode,\n" +
                                    "\"startPos\": 0\n" +
                                    "}"
                        )
                    LogUtils.d("MccExServiceWrapperTest MusicPlayer_open result: $result")
                }

                "MediaPlayerObserver_onPlayerStateChanged" -> {
                    var result =
                        TestWrapper.callApi(
                            "MediaPlayer_getState", "{\n" +
                                    "\"playerId\": $mPlayerId\n" +
                                    "}"
                        )
                    LogUtils.d("MccExServiceWrapperTest MediaPlayer_getState result: $result")

                    val state = callbackParamJson.optInt("state")
                    if (state == Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED.ordinal) {
                        result =
                            TestWrapper.callApi(
                                "MusicPlayer_setPlayMode", "{\n" +
                                        "\"playerId\": $mPlayerId," +
                                        "\"mode\": ${MusicPlayMode.MUSIC_PLAY_MODE_ORIGINAL.mode}\n" +
                                        "}"
                            )
                        LogUtils.d("MccExServiceWrapperTest MusicPlayer_setPlayMode result: $result")

                        result =
                            TestWrapper.callApi(
                                "MediaPlayer_setLoopCount", "{\n" +
                                        "\"playerId\": $mPlayerId," +
                                        "\"loopCount\": -1\n" +
                                        "}"
                            )
                        LogUtils.d("MccExServiceWrapperTest MediaPlayer_setLoopCount result: $result")

                        result =
                            TestWrapper.callApi(
                                "MusicPlayer_play", "{\n" +
                                        "\"playerId\": $mPlayerId\n" +
                                        "}"
                            )
                        LogUtils.d("MccExServiceWrapperTest MusicPlayer_play result: $result")

                        result =
                            TestWrapper.callApi(
                                "MediaPlayer_getDuration", "{\n" +
                                        "\"playerId\": $mPlayerId\n" +
                                        "}"
                            )
                        LogUtils.d("MccExServiceWrapperTest MediaPlayer_getDuration result: $result")
                    }
                }

                "MediaPlayerObserver_onPositionChanged" -> {
                    var result =
                        TestWrapper.callApi(
                            "MediaPlayer_getPlayPosition", "{\n" +
                                    "\"playerId\": $mPlayerId\n" +
                                    "}"
                        )
                    LogUtils.d("MccExServiceWrapperTest MediaPlayer_getPlayPosition result: $result")
                    val position = JSONObject(result).optInt("result")

                    if (position in 10001..10080) {
//                        result =
//                            TestWrapper.callApi(
//                                "MediaPlayer_pause", "{\n" +
//                                        "\"playerId\": $mPlayerId\n" +
//                                        "}"
//                            )
//                        LogUtils.d("MccExServiceWrapperTest MediaPlayer_pause result: $result")
//
//                        result =
//                            TestWrapper.callApi(
//                                "MediaPlayer_resume", "{\n" +
//                                        "\"playerId\": $mPlayerId\n" +
//                                        "}"
//                            )
//                        LogUtils.d("MccExServiceWrapperTest MediaPlayer_resume result: $result")

                        //                        result =
//                            TestWrapper.callApi(
//                                "MediaPlayer_stop", "{\n" +
//                                        "\"playerId\": $mPlayerId\n" +
//                                        "}"
//                            )
//                        LogUtils.d("MccExServiceWrapperTest MediaPlayer_stop result: $result")


                        result =
                            TestWrapper.callApi(
                                "MediaPlayer_seek", "{\n" +
                                        "\"playerId\": $mPlayerId,\n" +
                                        "\"newPos\": 20000\n" +
                                        "}"
                            )
                        LogUtils.d("MccExServiceWrapperTest MediaPlayer_seek result: $result")

//                        result =
//                            TestWrapper.callApi(
//                                "MediaPlayer_stop", "{\n" +
//                                        "\"playerId\": $mPlayerId\n" +
//                                        "}"
//                            )
//                        LogUtils.d("MccExServiceWrapperTest MediaPlayer_stop result: $result")
                    }
                }
            }
        }
    }

    fun joinChannel() {
        try {
            val mChannelId =
                io.agora.mccex_demo.utils.Utils.getCurrentDateStr("yyyyMMddHHmmss") + io.agora.mccex_demo.utils.Utils.getRandomString(
                    2
                )
            val ret = NativeMsgBridge.mRtcEngine?.joinChannel(
                KeyCenter.getRtcToken(
                    mChannelId,
                    KeyCenter.getUid()
                ),
                mChannelId,
                KeyCenter.getUid(),
                object : ChannelMediaOptions() {
                    init {
                        publishMicrophoneTrack = true
                        autoSubscribeAudio = true
                        clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
                    }
                })
            LogUtils.d("joinChannel ret:$ret")
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.e("joinChannel error:" + e.message)
        }
    }
}