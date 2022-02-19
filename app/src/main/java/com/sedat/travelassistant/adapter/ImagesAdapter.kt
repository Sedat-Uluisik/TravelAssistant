package com.sedat.travelassistant.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.sedat.travelassistant.databinding.ImageItemLayoutBinding
import com.sedat.travelassistant.model.image.PlaceImage
import com.sedat.travelassistant.model.image.Value
import javax.inject.Inject

class ImagesAdapter @Inject constructor(
        private val glide: RequestManager
): RecyclerView.Adapter<ImagesAdapter.Holder>() {

    private val diffUtil = object :DiffUtil.ItemCallback<Value>(){
        override fun areItemsTheSame(oldItem: Value, newItem: Value): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Value, newItem: Value): Boolean {
            return oldItem == newItem
        }
    }

    private val recyclerListDiffer = AsyncListDiffer(this, diffUtil)

    var images: List<Value>
        get() = recyclerListDiffer.currentList
        set(value) = recyclerListDiffer.submitList(value)

    private var imageClickListener: ((String) -> Unit) ?= null
    fun imageClick(listener: (String) -> Unit){
        imageClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagesAdapter.Holder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ImageItemLayoutBinding.inflate(inflater, parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: ImagesAdapter.Holder, position: Int) {
        if(images.isNotEmpty()){
            val image = images[position]
            glide.load(image.thumbnailUrl).into(holder.item.placeImage)

            holder.item.placeImage.setOnClickListener {
                imageClickListener?.let {
                    it(
                            if(image.contentUrl.isEmpty() || image.contentUrl == ""){
                                image.thumbnailUrl
                            }
                            else{
                                image.contentUrl
                            }
                    )
                }
            }

            holder.item.deleteImageButton.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    class Holder(val item: ImageItemLayoutBinding): RecyclerView.ViewHolder(item.root)

}