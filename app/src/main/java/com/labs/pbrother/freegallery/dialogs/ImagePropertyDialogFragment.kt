package com.labs.pbrother.freegallery.dialogs

import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.WindowManager

import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.controller.Item
import com.labs.pbrother.freegallery.settings.SettingsHelper
import kotlinx.android.synthetic.main.dialog_itemroperties.*

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by simon on 07.11.16.
 */

class ImagePropertyDialogFragment : DialogFragment() {

    private lateinit var settings: SettingsHelper
    private lateinit var item: Item
    private lateinit var listener: ImagePropertyDialogListener

    // Callback Interface
    interface ImagePropertyDialogListener {
        fun imgPropertyOk()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        settings = SettingsHelper(activity)

        val builder = AlertDialog.Builder(activity, settings.dialogTheme)
        val inflater = activity.layoutInflater

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_itemroperties, null))
                .setNeutralButton(R.string.ImgPropertyDone) { dialog, id -> listener.imgPropertyOk() }

        // Create the AlertDialog object and return it
        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Verify that the host activity implements the callback interface
        listener = try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            activity as ImagePropertyDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(activity.toString() + " must implement ImagePropertyDialogListener")
        }

    }

    override fun onStart() {
        super.onStart()

        dialog.imgproperty_txtPath.text = item.path
        dialog.imgproperty_txtName.text = item.fileName()
        dialog.imgproperty_txtDateAdded.text = unixToReadableDate(item.dateAdded)
        dialog.imgproperty_txtSize.text = byteSizeToNiceString(item.size)
        dialog.imgproperty_txtLatitude.text = item.latitude.toString()
        dialog.imgproperty_txtLongitude.text = item.longitude.toString()
        val dimens = String.format("%d px", item.width) + " x " + String.format("%d px", item.height)
        dialog.imgproperty_txtDimensions.text = dimens

        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    fun setItem(item: Item) {
        this.item = item
    }

    // takes date as unix time stamp and returns nice printable date
    private fun unixToReadableDate(date: Long): String {
        val d = Date(date * 1000)
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return df.format(d)
    }

    // calculates nice readable size for printing
    private fun byteSizeToNiceString(size: Long): String {
        var readableSize = (size / 1024).toFloat()

        if (readableSize < 0) {
            return size.toString() + " B"
        } else if (readableSize < 1024) {
            return readableSize.toString() + " KB"
        }
        readableSize /= 1024
        return String.format(Locale.US, "%.2f", readableSize) + " MB"
    }

}
