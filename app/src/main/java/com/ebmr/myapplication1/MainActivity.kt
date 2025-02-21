package com.ebmr.myapplication1

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.ebmr.myapplication1.databinding.ActivityMainBinding
import com.ebmr.myapplication1.ui.home.HomeFragment
import com.google.android.material.navigation.NavigationView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force Light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Set top-level destinations
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.history -> {
                showImageHistoryDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showImageHistoryDialog() {
        lifecycleScope.launch {
            val imageFiles = withContext(Dispatchers.IO) {
                val historyFolder = File(
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "captured image history"
                )

                if (!historyFolder.exists() || !historyFolder.isDirectory) {
                    return@withContext emptyList<File>()
                }

                val files = historyFolder.listFiles { file -> file.extension == "jpg" || file.extension == "png" }
                files?.sortedByDescending { it.lastModified() }?.take(5) ?: emptyList()
            }

            if (imageFiles.isEmpty()) {
                Toast.makeText(this@MainActivity, "No images found", Toast.LENGTH_SHORT).show()
            } else {
                val listView = ListView(this@MainActivity).apply {
                    adapter = ImageListAdapter(this@MainActivity, imageFiles)
                }

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Image History")
                    .setView(listView)
                    .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getRotatedBitmap(imagePath: String): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return null
        val exif = ExifInterface(File(imagePath))
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // Adapter for displaying images in the ListView and handling clicks
    class ImageListAdapter(private val activity: MainActivity, private val imageFiles: List<File>) : BaseAdapter() {
        private val inflater: LayoutInflater = LayoutInflater.from(activity)

        override fun getCount() = imageFiles.size

        override fun getItem(position: Int) = imageFiles[position]

        override fun getItemId(position: Int) = position.toLong()

        @RequiresApi(Build.VERSION_CODES.Q)
        @SuppressLint("SimpleDateFormat")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: inflater.inflate(R.layout.image_list_item, parent, false)
            val imageView = view.findViewById<ImageView>(R.id.imageView)
            val dateTextView = view.findViewById<TextView>(R.id.dateTextView)
            val emotionTextView = view.findViewById<TextView>(R.id.emotionTextView)

            val file = imageFiles[position]

            Glide.with(activity)
                .load(file)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.error_image)
                .into(imageView)

            val date = Date(file.lastModified())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            dateTextView.text = "Date: ${dateFormat.format(date)}"

            val emotionFile = File(file.parent, "${file.name}.txt")
            val detectedEmotion = if (emotionFile.exists()) {
                emotionFile.readText()
            } else {
                "Unknown"
            }
            emotionTextView.text = "Emotion: $detectedEmotion"

            return view
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        val currentFragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment
        if (currentFragment is HomeFragment && currentFragment.onBackPressed()) {
            // Fragment handled the back press
        } else {
            super.onBackPressed() // Let the system handle the back press
        }
    }
}
