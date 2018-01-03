package com.labs.pbrother.freegallery.dialogs

import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.WindowManager
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.activities.CollectionActivity
import com.labs.pbrother.freegallery.settings.SettingsHelper
import kotlinx.android.synthetic.main.dialog_colorize.view.*

/**
 * Created by simon on 07.11.16.
 */

class ColorizeDialogFragment() : DialogFragment() {

    interface ColorDialogListener {
        fun colorOk(color: Int)
        fun colorCancel()
    }

    private lateinit var settings: SettingsHelper
    private lateinit var listener: ColorDialogListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        settings = SettingsHelper(activity.applicationContext)

        val builder = AlertDialog.Builder(activity, settings.dialogTheme)
        val inflater = activity.layoutInflater
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        val mainView = inflater.inflate(R.layout.dialog_colorize, null)
        val lobsterPicker = mainView.colorDialogLobsterpicker
        val shadeSlider = mainView.colorDialogShadeslider
        //val opacitySlider = mainView.colorDialogOpacityslider

        lobsterPicker.color = activity.getColor(R.color.accent)
        if (activity is CollectionActivity) {
            val actualColor = (activity as CollectionActivity).collectionColor
            if (null != actualColor) {
                lobsterPicker.color = actualColor
            }
        }
        //opacitySlider.opacity = 255
        lobsterPicker.addDecorator(shadeSlider)
        //lobsterPicker.addDecorator(opacitySlider)

        builder.setView(mainView)
                .setPositiveButton(R.string.ColorizeOk) { dialog, id ->
                    listener.colorOk(lobsterPicker!!.color)
                }
                .setNegativeButton(R.string.ColorizeCancel) { dialog, id -> listener.colorCancel() }

        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Verify that the host activity implements the callback interface
        listener = try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            activity as ColorDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(activity.toString() + " must implement ColorDialogListener")
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as AlertDialog
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }
}
