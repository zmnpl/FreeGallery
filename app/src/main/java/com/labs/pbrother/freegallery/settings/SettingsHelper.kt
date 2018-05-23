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
        private val KEY_PREF_STYLE_COLOR = "pref_key_style_color"

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
        val Key_PREF_EXIF_ORIENTATION = "pref_key_orientation_exif"
    }

    private var sharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private lateinit var colorDefault: String
    private lateinit var colorIndigo: String
    private lateinit var colorTeal: String
    private lateinit var colorPurple: String
    private lateinit var colorBlueGrey: String

    private lateinit var orderByDateTaken: String
    private lateinit var orderByDateAdded: String

    init {
        val colorOptions = context.resources.getStringArray(R.array.prefStyleColorValues)
        colorDefault = colorOptions[0]
        colorBlueGrey = colorOptions[1]
        colorIndigo = colorOptions[2]
        colorTeal = colorOptions[3]
        colorPurple = colorOptions[4]

        val orderByOptions = context.resources.getStringArray(R.array.prefOrderByEntries)
        orderByDateAdded = orderByOptions[0]
        orderByDateTaken = orderByOptions[1]
        setItemSort()
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
            return sharedPref.getBoolean(Key_PREF_EXIF_ORIENTATION, true)
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

    val theme: Int
        get() {
            val color = sharedPref.getString(KEY_PREF_STYLE_COLOR, "")
            return R.style.DarkAppBase
            return when (color) {
                colorPurple -> R.style.Purple
                colorTeal -> R.style.Teal
                colorIndigo -> R.style.Indigo
                colorBlueGrey -> R.style.BlueGrey
                else -> R.style.DarkAppBase
            }
        }

    val dialogTheme: Int
        get() {
            val color = sharedPref.getString(KEY_PREF_STYLE_COLOR, "")
            return when (color) {
                colorPurple -> R.style.DialogPurple
                colorTeal -> R.style.DialogTeal
                colorIndigo -> R.style.DialogIndigo
                colorBlueGrey -> R.style.DialogBlueGrey
                else -> R.style.DarkDialogBase
            }
        }

    val defaultCollectionColor: Int
        get() {
            val color = sharedPref.getString(KEY_PREF_STYLE_COLOR, "")

            return ContextCompat.getColor(context, R.color.colorHighlightDefault)
        }

    val primaryColor: Int
        get() {
            val color = sharedPref.getString(KEY_PREF_STYLE_COLOR, "")
            return when (color) {
                colorPurple -> ContextCompat.getColor(context, R.color.colorPrimaryPurple)
                colorTeal -> ContextCompat.getColor(context, R.color.colorPrimaryTeal)
                colorIndigo -> ContextCompat.getColor(context, R.color.colorPrimaryIndigo)
                colorBlueGrey -> ContextCompat.getColor(context, R.color.colorPrimaryBlueGrey)
                else -> ContextCompat.getColor(context, R.color.primary)
            }
        }

    val colorPrimaryDark: Int
        get() {
            return ContextCompat.getColor(context, R.color.primary_dark)
        }

    val secondaryColor: Int
        get() {
            val color = sharedPref.getString(KEY_PREF_STYLE_COLOR, "")
            return when (color) {
                colorPurple -> ContextCompat.getColor(context, R.color.colorSecondaryPurple)
                colorTeal -> ContextCompat.getColor(context, R.color.colorSecondaryTeal)
                colorIndigo -> ContextCompat.getColor(context, R.color.colorSecondaryIndigo)
                colorBlueGrey -> ContextCompat.getColor(context, R.color.colorSecondaryBlueGrey)
                else -> ContextCompat.getColor(context, R.color.primary_light)
            }
        }

    val highlightColorAccent: Int
    // TODO - Accent from custom choice...
        get() = ContextCompat.getColor(context, R.color.accent)

    var columnsInPortrait: Int
        get() = Integer.valueOf(sharedPref.getInt(KEY_PREF_STYLE_COLUMNS, 4))!!
        set(value) {
            sharedPref.edit().apply() {
                putInt(KEY_PREF_STYLE_COLUMNS, value)
                commit()
            }
        }

    var mainColumnsInPortrait: Int
        get() = Integer.valueOf(sharedPref.getInt(KEY_PREF_STYLE_MAIN_COLUMNS, 2))!!
        set(value) {
            sharedPref.edit().apply() {
                putInt(KEY_PREF_STYLE_MAIN_COLUMNS, value)
                commit()
            }
        }

    fun colorizeTitlebar(): Boolean = sharedPref.getBoolean(KEY_PREF_STYLE_COLORTITLE, false)

    fun hideDrawerHeader(): Boolean = sharedPref.getBoolean(KEY_PREF_STYLE_HIDE_DRAWER_HEADER, false)
}
