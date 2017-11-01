package com.labs.pbrother.freegallery.settings

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat

import com.labs.pbrother.freegallery.R

/**
 * Created by simon on 07.12.16.
 */
// TODO - how to implement as singleton?
class SettingsHelper(val context: Context) {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var colorDefault: String
    private lateinit var colorIndigo: String
    private lateinit var colorTeal: String
    private lateinit var colorPurple: String
    private lateinit var colorBlueGrey: String

    companion object {
        @JvmStatic
        val PERMISSION_READ_STORAGE = 1337
        @JvmStatic
        val KEY_PREF_STYLE_COLOR = "pref_key_style_color"
        @JvmStatic
        val KEY_PREF_STYLE_MAIN_COLUMNS = "pref_key_style_main_portrait_columns"
        @JvmStatic
        val KEY_PREF_STYLE_COLUMNS = "pref_key_style_portrait_columns"
        @JvmStatic
        val KEY_PREF_STYLE_COLORTITLE = "pref_key_style_colorize_title"
        @JvmStatic
        val KEY_PREF_STYLE_HIDE_DRAWER_HEADER = "pref_key_style_hide_drawer_header"
    }

    init {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val colorOptions = context.resources.getStringArray(R.array.prefStyleColorValues)
        colorDefault = colorOptions[0]
        colorBlueGrey = colorOptions[1]
        colorIndigo = colorOptions[2]
        colorTeal = colorOptions[3]
        colorPurple = colorOptions[4]
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

    val higlightColor: Int
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
            val editor = sharedPref.edit()
            editor.putInt(KEY_PREF_STYLE_COLUMNS, value)
            editor.commit()
        }

    var mainColumnsInPortrait: Int
        get() = Integer.valueOf(sharedPref.getInt(KEY_PREF_STYLE_MAIN_COLUMNS, 2))!!
        set(value) {
            val editor = sharedPref.edit()
            editor.putInt(KEY_PREF_STYLE_MAIN_COLUMNS, value)
            editor.commit()
        }

    fun colorizeTitlebar(): Boolean = sharedPref.getBoolean(KEY_PREF_STYLE_COLORTITLE, false)

    fun hideDrawerHeader(): Boolean = sharedPref.getBoolean(KEY_PREF_STYLE_HIDE_DRAWER_HEADER, false)
}
