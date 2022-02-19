package com.sedat.travelassistant.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sedat.travelassistant.databinding.CategoryItemLayoutRootBinding
import com.sedat.travelassistant.databinding.CategoryItemLayoutSubBinding
import com.sedat.travelassistant.model.room.Categories
import javax.inject.Inject

class CategoriesAdapter @Inject constructor(

): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val diffUtil = object : DiffUtil.ItemCallback<Categories>(){
        override fun areItemsTheSame(oldItem: Categories, newItem: Categories): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Categories, newItem: Categories): Boolean {
            return oldItem == newItem
        }
    }

    private val recyclerListDiffer = AsyncListDiffer(this, diffUtil)

    var categoryList: List<Categories>
        get() = recyclerListDiffer.currentList
        set(value) = recyclerListDiffer.submitList(value)

    private var onCategoryClick:((String) -> Unit) ?= null
    fun setOnCategoryClick(listener: (String) -> Unit){
        onCategoryClick = listener
    }

    var languageCode = "tr"


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if(viewType == 1){
            val view = CategoryItemLayoutRootBinding.inflate(inflater, parent, false)
            return RootHolder(view, languageCode)
        }else{
            val view = CategoryItemLayoutSubBinding.inflate(inflater, parent, false)
            return SubHolder(view, languageCode)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(getItemViewType(position) == 1)
            (holder as RootHolder).bind(categoryList[position])
        else
            (holder as SubHolder).bind(categoryList[position])

        holder.itemView.setOnClickListener {
            if(!categoryList[position].path.isNullOrEmpty()){
                onCategoryClick?.let {
                    it(categoryList[position].path.toString())
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if(categoryList[position].type == 1)
            1
        else
            0
    }

    class RootHolder(val item: CategoryItemLayoutRootBinding, val languageCode: String): RecyclerView.ViewHolder(item.root){
        fun bind(categories: Categories){
            item.rootName.text = if(languageCode == "tr")categories.name_tr else categories.name_en
        }
    }
    class SubHolder(val item: CategoryItemLayoutSubBinding, val languageCode: String): RecyclerView.ViewHolder(item.root){
        fun bind(categories: Categories){
            item.subName.text = if(languageCode == "tr")categories.name_tr else categories.name_en
        }
    }

}