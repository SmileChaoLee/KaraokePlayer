package com.smile.karaokeplayer.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.models.SongInfo
import com.smile.karaokeplayer.models.SongListSQLite
import com.smile.smilelibraries.utilities.ScreenUtil

private const val TAG : String = "SelectedFavoriteAdapter"

class SelectedFavoriteAdapter (
        private var itemClickListener : OnRecyclerItemClickListener,
        private var songListSQLite : SongListSQLite,
        private var mList : ArrayList<SongInfo>,
        private var textFontSize: Float,
        private var yellow2Color: Int, private var yellow3Color: Int)

    : RecyclerView.Adapter<SelectedFavoriteAdapter.MyViewHolder>() {

    interface OnRecyclerItemClickListener {
        fun onRecyclerItemClick(v: View?, position: Int)
        fun setAudioLayoutVisibility(audioMusicLayout : LinearLayout)
        fun editSongButtonFunc(position : Int)
        fun deleteSongButtonFunc(position : Int)
        fun playSongButtonFunc(position : Int)
    }

    companion object {
        private var viewAdapter : SelectedFavoriteAdapter? = null
        @JvmStatic
        fun getInstance(itemClickListener: OnRecyclerItemClickListener,
                        songListSQLite : SongListSQLite,
                        mList: java.util.ArrayList<SongInfo>,
                        textFontSize: Float,
                        yellow2Color: Int, yellow3Color: Int) : SelectedFavoriteAdapter {

            Log.d(TAG, "getInstance.viewAdapter = $viewAdapter, mList.size = ${mList.size}")
            if (viewAdapter == null) {
                viewAdapter = SelectedFavoriteAdapter(itemClickListener, songListSQLite,
                        mList, textFontSize, yellow2Color, yellow3Color)
            } else {
                viewAdapter?.let {
                    it.itemClickListener = itemClickListener
                    it.songListSQLite = songListSQLite
                    it.mList = mList
                    it.textFontSize = textFontSize
                    it.yellow2Color = yellow2Color
                    it.yellow3Color = yellow3Color
                }
            }

            return viewAdapter!!
        }
    }

    class MyViewHolder(itemView: View, itemClickListener : OnRecyclerItemClickListener, textFontSize: Float)
        : RecyclerView.ViewHolder(itemView) {

        val titleNameTextView: TextView
        val filePathTextView: TextView
        val musicTrackTextView: TextView
        val musicChannelTextView: TextView
        val vocalTrackTextView: TextView
        val vocalChannelTextView: TextView
        val includedPlaylistCheckBox: CheckBox
        var inPlaylist: Boolean = true

        init {
            Log.d(TAG, "MyViewHolder() is called")
            val itemTextSize = textFontSize * 0.6f
            val buttonTextSize = textFontSize * 0.8f

            val titleStringTextView : TextView = itemView.findViewById(R.id.titleStringTextView)
            ScreenUtil.resizeTextSize(titleStringTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type)
            titleNameTextView = itemView.findViewById(R.id.titleNameTextView)
            ScreenUtil.resizeTextSize(titleNameTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type)

            val filePathStringTextView : TextView = itemView.findViewById(R.id.filePathStringTextView)
            ScreenUtil.resizeTextSize(filePathStringTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type)
            filePathTextView = itemView.findViewById(R.id.filePathTextView)
            ScreenUtil.resizeTextSize(filePathTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type)
            val audioMusicLinearLayout : LinearLayout = itemView.findViewById(R.id.audioMusicLinearLayout)
            val musicTrackStringTextView : TextView = itemView.findViewById(R.id.musicTrackStringTextView)
            ScreenUtil.resizeTextSize(musicTrackStringTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type)
            musicTrackTextView = itemView.findViewById(R.id.musicTrackTextView)
            ScreenUtil.resizeTextSize(musicTrackTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type)

            val musicChannelStringTextView : TextView = itemView.findViewById(R.id.musicChannelStringTextView)
            ScreenUtil.resizeTextSize(musicChannelStringTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type)
            musicChannelTextView = itemView.findViewById(R.id.musicChannelTextView)
            ScreenUtil.resizeTextSize(musicChannelTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type)
            val audioVocalLinearLayout : LinearLayout = itemView.findViewById(R.id.audioVocalLinearLayout)
            val vocalTrackStringTextView : TextView = itemView.findViewById(R.id.vocalTrackStringTextView)
            ScreenUtil.resizeTextSize(vocalTrackStringTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type)
            vocalTrackTextView = itemView.findViewById(R.id.vocalTrackTextView)
            ScreenUtil.resizeTextSize(vocalTrackTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type)

            val vocalChannelStringTextView : TextView = itemView.findViewById(R.id.vocalChannelStringTextView)
            ScreenUtil.resizeTextSize(vocalChannelStringTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type)
            vocalChannelTextView = itemView.findViewById(R.id.vocalChannelTextView)
            ScreenUtil.resizeTextSize(vocalChannelTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type)

            itemClickListener.setAudioLayoutVisibility(audioMusicLinearLayout) // abstract method
            itemClickListener.setAudioLayoutVisibility(audioVocalLinearLayout)

            val includedPlaylistTextView : TextView = itemView.findViewById(R.id.includedPlaylistTextView)
            ScreenUtil.resizeTextSize(includedPlaylistTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type)
            includedPlaylistCheckBox = itemView.findViewById(R.id.includedPlaylistCheckBox)
            ScreenUtil.resizeTextSize(includedPlaylistCheckBox, itemTextSize, ScreenUtil.FontSize_Pixel_Type)
            val editSongButton : Button = itemView.findViewById(R.id.editSongButton)
            ScreenUtil.resizeTextSize(editSongButton, buttonTextSize, ScreenUtil.FontSize_Pixel_Type)
            val deleteSongButton : Button = itemView.findViewById(R.id.deleteSongButton)
            ScreenUtil.resizeTextSize(deleteSongButton, buttonTextSize, ScreenUtil.FontSize_Pixel_Type)
            val playSongButton : Button = itemView.findViewById(R.id.playSongButton)
            ScreenUtil.resizeTextSize(playSongButton, buttonTextSize, ScreenUtil.FontSize_Pixel_Type)

            // the following is still needed to be test (have to reduce the usage of memory)
            // if (!com.smile.karaokeplayer.BuildConfig.DEBUG) playSongButton.setVisibility(View.GONE);
            editSongButton.setOnClickListener {
                Log.d(TAG, "editSongButton.bindingAdapterPosition = $bindingAdapterPosition")
                itemClickListener.editSongButtonFunc(bindingAdapterPosition)
            }
            deleteSongButton.setOnClickListener {
                Log.d(TAG, "deleteSongButton.bindingAdapterPosition = $bindingAdapterPosition")
                itemClickListener.deleteSongButtonFunc(bindingAdapterPosition)
            }
            playSongButton.setOnClickListener {
                Log.d(TAG, "playSongButton.bindingAdapterPosition = $bindingAdapterPosition")
                itemClickListener.playSongButtonFunc(bindingAdapterPosition)
            }
            itemView.setOnClickListener {
                Log.d(TAG, "itemView.bindingAdapterPosition = $bindingAdapterPosition")
                itemClickListener.onRecyclerItemClick(itemView, bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        Log.d(TAG, "onCreateViewHolder().mList.size = ${mList.size}")
        val layoutInflater = LayoutInflater.from(parent.context)
        val fileView = layoutInflater.inflate(R.layout.activity_favorite_list_item, parent, false)
        return MyViewHolder(fileView, itemClickListener, textFontSize)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder().position = $position")
        val singleSongInfo = mList[position]
        holder.apply {
            titleNameTextView.text = singleSongInfo.songName
            filePathTextView.text = singleSongInfo.filePath
            musicTrackTextView.text = singleSongInfo.musicTrackNo.toString()
            musicChannelTextView.text = BaseApplication.audioChannelMap[singleSongInfo.musicChannel]
            vocalTrackTextView.text = singleSongInfo.vocalTrackNo.toString()
            vocalChannelTextView.text = BaseApplication.audioChannelMap[singleSongInfo.vocalChannel]
            inPlaylist = singleSongInfo.included == "1"
            includedPlaylistCheckBox.isChecked = holder.inPlaylist
            includedPlaylistCheckBox.setOnCheckedChangeListener {
                _: CompoundButton?, isChecked: Boolean ->
                includedPlaylistCheckBox.isChecked = isChecked
                includedPlaylistCheckBox.jumpDrawablesToCurrentState()
                val included = if (isChecked) "1" else "0"
                singleSongInfo.included = included
                songListSQLite.updateOneSongFromSongList(singleSongInfo)
            }

            itemView.setBackgroundColor(if (position % 2 == 0) yellow2Color
            else yellow3Color)
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount().mList.size = ${mList.size}")
        return mList.size
    }
}