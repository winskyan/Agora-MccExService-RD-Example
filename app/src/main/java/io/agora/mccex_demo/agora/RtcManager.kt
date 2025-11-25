package io.agora.mccex_demo.agora

import android.content.Context
import io.agora.mccex_demo.utils.KeyCenter
import io.agora.mccex_demo.utils.LogUtils
import io.agora.mccex_demo.utils.Utils
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IAudioFrameObserver
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.audio.AudioParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale

object RtcManager : IAudioFrameObserver {
    private var mRtcEngine: RtcEngine? = null
    private var mCallback: RtcCallback? = null
    private var mChannelId: String = ""
    private var mTime: String = ""
    private const val SAVE_AUDIO_RECORD_PCM = false
    private var mTestInterfaceMode = false;
    private val mAudioFrameObserverMethods = mutableListOf<String>()
    private var mClientRole = Constants.CLIENT_ROLE_BROADCASTER


    fun initRtcEngine(context: Context, rtcCallback: RtcCallback) {
        mCallback = rtcCallback
        try {
            LogUtils.d("RtcEngine version:" + RtcEngine.getSdkVersion())
            val rtcEngineConfig = RtcEngineConfig()
            rtcEngineConfig.mContext = context
            rtcEngineConfig.mAppId = KeyCenter.APP_ID
            rtcEngineConfig.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            rtcEngineConfig.mEventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    LogUtils.d("onJoinChannelSuccess channel:$channel uid:$uid elapsed:$elapsed")
                    mCallback?.onJoinChannelSuccess(channel, uid, elapsed)
                }

                override fun onLeaveChannel(stats: RtcStats) {
                    LogUtils.d("onLeaveChannel")
                    mCallback?.onLeaveChannel(stats)
                }

                override fun onLocalAudioStateChanged(state: Int, error: Int) {
                    super.onLocalAudioStateChanged(state, error)
                    LogUtils.d("onLocalAudioStateChanged state:$state error:$error")
                    if (Constants.LOCAL_AUDIO_STREAM_STATE_RECORDING == state) {
                        mCallback?.onUnMuteSuccess()
                    } else if (Constants.LOCAL_AUDIO_STREAM_STATE_STOPPED == state) {
                        mCallback?.onMuteSuccess()
                    }
                }

                override fun onAudioVolumeIndication(
                    speakers: Array<out AudioVolumeInfo>?,
                    totalVolume: Int
                ) {
                    super.onAudioVolumeIndication(speakers, totalVolume)
                    mCallback?.onAudioVolumeIndication(speakers, totalVolume)
                }
            }
            rtcEngineConfig.mAudioScenario = Constants.AUDIO_SCENARIO_CHORUS
            mRtcEngine = RtcEngine.create(rtcEngineConfig)
            LogUtils.d("mRtcEngine native handler:${mRtcEngine?.nativeHandle}")

            mRtcEngine?.setParameters("{\"rtc.enable_debug_log\":true}")

            mRtcEngine?.enableAudio()
            mRtcEngine?.disableVideo()

            mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)

