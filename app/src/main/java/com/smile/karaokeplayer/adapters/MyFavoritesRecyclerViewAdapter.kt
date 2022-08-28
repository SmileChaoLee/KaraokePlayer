package com.smile.karaokeplayer.adapters

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.models.SongInfo
import com.smile.smilelibraries.utilities.ScreenUtil

private const val TAG = "FavoritesRecyclerVAdapter"

class MyFavoritesRecyclerViewAdapter(private val context: Context,
                                     private val recyclerItemClickListener: OnRecyclerItemClickListener,
                                     private val textFontSize: Float,
                                     private val songInfoList: java.util.ArrayList<SongInfo>)
    : RecyclerView.Adapter<MyFavoritesRecyclerViewAdapter.MyViewHolder>() {

    interface OnRecyclerItemClickListener {
        fun onRecyclerItemClick(v: View?, position: Int)
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
        val layoutInflater = LayoutInflater.from(parent.context)
        val fileView = layoutInflater.inflate(R.layout.fragment_my_favorites_item, parent, false)
        return MyViewHolder(fileView, recyclerItemClickListener, textFontSize)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.songNameTextView.apply {
            text = songInfoList[position].songName
            visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
        }
        holder.songPathTextView.apply {
            text = ""
            songInfoList[position].let {
                if (it.included == "1") setTextColor(ContextCompat.getColor(context, R.color.yellow))
                else setTextColor(Color.WHITE)
                it.filePath?.let {pathIt ->
                    val lastIndex = pathIt.lastIndexOf('/')
                    if (lastIndex >=0 ) text = pathIt.substring(lastIndex+1)
                }
            }
        }

        holder.itemView.setBackgroundColor(if (position % 2 == 0) Color.BLACK
        else ContextCompat.getColor(context, R.color.transparentLightGray))
    }

    override fun getItemCount(): Int {
        return songInfoList.size
    }
}