package com.labs.pbrother.freegallery.fragments

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.support.v7.graphics.Palette
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.labs.pbrother.freegallery.BuildConfig
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.controller.Item
import com.labs.pbrother.freegallery.controller.TYPE_IMAGE
import com.labs.pbrother.freegallery.dialogs.ColorizeDialogFragment
import com.labs.pbrother.freegallery.prefs
import kotlinx.android.synthetic.main.fragment_singlepicture_slide_page.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File


/**
 * Created by simon on 30.12.15.
 */
class ImagePageFragment() : Fragment() {

    private lateinit var imageView: SubsamplingScaleImageView
    private lateinit var vidView: ImageView
    private lateinit var vidIcon: ImageView
    private lateinit var gestureDetector: GestureDetector
    private lateinit var item: Item
    private var useImageColorAsBackground = true


    // exo player stuff
    private lateinit var exoView: PlayerView
    private var exoPlayer: SimpleExoPlayer? = null
    var playWhenReady = false
    var currentWindow = 0
    var playbackPosition: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = false
        useImageColorAsBackground = prefs.useImageColorAsBackground
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(
                R.layout.fragment_singlepicture_slide_page, container, false) as ViewGroup

        imageView = rootView.singlepicture_scrollview_Pic
        if (prefs.orientationFromExif) imageView.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF

        vidView = rootView.singlepicture_scrollview_Vid
        vidIcon = rootView.singlepicture_scrollview_VidIcon
        exoView = rootView.singlepicture_scrollview_video_view

        display()

        return rootView
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun initializePlayer(uri: Uri) {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(
                DefaultRenderersFactory(context),
                DefaultTrackSelector(), DefaultLoadControl())

        exoView.setPlayer(exoPlayer)

        exoPlayer?.setPlayWhenReady(playWhenReady)
        exoPlayer?.seekTo(currentWindow, playbackPosition)

        val mediaSource = buildMediaSource(uri);
        exoPlayer?.prepare(mediaSource, true, false)

        imageView.visibility = View.INVISIBLE
        exoView.visibility = View.VISIBLE
    }

    //private fun buildMediaSource(uri: Uri): MediaSource = ExtractorMediaSource.Factory(DefaultHttpDataSourceFactory("exoplayer-codelab")).createMediaSource(uri)
    private fun buildMediaSource(uri: Uri): MediaSource = ExtractorMediaSource(
            uri,
            DefaultDataSourceFactory(context, "fooagent"),
            DefaultExtractorsFactory(),
            null,
            null)

    private fun releasePlayer() {
        if (exoPlayer != null) {
            playbackPosition = exoPlayer?.getCurrentPosition() ?: 0
            currentWindow = exoPlayer?.getCurrentWindowIndex() ?: 0
            playWhenReady = exoPlayer?.getPlayWhenReady() ?: false
            exoPlayer?.release();
            exoPlayer = null;
        }
    }


    private fun display() {
        // videos will be displayes differently from images ...
        when (item.type) {
            TYPE_IMAGE -> {
                if ("gif" == File(item.path).extension.toLowerCase()) {
                    imageView.visibility = View.INVISIBLE
                    vidView.visibility = View.VISIBLE
                    Glide.with(this).load(item.fileUriString).into(vidView)
                } else {
                    imageView.apply {
                        setExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                        setMinimumTileDpi(196) // 196 -> recommendation from franciscofranco on github
                        setImage(ImageSource.uri(item.path))
                        setOnTouchListener { view, motionEvent -> gestureDetector.onTouchEvent(motionEvent) }
                        setBackgroundColorBasedOnImage()
                    }
                }
            }
            else -> {
                // TODO - evaluate if worth it to make own video activity
                if (false) {
                    initializePlayer(Uri.parse(item.fileUriString))
                } else {
                    displayGlideViewForVideo()
                }
            }
        }
    }

    fun displayGlideViewForVideo() {
        imageView.visibility = View.INVISIBLE
        vidView.visibility = View.VISIBLE
        vidIcon.visibility = View.VISIBLE

        vidIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.type = "video/*"
            val vid = File(item.path)
            val foo = Environment.getExternalStorageDirectory().absolutePath
            val uri = FileProvider.getUriForFile(context!!, BuildConfig.APPLICATION_ID + ".provider", vid)

            intent.data = uri
            startActivity(intent)
        }

        Glide.with(this).load(item.fileUriString).into(vidView)
    }

    fun setBackgroundColorBasedOnImage() {
        if (useImageColorAsBackground) {
            doAsync {
                val foo = Palette.from(BitmapFactory.decodeFile(item.path)).generate()
                var color = foo.getDarkVibrantColor(0)
                if (0 == color) foo.getDarkMutedColor(0)
                if (0 == color) color = foo.getVibrantColor(0)
                if (0 == color) color = foo.getMutedColor(0)
                uiThread {
                    imageView.setBackgroundColor(color)
                }
            }
        }
    }

    fun setmGestureDetector(gestureDetector: GestureDetector) {
        this.gestureDetector = gestureDetector
    }

    fun setItem(item: Item) {
        this.item = item
    }
}
