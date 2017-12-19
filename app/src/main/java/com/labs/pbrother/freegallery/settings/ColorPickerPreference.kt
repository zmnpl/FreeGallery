package com.labs.pbrother.freegallery.settings

import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.preference.DialogPreference
import android.preference.Preference
import android.support.annotation.ColorInt
import android.util.AttributeSet
import android.view.View
import com.labs.pbrother.freegallery.R
import com.larswerkman.lobsterpicker.LobsterPicker
import com.larswerkman.lobsterpicker.OnColorListener
import com.larswerkman.lobsterpicker.sliders.LobsterShadeSlider
import kotlinx.android.synthetic.main.preference_color.view.*


/**
 * Created by simon on 21.09.17.
 */

class ColorPickerPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {
    private lateinit var colorPicker: LobsterPicker
    private lateinit var shadeSlider: LobsterShadeSlider

    private val DEFAULT_COLOR = 255
    private var currentColor = 0
    var newColor = 0

    init {
        dialogLayoutResource = R.layout.preference_color
        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
        dialogIcon = null
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)

        colorPicker = view?.preferenceColorPicker ?: LobsterPicker(context)
        shadeSlider = view?.preferenceColorPickerShade ?: LobsterShadeSlider(context)
        colorPicker.addDecorator(shadeSlider)
        colorPicker.color = currentColor

        colorPicker.addOnColorListener(object : OnColorListener {
            override fun onColorChanged(@ColorInt color: Int) {
                newColor = color
            }

            override fun onColorSelected(@ColorInt color: Int) {
                newColor = color
            }
        })
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        // When the user selects "OK", persist the new value

        if (positiveResult) {
            currentColor = newColor
            persistInt(newColor)
        }
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        if (restorePersistedValue) {
            // Restore existing state
            currentColor = this.getPersistedInt(DEFAULT_COLOR)
        } else {
            // Set default state from the XML attribute
            currentColor = defaultValue as Int
            persistInt(currentColor)
        }

    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, DEFAULT_COLOR)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        // Check whether this Preference is persistent (continually saved)
        if (isPersistent) {
            // No need to save instance state since it's persistent,
            // use superclass state
            return superState
        }

        // Create instance of custom BaseSavedState
        val myState = SavedState(superState)
        // Set the state's value with the class member that holds current
        // setting value
        myState.value = newColor
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || state.javaClass != SavedState::class.java) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state)
            return
        }

        // Cast state to custom BaseSavedState and pass to superclass
        val myState = state as SavedState?
        super.onRestoreInstanceState(myState!!.superState)

        // Set this Preference's widget to reflect the restored state
        //colorPicker.liveColor = myState.value
    }

    private class SavedState : Preference.BaseSavedState {
        // Member that holds the setting's value
        // Change this data type to match the type saved by your Preference
        internal var value: Int = 0

        constructor(superState: Parcelable) : super(superState)

        constructor(source: Parcel) : super(source) {
            // Get the current preference's value
            value = source.readInt()  // Change this to read the appropriate data type
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            // Write the preference's value
            dest.writeInt(value)  // Change this to write the appropriate data type
        }

        companion object {

            // Standard creator object using an instance of this class
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {

                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}
