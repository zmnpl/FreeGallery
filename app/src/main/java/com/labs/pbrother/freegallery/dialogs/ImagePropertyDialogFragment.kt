package com.labs.pbrother.freegallery.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.view.WindowManager
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.byteSizeToNiceString
import com.labs.pbrother.freegallery.controller.Item
import com.labs.pbrother.freegallery.prefs
import com.labs.pbrother.freegallery.unixToReadableDate
import kotlinx.android.synthetic.main.dialog_itemroperties.*
import java.io.File

/**
 * Created by simon on 07.11.16.
 */

class ImagePropertyDialogFragment : androidx.fragment.app.DialogFragment() {

    private lateinit var item: Item
    private lateinit var listener: ImagePropertyDialogListener

    // Callback Interface
    interface ImagePropertyDialogListener {
        fun imgPropertyOk()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity as Context, prefs.dialogTheme)
        val inflater = activity?.layoutInflater

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater?.inflate(R.layout.dialog_itemroperties, null))
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
        val f = File(item.path)
//        dialog?.let {
//            it.imgproperty_txtPath.text = f?.parent ?: ""
//            it.imgproperty_txtTags.text = item.tagString
//            if (item.tagString.length == 0) {
//                it.imgproperty_lblTags.visibility = View.INVISIBLE
//                it.imgproperty_txtTags.visibility = View.INVISIBLE
//            }
//            it.imgproperty_txtName.text = item.fileName
//            it.imgproperty_txtDateAdded.text = unixToReadableDate(item.dateAdded)
//            it.imgproperty_txtSize.text = byteSizeToNiceString(item.size)
//            it.imgproperty_txtDimensions.text = String.format("%d px", item.width) + " x " + String.format("%d px", item.height)
//
//            // Exif
//            it.imgproperty_txtLatitude.text = item.latitude.toString()
//            it.imgproperty_txtLongitude.text = item.longitude.toString()
//            it.imgproperty_txtIso.text = item.iso.toString()
//            it.imgproperty_txtCameraModel.text = item.camera
//            //dialog.imgproperty_txtShutter.text = item.exposureTimeSeconds()
//
//            it.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
//        }
        dialog?.apply {
            imgproperty_txtPath.text = f?.parent ?: ""
            imgproperty_txtTags.text = item.tagString
            if (item.tagString.length == 0) {
                imgproperty_lblTags.visibility = View.INVISIBLE
                imgproperty_txtTags.visibility = View.INVISIBLE
            }
            imgproperty_txtName.text = item.fileName
            imgproperty_txtDateAdded.text = unixToReadableDate(item.dateAdded)
            imgproperty_txtSize.text = byteSizeToNiceString(item.size)
            imgproperty_txtDimensions.text = String.format("%d px", item.width) + " x " + String.format("%d px", item.height)

            // Exif
            imgproperty_txtLatitude.text = item.latitude.toString()
            imgproperty_txtLongitude.text = item.longitude.toString()
            imgproperty_txtIso.text = item.iso.toString()
            imgproperty_txtCameraModel.text = item.camera
            //dialog.imgproperty_txtShutter.text = item.exposureTimeSeconds()

            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    fun setItem(item: Item) {
        this.item = item
    }

}
