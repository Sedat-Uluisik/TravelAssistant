package com.sedat.travelassistant.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sedat.travelassistant.R
import com.sedat.travelassistant.model.Properties
import com.sedat.travelassistant.model.firebase.Comment
import com.sedat.travelassistant.model.image.PlaceImage
import com.sedat.travelassistant.model.info.Detail
import com.sedat.travelassistant.model.info.Info
import com.sedat.travelassistant.model.room.SavedPlace
import com.sedat.travelassistant.repo.PlaceRepositoryInterface
import com.sedat.travelassistant.util.VIKIPEDIA_URL
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import retrofit2.Call
import java.lang.reflect.Type
import javax.inject.Inject
import retrofit2.Callback
import retrofit2.Response

@HiltViewModel
class DetailsFragmentViewModel @Inject constructor(
        private val repository: PlaceRepositoryInterface,
        @ApplicationContext private val application: Context
): BaseViewModel(application as Application) {

    private val disposable = CompositeDisposable()

    val imageList = MutableLiveData<PlaceImage>()
    fun getPlaceImages(query: String){
        disposable.add(
                repository.getImage(query)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(object: DisposableSingleObserver<PlaceImage>(){
                            override fun onSuccess(t: PlaceImage) {
                                /*for (i in t.value){
                                    /* println("-------------------------")
                                     println(i.name)
                                     println("content url -> " + i.contentUrl)
                                     println("thumbnail url -> " + i.thumbnailUrl)
                                     println("-------------------------")*/
                                }*/
                                imageList.value = t
                            }

                            override fun onError(e: Throwable) {
                                println(e.message)
                            }

                        })
        )
    }

    val detailInfo = MutableLiveData<String>()
    fun getInfo(q: String){
        disposable.add(
                repository.getInfo(VIKIPEDIA_URL, q)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(object :DisposableSingleObserver<Info>(){
                            override fun onSuccess(t: Info) {
                                t.query.pages.map {
                                    detailInfo.value = it.value.detailText
                                }
                            }

                            override fun onError(e: Throwable) {
                                println(e.message)
                            }

                        })
        )
    }

    private val comments = MutableLiveData<List<Comment>>()
    val commentList: LiveData<List<Comment>>
        get() = comments
    private val ratingValue = MutableLiveData<Float>()
    val rating: LiveData<Float>
        get() = ratingValue
    fun checkLocationInDatabase(place: Properties) = repository.checkLocationInDatabase(place){ list, error ->
        if(list.isNotEmpty()){
            comments.value = list
            repository.getRating(place.placeId){
                ratingValue.value = it / list.size
            }
        }
        else{
            comments.value = list
            ratingValue.value = 0.0f
        }
    }

    val isDataSend = MutableLiveData<Boolean>(false)
    fun postComment(place: Properties, comment: Comment) = repository.postComment(place, comment){
        isDataSend.value = it
    }

    fun likeOrDislikeButtonClick(placeId: String, commentId: String, userId: String, likeOrDislike: Boolean) = repository.likeOrDislikeButtonClick(placeId, commentId, userId, likeOrDislike)

    fun updateComment(placeId: String, comment: Comment, listener: (Boolean) -> Unit) = repository.updateComment(placeId, comment, listener)
    fun deleteComment(placeId: String, commentId: String, userId: String) = repository.deleteComment(placeId, commentId, userId)
    fun updateRating(placeId: String, oldRating: Float, newRating: Float) = repository.updateRating(placeId, oldRating, newRating)

    fun clearData(){
        imageList.value = null
        detailInfo.value = null
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
        comments.value = listOf()
    }
}