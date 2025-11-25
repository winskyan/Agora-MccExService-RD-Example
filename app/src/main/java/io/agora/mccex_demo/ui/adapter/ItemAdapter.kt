package io.agora.mccex_demo.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import io.agora.mccex_demo.R
import io.agora.mccex_demo.model.ItemData

class ItemAdapter(private val itemList: MutableList<ItemData>) :
    RecyclerView.Adapter<ItemAdapter.MyViewHolder>() {
    private var selectedItem = -1
    private var downloadListener: OnDownloadClickListener? = null
    private var playListener: OnPlayClickListener? = null
    private var isClickable = false

    fun setOnDownloadClickListener(listener: OnDownloadClickListener) {
        this.downloadListener = listener
    }

    fun setOnPlayClickListener(listener: OnPlayClickListener) {
        this.playListener = listener
    }

    fun setClickable(clickable: Boolean) {
        isClickable = clickable
    }

    fun updateDataChanged() {
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return MyViewHolder(itemView)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(
        holder: MyViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        val item = itemList[position]
        holder.content.text = item.content
        holder.itemView.isSelected = item.bgResId != 0

        var resId = R.color.white
        if (item.bgResId != 0) {
            resId = item.bgResId
        }
        holder.itemLayout.setBackgroundColor(
            holder.itemView.resources.getColor(
                resId,
                null
            )
        )

        // 更新下载按钮状态
        when (item.downloadState) {
            io.agora.mccex_demo.model.DownloadState.IDLE -> {
                holder.downloadBtn.text = "下载"
                holder.downloadBtn.isEnabled = isClickable
            }

            io.agora.mccex_demo.model.DownloadState.WAITING -> {
                holder.downloadBtn.text = "等待下载"
                holder.downloadBtn.isEnabled = false
            }

            io.agora.mccex_demo.model.DownloadState.DOWNLOADING -> {
                holder.downloadBtn.text = "${item.downloadProgress}%"
                holder.downloadBtn.isEnabled = false
            }

            io.agora.mccex_demo.model.DownloadState.COMPLETED -> {
                holder.downloadBtn.text = "已下载"
                holder.downloadBtn.isEnabled = false
            }

            io.agora.mccex_demo.model.DownloadState.FAILED -> {
                holder.downloadBtn.text = "下载失败"
                holder.downloadBtn.isEnabled = false
            }
        }

        // 更新播放按钮状态
        if (item.isPlaying) {
            holder.playBtn.text = "播放中"
            holder.playBtn.isEnabled = false
        } else {
            holder.playBtn.text = "播放"
            // 只有下载完成且未播放时才能点击
            holder.playBtn.isEnabled = isClickable &&
                    item.downloadState == io.agora.mccex_demo.model.DownloadState.COMPLETED
        }

        // 下载按钮点击事件
        holder.downloadBtn.setOnClickListener {
            if (isClickable && item.downloadState == io.agora.mccex_demo.model.DownloadState.IDLE) {
                downloadListener?.onDownloadClick(position)
            }
        }

        // 播放按钮点击事件
        holder.playBtn.setOnClickListener {
            if (isClickable &&
                item.downloadState == io.agora.mccex_demo.model.DownloadState.COMPLETED &&
                !item.isPlaying
            ) {
                playListener?.onPlayClick(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemLayout: View = itemView.findViewById(R.id.item_layout)
        val content: TextView = itemLayout.findViewById(R.id.content_tv)
        val downloadBtn: Button = itemLayout.findViewById(R.id.download_btn)
        val playBtn: Button = itemLayout.findViewById(R.id.play_btn)
    }

    class MyItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.left = 0
            outRect.right = 0
            outRect.bottom = 0
            outRect.top = space

            // 如果是第一个item，设置top间隔
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = 0
            }
        }
    }

    interface OnDownloadClickListener {
        fun onDownloadClick(position: Int)
    }

    interface OnPlayClickListener {
        fun onPlayClick(position: Int)
    }
}