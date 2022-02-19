package com.sedat.travelassistant.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sedat.travelassistant.databinding.SelectedRoutesAdapterItemBinding
import com.sedat.travelassistant.model.selectedroute.SelectedRoute
import javax.inject.Inject

class SelectedRouteAdapter @Inject constructor(): RecyclerView.Adapter<SelectedRouteAdapter.Holder>() {

    private val diffUtil = object :DiffUtil.ItemCallback<SelectedRoute>(){
        override fun areItemsTheSame(oldItem: SelectedRoute, newItem: SelectedRoute): Boolean {
            return oldItem.location == newItem.location
        }

        override fun areContentsTheSame(oldItem: SelectedRoute, newItem: SelectedRoute): Boolean {
            return oldItem == newItem
        }
    }

    private val recyclerListDiffer = AsyncListDiffer(this, diffUtil)

    var selectedRouteList: List<SelectedRoute>
        get() = recyclerListDiffer.currentList
        set(value) = recyclerListDiffer.submitList(value)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedRouteAdapter.Holder {
        val inflater = LayoutInflater.from(parent.context)
        val view = SelectedRoutesAdapterItemBinding.inflate(inflater, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: SelectedRouteAdapter.Holder, position: Int) {
        val selectedRoute = selectedRouteList[position]
        holder.item.routeName.text = "(${position+1}) ${selectedRoute.name}"
    }

    override fun getItemCount(): Int {
        return selectedRouteList.size
    }

    class Holder(val item: SelectedRoutesAdapterItemBinding): RecyclerView.ViewHolder(item.root)

}