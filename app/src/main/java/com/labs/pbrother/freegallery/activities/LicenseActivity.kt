package com.labs.pbrother.freegallery.activities

import android.content.res.Resources
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.labs.pbrother.freegallery.R
import kotlinx.android.synthetic.main.activity_license.*
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

class LicenseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)

        try {
            licenseview.setLicenses(R.xml.licenses)
        } catch (e1: Resources.NotFoundException) {
        } catch (e1: XmlPullParserException) {
        } catch (e1: IOException) {
        }

    }

}
