package com.sedat.travelassistant.repo

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.storage.FirebaseStorage
import com.sedat.travelassistant.R
import com.sedat.travelassistant.api.PlacesApi
import com.sedat.travelassistant.model.Place
import com.sedat.travelassistant.model.Properties
import com.sedat.travelassistant.model.firebase.Comment
import com.sedat.travelassistant.model.firebase.User
import com.sedat.travelassistant.model.visitedlocaions.VisitedLocations
import com.sedat.travelassistant.model.image.PlaceImage
import com.sedat.travelassistant.model.info.Info
import com.sedat.travelassistant.model.room.Categories
import com.sedat.travelassistant.model.room.ImagePath
import com.sedat.travelassistant.model.room.SavedPlace
import com.sedat.travelassistant.model.route.Route
import com.sedat.travelassistant.room.TravelDatabase
import com.sedat.travelassistant.util.SaveImageToFile
import com.sedat.travelassistant.util.firebasereferences.References
import com.sedat.travelassistant.util.firebasereferences.References.comments
import com.sedat.travelassistant.util.firebasereferences.References.dislikeNumber
import com.sedat.travelassistant.util.firebasereferences.References.images
import com.sedat.travelassistant.util.firebasereferences.References.likeNumber
import com.sedat.travelassistant.util.firebasereferences.References.likeOrDislike
import com.sedat.travelassistant.util.firebasereferences.References.likeOrDislikeNumber
import com.sedat.travelassistant.util.firebasereferences.References.locations
import com.sedat.travelassistant.util.firebasereferences.References.userSavedLocations
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class PlaceRepository @Inject constructor(
        private val placesApi: PlacesApi,
        private val placesApiForRoute: PlacesApi,
        private val dbFirestore: FirebaseFirestore,
        private val auth: FirebaseAuth,
        private val storage: FirebaseStorage,
        @ApplicationContext private val context: Context
): PlaceRepositoryInterface {

    private val dao = TravelDatabase(context).dao()
    //private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun getPlace(category: String, latLong: String, limit: Int): Single<Place> {
        return placesApi.getPlace(category, latLong, limit)
    }

    override fun getImage(query: String): Single<PlaceImage> {
        return placesApi.getImage(query)
    }

    override fun getInfo(url: String, q: String): Single<Info> {
        return placesApi.getInfo(url, q)
    }

    override fun getRoute(routes: List<String>, profile: String): Single<Route> {
        return placesApiForRoute.getRoute(routes, profile)
    }

    override suspend fun getCategories(): List<Categories> {
        return dao.getCategories()
    }

    override suspend fun saveVisitedLocation(visitedLocations: VisitedLocations) {
        dao.saveVisitedLocation(visitedLocations)
    }

    override suspend fun getVisitedLocations(): List<VisitedLocations> {
        return dao.getVisitedLocations()
    }

    override suspend fun savePlaceForRoom(place: SavedPlace) {
        dao.savePlaceForRoom(place)
    }

    override suspend fun getPlaceWithLatLonFromRoom(lat: Double, lon: Double): SavedPlace {
        return dao.getPlaceWithLatLonFromRoom(lat, lon)
    }

    override suspend fun deleteSavedPlaceFromRoom(lat: Double, lon: Double) {
        dao.deleteSavedPlaceFromRoom(lat, lon)
    }

    override suspend fun getPlacesFromRoom(): List<SavedPlace> {
        return dao.getPlacesFromRoom()
    }

    override suspend fun updatePlaceFromRoom(savedPlace: SavedPlace) {
        dao.updatePlaceFromRoom(savedPlace)
    }

    override suspend fun saveImageForRoom(imagePath: ImagePath) {
        dao.saveImageForRoom(imagePath)
    }

    override suspend fun getSavedPlaceImages(latLong: String): List<ImagePath> {
        return dao.getSavedPlaceImages(latLong)
    }

    override suspend fun getAllSavedPlaceImages(
        callBack: (List<ImagePath>) -> Unit
    ) {
        callBack(dao.getAllSavedPlaceImages())
    }

    override suspend fun getOneImageFromSavedPlaces(latLongs: List<String>): List<ImagePath> {
        return dao.getOneImageFromSavedPlaces(latLongs)
    }

    override suspend fun deleteImagesFromRoom(id: Int, root_id: Int) {
        dao.deleteImagesFromRoom(id, root_id)
    }

    override suspend fun deleteAllImagesPathsWithLatLonFromRoom(latLong: String) {
        dao.deleteAllImagesPathsWithLatLonFromRoom(latLong)
    }

    override suspend fun fullTextSearch(query: String): List<SavedPlace> {
        return dao.fullTextSearch(query)
    }

    override fun checkLocationInDatabase(place: Properties, listener: (List<Comment>, error: String) -> Unit) {
        val docRef = dbFirestore.collection("Locations").document(place.placeId)
        docRef.get()
            .addOnSuccessListener { document ->
                if(document.data == null){
                    val place_ = HashMap<String, Any>()
                    place_["placeId"] = docRef.id
                    place_["rating"] = 0.0

                    docRef.set(place_)
                }

                getComments(docRef, place, listener)
            }
            .addOnFailureListener {
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                listener(listOf(), "error")
            }
    }

    override fun postComment(place: Properties, comment: Comment, callBack: (Boolean) -> Unit) {
        //check for place
        val docRef = dbFirestore.collection("Locations").document(place.placeId)
        docRef.get()
            .addOnSuccessListener { document ->
                if(document != null && document.data != null){
                    //lokasyon kayıtlı, yorumu yaz
                    sendComment(comment, docRef, callBack)
                }else{
                    //lokasyon kayıtlı değil, kaydet ve yorumu yaz
                    checkLocationInDatabase(place){ list, error ->

                    }
                    sendComment(comment, docRef, callBack)
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                callBack(false)
            }
    }

    override fun likeOrDislikeButtonClick(placeId: String, commentId: String, userId: String, likeOrDislike: Boolean) {
        /*
        likeOrDislike = true -> like button click
        likeOrDislike = false -> dislike button click
         */

        val commentRef = dbFirestore.collection(locations).document(placeId).collection(comments).document(commentId)
        val likeDislikeRef = dbFirestore.collection(locations).document(placeId).collection(comments).document(commentId)
            .collection(likeOrDislikeNumber).document(userId)

        likeDislikeRef.get()
            .addOnSuccessListener { doc ->
                if(doc.data == null){ //first like/dislike
                    incrementLikeOrDislikeNumber(commentRef, likeDislikeRef, likeOrDislike)
                }else{ //convert to like -> dislike or dislike -> like
                    switchLikeToDislikeOrDislikeToLike(commentRef, likeDislikeRef, likeOrDislike)
                }
            }
    }
    private fun switchLikeToDislikeOrDislikeToLike(commentRef: DocumentReference, likeDislikeRef: DocumentReference, likeOrDislike: Boolean){
        likeDislikeRef.get()
            .addOnSuccessListener {
                if(it.data != null){
                    val oldValue = it.get(References.likeOrDislike)
                    if(oldValue != likeOrDislike){
                        if(likeOrDislike){ //increment like number and decrement dislike number
                            likeDislikeRef.update(mapOf(
                                References.likeOrDislike to true
                            ))

                            commentRef.get()
                                .addOnSuccessListener{ doc ->
                                if(doc != null){
                                    var likeNumber = doc.get(likeNumber).toString().toFloat().toInt()
                                    var dislikeNumber = doc.get(dislikeNumber).toString().toFloat().toInt()

                                    likeNumber++
                                    dislikeNumber--
                                    commentRef.update(mapOf(
                                        References.likeNumber to likeNumber,
                                        References.dislikeNumber to dislikeNumber
                                    ))
                                }
                            }
                        }else{ //decrement like number and increment dislike number
                            likeDislikeRef.update(mapOf(
                                References.likeOrDislike to false
                            ))

                            commentRef.get()
                                .addOnSuccessListener{ doc ->
                                if(doc != null){
                                    var likeNumber = doc.get(likeNumber).toString().toFloat().toInt()
                                    var dislikeNumber = doc.get(dislikeNumber).toString().toFloat().toInt()

                                    likeNumber--
                                    dislikeNumber++
                                    commentRef.update(mapOf(
                                        References.likeNumber to likeNumber,
                                        References.dislikeNumber to dislikeNumber
                                    ))
                                }
                            }


                            /*commentRef.addSnapshotListener { value, error ->
                                if(value != null && error == null){
                                    var likeNumber = value.get(likeNumber).toString().toInt()
                                    var dislikeNumber = value.get(dislikeNumber).toString().toInt()

                                    likeNumber--
                                    dislikeNumber++
                                    commentRef.update(mapOf(
                                        References.likeNumber to likeNumber,
                                        References.dislikeNumber to dislikeNumber
                                    ))
                                }
                            }*/

                        }
                    }
                }
            }
    }
    private fun incrementLikeOrDislikeNumber(commentRef: DocumentReference, likeDislikeRef: DocumentReference, likeOrDislike: Boolean){
        commentRef.get()
            .addOnSuccessListener{ doc ->
            if(doc != null){
                var likeNumber = doc.get("likeNumber").toString().toFloat().toInt()
                var dislikeNumber = doc.get("dislikeNumber").toString().toFloat().toInt()
                if(likeOrDislike){ //increment like number
                    likeNumber++
                   commentRef.update(mapOf(
                        "likeNumber" to likeNumber
                    )).addOnSuccessListener {
                        val map = HashMap<String, Any>()
                        map["likeOrDislike"] = true
                        likeDislikeRef.set(map)
                    }
                }else{ //increment dislike number
                    dislikeNumber++
                   commentRef.update(mapOf(
                        "dislikeNumber" to dislikeNumber
                    )).addOnSuccessListener {
                        val map = HashMap<String, Any>()
                        map["likeOrDislike"] = false
                        likeDislikeRef.set(map)
                    }
                }
            }
        }


        /*commentRef.addSnapshotListener { value, error ->
            if(value != null && error == null){
                var likeNumber = value.get("likeNumber").toString().toFloat()
                var dislikeNumber = value.get("dislikeNumber").toString().toFloat()
                if(likeOrDislike){ //increment like number
                    likeNumber++
                    commentRef.update(mapOf(
                        "likeNumber" to likeNumber
                    )).addOnSuccessListener {
                        val map = HashMap<String, Any>()
                        map["likeOrDislike"] = true
                        likeDislikeRef.set(map)
                    }
                }else{ //increment dislike number
                    dislikeNumber--
                    commentRef.update(mapOf(
                        "dislikeNumber" to dislikeNumber
                    )).addOnSuccessListener {
                        val map = HashMap<String, Any>()
                        map["likeOrDislike"] = false
                        likeDislikeRef.set(map)
                    }
                }
            }
        }*/

    }

    override fun updateComment(placeId: String, comment: Comment, listener: (Boolean) -> Unit) {
        val commentRef = dbFirestore
            .collection("Locations")
            .document(placeId)
            .collection("Comments")
            .document(comment.commentId)

        commentRef.get()
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    commentRef.update(mapOf(
                        "comment" to comment.Comment,
                        "date" to System.currentTimeMillis(),
                        "rating" to comment.rating
                    )).addOnCompleteListener {
                        if(it.isSuccessful)
                            listener(true)
                        else
                            listener(false)
                    }
                }
            }

    }

    override fun deleteComment(placeId: String, commentId: String, userId: String) {
        val ref = dbFirestore.collection("Locations")
            .document(placeId)

        val ref2 = ref.collection("Comments")
            .document(commentId)

        ref2.get().addOnSuccessListener {
            if(it.data != null){
                val c_id = it.get("commentId")
                val u_id = it.get("userId")
                val commentRating = it.get("rating").toString().toFloat()

                if(c_id == commentId && u_id == userId){

                    ref2.collection("LikeOrDislikeNumber")
                        .addSnapshotListener { value, error ->

                            if(value != null && value.documents.isNotEmpty()){
                                for (i in value.documents){  //yoruma ait beğenen ve beğenmeyen kullanıcılar koleksiyonu silinir.
                                    i.reference.delete()
                                }
                                ref2.delete() //devamında yorumun kendisi silindi.
                            }else
                                ref2.delete()

                            //update rating for place
                            ref.get()
                                .addOnSuccessListener { doc ->
                                    if(doc != null){
                                        var originalRating = doc.get("rating").toString().toFloat()
                                        originalRating -= commentRating
                                        ref.update(mapOf(
                                            "rating" to originalRating
                                        ))
                                    }
                                }
                        }
                }
            }
        }
    }

    private fun getComments(ref: DocumentReference, place: Properties, listener: (List<Comment>, error: String) -> Unit) {
        ref.get()
            .addOnSuccessListener { document ->
                if(document != null && document.data != null){
                    if(place.placeId == document.get("placeId")){
                        val newRef = ref.collection("Comments")

                        newRef.orderBy("date", Query.Direction.DESCENDING)
                            .addSnapshotListener { snapshot, error ->
                                if(error != null){

                                    listener(listOf(),error.message.toString())
                                    return@addSnapshotListener
                                }
                                if(snapshot != null){

                                    val data = snapshot.toObjects<Comment>()

                                    if(data.isNotEmpty())
                                        listener(data, "")
                                    else
                                        listener(listOf(), "error")
                                }else
                                    listener(listOf(), "error")
                            }
                    }
                }
            }.addOnCanceledListener {
                listener(listOf(), "error")
            }.addOnFailureListener {
                listener(listOf(),"error")
            }
    }

    override fun getRating(placeId: String, listener: (Float) -> Unit){
        val ref = dbFirestore.collection("Locations").document(placeId) //yorumlarda değişiklikler olduğunda güncel değerleri alabilmek için yeniden referans ve listener oluşturuldu.

        ref.addSnapshotListener { value, error ->

            if(value != null){
                val rating = value.get("rating").toString().toFloat()
                listener(rating)
            }else
                listener(0.0f)

            if(error != null)
                listener(0.0f)
        }
    }

    override fun updateRating(placeId: String, oldRating: Float, newRating: Float) {
        val placeRef = dbFirestore
            .collection("Locations")
            .document(placeId)

        placeRef.get()
            .addOnSuccessListener { doc ->
                if(doc.data != null){
                    var rating = doc.get("rating").toString().toFloat()
                    rating -= oldRating
                    rating += newRating

                    placeRef.update(mapOf(
                        "rating" to rating
                    ))
                }
            }
    }

    override fun getUserInfo(userId: String, listener: (User) -> Unit) {
        dbFirestore.collection("Users").document(userId)
            .addSnapshotListener { value, error ->

                if(error != null)
                    return@addSnapshotListener

                if(value != null){
                    val user = value.toObject<User>()
                    if(user != null)
                        listener(user)
                }
            }
    }

    override fun sendVerificationEmail(listener: (Boolean) -> Unit) {
        if(auth.currentUser != null){
            auth.currentUser!!.sendEmailVerification()
                .addOnCompleteListener {
                    if(it.isSuccessful){
                        Toast.makeText(context, "doğrulama linki gönderildi", Toast.LENGTH_LONG).show()
                        auth.signOut()
                        listener(true)
                    }else{
                        auth.signOut()
                        Toast.makeText(context, "doğrulama linki gönderilemedi", Toast.LENGTH_LONG).show()
                        listener(false)
                    }
                }
        }
    }

    override fun saveLocationsToFirebaseAndDeleteOldLocations(locationList: List<SavedPlace>, imageList: List<ImagePath>, userId: String) {
        /*
        firebase deki kayıtları silip telefondaki kayıtları yükler.
         */
        val savedLocationsRef = dbFirestore
            .collection(userSavedLocations)
            .document(userId)
            .collection(locations)

        deleteOldLocationsToFirebase(savedLocationsRef){
            if(it){
                for (i in locationList){
                    val map = HashMap<String, Any>()
                    map["rowid"] = i.rowid
                    map["name"] = i.name
                    map["city"] = i.city
                    map["district"] = i.district
                    map["address"] = i.address
                    map["state"] = i.state
                    map["street"] = i.street
                    map["suburb"] = i.suburb
                    map["lat"] = i.lat
                    map["lon"] = i.lon

                    val imageUrlRef = savedLocationsRef.document("${i.lat}_${i.lon}")
                    imageUrlRef.set(map)

                    //kaydedilen resmin url si alınıp firestore da ilgili lokasyon altına kaydedilir.
                    if(imageList.isNotEmpty()){
                        val list = imageList.filter { imagePath ->
                            imagePath.latLong == "${i.lat}_${i.lon}"
                        }

                        saveLocationImagesToFirebase(list, userId, "${i.lat}_${i.lon}"){ downloadUrl, imageStorageRef, imageId -> //lokasyona ait resimler tekrar storage a yüklenir
                            imageUrlRef.collection(images).document(imageId)
                                .set(mapOf(
                                    "imageStorageUrl" to downloadUrl,
                                    "imageStorageRef" to imageStorageRef,
                                    "imageId" to imageId
                                ))
                        }
                    }
                }
                    Toast.makeText(context, "buluta yükleme başarılı", Toast.LENGTH_LONG).show()
            }else
                Toast.makeText(context, "buluta yükleme başarısız", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveLocationImagesToFirebase(imageList: List<ImagePath>, userId: String, latLong: String, callBack: (String,String, String) -> Unit){
        for (i in imageList){
            val stream = FileInputStream(File(i.image_path))

            val file = Uri.fromFile(File(i.image_path))
            val imageId = file.lastPathSegment

            val imageRef = storage.reference.child("TravelGuide/$userId/$latLong/$imageId")
            imageRef.putStream(stream)
                .continueWithTask { task->
                    if(!task.isSuccessful){
                        task.exception?.let {
                            throw it
                        }
                    }
                    imageRef.downloadUrl
                }.addOnCompleteListener { task->
                    if(task.isSuccessful)
                        callBack(task.result.toString(), imageRef.path, imageId.toString())
                }
        }
    }

    private fun deleteOldLocationsToFirebase(savedLocationsRef: CollectionReference, listener: (Boolean) -> Unit){
        /*
        telefondaki kayıtları firebase db ye kaydetmek için eski kayıtları siler.
         */
        savedLocationsRef.get().addOnCompleteListener {
            if(it.isSuccessful){
                if(it.result.documents.isNotEmpty()){
                    val size = it.result.documents.size
                    var j = 0
                    for (i in it.result.documents){

                        val ref = savedLocationsRef.document(i.id).collection(images)
                        deleteAllImageReferenceAndImagesToFirebase(ref){ bool-> //önce resim referansları silinir.
                            if(bool || !bool){
                                j++
                                i.reference.delete() //daha sonra bir üst coleksiyon/doküman a ait referans silinir.
                                if(j == size)
                                    listener(true)
                            }
                        }
                    }
                }else
                    listener(true)
            }else
                listener(false)
        }.addOnFailureListener {
            listener(false)
        }.addOnCanceledListener {
            listener(false)
        }
    }

    private fun deleteAllImageReferenceAndImagesToFirebase(collectionReference: CollectionReference, callBack: (Boolean) -> Unit){
        collectionReference.get().addOnCompleteListener { task-> //lokasyona ait resimlerin referansları silinir.
            if(task.isSuccessful){
                if(task.result.documents.isNotEmpty()){
                    val size = task.result.documents.size
                    var j = 0
                    for (k in task.result.documents){
                        storage.reference.child(k.get("imageStorageRef").toString()).delete()
                        j++
                        k.reference.delete()
                        if(j == size)
                            callBack(true)
                    }
                }else
                    callBack(false)
            }
        }
    }

    override fun saveDifferentLocationsToFirebase(locationList: List<SavedPlace>, imageList: List<ImagePath>, userId: String){
        //telefondaki kayıtlardan firebase de olmayanları firebase ye kaydeder.

        val savedLocationsRef = dbFirestore
            .collection(userSavedLocations)
            .document(userId)
            .collection(locations)

        for (i in locationList){
            val map = HashMap<String, Any>()
            map["rowid"] = i.rowid
            map["name"] = i.name
            map["city"] = i.city
            map["district"] = i.district
            map["address"] = i.address
            map["state"] = i.state
            map["street"] = i.street
            map["suburb"] = i.suburb
            map["lat"] = i.lat
            map["lon"] = i.lon

            val newRef = savedLocationsRef.document("${i.lat}_${i.lon}")

            newRef.set(map).addOnCompleteListener {
                if(it.isSuccessful){

                    if(imageList.isNotEmpty()) {
                        val list = imageList.filter { imagePath ->
                            imagePath.root_id == i.rowid
                        }

                        saveLocationImagesToFirebase(list, userId, "${i.lat}_${i.lon}"){ downloadUrl, imageStorageRef, imageId -> //lokasyona ait resimler tekrar storage a yüklenir
                            newRef.collection(images).document(imageId)
                                .set(mapOf(
                                    "imageStorageUrl" to downloadUrl,
                                    "imageStorageRef" to imageStorageRef,
                                    "imageId" to imageId
                                ))
                        }
                    }
                }
            }
        }
        Toast.makeText(context, "buluta yükleme başarılı", Toast.LENGTH_LONG).show()
    }

    override fun getUserSavedLocations(userId: String, listener: (List<SavedPlace>) -> Unit) { //kullanıcının firebase ye yüklediği kayıtları getirir.
        val locationsRef = dbFirestore
            .collection(userSavedLocations)
            .document(userId)
            .collection(locations)

        locationsRef.get().addOnCompleteListener { task ->
            if(task.isSuccessful){
                if(!task.result.isEmpty && task.result.documents.isNotEmpty()){
                    val locations = task.result.toObjects<SavedPlace>()
                    if(locations.isNotEmpty())
                        listener(locations)
                }
            }
        }
    }

    override suspend fun removeOldLocationsToRoomAndSaveNewLocationsFromFirebase(locationList: List<SavedPlace>, userId: String) {
        //telefondaki var olan kayıtları silip firebase den indirilen kayıtları kaydeder.
        if(locationList.isNotEmpty()){
            dao.deleteAllSavedLocations()
            dao.deleteAllImagePaths()
            SaveImageToFile().deletePicturesFile(context)

            dao.saveLocationListToRoom(*locationList.toTypedArray())

            Toast.makeText(context, "Mevcut kayıtlar silindi", Toast.LENGTH_SHORT).show()
            Toast.makeText(context, "Bulutaki kayıtlar indirildi", Toast.LENGTH_SHORT).show()
            Toast.makeText(context, "Sayfayı yenileyin", Toast.LENGTH_SHORT).show()
        }
    }

    override fun saveImagesFromFirebaseToFile(userId: String, callBack: (String, String) -> Unit){
        val imageRef = dbFirestore.collection(userSavedLocations).document(userId).collection(locations)

        imageRef.get().addOnSuccessListener {
            if(it.documents.isNotEmpty()){

                for (i in it.documents){
                    val latLong = i.id
                    imageRef.document(latLong).collection(images).get().addOnSuccessListener { snapshot->
                       if(snapshot.documents.isNotEmpty()){

                           for (j in snapshot.documents){
                               val imageName = j.get("imageId").toString()
                               val imageStorageRef = j.get("imageStorageRef").toString()

                               val dir = File(context.getExternalFilesDir("/pictures/"), latLong)
                               if(!dir.exists())
                                   dir.mkdir()

                               val file = File(dir, imageName)

                               if(!file.exists()){
                                   storage.reference.child(imageStorageRef).getFile(file).addOnSuccessListener { task->
                                       callBack(file.path, latLong)
                                   }
                               }
                           }
                       }
                    }
                }
            }
        }
    }

    override suspend fun saveDifferentUserSavedLocations(locationList: List<SavedPlace>) {
        //telefonda olmayan firebase kayıtlarını kaydeder.
        if(locationList.isNotEmpty()){
            dao.saveLocationListToRoom(*locationList.toTypedArray())

            Toast.makeText(context, "Farklı kayıtlar indirildi", Toast.LENGTH_SHORT).show()
            Toast.makeText(context, "Sayfayı yenileyin", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendComment(comment: Comment, docRef: DocumentReference, callBack: (Boolean) -> Unit){
        val newRef = docRef.collection("Comments").document()

        comment.commentId = newRef.id
        newRef.set(comment)  //yorum kaydedilir.
            .addOnSuccessListener {

                docRef.get() //mekana ait rating güncellemesi yapılıyor.
                    .addOnSuccessListener {
                        if(it.data != null){
                            var placeRating = it.get("rating").toString().toFloat()
                            placeRating += comment.rating

                            docRef.update(mapOf(
                                "rating" to placeRating
                            ))
                        }
                    }

                Toast.makeText(context, context.getString(R.string.comment_sent), Toast.LENGTH_SHORT).show()
                callBack(true)
            }
            .addOnFailureListener {
                Toast.makeText(context, "${context.getString(R.string.error)}: ${it.localizedMessage}}", Toast.LENGTH_SHORT).show()
                callBack(false)
            }
    }
}