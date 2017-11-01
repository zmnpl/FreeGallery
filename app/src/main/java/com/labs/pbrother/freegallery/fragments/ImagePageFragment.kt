package com.labs.pbrother.freegallery.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.labs.pbrother.freegallery.BuildConfig
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.controller.Item
import com.labs.pbrother.freegallery.controller.TYPE_IMAGE
import kotlinx.android.synthetic.main.fragment_singlepicture_slide_page.view.*
import java.io.File

/**
 * Created by simon on 30.12.15.
 */
class ImagePageFragment : Fragment() {

    private lateinit var imageView: SubsamplingScaleImageView
    private lateinit var vidView: ImageView
    private lateinit var vidIcon: ImageView
    private lateinit var gestureDetector: GestureDetector
    private lateinit var item: Item

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootView = inflater!!.inflate(
                R.layout.fragment_singlepicture_slide_page, container, false) as ViewGroup

        imageView = rootView.singlepicture_scrollview_Pic
        vidView = rootView.singlepicture_scrollview_Vid
        vidIcon = rootView.singlepicture_scrollview_VidIcon

        // videos will be displayes differently from images ...
        if (item.type == TYPE_IMAGE) {
            imageView = rootView.singlepicture_scrollview_Pic
            imageView.setParallelLoadingEnabled(true)
            imageView.setMinimumTileDpi(196) // 196 -> recommendation from franciscofranco on github
            imageView.setImage(ImageSource.uri(item.path))
        } else {
            imageView.visibility = View.INVISIBLE
            vidView.visibility = View.VISIBLE
            vidIcon.visibility = View.VISIBLE

            vidIcon.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                intent.type = "video/*"
                val vid = File(item.path)
                val foo = Environment.getExternalStorageDirectory().absolutePath

                val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", vid)

                intent.data = uri
                startActivity(intent)
            }

            Glide.with(this).load(item.fileUrl()).into(vidView)
        }

        imageView.setOnTouchListener { view, motionEvent -> gestureDetector.onTouchEvent(motionEvent) }

        return rootView
    }

    fun setmGestureDetector(gestureDetector: GestureDetector) {
        this.gestureDetector = gestureDetector
    }

    fun setItem(item: Item) {
        this.item = item
    }
}
