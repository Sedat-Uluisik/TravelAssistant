package com.sedat.travelassistant.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.sedat.travelassistant.databinding.ImageItemLayoutBinding
import com.sedat.travelassistant.model.room.ImagePath
import javax.inject.Inject

class SavedImagesAdapter @Inject constructor(val glide: RequestManager): RecyclerView.Adapter<SavedImagesAdapter.Holder>() {

    private val diffUtil = object :DiffUtil.ItemCallback<ImagePath>(){
        override fun areItemsTheSame(oldItem: ImagePath, newItem: ImagePath): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ImagePath, newItem: ImagePath): Boolean {
            return oldItem == newItem
        }
    }

    private val recyclerList = AsyncListDiffer(this, diffUtil)

    var imageList: List<ImagePath>
        get() = recyclerList.currentList
        set(value) = recyclerList.submitList(value)

    private var onImageClick: ((url: String) -> Unit) ?= null
    fun onImageClickListener(listener: (String) -> Unit){
        onImageClick = listener
    }

    private var onDeleteClick: ((image: ImagePath) -> Unit) ?= null
    fun onDeleteButtonClickListener(listener: (ImagePath) -> Unit){
        onDeleteClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedImagesAdapter.Holder {
        val view = ImageItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: SavedImagesAdapter.Holder, position: Int) {
        if(imageList.isNotEmpty()){

            val image = imageList[position]

            glide.load(image.image_path).into(holder.item.placeImage)

            holder.item.placeImage.setOnClickListener {
                onImageClick?.let {
                    it(image.image_path)
                }
            }

            holder.item.deleteImageButton.setOnClickListener {
                onDeleteClick?.let {
                    it(image)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    class Holder(val item: ImageItemLayoutBinding): RecyclerView.ViewHolder(item.root)

}