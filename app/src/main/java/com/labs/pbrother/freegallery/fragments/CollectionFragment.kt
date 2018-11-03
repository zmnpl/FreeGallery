package com.labs.pbrother.freegallery.fragments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.NavUtils
import android.support.v7.widget.GridLayoutManager
import android.view.*
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.activities.CollectionActivityViewModel
import com.labs.pbrother.freegallery.adapters.CollectionRecyclerViewAdapter
import com.labs.pbrother.freegallery.app
import com.labs.pbrother.freegallery.controller.Provider
import com.labs.pbrother.freegallery.controller.TYPE_TAG
import com.labs.pbrother.freegallery.dialogs.TagDialogFragment
import com.labs.pbrother.freegallery.extension.PORTRAIT
import com.labs.pbrother.freegallery.extension.REVERSE_PORTRAIT
import com.labs.pbrother.freegallery.extension.getRotation
import com.labs.pbrother.freegallery.extension.tagSymbol
import com.labs.pbrother.freegallery.prefs
import com.labs.pbrother.freegallery.uiother.ItemOffsetDecoration
import kotlinx.android.synthetic.main.fragment_collection.*
import kotlinx.android.synthetic.main.fragment_collection.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.image
import org.jetbrains.anko.uiThread

private const val CID = "collectionId"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [CollectionFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [CollectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class CollectionFragment : Fragment(), CollectionRecyclerViewAdapter.ViewHolder.ClickListener {

    // parameters
    private lateinit var cid: String

    // other
    private lateinit var viewModel: CollectionActivityViewModel
    private var listener: OnFragmentInteractionListener? = null

    // ui
    private lateinit var adapter: CollectionRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        arguments?.let {
            cid = it.getString(CID)
        }

        // bind to viewmodel
        viewModel = ViewModelProviders.of(this).get(CollectionActivityViewModel::class.java)

        viewModel.items.observe(this, Observer { items ->
            if (null != items) {
                adapter = CollectionRecyclerViewAdapter(this@CollectionFragment, activity as Context, items, Provider(app))
                collection_rclPictureCollection.adapter = adapter
            }
        })

        viewModel.collectionItem.observe(this, Observer { collectionItem ->
            //if (null != collectionItem) supportActionBar?.title = collectionItem.displayNameDetail
        })

        viewModel.liveColor.observe(this, Observer { color ->
            //if (null != color) changeColor(color)
        })
    }

    private fun refresh(collection: Boolean, drawer: Boolean, items: Boolean, cached: Boolean = false) {
        if (!swipeRefreshCollection.isRefreshing) swipeRefreshCollection.isRefreshing = true
        doAsync {
            viewModel.refresh(collection, drawer, items, cid, cached)
            uiThread {
                swipeRefreshCollection.isRefreshing = false
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_collection, container, false)

        // recycler list
        val colCount = columns
        rootView.collection_rclPictureCollection.apply {
            addItemDecoration(ItemOffsetDecoration(activity as Context, R.dimen.collection_picture_padding, colCount))
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(activity as Context, colCount)
            isSaveEnabled = true
        }

        rootView.swipeRefreshCollection.setOnRefreshListener {
            refresh(true, true, true,true)
        }

        return rootView
    }

    override fun onStart() {
        super.onStart()

        // floating action button
        collection_shareFloatingActionButton.setOnClickListener { tag() }
        collection_shareFloatingActionButton.image = activity?.tagSymbol()

        // trigger loading of data
        refresh(true, true, true,true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        if (cid == getString(R.string.trashName)) {
            inflater?.inflate(R.menu.menu_collection_trash, menu)
        } else {
            inflater?.inflate(R.menu.menu_collection, menu)
            if (viewModel.collectionType == TYPE_TAG && cid != getString(R.string.timelineName)) {
                menu?.findItem(R.id.menu_deleteTag)?.isVisible = true
            }
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
            R.id.menu_deleteTag -> {
                deleteTag()
                return true
            }
            R.id.menu_refresh -> {
                refresh(true, true, true, true)
                return true
            }
            R.id.menu_colorize -> {
                colorize()
                return true
            }
            R.id.menu_resetColor -> {
                doAsync {
                    dataChanged = true
                    informCallerOfChange()
                    viewModel.removeColor()
                }
                return true
            }
            R.id.menu_trash_emptyTrash -> {
                emptyTrash()
                return true
            }
            R.id.menu_collectionSortAsc -> {
                viewModel.setSortAsc()
                return true
            }
            R.id.menu_collectionSortDesc -> {
                viewModel.setSortDesc()
                return true
            }
            R.id.menu_collectionZoomViewIn -> {
                applyZoom(-1)
                return true
            }
            R.id.menu_collectionZoomViewOut -> {
                applyZoom(+1)
                return true
            }
            R.id.menu_selectAll -> {
                selectAll()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
       /* if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }*/
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    // functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Starts tag dialog
    // Callback method colorOk does the actual work
    private fun tag() {
        val std = TagDialogFragment()
        //std.setTags(viewModel.tags)
        //std.show(this.fragmentManager, "tagdialog")
    }

    override fun onItemClicked(position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onItemLongClicked(position: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // helper
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private val columns: Int
        get() = if (activity?.getRotation() === PORTRAIT || activity?.getRotation() === REVERSE_PORTRAIT) {
            prefs.columnsInPortrait
        } else {
            (prefs.columnsInPortrait * 1.5).toInt()
        }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CollectionFragment.
         */
        @JvmStatic
        fun newInstance(cid: String) =
                CollectionFragment().apply {
                    arguments = Bundle().apply {
                        putString(CID, cid)
                    }
                }
    }
}
