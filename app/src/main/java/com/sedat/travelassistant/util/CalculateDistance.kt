package com.sedat.travelassistant.util

import android.location.Location
import com.sedat.travelassistant.model.selectedroute.SelectedRoute
import kotlinx.coroutines.*

class CalculateDistance {
    //Seçilen rota noktalarını en yakına göre yeniden sıralar.
    suspend fun calculate(list: MutableList<SelectedRoute>): MutableList<SelectedRoute>{

        val currentList: MutableList<SelectedRoute> = list

        val userPoint = currentList[0].location

        for (i in 1 until currentList.size){

            val point = currentList[i].location

            var smallest = userPoint.distanceTo(point)
            var selectedRoute = currentList[i]

            for (j in i until currentList.size){

                val point1 = currentList[j].location

                val distance = userPoint.distanceTo(point1)

                if(distance < smallest){
                    currentList[i] = currentList[j]
                    currentList[j] = selectedRoute
                    selectedRoute = currentList[i]
                    smallest = distance
                }
            }
        }

        return currentList
    }

    suspend fun calculateDistanceTwoPoint(userLocation: Location?, endLocation: Location?): Float{
        return if(userLocation != null && endLocation != null) userLocation.distanceTo(endLocation) else 0f
    }
}