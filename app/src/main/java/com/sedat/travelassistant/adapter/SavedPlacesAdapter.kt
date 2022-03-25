package com.sedat.travelassistant.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.sedat.travelassistant.R
import com.sedat.travelassistant.databinding.SavedPlaceItemLayoutBinding
import com.sedat.travelassistant.model.room.ImagePath
import com.sedat.travelassistant.model.room.SavedPlace
import javax.inject.Inject

class SavedPlacesAdapter @Inject constructor(
    private val glide: RequestManager
): RecyclerView.Adapter<SavedPlacesAdapter.ViewHolder>() {

    private val diffCallBack = object :DiffUtil.ItemCallback<SavedPlace>(){
        override fun areItemsTheSame(oldItem: SavedPlace, newItem: SavedPlace): Boolean {
            return oldItem.lat == newItem.lat && oldItem.lon == newItem.lon
        }

        override fun areContentsTheSame(oldItem: SavedPlace, newItem: SavedPlace): Boolean {
            return oldItem == newItem
        }

    }

    private val recyclerListDiffer = AsyncListDiffer(this, diffCallBack)

    var placeList: List<SavedPlace>
        get() = recyclerListDiffer.currentList
        set(value) = recyclerListDiffer.submitList(value)

    var imageList = mutableListOf<ImagePath>()

    private var onItemClick: ((SavedPlace) -> Unit) ?= null
    fun onSavedItemClickListener(listener: (SavedPlace)-> Unit){
        onItemClick = listener
    }

    private var onMoreButtonClick: ((SavedPlace, View)-> Unit) ?= null
    fun onMoreButtonClickListener(listener: (SavedPlace, View) -> Unit){
        onMoreButtonClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedPlacesAdapter.ViewHolder {
        val view = SavedPlaceItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedPlacesAdapter.ViewHolder, position: Int) {
        val place = placeList[position]

        holder.item.savedCity.text = place.name

        holder.itemView.setOnClickListener {
            onItemClick?.let {
                it(place)
            }
        }

        holder.item.moreButton.setOnClickListener { view ->
            onMoreButtonClick?.let {
                it(place, view)
            }
        }

        if(imageList.size > 0){
            var isImage = false
            for (i in imageList){
                if(i.latLong == "${place.lat}_${place.lon}"){
                    isImage = true
                    glide.load(i.image_path).transform(CircleCrop()).into(holder.item.imageView)
                    break
                }
            }
            if(!isImage)
                glide.load(R.drawable.add_image_white_100).into(holder.item.imageView)
        }
    }

    override fun getItemCount(): Int {
        return placeList.size
    }

    class ViewHolder(val item: SavedPlaceItemLayoutBinding): RecyclerView.ViewHolder(item.root)

    @SuppressLint("NotifyDataSetChanged")
    fun refreshData(){
        notifyDataSetChanged()
    }

}