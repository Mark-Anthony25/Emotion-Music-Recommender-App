package com.ebmr.myapplication1.ui.gallery



import android.os.Bundle
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ebmr.myapplication1.databinding.FragmentGalleryBinding
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Locale
import android.media.AudioManager
import android.widget.SeekBar
import android.widget.TextView

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private lateinit var songsAdapter: SongsAdapter
    private lateinit var galleryViewModel: GalleryViewModel
    private var mediaPlayer: MediaPlayer? = null // MediaPlayer instance
    private var songList = mutableListOf<Song>()
    private var currentSongIndex = 0

    private lateinit var seekBar: SeekBar
    private lateinit var tvTime: TextView
    private var isUserSeeking = false
    private val handler = Handler(Looper.getMainLooper())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize ViewModel and Adapter
        galleryViewModel = ViewModelProvider(this).get(GalleryViewModel::class.java)
        songsAdapter = SongsAdapter(emptyList(), onSongClick = { song ->
            playSong(song)
        })
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = songsAdapter

        // Set up button click listeners
        setupButtonListeners()

        // Load songs initially
        loadSongs("All")

        // Set up playback controls
        setupPlaybackControls()

        return root
    }



    private fun setupButtonListeners() {
        binding.buttonAll.setOnClickListener { loadSongs("All") }
        binding.buttonAngry.setOnClickListener { loadSongs("anger") }
        binding.buttonDisgust.setOnClickListener { loadSongs("disgust") }
        binding.buttonFear.setOnClickListener { loadSongs("fear") }
        binding.buttonHappy.setOnClickListener { loadSongs("happy") }
        binding.buttonSad.setOnClickListener { loadSongs("sad") }
        binding.buttonSurprise.setOnClickListener { loadSongs("surprise") }
        binding.buttonNeutral.setOnClickListener { loadSongs("neutral") }
    }



    private fun loadSongs(category: String) {
        val db = FirebaseFirestore.getInstance()
        val query = if (category == "All") {
            db.collection("songs")
        } else {
            db.collection("songs").whereEqualTo("emotion", category)
        }

        query.get().addOnSuccessListener { result ->
            val songs = result.map { document ->
                Song(
                    mediaid = document.getString("mediaid") ?: "",
                    subtitle = document.getString("subtitle") ?: "",
                    title = document.getString("title") ?: "",
                    url = document.getString("url") ?: "",
                    emotion = document.getString("emotion") ?: ""
                )
            }
            songList = songs.toMutableList()
            songsAdapter.updateSongs(songs)
        }.addOnFailureListener { exception ->
            // Handle errors
        }
    }


    private fun setupPlaybackControls() {
        binding.playPauseButton.setOnClickListener {
            if (songList.isEmpty()) {
                Toast.makeText(context, "No songs available to play", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                binding.playPauseButton.text = "Play"
                handler.removeCallbacks(updateSeekBarTask)
            } else {
                if (mediaPlayer == null) {
                    playSong(songList[0]) // Play the first song
                } else {
                    mediaPlayer?.start()
                    binding.playPauseButton.text = "Pause"
                    handler.post(updateSeekBarTask)
                }
            }
        }


        binding.nextButton.setOnClickListener {
            playNextSong()
        }

        binding.previousButton.setOnClickListener {
            playPreviousSong()
        }
        binding.seekBarMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.currentTimeTextView.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                mediaPlayer?.let {
                    it.seekTo(seekBar?.progress ?: 0)
                }
            }
        })


    }

    private fun playSong(song: Song) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setDataSource(song.url)
            setOnPreparedListener {
                start()
                binding.playPauseButton.text = "Pause"
                binding.songTitleTextView.text = song.title
                updateDuration()
                binding.seekBarMusic.max = duration
                handler.post(updateSeekBarTask)
            }
            setOnCompletionListener { playNextSong() }
            setOnErrorListener { _, what, extra ->
                Log.e("GalleryFragment", "Error playing song: $what, $extra")
                Toast.makeText(context, "Error playing song", Toast.LENGTH_SHORT).show()
                release()
                true
            }
            prepareAsync()
        }
        currentSongIndex = songList.indexOf(song)
        Toast.makeText(context, "Playing ${song.title}", Toast.LENGTH_SHORT).show()
    }

    private val updateSeekBarTask = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        if (!isUserSeeking) {
                            val currentPosition = player.currentPosition
                            binding.seekBarMusic.progress = currentPosition
                            binding.currentTimeTextView.text = formatTime(currentPosition)
                        }
                        // Schedule the next update
                        handler.postDelayed(this, 1000)
                    } else {
                        // Do nothing if the player is not playing
                    }
                } catch (e: IllegalStateException) {
                    Log.e("GalleryFragment", "MediaPlayer is in an invalid state", e)
                }
            }
        }
    }


    private fun playNextSong() {
        if (songList.isNotEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % songList.size
            playSong(songList[currentSongIndex])
        }
    }

    private fun playPreviousSong() {
        if (songList.isNotEmpty()) {
            currentSongIndex = if (currentSongIndex - 1 < 0) songList.size - 1 else currentSongIndex - 1
            Log.d("GalleryFragment", "Playing previous song at index: $currentSongIndex") // Debug log
            playSong(songList[currentSongIndex])
        } else {
            Toast.makeText(context, "No songs available to play", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateDuration() {
        mediaPlayer?.let { player ->
            /*
            val totalDuration = player.duration
            binding.durationTextView.text = formatTime(totalDuration)
            updateCurrentTime()*/
            val totalDuration = player.duration
            binding.durationTextView.text = formatTime(totalDuration)
        }
    }

    private fun updateCurrentTime() {
        mediaPlayer?.let { player ->
            binding.currentTimeTextView.text = formatTime(player.currentPosition)
            if (player.isPlaying) {
                handler.postDelayed({ updateCurrentTime() }, 1000)
            }
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val formatter = SimpleDateFormat("mm:ss", Locale.getDefault())
        return formatter.format(milliseconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateSeekBarTask)
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }
}