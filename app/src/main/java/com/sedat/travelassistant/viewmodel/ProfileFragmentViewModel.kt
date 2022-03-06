package com.sedat.travelassistant.viewmodel

import android.app.Application
import android.content.Context
import com.sedat.travelassistant.model.firebase.User
import com.sedat.travelassistant.repo.PlaceRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class ProfileFragmentViewModel @Inject constructor(
    private val repository: PlaceRepositoryInterface,
    @ApplicationContext private val context: Context
): BaseViewModel(context as Application) {
    fun getUserInfo(userId: String, listener: (User)->Unit) = repository.getUserInfo(userId, listener)
}