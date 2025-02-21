package com.ebmr.myapplication1.ui.gallery

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ebmr.myapplication1.R

// Data model for a Song
data class Song(
    val mediaid: String = "",
    val subtitle: String = "",
    val title: String = "",
    val url: String = "",
    val emotion: String = ""
)

class SongsAdapter(
    private var songs: List<Song>,
    private val onSongClick: (Song) -> Unit // Pass a function to handle clicks
) : RecyclerView.Adapter<SongsAdapter.SongViewHolder>() {

    // ViewHolder class to hold the view references
    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.songTitle)
        private val subtitleTextView: TextView = itemView.findViewById(R.id.songSubtitle)

        fun bind(song: Song, onClick: (Song) -> Unit) {
            titleTextView.text = song.title
            subtitleTextView.text = song.subtitle
            itemView.setOnClickListener { onClick(song) } // Handle click events


        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        // Inflate the item layout
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song, onSongClick)
    }

    override fun getItemCount(): Int = songs.size

    // Method to update the list of songs
    @SuppressLint("NotifyDataSetChanged")
    fun updateSongs(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }
}