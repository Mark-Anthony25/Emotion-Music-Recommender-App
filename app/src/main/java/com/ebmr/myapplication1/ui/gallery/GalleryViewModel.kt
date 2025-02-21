package com.ebmr.myapplication1.ui.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class GalleryViewModel : ViewModel() {

    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> = _songs

    init {
        fetchSongs()
    }

    private fun fetchSongs() {
        val db = FirebaseFirestore.getInstance()
        db.collection("songs").get()
            .addOnSuccessListener { result ->
                val songList = mutableListOf<Song>()
                for (document in result) {
                    val song = document.toObject(Song::class.java)
                    songList.add(song)
                }
                _songs.value = songList
            }
            .addOnFailureListener { exception ->
                // Handle the error
            }
    }
}
