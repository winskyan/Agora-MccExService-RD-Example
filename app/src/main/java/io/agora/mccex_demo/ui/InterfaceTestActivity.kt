package io.agora.mccex_demo.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.ConfirmPopupView
import com.lxj.xpopup.impl.LoadingPopupView
import io.agora.mccex.IMusicPlayer
import io.agora.mccex.constants.LyricType
import io.agora.mccex.constants.MccExState
import io.agora.mccex.constants.MccExStateReason
import io.agora.mccex.constants.MusicPlayMode
import io.agora.mccex.constants.ScoreHardLevel
import io.agora.mccex.constants.ServiceCode
import io.agora.mccex.constants.ServiceVendor
import io.agora.mccex.model.LineScoreData
import io.agora.mccex.model.RawScoreData
import io.agora.mccex.utils.Utils
import io.agora.mccex_demo.BuildConfig
import io.agora.mccex_demo.R
import io.agora.mccex_demo.agora.MccExManager
import io.agora.mccex_demo.agora.RtcManager
import io.agora.mccex_demo.constants.Constants
import io.agora.mccex_demo.databinding.ActivityInterfaceTestBinding
import io.agora.mccex_demo.model.ItemData
import io.agora.mccex_demo.ui.adapter.ItemAdapter
import io.agora.mccex_demo.utils.LogUtils
import io.agora.mediaplayer.IMediaPlayerObserver
import io.agora.mediaplayer.data.CacheStatistics
import io.agora.mediaplayer.data.PlayerPlaybackStats
import io.agora.mediaplayer.data.PlayerUpdatedInfo
import io.agora.mediaplayer.data.SrcInfo
import io.agora.rtc2.IRtcEngineEventHandler
import java.lang.reflect.Method

