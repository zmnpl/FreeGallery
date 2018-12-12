package com.labs.pbrother.freegallery.activities

import android.Manifest
import android.os.Bundle
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.AppIntro
import com.github.paolorotolo.appintro.AppIntroFragment
import com.github.paolorotolo.appintro.R.id.description
import com.github.paolorotolo.appintro.R.id.image
import com.github.paolorotolo.appintro.model.SliderPage
import com.labs.pbrother.freegallery.R

import kotlinx.android.synthetic.main.activity_intro.*
import androidx.annotation.ColorInt
import com.github.paolorotolo.appintro.ISlideBackgroundColorHolder
import com.mikepenz.ionicons_typeface_library.Ionicons


class IntroActivity : AppIntro() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /// Instead of fragments, you can also use our default slide.
        // Just create a `SliderPage` and provide title, description, background and image.
        // AppIntro will do the rest.
        val helloPage = SliderPage()
        helloPage.apply {
            title = getString(R.string.intro_hello_title)
            description = getString(R.string.intro_hello_description)
            bgColor = getColor(R.color.introBGHello)
            imageDrawable = R.mipmap.bgbird
        }
        addSlide(AppIntroFragment.newInstance(helloPage))

        val timelinePage = SliderPage()
        timelinePage.apply {
            title = getString(R.string.intro_timeline_title)
            description = getString(R.string.intro_timeline_description)
            bgColor = getColor(R.color.introBGTimeline)
            imageDrawable = R.drawable.ic_camera_roll_white_120dp
        }
        addSlide(AppIntroFragment.newInstance(timelinePage))

        val tagsPage = SliderPage()
        tagsPage.apply {
            title = getString(R.string.intro_tags_title)
            description = getString(R.string.intro_tags_description)
            bgColor = getColor(R.color.introBGTags)
            imageDrawable = R.drawable.ic_bookmark_white_120dp
        }
        addSlide(AppIntroFragment.newInstance(tagsPage))

        val colorsPage = SliderPage()
        colorsPage.apply {
            title = getString(R.string.intro_colors_title)
            description = getString(R.string.intro_colors_description)
            bgColor = getColor(R.color.introBGColors)
            imageDrawable = R.drawable.ic_colorize_white_120dp
        }
        addSlide(AppIntroFragment.newInstance(colorsPage))

        val permissionsPage = SliderPage()
        permissionsPage.apply {
            title = getString(R.string.intro_permissions_title)
            description = getString(R.string.intro_permissions_description)
            bgColor = getColor(R.color.introBGPermissions)
            imageDrawable = R.drawable.ic_lock_open_white_120dp
        }
        addSlide(AppIntroFragment.newInstance(permissionsPage))

        askForPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 5)
        // OPTIONAL METHODS
        // Override bar/separator color.
        //setBarColor(Color.parseColor("#000000"))
        setSeparatorColor(getColor(R.color.introDevider))

        // Hide Skip/Done button.
        showSkipButton(false)
        setProgressButtonEnabled(true)

        // Turn vibration on and set intensity.
        // NOTE: you will probably need to ask VIBRATE permission in Manifest.
        //setVibrate(true);
        //setVibrateIntensity(30);

        //setFadeAnimation() // OR
        //setZoomAnimation() // OR
        //setFlowAnimation() // OR
        //setSlideOverAnimation() // OR
        setDepthAnimation() // OR
    }

    override fun onSkipPressed(currentFragment: Fragment) {
        super.onSkipPressed(currentFragment)
        // Do something when users tap on Skip button.
    }

    override fun onDonePressed(currentFragment: Fragment) {
        super.onDonePressed(currentFragment)
        finish()
        // Do something when users tap on Done button.
    }

    override fun onSlideChanged(oldFragment: Fragment?, newFragment: Fragment?) {
        super.onSlideChanged(oldFragment, newFragment)
        // Do something when the slide changes.
    }

//    inner class MySlide : Fragment(), ISlideBackgroundColorHolder {
//        override fun getDefaultBackgroundColor(): Int {
//            // Return the default background color of the slide.
//            return Color.parseColor("#000000")
//        }
//
//        override fun setBackgroundColor(@ColorInt backgroundColor: Int) {
//            // Set the background color of the view within your slide to which the transition should be applied.
//            if (layoutContainer != null) {
//                layoutContainer.setBackgroundColor(backgroundColor)
//            }
//        }
//    }

}
