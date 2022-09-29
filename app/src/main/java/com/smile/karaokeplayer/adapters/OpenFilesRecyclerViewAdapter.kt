package com.smile.karaokeplayer.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.models.FileDescription
import com.smile.smilelibraries.utilities.ScreenUtil

private const val TAG = "FilesRecyclerVAdapter"

class OpenFilesRecyclerViewAdapter private constructor(
        private var recyclerItemClickListener: OnRecyclerItemClickListener,
        private var textFontSize: Float,
        private var files: java.util.ArrayList<FileDescription>,
        private var yellow: Int, private var transparentLightGray: Int)

    : RecyclerView.Adapter<OpenFilesRecyclerViewAdapter.MyViewHolder>() {

    interface OnRecyclerItemClickListener {
        fun onRecyclerItemClick(v: View?, position: Int)
    }

    companion object {
        private var viewAdapter : OpenFilesRecyclerViewAdapter? = null
        @JvmStatic
        fun getInstance(recyclerItemClickListener: OnRecyclerItemClickListener,
                        textFontSize: Float,
                        files: java.util.ArrayList<FileDescription>,
                        yellow: Int, transparentLightGray: Int) : OpenFilesRecyclerViewAdapter {

            Log.d(TAG, "getInstance.viewAdapter = $viewAdapter")
            if (viewAdapter == null) {
                viewAdapter = OpenFilesRecyclerViewAdapter(recyclerItemClickListener,
                        textFontSize, files, yellow, transparentLightGray)
            } else {
                viewAdapter?.let {
                    it.recyclerItemClickListener = recyclerItemClickListener
                    it.textFontSize = textFontSize
                    it.files = files
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
        val fileNameTextView: TextView
        val folderImageView: ImageView
        init {
            Log.d(TAG, "MyViewHolder() is called")
            fileNameTextView = itemView.findViewById(R.id.openFileNameTextView)
            ScreenUtil.resizeTextSize(fileNameTextView, textFontSize, BaseApplication.FontSize_Scale_Type)

            folderImageView = itemView.findViewById(R.id.folderImageView)
            val layoutParams: ViewGroup.MarginLayoutParams = folderImageView.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = (textFontSize * 1.0f).toInt()
            layoutParams.height = layoutParams.width

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
        val fileView = layoutInflater.inflate(R.layout.fragment_open_file_item, parent, false)
        return MyViewHolder(fileView, recyclerItemClickListener, textFontSize)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.folderImageView.apply {
            visibility = if (files[position].file.isDirectory) View.VISIBLE else View.INVISIBLE
        }
        holder.fileNameTextView.apply {
            text = files[position].file.name
            setTextColor(Color.WHITE)
            if (files[position].selected) setTextColor(yellow)
        }

        holder.itemView.setBackgroundColor(if (position % 2 == 0) Color.BLACK
        else transparentLightGray)
    }

    override fun getItemCount(): Int {
        return files.size
    }
}