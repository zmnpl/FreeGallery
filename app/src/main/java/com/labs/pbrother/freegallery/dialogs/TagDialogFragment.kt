package com.labs.pbrother.freegallery.dialogs

import android.app.Activity
import android.app.Dialog
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.prefs
import kotlinx.android.synthetic.main.dialog_tag.*


/**
 * Created by simon on 07.11.16.
 */

class TagDialogFragment : DialogFragment() {

    // Callback Interface
    interface TagDialogListener {
        fun tagOk(tag: String)
        fun tagCancel()
    }

    private lateinit var tags: List<String>
    private lateinit var listener: TagDialogListener
    private lateinit var tagField: AutoCompleteTextView

    fun setTags(tags: List<String>) {
        this.tags = tags.map { it.substring(1) }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val actvty = activity
        val builder = AlertDialog.Builder(actvty as Context, prefs.dialogTheme)
        val inflater = actvty.layoutInflater

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_tag, null))
                .setPositiveButton(R.string.SnaketagOk) { dialog, id ->
                    listener.tagOk(getString(R.string.tagLetter) +
                            tagField.text?.split(" ")
                                    ?.map { it.capitalize() }
                                    ?.joinToString("")
                                    ?.decapitalize()
                                    ?.trim())
                }
                .setNegativeButton(getString(R.string.SnaketagCancel)) { dialog, id ->
                    listener.tagCancel()
                    hideKeyboardFrom(actvty, tagField)
                }

        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Verify that the host activity implements the callback interface
        listener = try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            parentFragment as TagDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(parentFragment.toString() + " must implement ColorDialogListener")
        }

    }

    override fun onStart() {
        super.onStart()

        val adapter = ArrayAdapter(dialog.context, android.R.layout.select_dialog_item, tags)
        tagField = dialog.tagAutocomplete
        tagField.threshold = 1
        tagField.setAdapter(adapter)
        tagField.requestFocus()

        // show keyboard ...
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }


    // hideous ...
    // thanks to rmirabelle on https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
    private fun hideKeyboardFrom(context: Context, view: View) {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

}
