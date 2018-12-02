package com.labs.pbrother.freegallery.settings

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat

import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.controller.Item

/**
 * Created by simon on 07.12.16.
 */
class SettingsHelper(val context: Context) {

    companion object {
        @JvmStatic
        val ORDER_BY_DATE_TAKEN = 0
        @JvmStatic
        val ORDER_BY_DATE_ADDED = 1
        @JvmStatic
        private val KEY_PREF_STYLE_STYLE_LESSBLACK = "pref_key_style_lessblack"
        @JvmStatic
        private val KEY_PREF_STYLE_MAIN_COLUMNS = "pref_key_style_main_portrait_columns"
        @JvmStatic
        private val KEY_PREF_STYLE_COLUMNS = "pref_key_style_portrait_columns"
        @JvmStatic
        private val KEY_PREF_STYLE_COLORTITLE = "pref_key_style_colorize_title"
        @JvmStatic
        private val KEY_PREF_STYLE_COLOR_IMAGE_BACKGROUND = "pref_key_style_colorize_imagebackground"
        @JvmStatic
        private val KEY_PREF_STYLE_HIDE_DRAWER_HEADER = "pref_key_style_hide_drawer_header"
        @JvmStatic
        val KEY_PREF_ORDER_BY = "pref_key_order_by"
        @JvmStatic
        val KEY_PREF_EXIF_ORIENTATION = "pref_key_orientation_exif"
        @JvmStatic
        val KEY_SDURI = "SDCARDURI"
        @JvmStatic
        val KEY_SDROOT = "SDCARDROOTPATH"
        @JvmStatic
        private val KEY_PREF_STYLE_COLOR = "pref_key_style_color"

    }

    private var sharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var orderByDateTaken: String
    private var orderByDateAdded: String
    private var colorNerd: String
    private var colorClassic: String

    init {
        val orderByOptions = context.resources.getStringArray(R.array.prefOrderByEntries)
        orderByDateAdded = orderByOptions[0]
        orderByDateTaken = orderByOptions[1]
        setItemSort()

        val colorOptions = context.resources.getStringArray(R.array.prefStyleColorValues)
        colorNerd = colorOptions[0]
        colorClassic = colorOptions[1]
    }

    fun reactToSettingChange(key: String) {
        when (key) {
            KEY_PREF_ORDER_BY -> {
                setItemSort()
            }
        }
    }

    private fun setItemSort() {
        val setting = sharedPref.getString(KEY_PREF_ORDER_BY, "")
        when (setting) {
            orderByDateAdded -> {
                Item.ORDER_BY = Item.ORDER_BY_DATE_ADDED
            }
            orderByDateTaken -> {
                Item.ORDER_BY = Item.ORDER_BY_DATE_TAKEN
            }
        }
    }

    val orientationFromExif: Boolean
        get() {
            return sharedPref.getBoolean(KEY_PREF_EXIF_ORIENTATION, true)
        }

    val orderBy: Int
        get() {
            val orderbyoptions = context.resources.getStringArray(R.array.prefOrderByEntries)
            return when (sharedPref.getString(KEY_PREF_ORDER_BY, "")) {
                orderbyoptions[0] -> {
                    ORDER_BY_DATE_ADDED
                }
                orderbyoptions[1] -> {
                    ORDER_BY_DATE_TAKEN
                }
                else -> {
                    ORDER_BY_DATE_ADDED
                }
            }
        }

    val useImageColorAsBackground: Boolean
        get() {
            return sharedPref.getBoolean(KEY_PREF_STYLE_COLOR_IMAGE_BACKGROUND, true)
        }

    var columnsInPortrait: Int
        get() = Integer.valueOf(sharedPref.getInt(KEY_PREF_STYLE_COLUMNS, 4))!!
        set(value) {
            sharedPref.edit().apply() {
                putInt(KEY_PREF_STYLE_COLUMNS, value)
                apply()
            }
        }

    var mainColumnsInPortrait: Int
        get() = Integer.valueOf(sharedPref.getInt(KEY_PREF_STYLE_MAIN_COLUMNS, 2))
        set(value) {
            sharedPref.edit().apply() {
                putInt(KEY_PREF_STYLE_MAIN_COLUMNS, value)
                apply()
            }
        }

    var sdCardUri: String
        get() = sharedPref.getString((KEY_SDURI), "")
        set(value) {
            sharedPref.edit().apply() {
                putString(KEY_SDURI, value)
                apply()
            }
        }

    var sdCardRootPath: String
        get() = sharedPref.getString((KEY_SDROOT), "/NOUSABLEPATH")
        set(value) {
            sharedPref.edit().apply() {
                putString(KEY_SDROOT, value)
                apply()
            }
        }

    val colorizeTitlebar: Boolean = sharedPref.getBoolean(KEY_PREF_STYLE_COLORTITLE, false)
    val hideDrawerHeader: Boolean = sharedPref.getBoolean(KEY_PREF_STYLE_HIDE_DRAWER_HEADER, false)

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // themeing

    val theme: Int
        get() {
            val lessBlack = sharedPref.getBoolean(KEY_PREF_STYLE_STYLE_LESSBLACK, true)
            if (lessBlack) return R.style.DarkAppBase
            return R.style.NerdAppBase
        }

    val dialogTheme: Int
        get() {
            val lessBlack = sharedPref.getBoolean(KEY_PREF_STYLE_STYLE_LESSBLACK, true)
            if (lessBlack) return R.style.DarkDialogBase
            return R.style.NerdDialogBase
        }

    val popupTheme: Int
        get() {
            val lessBlack = sharedPref.getBoolean(KEY_PREF_STYLE_STYLE_LESSBLACK, true)
            if (lessBlack) return R.style.DarkPopup
            return R.style.NerdPopup
        }

    val defaultCollectionColor: Int = ContextCompat.getColor(context, R.color.colorHighlightDefault)

    val colorPrimary: Int
        get() {
            val lessBlack = sharedPref.getBoolean(KEY_PREF_STYLE_STYLE_LESSBLACK, true)
            if (lessBlack) return ContextCompat.getColor(context, R.color.primary)
            return ContextCompat.getColor(context, R.color.nerd_primary)
        }

    val colorPrimaryDark: Int
        get() {
            val lessBlack = sharedPref.getBoolean(KEY_PREF_STYLE_STYLE_LESSBLACK, true)
            if (lessBlack) return ContextCompat.getColor(context, R.color.primary_dark)
            return ContextCompat.getColor(context, R.color.nerd_primary_dark)
        }

    val highlightColorAccent: Int
        get() {
            return ContextCompat.getColor(context, R.color.accent)
        }
}
