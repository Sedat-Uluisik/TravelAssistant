package com.sedat.travelassistant.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
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

    fun checkLocationInDatabase(place: Properties) = repository.checkLocationInDatabase(place)
    fun postComment(place: Properties, comment: Comment) = repository.postComment(place, comment)

    fun clearData(){
        imageList.value = null
        detailInfo.value = null
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}