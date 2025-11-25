package io.agora.mccex_demo.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.enums.PopupStatus
import com.lxj.xpopup.impl.LoadingPopupView
import io.agora.karaoke_view_ex.KaraokeEvent
import io.agora.karaoke_view_ex.KaraokeView
import io.agora.karaoke_view_ex.internal.model.LyricsLineModel
import io.agora.karaoke_view_ex.model.LyricModel
import io.agora.karaoke_view_ex.utils.LyricsCutter
import io.agora.mccex.IMusicContentCenterEx
import io.agora.mccex.constants.LyricType
import io.agora.mccex.constants.MccExState
import io.agora.mccex.constants.MccExStateReason
import io.agora.mccex.constants.MusicPlayMode
import io.agora.mccex.model.LineScoreData
import io.agora.mccex.model.RawScoreData
import io.agora.mccex.utils.EncryptUtils
import io.agora.mccex_demo.BuildConfig
import io.agora.mccex_demo.R
import io.agora.mccex_demo.agora.MccExManager
import io.agora.mccex_demo.agora.RtcManager
import io.agora.mccex_demo.constants.Constants
import io.agora.mccex_demo.databinding.ActivityMainBinding
import io.agora.mccex_demo.model.ItemData
import io.agora.mccex_demo.model.SongInfo
import io.agora.mccex_demo.net.NetworkClient
import io.agora.mccex_demo.ui.adapter.ItemAdapter
import io.agora.mccex_demo.utils.KeyCenter
import io.agora.mccex_demo.utils.LogUtils
import io.agora.mccex_demo.utils.MccExKeys
import io.agora.mccex_demo.utils.SharedPreferencesUtils
import io.agora.mccex_demo.utils.ToastUtils
import io.agora.mccex_demo.utils.Utils
import io.agora.rtc2.IRtcEngineEventHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), MccExManager.MccExCallback, RtcManager.RtcCallback {
    companion object {
        const val TAG: String = Constants.TAG + "-MainActivity"
        const val MY_PERMISSIONS_REQUEST_CODE = 123
    }

    private lateinit var binding: ActivityMainBinding
    private var mLoadingPopup: LoadingPopupView? = null
    private var mJoinSuccess = false
    private val mSongCacheList: MutableList<SongInfo> = mutableListOf()
    private val mCoroutineScope = CoroutineScope(Dispatchers.IO)
    private var mSongAdapter: ItemAdapter? = null
    private val mSongList: MutableList<ItemData> = mutableListOf()
    private var mKaraokeView: KaraokeView? = null
    private var mLyricsModel: LyricModel? = null
    private var mSongFull: Boolean = true

    private var mCurrentAudioPitch = 0
    private var mIncreaseAudioPitch = true
    private var mCurrentPlayingPosition: Int = -1  // 当前正在播放的歌曲位置
    private val mLastUpdateProgress = mutableMapOf<Int, Int>()  // 记录每首歌上次更新的进度

    init {
        initSongList()
    }

    private fun initSongList() {
        mSongCacheList.clear()
        mSongCacheList.add(
            SongInfo(
                "112643951",
                "{\"format\":{\"highPart\":2}}",
                "Test-ACCOMPANY_FRAGMENT",
                "Test-ACCOMPANY_FRAGMENT",
                0L,
                "",
                ""
            )
        )

        mSongCacheList.add(
            SongInfo(
                "591007605",
                "{\"format\":{\"highPart\":0}}",
                "Test-no-pitch",
                "Test-no-pitch",
                0L,
                "",
                ""
            )
        )

        mSongCacheList.add(
            SongInfo(
                "658153140",
                "{\"format\":{\"highPart\":2}}",
                "Test-no-accompany",
                "Test-no-accompany",
                0L,
                "",
                ""
            )
        )

        mSongCacheList.add(
            SongInfo(
                "32077008",
                "{\"format\":{\"highPart\":0}}",
                "Test-accompany-m4a",
                "Test-accompany-m4a",
                0L,
                "",
                ""
            )
        )

        mSongCacheList.add(
            SongInfo(
                "346598690",
                "{\"format\":{\"highPart\":0}}",
                "Test-accompany-mp3",
                "Test-accompany-mp3",
                0L,
                "",
                ""
            )
        )

        mSongCacheList.add(
            SongInfo(
                "276577308",
                "{\"format\":{\"highPart\":2}}",
                "Test-no-accompany",
                "Test-no-accompany",
                0L,
                "",
                ""
            )
        )

        mSongCacheList.add(
            SongInfo(
                "525442393",
                "{\"format\":{\"highPart\":0}}",
                "Test-Fail",
                "Test-Fail",
                0L,
                "",
                ""
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPermissions()
        initData()
        initView()
    }

    override fun onResume() {
        super.onResume()
        isNetworkConnected();
        initParams()
    }

    private fun checkPermissions() {
        val permissions =
            arrayOf(Manifest.permission.RECORD_AUDIO)
        if (EasyPermissions.hasPermissions(this, *permissions)) {
            // 已经获取到权限，执行相应的操作
        } else {
            EasyPermissions.requestPermissions(
                this,
                "需要录音权限",
                MY_PERMISSIONS_REQUEST_CODE,
                *permissions
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        // 权限被授予，执行相应的操作
        LogUtils.d(TAG, "onPermissionsGranted requestCode:$requestCode perms:$perms")
    }

    fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        LogUtils.d(TAG, "onPermissionsDenied requestCode:$requestCode perms:$perms")
        // 权限被拒绝，显示一个提示信息
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            // 如果权限被永久拒绝，可以显示一个对话框引导用户去应用设置页面手动授权
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    private fun initData() {
        mLoadingPopup = XPopup.Builder(this@MainActivity)
            .hasBlurBg(true)
            .asLoading("正在加载中")
    }

    private fun initView() {
        handleOnBackPressed()
        enableView(false)

        if (mSongFull) {
            binding.radioSongFull.isChecked = true
        } else {
            binding.radioSongHighPart.isChecked = true
        }

        val versionName = applicationContext.packageManager.getPackageInfo(
            applicationContext.packageName,
            0
        ).versionName

        binding.versionTv.text = """
            Demo Version: $versionName
            SDK Version: ${IMusicContentCenterEx.getSdkVersion()}
        """.trimIndent()

        binding.radioSongFull.setOnCheckedChangeListener { _, isChecked ->
            mSongFull = isChecked
            initParams()
        }

        binding.radioSongHighPart.setOnCheckedChangeListener { _, isChecked ->
            mSongFull = !isChecked
            initParams()
        }


        binding.playBtn.setOnClickListener {
            if (MccExManager.isMusicPlaying()) {
                MccExManager.pause()
            } else if (MccExManager.isMusicPause()) {
                MccExManager.resume()
            }
        }

        binding.stopBtn.setOnClickListener {
            MccExManager.stop()
            // 重置播放状态
            if (mCurrentPlayingPosition >= 0 && mCurrentPlayingPosition < mSongList.size) {
                mSongList[mCurrentPlayingPosition].isPlaying = false
                mSongAdapter?.updateDataChanged()
                mCurrentPlayingPosition = -1
            }
        }

        binding.playModeBtn.setOnClickListener {
            if (MccExManager.isPlayOriginal()) {
                MccExManager.setPlayMode(MusicPlayMode.MUSIC_PLAY_MODE_ACCOMPANY)
            } else {
                MccExManager.setPlayMode(MusicPlayMode.MUSIC_PLAY_MODE_ORIGINAL)
            }
            updateView()
        }

        binding.joinRoomBtn.setOnClickListener {
            if (BuildConfig.IS_WRAPPER_MODE) {
                if (!mJoinSuccess) {
                    testMccExWrapperStart()
                    mJoinSuccess = true
                    binding.joinRoomBtn.text = resources.getString(R.string.leave)
                } else {
                    testMccExWrapperEnd()
                    mJoinSuccess = false
                    binding.joinRoomBtn.text = resources.getString(R.string.join)
                }
            } else {
                if (!mJoinSuccess) {
                    joinRoom()
                } else {
                    leaveRoom()
                }
            }
        }


        binding.interfaceTestBtn.setOnClickListener {
            val intent = Intent(this, InterfaceTestActivity::class.java)
            startActivity(intent)
        }

        binding.skipTheIntroBtn.setOnClickListener {
            LogUtils.d("skipTheIntroBtn mLyricsModel preludeEndPosition: ${mLyricsModel?.preludeEndPosition}")
            if ((mLyricsModel?.preludeEndPosition ?: 0) > 0) {
                mLyricsModel?.preludeEndPosition?.let { it1 ->
                    val position = it1 - 1000
                    MccExManager.seek(position)
                    MccExManager.updateMusicPosition(position)
                }
            }
        }

        binding.setAudioPitchBtn.setOnClickListener {
            MccExManager.setAudioPitch(mCurrentAudioPitch)
            if (mIncreaseAudioPitch) {
                mCurrentAudioPitch += 2
            } else {
                mCurrentAudioPitch -= 2
            }
            if (mCurrentAudioPitch > Constants.MAX_AUDIO_PITCH) {
                mCurrentAudioPitch = Constants.MAX_AUDIO_PITCH
                mIncreaseAudioPitch = false
            }

            if (mCurrentAudioPitch < Constants.MIN_AUDIO_PITCH) {
                mCurrentAudioPitch = Constants.MIN_AUDIO_PITCH
                mIncreaseAudioPitch = true
            }
        }

        updateView();
        initKaraokeView()
    }

    private fun initKaraokeView() {
        mKaraokeView = KaraokeView(
            binding.lyricsView, binding.scoringView
        )

        binding.lyricsView.enableDragging(true)

        mKaraokeView?.setKaraokeEvent(object : KaraokeEvent {
            override fun onDragTo(view: KaraokeView?, progress: Long) {
                LogUtils.d("onDragTo progress: $progress")
                mKaraokeView?.setProgress(progress)
                MccExManager.updateMusicPosition(progress)
                MccExManager.seek(progress)
            }

            override fun onLineFinished(
                view: KaraokeView?,
                line: LyricsLineModel?,
                score: Int,
                cumulativeScore: Int,
                index: Int,
                lineCount: Int
            ) {
                LogUtils.d("onLineFinished score: $score cumulativeScore: $cumulativeScore index: $index lineCount: $lineCount")
            }

        })
    }

    private fun updateSongListView() {
        if (null == mSongAdapter) {
            binding.songListRv.layoutManager =
                LinearLayoutManager(
                    this@MainActivity,
                    LinearLayoutManager.VERTICAL,
                    false
                )

            mSongList.clear()
            mSongCacheList.forEach {
                mSongList.add(ItemData(it.songId + " - " + it.songName + " - " + it.singerName))
            }
            mSongAdapter = ItemAdapter(mSongList)

            // 设置下载按钮监听器
            mSongAdapter?.setOnDownloadClickListener(object :
                ItemAdapter.OnDownloadClickListener {
                override fun onDownloadClick(position: Int) {
                    val songInfo = mSongCacheList[position]
                    LogUtils.i("Download clicked for position: $position, songInfo: $songInfo")

                    // 立即设置为等待下载状态
                    mSongList[position].downloadState =
                        io.agora.mccex_demo.model.DownloadState.WAITING
                    mSongList[position].downloadProgress = 0
                    mSongAdapter?.notifyItemChanged(position)
                    LogUtils.d("Set download state to WAITING for position: $position")

                    // 计算并保存 songCode
                    val songCode =
                        MccExManager.getInternalSongCode(songInfo.songId, songInfo.optionJson)
                    mSongCacheList[position].songCode = songCode
                    LogUtils.d("Saved songCode: $songCode for position: $position")

                    MccExManager.preloadMusic(songInfo.songId, songInfo.optionJson)
                    //MccExManager.getPitch(songCode)
                    //MccExManager.getLyric(songCode, LyricType.KRC)
                }
            })

            // 设置播放按钮监听器
            mSongAdapter?.setOnPlayClickListener(object :
                ItemAdapter.OnPlayClickListener {
                override fun onPlayClick(position: Int) {
                    val songInfo = mSongCacheList[position]
                    LogUtils.i("Play clicked for songInfo: $songInfo")

                    // 检查是否已下载完成
                    if (mSongList[position].downloadState != io.agora.mccex_demo.model.DownloadState.COMPLETED) {
                        ToastUtils.showLongToast(this@MainActivity, "歌曲尚未下载完成，请先下载")
                        return
                    }

                    // 检查 songCode 是否有效
                    if (songInfo.songCode == 0L) {
                        LogUtils.e("Play failed: songCode is 0")
                        ToastUtils.showLongToast(this@MainActivity, "歌曲代码无效")
                        return
                    }

                    // 清除之前的选中状态和播放状态
                    mSongList.forEach {
                        it.bgResId = 0
                        it.isPlaying = false
                    }
                    mSongList[position].bgResId = R.color.gray

                    // 如果正在播放其他歌曲，先停止
                    if (MccExManager.isMusicPlaying()) {
                        MccExManager.stop()
                    }

                    // 设置歌词并开始播放
                    mKaraokeView?.reset()

                    // 解析歌词数据
                    try {
                        val lyricFile =
                            if (songInfo.lyricPath.isEmpty()) null else File(songInfo.lyricPath)
                        val pitchFile =
                            if (songInfo.pitchPath.isEmpty()) null else File(songInfo.pitchPath)

                        LogUtils.d("onClickPlay songInfo:${songInfo} lyricPath: $lyricFile pitchPath: $lyricFile")

                        if (lyricFile != null) {
                            mLyricsModel = KaraokeView.parseLyricData(
                                lyricFile,
                                pitchFile,
                                true,
                                songInfo.lyricOffset
                            )
                            if (mLyricsModel != null) {
                                mKaraokeView?.setLyricData(mLyricsModel, false)
                            }
                        } else {
                            mLyricsModel = null
                        }
                    } catch (e: Exception) {
                        LogUtils.e("Parse lyric failed: ${e.message}")
                        ToastUtils.showLongToast(this@MainActivity, "解析歌词失败")
                        return
                    }

                    // 设置当前播放位置和播放状态
                    mCurrentPlayingPosition = position
                    mSongList[position].isPlaying = true
                    mSongAdapter?.updateDataChanged()

                    // 开始播放
                    MccExManager.startScoreOrPlay(
                        songInfo.songCode,
                        songInfo.lyricPath,
                        songInfo.pitchPath
                    )
                }
            })

            binding.songListRv.adapter = mSongAdapter
            binding.songListRv.addItemDecoration(
                ItemAdapter.MyItemDecoration(
                    resources.getDimensionPixelSize(
                        R.dimen.item_space
                    )
                )
            )
        } else {
            // 保存旧的状态信息
            val oldStates = mSongList.mapIndexed { index, item ->
                index to Triple(item.downloadState, item.downloadProgress, item.isPreloaded)
            }.toMap()

            mSongList.clear()
            mSongCacheList.forEachIndexed { index, songInfo ->
                val newItem =
                    ItemData(songInfo.songId + " - " + songInfo.songName + " - " + songInfo.singerName)

                // 恢复之前的状态
                oldStates[index]?.let { (downloadState, downloadProgress, isPreloaded) ->
                    newItem.downloadState = downloadState
                    newItem.downloadProgress = downloadProgress
                    newItem.isPreloaded = isPreloaded
                }

                mSongList.add(newItem)
            }
            mSongAdapter?.updateDataChanged()
        }
    }

    private fun updateView() {
        if (MccExManager.isMusicPlaying()) {
            binding.playBtn.text = resources.getString(R.string.pause)
            binding.stopBtn.isEnabled = true
        } else if (MccExManager.isMusicPause()) {
            binding.playBtn.text = resources.getString(R.string.play)
            binding.stopBtn.isEnabled = true
        } else {
            binding.playBtn.text = resources.getString(R.string.play)
            binding.stopBtn.isEnabled = false
        }

        if (MccExManager.isPlayOriginal()) {
            binding.playModeBtn.text = resources.getString(R.string.play_accompany)
        } else {
            binding.playModeBtn.text = resources.getString(R.string.play_original)
        }
    }

    private fun enableView(enable: Boolean) {
        mSongAdapter?.setClickable(enable)
        if (!enable) {
            mSongList.forEach {
                it.bgResId = 0
                it.isPlaying = false
                // 重置下载状态（如果需要）
                if (it.downloadState == io.agora.mccex_demo.model.DownloadState.WAITING ||
                    it.downloadState == io.agora.mccex_demo.model.DownloadState.DOWNLOADING
                ) {
                    it.downloadState = io.agora.mccex_demo.model.DownloadState.IDLE
                    it.downloadProgress = 0
                    it.isPreloaded = false
                }
            }
            mSongAdapter?.updateDataChanged()
            mCurrentPlayingPosition = -1
            mLastUpdateProgress.clear()
        }
        binding.playBtn.isEnabled = enable
        binding.stopBtn.isEnabled = enable
        binding.playModeBtn.isEnabled = enable
        binding.skipTheIntroBtn.isEnabled = enable

    }

    private fun handleOnBackPressed() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val xPopup = XPopup.Builder(this@MainActivity)
                    .asConfirm("退出", "确认退出程序", {
                        exit()
                    }, {})
                xPopup.show()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun isNetworkConnected(): Boolean {
        val isConnect = Utils.isNetworkConnected(this)
        if (!isConnect) {
            LogUtils.d("Network is not connected")
            ToastUtils.showLongToast(this, "请连接网络!")
        }
        return isConnect
    }

    private fun initParams() {
        if (MccExKeys.ysdToken.isNotEmpty()) {
            getTops()
            searchSong()
            return
        }
        val tokenTime = SharedPreferencesUtils.readLong(this, Constants.SP_KEY_YSD_TOKEN_TIME, 0)
        if (tokenTime == 0L || System.currentTimeMillis() - tokenTime >= Constants.TOKEN_EXPIRE_TIME) {
            LogUtils.d("token is expired")
            mLoadingPopup?.show()
            mCoroutineScope.launch {
                NetworkClient.sendHttpsRequest(
                    BuildConfig.YSD_TOKEN_URL + KeyCenter.getUid(),
                    emptyMap<Any, Any>(),
                    "",
                    NetworkClient.Method.GET,
                    object :
                        Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            LogUtils.e("get token failed ${e.message}")
                            runOnUiThread {
                                ToastUtils.showLongToast(
                                    this@MainActivity,
                                    "获取token失败 ${e.message}"
                                )
                                mLoadingPopup?.dismiss()
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val responseData = response.body?.string() ?: ""
                            LogUtils.d("get token success $responseData")
                            val responseJson = JSONObject(responseData)
                            val dataJson = responseJson.getJSONObject("data")
                            val token = dataJson.getString("token")
                            val userId = dataJson.getString("yinsuda_uid")
                            SharedPreferencesUtils.writeString(
                                this@MainActivity,
                                Constants.SP_KEY_YSD_TOKEN,
                                token
                            )
                            SharedPreferencesUtils.writeLong(
                                this@MainActivity,
                                Constants.SP_KEY_YSD_TOKEN_TIME,
                                System.currentTimeMillis()
                            )
                            SharedPreferencesUtils.writeString(
                                this@MainActivity,
                                Constants.SP_KEY_YSD_USER_ID,
                                userId
                            )
                            MccExKeys.ysdToken = token
                            MccExKeys.ysdUserId = userId
                            getTops()
                            searchSong()
                        }
                    })

            }
        } else {
            MccExKeys.ysdToken =
                SharedPreferencesUtils.readString(this, Constants.SP_KEY_YSD_TOKEN, "")!!
            MccExKeys.ysdUserId =
                SharedPreferencesUtils.readString(this, Constants.SP_KEY_YSD_USER_ID, "")!!
            getTops()
            searchSong()
        }
    }

    private fun getTops() {
        mCoroutineScope.launch {
            val headers = mutableMapOf<String, String>()
            val bodyJSONObject = JSONObject()
            bodyJSONObject.put("pid", MccExKeys.ysdAppId)
            bodyJSONObject.put("sp", "KG")
            bodyJSONObject.put("device_id", io.agora.mccex.utils.Utils.getUuid())
            bodyJSONObject.put("client_ip", io.agora.mccex.utils.Utils.getIpAddress())
            bodyJSONObject.put("timestamp", io.agora.mccex.utils.Utils.getCurrentTimestampSeconds())
            bodyJSONObject.put("nonce", io.agora.mccex.utils.Utils.getUuid())
            bodyJSONObject.put(
                "userid", MccExKeys.ysdUserId
            )
            bodyJSONObject.put(
                "token",
                MccExKeys.ysdToken
            )

            headers["Content-Type"] = "application/json"
            headers["signature"] =
                EncryptUtils.MD5(bodyJSONObject.toString() + MccExKeys.ysdAppKey)

            NetworkClient.sendHttpsRequest(
                "https://commercial.kugou.com/v2/commercial/tops",
                headers,
                bodyJSONObject.toString(),
                NetworkClient.Method.POST,
                object :
                    Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        LogUtils.e("get tops failed ${e.message}")
                        runOnUiThread {
                            ToastUtils.showLongToast(
                                this@MainActivity,
                                "获取榜单列表 ${e.message}"
                            )
                            mLoadingPopup?.dismiss()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseData = response.body?.string() ?: ""
                        LogUtils.d("get tops success $responseData")
                        val responseJson = JSONObject(responseData)
                        val dataJson = responseJson.getJSONObject("data")
                        val topsJson = dataJson.getJSONArray("tops")
                        val firstTopId = topsJson.getJSONObject(0).getString("top_id")
                        getTopSongs(firstTopId)
                    }
                })
        }
    }

    private fun getTopSongs(topId: String) {
        LogUtils.d("get top song topId: $topId")
        mCoroutineScope.launch {
            val headers = mutableMapOf<String, String>()
            val bodyJSONObject = JSONObject()
            bodyJSONObject.put("pid", MccExKeys.ysdAppId)
            bodyJSONObject.put("sp", "KG")
            bodyJSONObject.put("device_id", io.agora.mccex.utils.Utils.getUuid())
            bodyJSONObject.put("client_ip", io.agora.mccex.utils.Utils.getIpAddress())
            bodyJSONObject.put("timestamp", io.agora.mccex.utils.Utils.getCurrentTimestampSeconds())
            bodyJSONObject.put("nonce", io.agora.mccex.utils.Utils.getUuid())
            bodyJSONObject.put(
                "userid", MccExKeys.ysdUserId
            )
            bodyJSONObject.put(
                "token",
                MccExKeys.ysdToken
            )
            bodyJSONObject.put("page", 1)
            bodyJSONObject.put("size", 50)
            bodyJSONObject.put("top_id", topId)
            bodyJSONObject.put("filter", JSONArray(Gson().toJson(arrayOf<Int>())))
            bodyJSONObject.put("fields_ext", JSONArray(Gson().toJson(arrayOf<String>())))


            headers["Content-Type"] = "application/json"
            headers["signature"] =
                EncryptUtils.MD5(bodyJSONObject.toString() + MccExKeys.ysdAppKey)

            LogUtils.d("get top song request $bodyJSONObject")
            NetworkClient.sendHttpsRequest(
                "https://commercial.kugou.com/v2/commercial/top/song_v2",
                headers,
                bodyJSONObject.toString(),
                NetworkClient.Method.POST,
                object :
                    Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        LogUtils.e("get top song failed ${e.message}")
                        runOnUiThread {
                            ToastUtils.showLongToast(
                                this@MainActivity,
                                "获取歌曲列表失败 ${e.message}"
                            )
                            mLoadingPopup?.dismiss()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseData = response.body?.string() ?: ""
                        LogUtils.d("get top song success $responseData")

                        val responseJson = JSONObject(responseData)
                        val dataJson = responseJson.getJSONObject("data")
                        val songListJson = dataJson.getJSONArray("songs")
                        for (i in 0 until songListJson.length()) {
                            val songJson = songListJson.getJSONObject(i)
                            val songId = songJson.getString("song_id")
                            var optionJson = "{\"format\":{\"dumpAudio\":false}}"
                            if (!mSongFull) {
                                optionJson = "{\"format\":{\"highPart\":1,\"dumpAudio\":true}}"
                            }
                            val songName = songJson.getString("song_name")
                            val singerName = songJson.getString("singer_name")
                            mSongCacheList.add(
                                SongInfo(
                                    songId,
                                    optionJson,
                                    songName,
                                    singerName,
                                    0L,
                                    "",
                                    ""
                                )
                            )
                        }
                        runOnUiThread {
                            LogUtils.d("get top song success $mSongCacheList")
                            updateSongListView()
                            mLoadingPopup?.dismiss()
                        }
                    }
                })
        }
    }

    private fun searchSong() {
        val keyword = "钢琴曲";
        LogUtils.d("searchSong keyword: $keyword")
        if (keyword.isEmpty()) {
            return
        }
        mCoroutineScope.launch {
            val headers = mutableMapOf<String, String>()
            val bodyJSONObject = JSONObject()
            bodyJSONObject.put("pid", MccExKeys.ysdAppId)
            bodyJSONObject.put("sp", "KG")
            bodyJSONObject.put("device_id", io.agora.mccex.utils.Utils.getUuid())
            bodyJSONObject.put("client_ip", io.agora.mccex.utils.Utils.getIpAddress())
            bodyJSONObject.put("timestamp", io.agora.mccex.utils.Utils.getCurrentTimestampSeconds())
            bodyJSONObject.put("nonce", io.agora.mccex.utils.Utils.getUuid())
            bodyJSONObject.put(
                "userid", MccExKeys.ysdUserId
            )
            bodyJSONObject.put(
                "token",
                MccExKeys.ysdToken
            )
            bodyJSONObject.put("keyword", keyword)
            bodyJSONObject.put("page", 1)
            bodyJSONObject.put("size", 10)
            bodyJSONObject.put("filter", JSONArray(Gson().toJson(arrayOf<Int>())))
            bodyJSONObject.put("fields_ext", JSONArray(Gson().toJson(arrayOf<String>())))


            headers["Content-Type"] = "application/json"
            headers["signature"] =
                EncryptUtils.MD5(bodyJSONObject.toString() + MccExKeys.ysdAppKey)

            LogUtils.d("search song request $bodyJSONObject")
            NetworkClient.sendHttpsRequest(
                "https://commercial.kugou.com/v2/commercial/search/song_v2",
                headers,
                bodyJSONObject.toString(),
                NetworkClient.Method.POST,
                object :
                    Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        LogUtils.e("search song failed ${e.message}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseData = response.body?.string() ?: ""
                        LogUtils.d("search song success $responseData")

                        val responseJson = JSONObject(responseData)
                        val dataJson = responseJson.getJSONObject("data")
                        val songListJson = dataJson.getJSONArray("songs")
                        for (i in 0 until songListJson.length()) {
                            val songJson = songListJson.getJSONObject(i)
                            val songId = songJson.getString("song_id")
                            val optionJson = "{\"format\":{\"highPart\":2}}"
                            val songName = "[" + keyword + "]" + songJson.getString("song_name")
                            val singerName = songJson.getString("singer_name")
                            mSongCacheList.add(
                                SongInfo(
                                    songId,
                                    optionJson,
                                    songName,
                                    singerName,
                                    0L,
                                    "",
                                    ""
                                )
                            )
                        }
                        runOnUiThread {
                            LogUtils.d("get top song success $mSongCacheList")
                            updateSongListView()
                            mLoadingPopup?.dismiss()
                        }
                    }
                })
        }
    }


    private fun exit() {
        LogUtils.destroy()
        MccExManager.destroy()
        finishAffinity()
        finish()
        exitProcess(0)
    }

    private fun joinRoom() {
        RtcManager.setTestInterfaceMode(false)
        MccExManager.setTestInterfaceMode(false)
        RtcManager.initRtcEngine(this, this)
        RtcManager.getRtcEngine()?.let {
            MccExManager.initMccExService(
                it,
                RtcManager,
                applicationContext,
                this
            )
        }
    }


    private fun leaveRoom() {
        mLoadingPopup?.show()
        lifecycleScope.launch(Dispatchers.Main) {
            if (MccExManager.isMusicPlaying()) {
                MccExManager.stop()
                updateView()
                mKaraokeView?.reset()

                // 重置播放状态
                if (mCurrentPlayingPosition >= 0 && mCurrentPlayingPosition < mSongList.size) {
                    mSongList[mCurrentPlayingPosition].isPlaying = false
                    mSongAdapter?.updateDataChanged()
                    mCurrentPlayingPosition = -1
                }
            }

            withContext(Dispatchers.IO) {
                try {
                    // wait for the music stop
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            MccExManager.destroy()
            RtcManager.leaveChannel()
            mLoadingPopup?.dismiss()
        }
    }

    private fun test() {
        val songCode =
            MccExManager.getMccExService()
                ?.getInternalSongCode("32259070", "{\"format\":{\"highPart\":0}}")
        LogUtils.d("getInternalSongCode songCode: $songCode")

//        val songCode2 =
//            MccExManager.getMccExService()
//                ?.getInternalSongCode("625628712", "{\"format\":{\"highPart\":0}}")
//        LogUtils.d("getInternalSongCode songCode2: $songCode2")

//        val requestId =
//            songCode?.let { MccExManager.getMccExService()?.preload(it) }
//        LogUtils.d("preload ret: $requestId")
//
//
//        val requestId2 =
//            songCode2?.let { MccExManager.getMccExService()?.preload(it) }
//        LogUtils.d("preload ret2 ret: $requestId2")

//        val preloadRequestId =
//            songCode?.let { MccExManager.getMccExService()?.preload(it) }
//        LogUtils.d("preload ret: $preloadRequestId")

        val getLyricRequestId =
            songCode?.let { MccExManager.getMccExService()?.getLyric(it, LyricType.KRC) }
        LogUtils.d("getLyric ret: $getLyricRequestId")

//        val getPitchRequestId =
//            songCode?.let { MccExManager.getMccExService()?.getPitch(it) }
//        LogUtils.d("getPitch ret: $getPitchRequestId")
    }

    override fun onInitializeResult(state: MccExState, reason: MccExStateReason) {
        runOnUiThread {
            RtcManager.joinChannel()
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
        runOnUiThread {
            // 根据 songCode 在 mSongCacheList 中查找对应的 position
            var position = -1
            for (i in mSongCacheList.indices) {
                if (mSongCacheList[i].songCode == songCode) {
                    position = i
                    LogUtils.d("Found position: $position for songCode: $songCode")
                    break
                }
            }

            // 如果没有找到，尝试计算 songCode 并匹配
            if (position == -1) {
                LogUtils.d("songCode not found in cache, trying to calculate...")
                for (i in mSongCacheList.indices) {
                    val songInfo = mSongCacheList[i]
                    val calculatedCode = MccExManager.getMccExService()
                        ?.getInternalSongCode(songInfo.songId, songInfo.optionJson)
                    if (calculatedCode == songCode) {
                        position = i
                        // 保存 songCode 到 mSongCacheList
                        mSongCacheList[i].songCode = songCode
                        LogUtils.d("Calculated and saved songCode: $songCode for position: $position")
                        break
                    }
                }
            }

            if (position >= 0 && position < mSongList.size) {
                // 更新下载进度到 mSongList
                mSongList[position].downloadProgress = percent

                // 同时更新歌词和音频路径到 mSongCacheList
                mSongCacheList[position].lyricPath = lyricPath
                mSongCacheList[position].pitchPath = pitchPath
                mSongCacheList[position].lyricOffset = lyricOffset
                mSongCacheList[position].songOffsetBegin = songOffsetBegin
                mSongCacheList[position].songOffsetEnd = songOffsetEnd

                when (state) {
                    MccExState.PRELOAD_STATE_COMPLETED -> {
                        if (percent == 100) {
                            mLoadingPopup?.dismiss()
                            mSongList[position].isPreloaded = true
                            mSongList[position].downloadProgress = 100
                            mSongList[position].downloadState =
                                io.agora.mccex_demo.model.DownloadState.COMPLETED
                            mLastUpdateProgress.remove(position)
                            mSongAdapter?.notifyItemChanged(position)

                            LogUtils.d("onPreLoadEvent completed lyricPath: $lyricPath pitchPath: $pitchPath")

                            // 预加载歌词数据（不自动播放）
                            if (lyricPath.isNotEmpty() && File(lyricPath).exists()) {
                                mLyricsModel =
                                    KaraokeView.parseLyricData(
                                        File(lyricPath),
                                        File(pitchPath),
                                        true,
                                        lyricOffset
                                    )
                                if (songOffsetBegin != 0) {
                                    mLyricsModel = LyricsCutter.cut(
                                        mLyricsModel,
                                        songOffsetBegin - lyricOffset,
                                        songOffsetEnd - lyricOffset
                                    )
                                }
                            }
                            ToastUtils.showLongToast(this, "下载完成，可以播放了")
                        }
                    }

                    MccExState.PRELOAD_STATE_PRELOADING -> {
                        // 更新为下载中状态（首次）
                        if (mSongList[position].downloadState != io.agora.mccex_demo.model.DownloadState.DOWNLOADING) {
                            mSongList[position].downloadState =
                                io.agora.mccex_demo.model.DownloadState.DOWNLOADING
                            mLastUpdateProgress[position] = 0
                            mSongAdapter?.notifyItemChanged(position)
                        } else {
                            // 只在进度变化超过5%时才更新UI，减少刷新频率
                            val lastProgress = mLastUpdateProgress[position] ?: 0
                            if (percent - lastProgress >= 5 || percent == 100) {
                                mLastUpdateProgress[position] = percent
                                mSongAdapter?.notifyItemChanged(position)
                                LogUtils.d("Update progress for position $position: $percent%")
                            }
                        }
                    }

                    MccExState.PRELOAD_STATE_FAILED -> {
                        mSongList[position].isPreloaded = false
                        mSongList[position].downloadProgress = 0
                        mSongList[position].downloadState =
                            io.agora.mccex_demo.model.DownloadState.FAILED
                        mLastUpdateProgress.remove(position)
                        mSongAdapter?.notifyItemChanged(position)
                        ToastUtils.showLongToast(this, "下载失败，请重试")
                        mLoadingPopup?.dismiss()
                    }

                    else -> {
                        // 其他状态保持不变
                    }
                }
            } else {
                LogUtils.e("Position not found for songCode: $songCode")
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
    }

    override fun onLineScore(songCode: Long, value: LineScoreData) {
        runOnUiThread {
            ToastUtils.showLongToast(
                this,
                "Index:${value.performedLineIndex}:${value.performedTotalLines} Score: ${value.linePitchScore}:${value.cumulativeTotalLinePitchScores}"
            )
        }

    }

    override fun onPitch(songCode: Long, data: RawScoreData) {
        runOnUiThread { mKaraokeView?.setPitch(data.speakerPitch, 0F, data.progressInMs) }
    }

    override fun onPlayStateChange() {
        super.onPlayStateChange()
        runOnUiThread {
            if (((mLoadingPopup?.isShow == true) || (PopupStatus.Showing == mLoadingPopup?.popupStatus)) && MccExManager.isMusicPlaying()) {
                mLoadingPopup?.dismiss()
            }

            // 当开始播放时，设置歌词
            if (MccExManager.isMusicPlaying() && mLyricsModel != null) {
                mKaraokeView?.setLyricData(mLyricsModel, false)
            }

            // 当播放停止时，重置播放状态
            if (!MccExManager.isMusicPlaying() && !MccExManager.isMusicPause()) {
                if (mCurrentPlayingPosition >= 0 && mCurrentPlayingPosition < mSongList.size) {
                    mSongList[mCurrentPlayingPosition].isPlaying = false
                    mSongAdapter?.updateDataChanged()
                    mCurrentPlayingPosition = -1
                }
            }

            updateView()
        }
    }

    override fun onMusicPositionChange(position: Long) {
        runOnUiThread { mKaraokeView?.setProgress(position) }

    }

    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        runOnUiThread {
            mJoinSuccess = true
            ToastUtils.showLongToast(this, "加入房间成功")
            enableView(true)
            binding.joinRoomBtn.text = resources.getString(R.string.leave)
            //test()
        }
    }

    override fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats) {
        mJoinSuccess = false
        runOnUiThread {
            ToastUtils.showLongToast(this, "离开房间成功")
            enableView(false)
            binding.joinRoomBtn.text = resources.getString(R.string.join)
        }
    }

    private fun testMccExWrapperStart() {
        try {
            val className =
                "io.agora.mccex_demo.utils.MccExServiceWrapperTest" // 替换成实际的包名和类名
            val clazz = Class.forName(className)
            val instance = clazz.getField("INSTANCE").get(null) // 获取单例对象

            val methodName = "testStart"
            val method = clazz.getDeclaredMethod(
                methodName,
            )
            method.invoke(
                instance
            ) // 调用方法
        } catch (e: Exception) {
            LogUtils.e(InterfaceTestActivity.TAG, "testPlaySong fail:${e}")
        }
    }

    private fun testMccExWrapperEnd() {
        try {
            val className =
                "io.agora.mccex_demo.utils.MccExServiceWrapperTest" // 替换成实际的包名和类名
            val clazz = Class.forName(className)
            val instance = clazz.getField("INSTANCE").get(null) // 获取单例对象

            val methodName = "testEnd"
            val method = clazz.getDeclaredMethod(
                methodName,
            )
            method.invoke(
                instance
            ) // 调用方法
        } catch (e: Exception) {
            LogUtils.e(InterfaceTestActivity.TAG, "testPlaySong fail:${e}")
        }
    }

}





