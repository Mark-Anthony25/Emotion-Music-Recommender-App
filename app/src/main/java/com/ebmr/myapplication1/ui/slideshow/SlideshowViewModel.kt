package com.ebmr.myapplication1.ui.slideshow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SlideshowViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This research presents a mobile application designed to provide emotion-driven music recommendations using Convolutional Neural Networks (CNNs). By analyzing the key points of the static image of the user's emotion, the application effectively identifies emotional states and recommends music based on the user's current emotion. The study focuses on utilizing CNN as an algorithm to detect emotions, aiming to assist student counselees in alleviating their emotional distress or promoting improved well-being through music. As a result, the researchers developed an innovative tool that helps transform how users interact with music, catering to their emotional needs and promoting improved well-being."
    }
    val text: LiveData<String> = _text
}