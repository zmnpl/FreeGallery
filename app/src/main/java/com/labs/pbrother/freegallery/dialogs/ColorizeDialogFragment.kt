package com.labs.pbrother.freegallery.dialogs


import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog


import android.view.WindowManager
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.prefs
import kotlinx.android.synthetic.main.dialog_colorize.view.*

/**
 * Created by simon on 07.11.16.
 */

class ColorizeDialogFragment() : DialogFragment() {

    interface ColorDialogListener {
        fun colorOk(color: Int)
        fun colorCancel()
    }

    private lateinit var listener: ColorDialogListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val actvty = activity
        val builder = AlertDialog.Builder(actvty as Context, prefs.dialogTheme)
        val inflater = actvty.layoutInflater
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        val mainView = inflater.inflate(R.layout.dialog_colorize, null)
        val lobsterPicker = mainView.colorDialogLobsterpicker
        val shadeSlider = mainView.colorDialogShadeslider
        //val opacitySlider = mainView.colorDialogOpacityslider

        lobsterPicker.color = actvty.getColor(R.color.accent)

        // TODO - if (activity is CollectionActivity) {
        if (false) {
            val actualColor = 0//(activity as CollectionActivity).collectionColor
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
            parentFragment as ColorDialogListener
        } catch (e: ClassCastException) {
            // The parent doesn't implement the interface, throw exception
            throw ClassCastException(parentFragment.toString() + " must implement ColorDialogListener")
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as AlertDialog
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }
}