class InterfaceTestActivity : AppCompatActivity(), MccExManager.MccExCallback,
    RtcManager.RtcCallback {
    companion object {
        const val TAG: String = Constants.TAG + "-InterfaceTestActivity"
        const val TEST_SONG_ID = "32238252"
        const val TEST_SONG_ID_WITH_OFFSET = "32238252"

        const val TEST_CASE_INDEX_RTC_INIT = 0
        const val TEST_CASE_INDEX_MCC_EX_INITIALIZE = 1
        const val TEST_CASE_INDEX_RTC_JOIN_CHANNEL = 2
        const val TEST_CASE_INDEX_RTC_AUDIO_FRAME_OBSERVER_METHODS = 3
        const val TEST_CASE_INDEX_GET_INTERNAL_SONG_CODE = 4
        const val TEST_CASE_INDEX_IS_PRELOADED = 5
        const val TEST_CASE_INDEX_PRELOAD = 6
        const val TEST_CASE_INDEX_GET_LYRIC = 7
        const val TEST_CASE_INDEX_GET_PITCH = 8
        const val TEST_CASE_INDEX_PRELOAD_GET_LYRIC = 9
        const val TEST_CASE_INDEX_START_SCORE = 10
        const val TEST_CASE_INDEX_SET_SCORE_LEVEL = 11
        const val TEST_CASE_INDEX_PAUSE_SCORE = 12
        const val TEST_CASE_INDEX_RESUME_SCORE = 13
        const val TEST_CASE_INDEX_PLAY_SONG = 14
        const val TEST_CASE_INDEX_RTC_MEDIA_PLAYER_OBSERVER_METHODS = 15
        const val TEST_CASE_INDEX_PLAY_SWITCH_ACCOMPANY = 16
        const val TEST_CASE_INDEX_PLAY_SWITCH_ORIGINAL = 17
        const val TEST_CASE_INDEX_STOP_PLAY_SONG = 18
        const val TEST_CASE_INDEX_TEST_LYRIC_SCORE_RECORDER = 19
        const val TEST_CASE_INDEX_MCC_EX_DESTROY = 20
        const val TEST_CASE_INDEX_RTC_LEAVE_CHANNEL = 21
    }

    private lateinit var binding: ActivityInterfaceTestBinding
    private var mLoadingPopup: LoadingPopupView? = null
    private var mConfirmPopup: ConfirmPopupView? = null
    private val mTestCaseList: MutableList<ItemData> = mutableListOf()
    private var mTestCaseAdapter: ItemAdapter? = null
    private var mOriginalSongCode: Long = 0
    private var mAccompanySongCode: Long = 0

    private var mTestCaseIndex = -1;

    private var mPreloadSuccess = false;
    private var mGetLyricSuccess = false;

    private var mMusicPlayer: IMusicPlayer? = null

    init {
        mTestCaseList.add(
            TEST_CASE_INDEX_RTC_INIT,
            ItemData(
                "testRtcInit",
                nextTestCaseIndex = TEST_CASE_INDEX_MCC_EX_INITIALIZE
            )
        )

        mTestCaseList.add(
            TEST_CASE_INDEX_MCC_EX_INITIALIZE, ItemData(
                "testMccExInitialize",
                nextTestCaseIndex = TEST_CASE_INDEX_RTC_JOIN_CHANNEL
            )
        )

        mTestCaseList.add(
            TEST_CASE_INDEX_RTC_JOIN_CHANNEL, ItemData(
                "testRtcJoinChannel",
                nextTestCaseIndex = TEST_CASE_INDEX_RTC_AUDIO_FRAME_OBSERVER_METHODS
            )
        )

        mTestCaseList.add(
            TEST_CASE_INDEX_RTC_AUDIO_FRAME_OBSERVER_METHODS,
            ItemData(
                "testRtcAudioFrameObserverMethods",
                nextTestCaseIndex = TEST_CASE_INDEX_GET_INTERNAL_SONG_CODE
            )
        )



        mTestCaseList.add(
            TEST_CASE_INDEX_GET_INTERNAL_SONG_CODE,
            ItemData(
                "testGetInternalSongCode", nextTestCaseIndex = TEST_CASE_INDEX_IS_PRELOADED
            )
        )
        mTestCaseList.add(
            TEST_CASE_INDEX_IS_PRELOADED, ItemData(
                "testIsPreloaded", nextTestCaseIndex = TEST_CASE_INDEX_PRELOAD
            )
        )
        mTestCaseList.add(
            TEST_CASE_INDEX_PRELOAD, ItemData(
                "testPreload", nextTestCaseIndex = TEST_CASE_INDEX_GET_LYRIC
            )
        )
        mTestCaseList.add(
            TEST_CASE_INDEX_GET_LYRIC, ItemData(
                "testGetLyric", nextTestCaseIndex = TEST_CASE_INDEX_GET_PITCH
            )
        )
        mTestCaseList.add(
            TEST_CASE_INDEX_GET_PITCH, ItemData(
                "testGetPitch", nextTestCaseIndex = TEST_CASE_INDEX_PRELOAD_GET_LYRIC
            )
        )

        mTestCaseList.add(
            TEST_CASE_INDEX_PRELOAD_GET_LYRIC, ItemData(
                "testPreloadAndGetLyric", nextTestCaseIndex = TEST_CASE_INDEX_START_SCORE
            )
        )

        mTestCaseList.add(
            TEST_CASE_INDEX_START_SCORE, ItemData(
                "testStartScore", nextTestCaseIndex = TEST_CASE_INDEX_SET_SCORE_LEVEL
            )
        )
        mTestCaseList.add(
            TEST_CASE_INDEX_SET_SCORE_LEVEL, ItemData(
                "testSetScoreLevel", nextTestCaseIndex = TEST_CASE_INDEX_PAUSE_SCORE
            )
        )

        mTestCaseList.add(
            TEST_CASE_INDEX_PAUSE_SCORE, ItemData(
                "testPauseScore", nextTestCaseIndex = TEST_CASE_INDEX_RESUME_SCORE
            )
        )
        mTestCaseList.add(
            TEST_CASE_INDEX_RESUME_SCORE, ItemData(
                "testResumeScore", nextTestCaseIndex = TEST_CASE_INDEX_PLAY_SONG
            )
        )

        mTestCaseList.add(
            TEST_CASE_INDEX_PLAY_SONG, ItemData(
                "testPlaySong",
                nextTestCaseIndex = TEST_CASE_INDEX_RTC_MEDIA_PLAYER_OBSERVER_METHODS
            )
        )

        mTestCaseList.add(
            TEST_CASE_INDEX_RTC_MEDIA_PLAYER_OBSERVER_METHODS,
            ItemData(
                "testRtcMediaPlayerObserverMethods",
                nextTestCaseIndex = TEST_CASE_INDEX_PLAY_SWITCH_ACCOMPANY
            )
        )

        mTestCaseList.add(
            TEST_CASE_INDEX_PLAY_SWITCH_ACCOMPANY, ItemData(
                "testPlaySwitchAccompany",
                nextTestCaseIndex = TEST_CASE_INDEX_PLAY_SWITCH_ORIGINAL
            )
        )



        mTestCaseList.add(
            TEST_CASE_INDEX_PLAY_SWITCH_ORIGINAL, ItemData(
                "testPlaySwitchOriginal", nextTestCaseIndex = TEST_CASE_INDEX_STOP_PLAY_SONG
            )
        )

        mTestCaseList.add(
            TEST_CASE_INDEX_STOP_PLAY_SONG, ItemData(
                "testStopPlaySong", nextTestCaseIndex = TEST_CASE_INDEX_TEST_LYRIC_SCORE_RECORDER
            )
        )

        mTestCaseList.add(
            TEST_CASE_INDEX_TEST_LYRIC_SCORE_RECORDER, ItemData(
                "testLyricScoreRecorder", nextTestCaseIndex = TEST_CASE_INDEX_MCC_EX_DESTROY
            )
        )

        mTestCaseList.add(
            TEST_CASE_INDEX_MCC_EX_DESTROY, ItemData(
                "testMccExDestroy", nextTestCaseIndex = TEST_CASE_INDEX_RTC_LEAVE_CHANNEL
            )
        )
        mTestCaseList.add(
            TEST_CASE_INDEX_RTC_LEAVE_CHANNEL, ItemData(
                "testRtcLeaveChannel", nextTestCaseIndex = 0
            )
        )

    }

    private val mMediaPlayerObserver: IMediaPlayerObserver = object : IMediaPlayerObserver {
        //4.1  4.2
//        override fun onPlayerStateChanged(
//            state: io.agora.mediaplayer.Constants.MediaPlayerState,
//            error: io.agora.mediaplayer.Constants.MediaPlayerError
//        ) {
//            LogUtils.d("onPlayerStateChanged: $state $error")
//
//            if (io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED == state) {
//                mMusicPlayer?.setLoopCount(10)
//                mMusicPlayer?.adjustPlayoutVolume(80)
//                mMusicPlayer?.play()
//                runOnUiThread {
//                    mConfirmPopup = XPopup.Builder(this@InterfaceTestActivity)
//                        .asConfirm(
//                            "提示", "是否播放歌曲？",
//                            {
//                                mConfirmPopup?.dismiss()
//                                startNextTestCase()
//                            }, {
//                                mMusicPlayer?.stop()
//                                mMusicPlayer?.destroy()
//                                testFail()
//                            }
//                        )
//                    mConfirmPopup?.show()
//                }
//            }
//            if (io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING == state) {
//
//            } else if (io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PAUSED == state) {
//
//            } else if (io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_STOPPED == state) {
//
//            } else if (io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYBACK_ALL_LOOPS_COMPLETED == state) {
//
//            } else if (io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_FAILED == state) {
//
//            }
//        }

        //4.3 4.4
        override fun onPlayerStateChanged(
            state: io.agora.mediaplayer.Constants.MediaPlayerState,
            reaseon: io.agora.mediaplayer.Constants.MediaPlayerReason
        ) {
            LogUtils.d("onPlayerStateChanged: $state $reaseon")

            if (io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED == state) {
                mMusicPlayer?.setLoopCount(10)
                mMusicPlayer?.adjustPlayoutVolume(80)
                mMusicPlayer?.play()
                runOnUiThread {
                    mConfirmPopup = XPopup.Builder(this@InterfaceTestActivity)
                        .asConfirm(
                            "提示", "是否播放歌曲？",
                            {
                                mConfirmPopup?.dismiss()
                                startNextTestCase()
                            }, {
                                mMusicPlayer?.stop()
                                mMusicPlayer?.destroy()
                                testFail()
                            }
                        )
                    mConfirmPopup?.show()
                }
            }
            if (io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING == state) {

            } else if (io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PAUSED == state) {

            } else if (io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_STOPPED == state) {

            } else if (io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYBACK_ALL_LOOPS_COMPLETED == state) {

            } else if (io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_FAILED == state) {

            }
        }

        //4.2.6.12
//        override fun onPositionChanged(position_ms: Long) {
//
//        }

        //4.1.1.24 4.4
        override fun onPositionChanged(positionMs: Long, timestampMs: Long) {

        }

        override fun onPlayerEvent(
            eventCode: io.agora.mediaplayer.Constants.MediaPlayerEvent?,
            elapsedTime: Long,
            message: String?
        ) {
        }

        override fun onMetaData(
            type: io.agora.mediaplayer.Constants.MediaPlayerMetadataType,
            data: ByteArray
        ) {
        }

        override fun onPlayBufferUpdated(playCachedBuffer: Long) {}
        override fun onPreloadEvent(
            src: String,
            event: io.agora.mediaplayer.Constants.MediaPlayerPreloadEvent
        ) {
        }

        override fun onAgoraCDNTokenWillExpire() {}
        override fun onPlayerSrcInfoChanged(from: SrcInfo, to: SrcInfo) {}
        override fun onPlayerInfoUpdated(info: PlayerUpdatedInfo) {}
        override fun onAudioVolumeIndication(volume: Int) {}

        //4.3 4.4
        override fun onPlayerCacheStats(stats: CacheStatistics?) {

        }

        //4.3 4.4
        override fun onPlayerPlaybackStats(stats: PlayerPlaybackStats?) {

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInterfaceTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        initView()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        initData()
        initView()
    }

    override fun onResume() {
        super.onResume()
        mTestCaseIndex = TEST_CASE_INDEX_RTC_INIT
        startTestCase()
    }


    private fun initData() {
        mLoadingPopup = XPopup.Builder(this@InterfaceTestActivity)
            .asLoading("接口测试中")


    }

    private fun initView() {
        handleOnBackPressed()
        binding.testFinishBtn.isEnabled = false

        binding.testCaseRv.layoutManager =
            LinearLayoutManager(
                this@InterfaceTestActivity,
                LinearLayoutManager.VERTICAL,
                false
            )

        mTestCaseAdapter = ItemAdapter(mTestCaseList)
        mTestCaseAdapter?.setClickable(false)
        binding.testCaseRv.adapter = mTestCaseAdapter
        binding.testCaseRv.addItemDecoration(
            ItemAdapter.MyItemDecoration(
                resources.getDimensionPixelSize(
                    R.dimen.item_space
                )
            )
        )


        binding.testFinishBtn.setOnClickListener {
            finish()
        }

    }

    @Synchronized
    private fun updateTestCaseIndex(index: Int, success: Boolean) {
        LogUtils.d(TAG, "updateTestCaseIndex index:$index success:$success")

        mTestCaseList[index].success = success
        mTestCaseList[index].bgResId = if (success) R.color.green else R.color.red
        mTestCaseAdapter?.notifyItemChanged(index)

        if (!success) {
            mTestCaseIndex = TEST_CASE_INDEX_MCC_EX_DESTROY
            testMccExDestroy()
        }
    }


    private fun updateTestCaseResult() {
        binding.testFinishBtn.isEnabled = true
        var allCaseSuccess = true
        for (item in mTestCaseList) {
            if (!item.success) {
                allCaseSuccess = false
                break
            }
        }
        if (allCaseSuccess) {
            binding.testFinishBtn.text = resources.getString(R.string.test_success)
            binding.testFinishBtn.setBackgroundColor(resources.getColor(R.color.green))
        } else {
            binding.testFinishBtn.text = resources.getString(R.string.test_failed)
            binding.testFinishBtn.setBackgroundColor(resources.getColor(R.color.red))
        }

    }

    private fun handleOnBackPressed() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // nothing to do
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun clearSongCache() {
        Utils.deleteFolder((this@InterfaceTestActivity.externalCacheDir?.path ?: "") + "/song/")
    }


    private fun startTestCase() {
        val testItem = mTestCaseList[mTestCaseIndex]
        LogUtils.d(TAG, "startTestCase index:$mTestCaseIndex testItem:${testItem}")

        try {
            val method: Method =
                InterfaceTestActivity::class.java.getDeclaredMethod(testItem.content)
            method.isAccessible = true
            method.invoke(this)
        } catch (e: Exception) {
            LogUtils.e(TAG, "startTestCase fail:${e}")
            testFail()
        }
    }

    private fun startNextTestCase() {
        runOnUiThread {
            updateTestCaseIndex(mTestCaseIndex, true)
            mTestCaseIndex = mTestCaseList[mTestCaseIndex].nextTestCaseIndex
            LogUtils.d(TAG, "startNextTestCase index:$mTestCaseIndex")
            if (mTestCaseIndex != -1) {
                startTestCase()
            }
        }
    }

    private fun testFail() {
        runOnUiThread {
            updateTestCaseIndex(mTestCaseIndex, false)
        }
    }

    private fun testRtcInit() {
        LogUtils.d(TAG, "testRtcInit")
        RtcManager.setTestInterfaceMode(true)
        RtcManager.initRtcEngine(this, this)
        startNextTestCase()
    }

    private fun testRtcJoinChannel() {
        LogUtils.d(TAG, "testRtcJoinChannel")
        RtcManager.joinChannel()
    }

    private fun testRtcAudioFrameObserverMethods() {
        LogUtils.d(TAG, "testRtcAudioFrameObserverMethods")
        try {
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                val methods = RtcManager.getAudioFrameObserverMethods()
                if (methods.isEmpty()) {
                    startNextTestCase()
                } else {
                    LogUtils.i(TAG, "testRtcAudioFrameObserverMethods methods:${methods}")
                    testFail()
                }
            }, 2000)
        } catch (e: Exception) {
            LogUtils.e(TAG, "testRtcAudioFrameObserverMethods fail:${e}")
            testFail()
        }

    }

    private fun testRtcMediaPlayerObserverMethods() {
        LogUtils.d(TAG, "testRtcMediaPlayerObserverMethods")
        try {
            mMusicPlayer?.seek(1234)
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                val methods = MccExManager.getMediaPlayerObserverMethods()
                if (methods.isEmpty()) {
                    startNextTestCase()
                } else {
                    LogUtils.i(TAG, "testRtcMediaPlayerObserverMethods methods:${methods}")
                    testFail()
                }
            }, 2000)
        } catch (e: Exception) {
            LogUtils.e(TAG, "testRtcMediaPlayerObserverMethods fail:${e}")
            testFail()
        }
    }


    private fun testMccExInitialize() {
        RtcManager.getRtcEngine()?.let {
            MccExManager.setTestInterfaceMode(true)
            MccExManager.initMccExService(
                it,
                RtcManager,
                applicationContext,
                this
            )
        } ?: run {
            testFail()
        }
    }

    private fun testGetInternalSongCode() {
        val songCode = MccExManager.getInternalSongCode(TEST_SONG_ID, null)
        if (songCode > 0) {
            mOriginalSongCode =
                MccExManager.getInternalSongCode(TEST_SONG_ID, "{\"format\":{\"highPart\":0}}")
            mAccompanySongCode =
                MccExManager.getInternalSongCode(TEST_SONG_ID, "{\"format\":{\"highPart\":1}}")

            val invalidSongCode =
                MccExManager.getInternalSongCode(TEST_SONG_ID, "{\"format\":{\"222\"}}")
            if (mOriginalSongCode > 0 && mAccompanySongCode > 0 && invalidSongCode == 0L) {
                startNextTestCase()
            } else {
                testFail()
            }
        } else {
            testFail()
        }
    }

    private fun testIsPreloaded() {
        clearSongCache()
        mOriginalSongCode = MccExManager.getInternalSongCode(TEST_SONG_ID, null)
        val isPreloadedWithOriginalSongCode = MccExManager.isPreloaded(mOriginalSongCode)
        val isPreloadedWithAccompanySongCode = MccExManager.isPreloaded(mAccompanySongCode)
        val isPreloadedWithInvalidSongCode = MccExManager.isPreloaded(0)
        if (isPreloadedWithOriginalSongCode != 0 && isPreloadedWithAccompanySongCode != 0 && isPreloadedWithInvalidSongCode == ServiceCode.INVALID_ARGUMENT.code) {
            startNextTestCase()
        } else {
            testFail()
        }
    }

    private fun testPreload() {
        val invalidRequestId = MccExManager.preload(0)
        if (invalidRequestId.isNullOrEmpty()) {
            mOriginalSongCode = MccExManager.getInternalSongCode(TEST_SONG_ID, null)
            val requestId = MccExManager.preload(mOriginalSongCode)
            if (requestId.isNullOrEmpty()) {
                testFail()
            }
        } else {
            testFail()
        }
    }

    private fun testGetLyric() {
        clearSongCache()
        mOriginalSongCode = MccExManager.getInternalSongCode(TEST_SONG_ID_WITH_OFFSET, null)
        val requestId = MccExManager.getLyric(mOriginalSongCode, LyricType.KRC)
        if (requestId.isEmpty()) {
            testFail()
        }
    }

    private fun testGetPitch() {
        clearSongCache()
        mOriginalSongCode = MccExManager.getInternalSongCode(TEST_SONG_ID, null)
        val requestId = MccExManager.getPitch(mOriginalSongCode)
        if (requestId.isEmpty()) {
            testFail()
        }
    }

    private fun testPreloadAndGetLyric() {
        mPreloadSuccess = false
        mGetLyricSuccess = false
        clearSongCache()
        mOriginalSongCode = MccExManager.getInternalSongCode(TEST_SONG_ID, null)
        val preloadRequestId = MccExManager.preload(mOriginalSongCode)
        if (preloadRequestId != null) {
            if (preloadRequestId.isNotEmpty()) {
                val lyricRequestId = MccExManager.getLyric(mOriginalSongCode, LyricType.KRC)
                if (lyricRequestId.isEmpty()) {
                    testFail()
                }
            }
        }
    }

    private fun testStartScore() {
        clearSongCache()
        mOriginalSongCode = MccExManager.getInternalSongCode(TEST_SONG_ID, null)
        MccExManager.preload(mOriginalSongCode)
    }

    private fun testSetScoreLevel() {
        val ret = MccExManager.setScoreLevel(ScoreHardLevel.LEVEL5)
        if (ret == 0) {
            startNextTestCase()
        } else {
            testFail()
        }
    }

    private fun testPauseScore() {
        val ret = MccExManager.pauseScore()
        if (ret == 0) {

            startNextTestCase()
        } else {
            testFail()
        }
    }

    private fun testResumeScore() {
        val ret = MccExManager.resumeScore()
        if (ret == 0) {

            startNextTestCase()
        } else {
            testFail()
        }
    }

    private fun testPlaySong() {
        LogUtils.d(TAG, "testPlaySong")
        clearSongCache()
        mOriginalSongCode =
            MccExManager.getInternalSongCode(TEST_SONG_ID, "{\"format\":{\"highPart\":1}}")
        if (mOriginalSongCode != 0L) {
            val preloadRequestId = MccExManager.preload(mOriginalSongCode)
            if (preloadRequestId.isNullOrEmpty()) {
                testFail()
            } else {
                mMusicPlayer = MccExManager.createMusicPlayer()
                mMusicPlayer?.registerPlayerObserver(mMediaPlayerObserver)
                    ?: testFail()
            }
        } else {
            testFail()
        }
    }

    private fun testPlaySwitchAccompany() {
        LogUtils.d(TAG, "testPlaySwitchAccompany")
        mMusicPlayer?.setPlayMode(MusicPlayMode.MUSIC_PLAY_MODE_ACCOMPANY)

        runOnUiThread {
            mConfirmPopup = XPopup.Builder(this@InterfaceTestActivity)
                .asConfirm(
                    "提示", "是否播放伴唱？",
                    {
                        mConfirmPopup?.dismiss()
                        startNextTestCase()
                    }, {
                        mMusicPlayer?.stop()
                        mMusicPlayer?.destroy()
                        testFail()
                    }
                )
            mConfirmPopup?.show()
        }
    }

    private fun testPlaySwitchOriginal() {
        LogUtils.d(TAG, "testPlaySwitchOriginal")
        mMusicPlayer?.setPlayMode(MusicPlayMode.MUSIC_PLAY_MODE_ORIGINAL)

        runOnUiThread {
            mConfirmPopup = XPopup.Builder(this@InterfaceTestActivity)
                .asConfirm(
                    "提示", "是否播放原唱？",
                    {
                        mConfirmPopup?.dismiss()
                        startNextTestCase()
                    }, {
                        mMusicPlayer?.stop()
                        mMusicPlayer?.destroy()
                        testFail()
                    }
                )
            mConfirmPopup?.show()
        }
    }

    private fun testStopPlaySong() {
        LogUtils.d(TAG, "testStopPlaySong")
        mMusicPlayer?.stop()

        runOnUiThread {
            mConfirmPopup = XPopup.Builder(this@InterfaceTestActivity)
                .asConfirm(
                    "提示", "是否停止播放？",
                    {
                        mConfirmPopup?.dismiss()
                        startNextTestCase()
                    }, {
                        mMusicPlayer?.stop()
                        mMusicPlayer?.destroy()
                        testFail()
                    }
                )
            mConfirmPopup?.show()
        }
    }

    private fun testLyricScoreRecorder() {
        LogUtils.d("testLyricScoreRecorder")
        var testResult = false
        if (BuildConfig.IS_WRAPPER_MODE) {
            startNextTestCase()
        } else {
            mOriginalSongCode =
                MccExManager.getInternalSongCode(TEST_SONG_ID, "{\"format\":{\"highPart\":1}}")
            if (mOriginalSongCode != 0L) {
                val preloadRequestId = MccExManager.preload(mOriginalSongCode)
                if (preloadRequestId.isNullOrEmpty()) {
                    testFail()
                }
            } else {
                testFail()
            }
        }
    }

    private fun testMccExDestroy() {
        if (MccExManager.hasInitialized()) {
            runOnUiThread {
                MccExManager.destroy()
                startNextTestCase()
            }
        }
    }

    private fun testRtcLeaveChannel() {
        RtcManager.leaveChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onInitializeResult(state: MccExState, reason: MccExStateReason) {
        super.onInitializeResult(state, reason)
        if (state == MccExState.INITIALIZE_STATE_COMPLETED) {
            startNextTestCase()
        } else {
            testFail()
        }
    }

    override fun onPreLoadEvent(
        requestId: String,
        songCode: Long,
        percent: Int,
        lyricPath: String,
        pitchPath: String,
        musicPath: String,
        songOffsetBegin: Int,
        songOffsetEnd: Int,
        lyricOffset: Int,
        state: MccExState,
        reason: MccExStateReason
    ) {
        super.onPreLoadEvent(
            requestId,
            songCode,
            percent,
            lyricPath,
            pitchPath,
            musicPath,
            songOffsetBegin,
            songOffsetEnd,
            lyricOffset,
            state,
            reason
        )

        if (state == MccExState.PRELOAD_STATE_COMPLETED && percent == 100) {
            if (songCode == mOriginalSongCode && lyricPath.isNotEmpty() && pitchPath.isNotEmpty()) {
                if (MccExManager.isPreloaded(songCode) == 0) {
                    if (mTestCaseIndex == TEST_CASE_INDEX_PRELOAD) {
                        startNextTestCase()
                    } else if (mTestCaseIndex == TEST_CASE_INDEX_START_SCORE) {
                        runOnUiThread {
                            MccExManager.startScore(songCode)
                        }
                    } else if (mTestCaseIndex == TEST_CASE_INDEX_PRELOAD_GET_LYRIC) {
                        mPreloadSuccess = true
                        if (mGetLyricSuccess) {
                            startNextTestCase()
                        }
                    } else if (mTestCaseIndex == TEST_CASE_INDEX_PLAY_SONG) {
                        mMusicPlayer?.open(songCode, 0)
                    } else if (mTestCaseIndex == TEST_CASE_INDEX_TEST_LYRIC_SCORE_RECORDER) {
                        if (!BuildConfig.IS_WRAPPER_MODE) {
                            try {
                                val className =
                                    "io.agora.mccex_demo.utils.InternalTestCase" // 替换成实际的包名和类名
                                val clazz = Class.forName(className)
                                val instance = clazz.getField("INSTANCE").get(null) // 获取单例对象

                                val methodName = "initLyricData"
                                val method = clazz.getDeclaredMethod(
                                    methodName,
                                    Long::class.java,
                                    String::class.java,
                                    ServiceVendor::class.java
                                )
                                method.invoke(
                                    instance, mOriginalSongCode,
                                    lyricPath,
                                    ServiceVendor.YSD
                                ) // 调用方法


                                val testLyricScoreRecorderMethodName = "testLyricScoreRecorder"
                                val testLyricScoreRecorderMethod = clazz.getDeclaredMethod(
                                    testLyricScoreRecorderMethodName
                                )
                                val testResult = testLyricScoreRecorderMethod.invoke(
                                    instance
                                ) as Boolean// 调用方法
                                if (testResult) {
                                    startNextTestCase()
                                } else {
                                    testFail()
                                }
                            } catch (e: Exception) {
                                LogUtils.e(TAG, "testPlaySong fail:${e}")
                            }
                        }
                    } else {
                        testFail()
                    }
                }
            } else {
                testFail()
            }
        }
    }

    override fun onLyricResult(
        requestId: String,
        songCode: Long,
        lyricPath: String,
        songOffsetBegin: Int,
        songOffsetEnd: Int,
        lyricOffset: Int,
        reason: MccExStateReason
    ) {
        super.onLyricResult(
            requestId,
            songCode,
            lyricPath,
            songOffsetBegin,
            songOffsetEnd,
            lyricOffset,
            reason
        )
        if (songCode == mOriginalSongCode && lyricPath.isNotEmpty()) {
            if (mTestCaseIndex == TEST_CASE_INDEX_PRELOAD_GET_LYRIC) {
                mGetLyricSuccess = true
                if (mPreloadSuccess) {
                    startNextTestCase()
                }
            } else {
                if (lyricOffset != 0) {
                    startNextTestCase()
                } else {
                    testFail()
                }
            }
        } else {
            testFail()
        }
    }

    override fun onPitchResult(
        requestId: String,
        songCode: Long,
        pitchPath: String,
        songOffsetBegin: Int,
        songOffsetEnd: Int,
        reason: MccExStateReason
    ) {
        super.onPitchResult(requestId, songCode, pitchPath, songOffsetBegin, songOffsetEnd, reason)

        if (songCode == mOriginalSongCode && pitchPath.isNotEmpty()) {
            startNextTestCase()
        } else {
            testFail()
        }
    }


    override fun onStartScoreResult(
        songCode: Long,
        state: MccExState,
        reason: MccExStateReason
    ) {
        super.onStartScoreResult(songCode, state, reason)
        if (songCode == mOriginalSongCode && state == MccExState.START_SCORE_STATE_COMPLETED) {
            startNextTestCase()
        } else {
            testFail()
        }
    }

    override fun onPitch(songCode: Long, data: RawScoreData) {

    }

    override fun onLineScore(songCode: Long, value: LineScoreData) {

    }

    override fun onMusicPositionChange(position: Long) {

    }

    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        LogUtils.d(TAG, "onJoinChannelSuccess channel:$channel uid:$uid elapsed:$elapsed")
        startNextTestCase()
    }

    override fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats) {
        LogUtils.d(TAG, "onLeaveChannel")

        runOnUiThread {
            RtcManager.destroy()
            updateTestCaseIndex(mTestCaseIndex, true)
            updateTestCaseResult()
        }
    }
}





