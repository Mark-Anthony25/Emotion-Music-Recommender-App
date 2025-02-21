package com.ebmr.myapplication1.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ebmr.myapplication1.databinding.FragmentHomeBinding
import com.ebmr.myapplication1.ml.FacialExpressionv1
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Locale
import android.widget.TextView // Import for TextView
import android.hardware.Camera
import android.graphics.Typeface
import android.widget.SeekBar
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine


import com.ebmr.myapplication1.R // Import for your project's R class
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var songsAdapter: SongsAdapter
    private var songList = mutableListOf<Song>()
    private var mediaPlayer: MediaPlayer? = null // MediaPlayer instance
    private var currentSongIndex = 0
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var durationTextView: TextView
    private lateinit var currentTimeTextView: TextView
    private var isUserSeeking = false
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var photoFile: File

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    private val captureImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Load the full-resolution image from the file
            val imageBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            detectFaceAndRecognizeEmotion(imageBitmap)

            // Update the UI
            binding.captureImagePanel.visibility = View.GONE
            binding.musicListPanel.visibility = View.VISIBLE
            binding.searchView.visibility = View.VISIBLE
        } else {
            // Reset UI if no image is captured
            binding.captureImagePanel.visibility = View.VISIBLE
            binding.musicListPanel.visibility = View.GONE
            binding.searchView.visibility = View.GONE
        }
    }
    private fun saveImageToHistoryFolder(bitmap: Bitmap, emotion: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val folderName = "Captured Image History"
            val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), folderName)
            } else {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderName)
            }

            if (!directory.exists()) {
                directory.mkdirs() // Create the folder if it doesn't exist
            }

            val fileName = "IMG_${System.currentTimeMillis()}.jpg"
            val file = File(directory, fileName)

            try {
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream) // Save as JPEG
                outputStream.flush()
                outputStream.close()

                // Save the emotion to a text file
                val emotionFile = File(directory, "${fileName}.txt")
                FileOutputStream(emotionFile).use { emotionOutputStream ->
                    emotionOutputStream.write(emotion.toByteArray())
                }

                // Notify the media scanner to show the image in the gallery
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    requireContext().sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                }

                // Switch back to the main thread to show a Toast
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Image saved to $directory", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error saving image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private val quotesMap = mapOf(
        "anger" to listOf(
            "Anger is a wind which blows out the lamp of the mind.",
            "Speak when you are angry - and you'll make the best speech you'll ever regret.",
            "Holding on to anger is like grasping a hot coal with the intent of throwing it at someone else; you are the one who gets burned.",
            "For every minute you are angry you lose sixty seconds of happiness.",
            "The greatest remedy for anger is delay."
        ),
        "disgust" to listOf(
            "Disgust is a powerful emotion that can protect us from harm, but it can also be a source of prejudiceand discrimination.",
            "When disgust is directed at ourselves, it can lead to feelings of shame and self-loathing.",
            "Disgust can be a difficult emotion to manage, but it is important to remember that it is a normal and natural human experience.",
            "If you want to overcome disgust, it is important to understand what triggers it and to develop coping mechanisms for dealing with it.",
            "Remember that disgust is a feeling, and like all feelings, it will eventually pass."
        ),
        "fear" to listOf(
            "Fear is a natural emotion that can help us to avoid danger, but it can also hold us back from living our lives to the fullest.",
            "When we are afraid, it is important to remember that we are not alone.",
            "There are many people who care about us and want to help us.",
            "If you are struggling with fear, it is important to reach out for help.",
            "There are many resources available to help you overcome your fear and live a happier, more fulfilling life."
        ),
        "happy" to listOf(
            "Happiness is not something ready made. It comes from your own actions.",
            "The bestway to cheer yourself up is to try to cheer somebody else up.",
            "Happiness is a journey, not a destination.",
            "Happiness is when what you think, what you say, and what you do are in harmony.",
            "Happiness is not the absence of problems, it's the ability to deal with them."
        ),
        "sad" to listOf(
            "Sadness is a natural emotion that is often triggered by loss or disappointment.",
            "It is important to allow yourself to feel sadness, but it is also important to find healthy ways to cope with it.",
            "Talking to a friend or family member, journaling, or spending time in nature can all be helpful ways to deal with sadness.",
            "If you are struggling to cope with sadness, it is important to reach out for help.",
            "There are many resources available to help you through difficult times."
        ),
        "surprise" to listOf(
            "Surprise is a brief mental and physiological state, a startle response experienced by animals and humans as the result of an unexpected event.",
            "Surprise can be positive, negative, or neutral.",
            "The feeling of surprise can be beneficial, as it can help us to learn and adapt to new situations.",
            "Surprise can also be a source of joy and excitement.",
            "When we are surprised, it is important to take a moment to process what has happened and to decide how we want to respond."
        ),
        "neutral" to listOf(
            "Neutral is a state of not supporting or helping either side in a conflict, disagreement, etc.; impartial.",
            "Neutral can also refer to a color that is not bright or strong, such as beige or gray.",
            "In chemistry, a neutral substance is one that is neither acidic nor alkaline.",
            "In physics, a neutral object has no net electrical charge.",
            "In general, neutral can be used to describe anything that is not extreme or excessive."
        )
    )



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ):

            View {

        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root


        // Initialize the adapter and RecyclerView
        songsAdapter = SongsAdapter(songList, onSongClick = { song ->
            // Handle song click, e.g., play song or show details
            //Toast.makeText(requireContext(), "Clicked on ${song.title}", Toast.LENGTH_SHORT).show()
            playSong(song)
        })
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = songsAdapter
        setupPlaybackControls()
        // Setup SearchView
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { filterSongs(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { filterSongs(it) }
                return true
            }
        })
        binding.uploadButton.setOnClickListener {
            openImagePicker()
        }







        val button: Button = binding.button
        button.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openCamera()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    Toast.makeText(requireContext(), "Camera permission is needed to capture images", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }

        return root
    }
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*" // Limit to image files
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun filterSongs(query: String) {
        val filteredSongs = songList.filter { song ->
            song.title.contains(query, ignoreCase = true)
        }
        songsAdapter.updateSongs(filteredSongs)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // Use the URI to load the image
                try {
                    val imageStream = requireActivity().contentResolver.openInputStream(uri)
                    val imageBitmap = BitmapFactory.decodeStream(imageStream)
                    imageStream?.close()

                    // Process the image (e.g., detect faces or emotions)
                    detectFaceAndRecognizeEmotion(imageBitmap)

                    // Update the UI
                    binding.captureImagePanel.visibility = View.GONE
                    binding.musicListPanel.visibility = View.VISIBLE
                    binding.searchView.visibility = View.VISIBLE
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun openCamera() {
        //val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Create a file to save the image
        val folderName = "Captured Image History"
        val directory = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), folderName)
        if (!directory.exists()) {
            directory.mkdirs() // Create the folder if it doesn't exist
        }
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        photoFile = File(directory, fileName)

        // Get the URI for the file using FileProvider
        val photoURI: Uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoURI) // Pass the content URI
        }

        //captureImageLauncher.launch(cameraIntent)

        // Add extras to specify the front camera
        cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", Camera.CameraInfo.CAMERA_FACING_FRONT)
        cameraIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1) // Newer Android versions
        cameraIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true) // Compatibility across devices

        captureImageLauncher.launch(cameraIntent)
        //binding.captureImagePanel.visibility = View.GONE
        //binding.musicListPanel.visibility = View.VISIBLE
        //binding.searchView.visibility = View.VISIBLE
        // Keep the UI state unchanged until the result is received
        binding.musicListPanel.visibility = View.GONE
        binding.searchView.visibility = View.GONE
        binding.captureImagePanel.visibility = View.VISIBLE



        //currentTimeTextView = binding.musicListPanel.findViewById(R.id.currentTimeTextView)
        binding.captureImagePanel.visibility = View.GONE
        binding.musicListPanel.visibility = View.VISIBLE
        binding.searchView.visibility = View.VISIBLE

        durationTextView = binding.musicListPanel.findViewById(R.id.durationTextView)
        currentTimeTextView = binding.musicListPanel.findViewById(R.id.currentTimeTextView)


    }

    private fun displayMusicForEmotion(emotion: String) {
        Log.d("HomeFragment", "Detected Emotion: $emotion") // Log the detected emotion
        //loadSongs(emotion.lowercase())  // Load songs based on the detected emotion
        //load quotes
        val quoteTextView = binding.musicListPanel.findViewById<TextView>(R.id.quoteTextView)
        val quotesForEmotion = quotesMap[emotion] ?: emptyList()
        val randomQuote = quotesForEmotion.randomOrNull()
        quoteTextView.text = randomQuote
        quoteTextView.setTypeface(quoteTextView.typeface, Typeface.BOLD)
        loadSongs(emotion)
        quoteTextView.postDelayed({
            quoteTextView.animate()
                .alpha(0f)
                .setDuration(1000) // 1 second fade out duration
                .withEndAction {
                    quoteTextView.visibility = View.GONE
                }
                .start()
        }, 8500) // 7 seconds delay before fade
        //binding.button.visibility = View.GONE // Hide the button after clicking can be remove test
        binding.captureImagePanel.visibility = View.GONE // Hide capture image panel
        binding.musicListPanel.visibility = View.VISIBLE // Show music list panel
        binding.searchView.visibility = View.VISIBLE
        durationTextView = binding.musicListPanel.findViewById(R.id.durationTextView)
        currentTimeTextView = binding.musicListPanel.findViewById(R.id.currentTimeTextView)

    }

    fun onBackPressed(): Boolean {
        binding.captureImagePanel.visibility = View.VISIBLE
        binding.musicListPanel.visibility = View.GONE
        binding.searchView.visibility = View.GONE
        return true // Handle the back press
    }



    private fun loadSongs(category: String) {
        val db = FirebaseFirestore.getInstance()
        // Fetch only based on the detected emotion (no "All" category needed)
        val query = db.collection("songs").whereEqualTo("emotion", category)

        query.get().addOnSuccessListener { result ->
            if (!result.isEmpty) {
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
                //songsAdapter.updateSongs(songs)
                requireActivity().runOnUiThread {
                    songsAdapter.updateSongs(songs)
                }

                Log.d("HomeFragment", "Number of songs loaded: ${songs.size}")
            } else {
                Toast.makeText(requireContext(), "No songs found for this emotion", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Failed to load songs: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPlaybackControls() {
        binding.playPauseButton.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                binding.playPauseButton.text = "Play"
                //handler.removeCallbacksAndMessages(null)
                handler.removeCallbacks(updateSeekBarTask)
            } else {
                if (songList.isNotEmpty() && mediaPlayer == null) { // Check if nosong is playing
                    playSong(songList[0]) // Play the first song
                } else {
                    mediaPlayer?.start()
                    binding.playPauseButton.text = "Pause"
                    //updateCurrentTime()
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
                mediaPlayer?.seekTo(seekBar?.progress ?: 0)
            }
        })
    }


    private fun playSong(song: Song) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.url)
            setOnPreparedListener {
                start()
                binding.playPauseButton.text = "Pause"
                binding.songTitleTextView.text = song.title // Display song title
                updateDuration()
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
                if (!isUserSeeking) {
                    binding.seekBarMusic.progress = player.currentPosition
                    binding.currentTimeTextView.text = formatTime(player.currentPosition)
                }
                if (player.isPlaying) {
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateSeekBarTask)
        mediaPlayer?.pause()
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
            playSong(songList[currentSongIndex])
        }
    }

    private fun updateDuration() {
        mediaPlayer?.let { player ->
            val totalDuration = player.duration
            durationTextView.text = formatTime(totalDuration) // Access durationTextView here
            updateCurrentTime()
        }
    }
    private fun updateCurrentTime() {
        mediaPlayer?.let { player ->
            currentTimeTextView.text = formatTime(player.currentPosition) // Access currentTimeTextView here
            if (player.isPlaying) {
                handler.postDelayed({ updateCurrentTime() }, 1000)
            }
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val formatter = SimpleDateFormat("mm:ss", Locale.getDefault())
        return formatter.format(milliseconds)
    }
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(task.exception ?: Exception("Task failed"))
                }
            }
        }
    }



    private fun detectFaceAndRecognizeEmotion(bitmap: Bitmap) {
        CoroutineScope(Dispatchers.Main).launch {
            val image = InputImage.fromBitmap(bitmap, 0)

            val highAccuracyOpts = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build()

            val detector = FaceDetection.getClient(highAccuracyOpts)

            try {
                // Use withContext to switch to IO thread for face detection
                val faces = withContext(Dispatchers.IO) {
                    detector.process(image).await() // Await the result of face detection
                }

                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val bounds = face.boundingBox

                    // Crop the face from the original bitmap
                    val croppedBitmap = Bitmap.createBitmap(
                        bitmap,
                        bounds.left.coerceAtLeast(0),
                        bounds.top.coerceAtLeast(0),
                        bounds.width().coerceAtMost(bitmap.width - bounds.left),
                        bounds.height().coerceAtMost(bitmap.height - bounds.top)
                    )

                    // Resize the cropped face to 100x100 pixels
                    val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 100, 100, true)

                    val byteBuffer = ByteBuffer.allocateDirect(4 * 100 * 100 * 3)
                    byteBuffer.order(ByteOrder.nativeOrder())

                    for (y in 0 until 100) {
                        for (x in 0 until 100) {
                            val pixel = resizedBitmap.getPixel(x, y)
                            val r = (pixel shr 16 and 0xFF) / 255.0f
                            val g = (pixel shr 8 and 0xFF) / 255.0f
                            val b = (pixel and 0xFF) / 255.0f
                            byteBuffer.putFloat(r)
                            byteBuffer.putFloat(g)
                            byteBuffer.putFloat(b)
                        }
                    }

                    // Continue with emotion recognition
                    val detectedEmotion = withContext(Dispatchers.IO) {
                        val model = FacialExpressionv1.newInstance(requireContext())

                        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 100, 100, 3), DataType.FLOAT32)
                        inputFeature0.loadBuffer(byteBuffer)

                        val outputs = model.process(inputFeature0)
                        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

                        model.close()

                        val emotions = arrayOf("surprise", "fear", "disgust", "happy", "sad", "anger", "neutral")
                        val confidenceScores = outputFeature0.floatArray
                        val maxIndex = confidenceScores.indices.maxByOrNull { confidenceScores[it] } ?: -1
                        val confidence = confidenceScores[maxIndex]

                        if (confidence > 0.4) { // Adjusted confidence threshold
                            emotions[maxIndex]
                        } else {
                            "Unknown"
                        }
                    }

                    // Switch back to the main thread to update the UI
                    withContext(Dispatchers.Main) {
                        if (detectedEmotion != "Unknown") {
                            saveImageToHistoryFolder(bitmap, detectedEmotion) // Save the image with the detected emotion
                            Toast.makeText(requireContext(), "Detected Emotion: $detectedEmotion", Toast.LENGTH_SHORT).show()
                            binding.captureImagePanel.visibility = View.GONE
                            binding.musicListPanel.visibility = View.VISIBLE
                            displayMusicForEmotion(detectedEmotion)
                        } else {
                            // Avoid showing Toast if the emotion is unknown
                            binding.captureImagePanel.visibility = View.GONE
                            binding.musicListPanel.visibility = View.VISIBLE
                            displayMusicForEmotion(detectedEmotion)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No face detected", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error detecting face", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }
    override fun onResume() {
        super.onResume()
        // Show capture image panel when returning to the fragment
        binding.captureImagePanel.visibility = View.VISIBLE
        binding.musicListPanel.visibility = View.GONE
        //binding.button.visibility = View.VISIBLE
        binding.searchView.visibility = View.GONE

    }
}
