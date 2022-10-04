package com.smile.karaokeplayer.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.models.SongInfo
import com.smile.smilelibraries.utilities.ScreenUtil

private const val TAG = "FaRecyclerVAdapter"

class FavoriteRecyclerViewAdapter private constructor(
        private var recyclerItemClickListener : OnRecyclerItemClickListener,
        private var textFontSize : Float,
        private var mList:  java.util.ArrayList<SongInfo>,
        private var yellow : Int, private var transparentLightGray : Int)

    : RecyclerView.Adapter<FavoriteRecyclerViewAdapter.MyViewHolder>() {

    interface OnRecyclerItemClickListener {
        fun onRecyclerItemClick(v: View?, position: Int)
    }

    companion object {
        private var viewAdapter : FavoriteRecyclerViewAdapter? = null
        @JvmStatic
        fun getInstance(recyclerItemClickListener : OnRecyclerItemClickListener,
                        textFontSize : Float,
                        mList : java.util.ArrayList<SongInfo>,
                        yellow : Int, transparentLightGray : Int) : FavoriteRecyclerViewAdapter {

            Log.d(TAG, "getInstance.viewAdapter = $viewAdapter")
            if (viewAdapter == null) {
                viewAdapter = FavoriteRecyclerViewAdapter(recyclerItemClickListener,
                        textFontSize, mList, yellow, transparentLightGray)
            } else {
                viewAdapter?.let {
                    it.recyclerItemClickListener = recyclerItemClickListener
                    it.textFontSize = textFontSize
                    it.mList = mList
                    it.yellow = yellow
                    it.transparentLightGray = transparentLightGray
                }
            }

            return viewAdapter!!
        }
    }

    class MyViewHolder(itemView: View,
                       recyclerItemClickListener : OnRecyclerItemClickListener,
                       textFontSize: Float)
        : RecyclerView.ViewHolder(itemView) {
        val songNameTextView: TextView
        val songPathTextView: TextView
        init {
            Log.d(TAG, "MyViewHolder() is called")
            songNameTextView = itemView.findViewById(R.id.myListNameTextView)
            ScreenUtil.resizeTextSize(songNameTextView, textFontSize, BaseApplication.FontSize_Scale_Type)
            songPathTextView = itemView.findViewById(R.id.myListPathTextView)
            ScreenUtil.resizeTextSize(songPathTextView, textFontSize, BaseApplication.FontSize_Scale_Type)

            itemView.setOnClickListener {
                recyclerItemClickListener.onRecyclerItemClick(
                    itemView, bindingAdapterPosition
                )
            }
        }
    }

    // Involves populating data into the item through holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        Log.d(TAG, "onCreateViewHolder().mList.size = ${mList.size}")
        val layoutInflater = LayoutInflater.from(parent.context)
        val fileView = layoutInflater.inflate(R.layout.fragment_my_favorites_item, parent, false)
        return MyViewHolder(fileView, recyclerItemClickListener, textFontSize)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.songNameTextView.apply {
            text = mList[position].songName
            visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
        }
        holder.songPathTextView.apply {
            mList[position].let {
                if (it.included == "1") setTextColor(yellow)
                else setTextColor(Color.WHITE)
                text = it.filePath?: ""
            }
        }

        holder.itemView.setBackgroundColor(if (position % 2 == 0) Color.BLACK
        else transparentLightGray)
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount().mList.size = ${mList.size}")
        return mList.size
    }
}