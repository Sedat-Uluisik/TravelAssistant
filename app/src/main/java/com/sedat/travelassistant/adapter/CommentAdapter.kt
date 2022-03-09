package com.sedat.travelassistant.adapter

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sedat.travelassistant.databinding.CommentItemLayoutBinding
import com.sedat.travelassistant.model.firebase.Comment
import javax.inject.Inject

class CommentAdapter @Inject constructor(

): RecyclerView.Adapter<CommentAdapter.Holder>() {

    private val diffUtil = object :DiffUtil.ItemCallback<Comment>(){
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            //Burada genellikle, item'lerin benzersiz id'leri varsa id'ler karşılaştırılır.
            return oldItem.commentId == newItem.commentId
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            //Burada item'lerin içerikleri karşılaştırılır, eğer değişiklik varsa gösterilir.
            return oldItem == newItem
        }
    }

    private val recyclerListDiffer = AsyncListDiffer(this, diffUtil)

    var commentList: List<Comment>
        get() = recyclerListDiffer.currentList
        set(value) = recyclerListDiffer.submitList(value)

    private var onLikeDislikeButtonClick: ((String, Int) -> Unit) ?= null  //String: commentId Int: likeButton or dislikeButton
    fun likeDislikeButton(listener: (String, Int) -> Unit){
        onLikeDislikeButtonClick = listener
    }
    private var onMoreButtonClick: ((Comment, View) -> Unit) ?= null
    fun moreButtonClickListener(listener: (Comment, View) -> Unit){
        onMoreButtonClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentAdapter.Holder {
        val view = CommentItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: CommentAdapter.Holder, position: Int) {
        val comment = commentList[position]
        holder.bind(comment)

        holder.item.likeButton.setOnClickListener { view ->
            onLikeDislikeButtonClick?.let {
                it(comment.commentId, 1)       //1: likeButton 2: dislikeButton
                view.isEnabled = false
                holder.item.dislikeButton.isEnabled = true
            }
        }
        holder.item.dislikeButton.setOnClickListener { view ->
            onLikeDislikeButtonClick?.let {
                it(comment.commentId, 2)
                view.isEnabled = false
                holder.item.likeButton.isEnabled = true
            }
        }
        holder.item.commentMoreButton.setOnClickListener { v ->
            onMoreButtonClick?.let {
                it(comment, v)
            }
        }
    }

    override fun getItemCount(): Int {
        return commentList.size
    }

    class Holder(val item: CommentItemLayoutBinding): RecyclerView.ViewHolder(item.root){
        fun bind(comment: Comment){
            item.usernameCommentItem.text = comment.userName
            val date = DateFormat.format("dd/MM/yyyy", comment.date)
            item.dateCommentItem.text = date.toString()
            item.ratingBarCommentItem.rating = comment.rating
            item.commentTextCommentItem.text = comment.Comment
            item.likeNumber.text = comment.likeNumber.toString()
            item.dislikeNumber.text = comment.dislikeNumber.toString()
        }
    }

}