//            mRtcEngine?.setRecordingAudioFrameParameters(
//                16000,
//                1,
//                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY,
//                640
//            )

            if (mTestInterfaceMode) {
                Utils.getAllInterfaceMethodNames(IAudioFrameObserver::class.java)
                    .forEach { methodName ->
                        mAudioFrameObserverMethods.add(methodName)
                    }
                LogUtils.d("mAudioFrameObserverMethods:$mAudioFrameObserverMethods")

                // not test
                mAudioFrameObserverMethods.remove("onPublishAudioFrame")
                mAudioFrameObserverMethods.remove("onPlaybackAudioFrameBeforeMixing")
                mAudioFrameObserverMethods.remove("getEarMonitoringAudioParams")
                mAudioFrameObserverMethods.remove("getMixedAudioParams")
                mAudioFrameObserverMethods.remove("getObservedAudioFramePosition")
                mAudioFrameObserverMethods.remove("getPlaybackAudioParams")
                mAudioFrameObserverMethods.remove("getRecordAudioParams")
                mAudioFrameObserverMethods.remove("getPublishAudioParams")

                mRtcEngine?.setPlaybackAudioFrameParameters(
                    16000,
                    1,
                    Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY,
                    640
                )

                mRtcEngine?.setMixedAudioFrameParameters(
                    16000,
                    1,
                    640
                )

                mRtcEngine?.enableInEarMonitoring(
                    true,
                    Constants.EAR_MONITORING_FILTER_BUILT_IN_AUDIO_FILTERS
                )
                mRtcEngine?.setEarMonitoringAudioFrameParameters(
                    16000,
                    1,
                    Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY,
                    640
                )
            }

            //min 50ms
            mRtcEngine?.enableAudioVolumeIndication(
                50,
                3,
                true
            )
            LogUtils.d("initRtcEngine success")
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.e("initRtcEngine error:" + e.message)
        }
    }

    fun joinChannel() {
        try {
            mChannelId = Utils.getCurrentDateStr("yyyyMMddHHmmss") + Utils.getRandomString(2)
            //mChannelId = "test-mccex"
            val ret = mRtcEngine?.joinChannel(
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
                        clientRoleType = mClientRole
                    }
                })
            LogUtils.d("joinChannel ret:$ret")
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.e("joinChannel error:" + e.message)
        }
    }

    override fun onRecordAudioFrame(
        channelId: String?,
        type: Int,
        samplesPerChannel: Int,
        bytesPerSample: Int,
        channels: Int,
        samplesPerSec: Int,
        buffer: ByteBuffer?,
        renderTimeMs: Long,
        avsync_type: Int
    ): Boolean {
        CoroutineScope(Dispatchers.IO).launch {
            if (mTestInterfaceMode) {
                if (mAudioFrameObserverMethods.contains("onRecordAudioFrame")) {
                    mAudioFrameObserverMethods.remove("onRecordAudioFrame")
                }
            }
            val length = buffer!!.remaining()
            val origin = ByteArray(length)
            buffer[origin]
            buffer.flip()
            if (SAVE_AUDIO_RECORD_PCM) {
                try {
                    val fos = FileOutputStream(
                        "/sdcard/Android/Data/io.agora.mccex_demo/cache/audio_" + mTime + ".pcm",
                        true
                    )
                    fos.write(origin)
                    fos.close()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }
        return true
    }

    override fun onPlaybackAudioFrame(
        channelId: String?,
        type: Int,
        samplesPerChannel: Int,
        bytesPerSample: Int,
        channels: Int,
        samplesPerSec: Int,
        buffer: ByteBuffer?,
        renderTimeMs: Long,
        avsync_type: Int
    ): Boolean {
        if (mTestInterfaceMode) {
            if (mAudioFrameObserverMethods.contains("onPlaybackAudioFrame")) {
                mAudioFrameObserverMethods.remove("onPlaybackAudioFrame")
            }
        }
        return true
    }

    override fun onMixedAudioFrame(
        channelId: String?,
        type: Int,
        samplesPerChannel: Int,
        bytesPerSample: Int,
        channels: Int,
        samplesPerSec: Int,
        buffer: ByteBuffer?,
        renderTimeMs: Long,
        avsync_type: Int
    ): Boolean {
        if (mTestInterfaceMode) {
            if (mAudioFrameObserverMethods.contains("onMixedAudioFrame")) {
                mAudioFrameObserverMethods.remove("onMixedAudioFrame")
            }
        }

        return true
    }

    override fun onEarMonitoringAudioFrame(
        type: Int,
        samplesPerChannel: Int,
        bytesPerSample: Int,
        channels: Int,
        samplesPerSec: Int,
        buffer: ByteBuffer?,
        renderTimeMs: Long,
        avsync_type: Int
    ): Boolean {
        if (mTestInterfaceMode) {
            if (mAudioFrameObserverMethods.contains("onEarMonitoringAudioFrame")) {
                mAudioFrameObserverMethods.remove("onEarMonitoringAudioFrame")
            }
        }
        return true
    }

    //4.1.1.24
//    override fun onPlaybackAudioFrameBeforeMixing(
//        channelId: String?,
//        userId: Int,
//        type: Int,
//        samplesPerChannel: Int,
//        bytesPerSample: Int,
//        channels: Int,
//        samplesPerSec: Int,
//        buffer: ByteBuffer?,
//        renderTimeMs: Long,
//        avsync_type: Int
//    ): Boolean {
//        if (mTestInterfaceMode) {
//            if (mAudioFrameObserverMethods.contains("onPlaybackAudioFrameBeforeMixing")) {
//                mAudioFrameObserverMethods.remove("onPlaybackAudioFrameBeforeMixing")
//            }
//        }
//        return true
//    }

    //4.2.6.12  4.3.1.132 4.4.1
    override fun onPlaybackAudioFrameBeforeMixing(
        channelId: String?,
        userId: Int,
        type: Int,
        samplesPerChannel: Int,
        bytesPerSample: Int,
        channels: Int,
        samplesPerSec: Int,
        buffer: ByteBuffer?,
        renderTimeMs: Long,
        avsync_type: Int,
        rtpTimestamp: Int
    ): Boolean {
        if (mTestInterfaceMode) {
            if (mAudioFrameObserverMethods.contains("onPlaybackAudioFrameBeforeMixing")) {
                mAudioFrameObserverMethods.remove("onPlaybackAudioFrameBeforeMixing")
            }
        }
        return true
    }


    //4.1.1.24
//    override fun onPublishAudioFrame(
//        channelId: String?,
//        type: Int,
//        samplesPerChannel: Int,
//        bytesPerSample: Int,
//        channels: Int,
//        samplesPerSec: Int,
//        buffer: ByteBuffer?,
//        renderTimeMs: Long,
//        avsync_type: Int
//    ): Boolean {
//        if (mTestInterfaceMode) {
//            if (mAudioFrameObserverMethods.contains("onPublishAudioFrame")) {
//                mAudioFrameObserverMethods.remove("onPublishAudioFrame")
//            }
//        }
//        return true
//    }


    override fun getObservedAudioFramePosition(): Int {
        if (mTestInterfaceMode) {
            if (mAudioFrameObserverMethods.size > 0) {
                if (mAudioFrameObserverMethods.contains("getObservedAudioFramePosition")) {
                    mAudioFrameObserverMethods.remove("getObservedAudioFramePosition")
                }
            }
        }
        return 0
    }

    override fun getRecordAudioParams(): AudioParams {
        if (mTestInterfaceMode) {
            if (mAudioFrameObserverMethods.contains("getRecordAudioParams")) {
                mAudioFrameObserverMethods.remove("getRecordAudioParams")
            }
        }
        return AudioParams(0, 0, 0, 0)
    }

    override fun getPlaybackAudioParams(): AudioParams {
        if (mTestInterfaceMode) {
            if (mAudioFrameObserverMethods.contains("getPlaybackAudioParams")) {
                mAudioFrameObserverMethods.remove("getPlaybackAudioParams")
            }
        }
        return AudioParams(0, 0, 0, 0)
    }

    override fun getMixedAudioParams(): AudioParams {
        if (mTestInterfaceMode) {
            if (mAudioFrameObserverMethods.contains("getMixedAudioParams")) {
                mAudioFrameObserverMethods.remove("getMixedAudioParams")
            }
        }
        return AudioParams(0, 0, 0, 0)
    }

    override fun getEarMonitoringAudioParams(): AudioParams {
        if (mTestInterfaceMode) {
            if (mAudioFrameObserverMethods.contains("getEarMonitoringAudioParams")) {
                mAudioFrameObserverMethods.remove("getEarMonitoringAudioParams")
            }
        }
        return AudioParams(0, 0, 0, 0)
    }

    //4.1.1.24
//    override fun getPublishAudioParams(): AudioParams {
//        if (mTestInterfaceMode) {
//            if (mAudioFrameObserverMethods.contains("getPublishAudioParams")) {
//                mAudioFrameObserverMethods.remove("getPublishAudioParams")
//            }
//        }
//        return AudioParams(0, 0, 0, 0)
//    }


    fun mute(enable: Boolean) {
        val ret = mRtcEngine?.enableLocalAudio(!enable)
        LogUtils.d("mute enable:$enable ret:$ret")
        if (SAVE_AUDIO_RECORD_PCM) {
            if (!enable) {
                val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                mTime = format.format(System.currentTimeMillis())
            }
        }
    }

    fun leaveChannel() {
        LogUtils.d("RtcManager leaveChannel")
        mRtcEngine?.leaveChannel()
    }

    fun destroy() {
        LogUtils.d("RtcManager destroy")
        RtcEngine.destroy()
    }

    fun switchClientRole() {
        mClientRole =
            if (mClientRole == Constants.CLIENT_ROLE_BROADCASTER) Constants.CLIENT_ROLE_AUDIENCE else Constants.CLIENT_ROLE_BROADCASTER
        mRtcEngine?.setClientRole(mClientRole)
    }


    fun getChannelId(): String {
        return mChannelId
    }

    fun getRtcEngine(): RtcEngine? {
        return mRtcEngine
    }

    fun setTestInterfaceMode(testInterfaceMode: Boolean) {
        mTestInterfaceMode = testInterfaceMode
    }

    fun getAudioFrameObserverMethods(): MutableList<String> {
        return mAudioFrameObserverMethods
    }

    fun updatePublishMediaPlayerOption(playerId: Int) {
        val options = ChannelMediaOptions()
        options.publishMediaPlayerId = playerId
        options.publishMediaPlayerAudioTrack = true
        mRtcEngine?.updateChannelMediaOptions(options)
    }

    interface RtcCallback {
        fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int)
        fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats)
        fun onMuteSuccess() {

        }

        fun onUnMuteSuccess() {

        }

        fun onAudioVolumeIndication(
            speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
            totalVolume: Int
        ) {

        }
    }
}
