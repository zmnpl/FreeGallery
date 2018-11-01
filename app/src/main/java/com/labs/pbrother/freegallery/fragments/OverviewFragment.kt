package com.labs.pbrother.freegallery.fragments

import android.app.Fragment
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.activities.MainActivity
import com.labs.pbrother.freegallery.activities.MainActivityViewModel
import com.labs.pbrother.freegallery.adapters.OverviewRecyclerViewAdapter
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.controller.Provider
import com.labs.pbrother.freegallery.prefs
import com.labs.pbrother.freegallery.uiother.ItemOffsetDecoration
import kotlinx.android.synthetic.main.fragment_overview.*
import kotlinx.android.synthetic.main.fragment_overview.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private lateinit var viewModel: MainActivityViewModel
private lateinit var adapter: OverviewRecyclerViewAdapter

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OverviewFragment.OnMainFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [OverviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class OverviewFragment : android.support.v4.app.Fragment(), OverviewRecyclerViewAdapter.ViewHolder.ClickListener {

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnMainFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        bindViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_overview, container, false)

        rootView.overviewRecycler.apply {
            setHasFixedSize(true)
            val ctx = activity as Context
            layoutManager = GridLayoutManager(ctx, prefs.mainColumnsInPortrait)
            addItemDecoration(ItemOffsetDecoration(ctx, R.dimen.collection_picture_padding, prefs.mainColumnsInPortrait))
        }

        rootView.swipeRefreshMain.setOnRefreshListener { buildUiSafe() }

        // Inflate the layout for this fragment
        return rootView
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        //listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnMainFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnMainFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun bindViewModel() {
        viewModel = ViewModelProviders.of(activity as MainActivity).get(MainActivityViewModel::class.java!!)

        viewModel.overviewItems.observe(activity as MainActivity, Observer { overviewItems ->
            populateAdapter(overviewItems)
        })
    }

    private fun populateAdapter(overviewItems: ArrayList<CollectionItem>?) {
        if (null != overviewItems) {
            val fract = activity as FragmentActivity
            adapter = OverviewRecyclerViewAdapter(this, fract, overviewItems, Provider(fract.application))
            adapter.setHasStableIds(true)
            overviewRecycler.adapter = adapter
        }
    }

    private fun buildUiSafe() {
        //if (permissionsGood) {
        refresh()
        //    reloadPlz = false
        //}
    }

    private fun refresh() {
        swipeRefreshMain.isRefreshing = true
        doAsync {
            viewModel.refresh()
            uiThread {
                swipeRefreshMain.isRefreshing = false
            }
        }
    }


    // clicks on item in main view
    override fun onItemClicked(position: Int) {
        //if (actionMode != null) {
        //    toggleSelection(position)
        //} else {
        //    startActivityForResult(
        //            intentFor<CollectionActivity>(
        //                    EXTRA_ITEM_INDEX to position,
        //                    EXTRA_COLLECTIONID to adapter.getItemStringId(position)),
        //            COLLECTION_ACTIVITY_REQUEST_CODE)
        //}
        listener?.onMainItemClick(position)
    }

    override fun onItemLongClicked(position: Int): Boolean {
        //if (actionMode == null) {
        //    actionMode = startSupportActionMode(actionModeCallback)
        //}
        //toggleSelection(position)

        return true
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
    interface OnMainFragmentInteractionListener {
        fun onMainItemClick(position: Int)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment OverviewFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                OverviewFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
