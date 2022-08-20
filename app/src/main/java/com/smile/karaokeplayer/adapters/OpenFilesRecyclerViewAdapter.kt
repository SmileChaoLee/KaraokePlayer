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
import com.smile.smilelibraries.utilities.ScreenUtil
import java.io.File

private const val TAG = "FilesRecyclerVAdapter"

class OpenFilesRecyclerViewAdapter(private val context: Context,
                                   private val recyclerItemClickListener: OnRecyclerItemClickListener,
                                   private val textFontSize: Float,
                                   private val files: java.util.ArrayList<File>)
    : RecyclerView.Adapter<OpenFilesRecyclerViewAdapter.MyViewHolder>() {

    interface OnRecyclerItemClickListener {
        fun onRecyclerItemClick(v: View?, position: Int)
    }

    class MyViewHolder(itemView: View,
                       recyclerItemClickListener : OnRecyclerItemClickListener,
                       textFontSize: Float)
        : RecyclerView.ViewHolder(itemView) {
        val fileNameTextView: TextView
        init {
            Log.d(TAG, "MyViewHolder() is called")
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView)
            ScreenUtil.resizeTextSize(fileNameTextView, textFontSize,
                BaseApplication.FontSize_Scale_Type
            )
            itemView.setOnClickListener {
                Log.d(TAG, "itemView.setOnClickListener() is called")
                recyclerItemClickListener.onRecyclerItemClick(
                    itemView, bindingAdapterPosition
                )
            }
        }
    }

    // Involves populating data into the item through holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val fileView = layoutInflater.inflate(R.layout.files_list_item, parent, false)
        return MyViewHolder(fileView, recyclerItemClickListener, textFontSize)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.fileNameTextView.text = files[position].name
        val backgroundColor = if (position % 2 == 0) Color.BLACK
        else ContextCompat.getColor(context, R.color.transparentLightGray)
        holder.itemView.setBackgroundColor(backgroundColor)
    }

    override fun getItemCount(): Int {
        return files.size
    }
}