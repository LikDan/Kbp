package com.ldc.kbp.views.fragments

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.ldc.kbp.models.Groups
import com.ldc.kbp.views.adapters.search.CategoryAdapter
import com.ldc.kbp.views.adapters.search.SearchAdapter
import kotlinx.android.synthetic.main.fragment_search.view.*

class SearchFragment<T>(
    val layout: View,
    val items: List<Pair<T, String>>,
    var build: (T) -> String,
    var onSelected: ((T) -> Unit)? = null
) {

    private lateinit var searchAdapter: SearchAdapter<T>
    private lateinit var categoryAdapter: CategoryAdapter

    private val activity = layout.context

    private var keyboard = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

    init {
        updateGroups()

        layout.search_edit.doOnTextChanged { _, _, _, _ -> updateSearch() }

        layout.groups_recycler.setOnTouchListener { view, _ ->
            view.performClick()
            view.parent.requestDisallowInterceptTouchEvent(true)

            false
        }
    }

    private fun updateGroups() {
        if (Groups.timetable.isEmpty()) return

        val unzipped = items.unzip()

        searchAdapter = SearchAdapter(activity, unzipped.first, build)
        categoryAdapter = CategoryAdapter(activity, unzipped.second.distinct())

        layout.category_recycler.adapter = categoryAdapter
        layout.groups_recycler.adapter = searchAdapter

        searchAdapter.onItemClickListener = { _, item ->
            keyboard.hideSoftInputFromWindow(layout.windowToken, 0)

            onSelected?.invoke(item)
        }

        categoryAdapter.onItemClickListener = { _, _ -> updateSearch() }

    }

    private fun updateSearch() {
        val text: String = layout.search_edit.text.toString().lowercase()

        searchAdapter.items = items.filter {
            val built = build(it.first)
            val category = categoryAdapter.selectedItem == null || it.second == categoryAdapter.selectedItem
            built.lowercase().contains(text) && category
        }.unzip().first
    }

    fun show() {
        layout.isVisible = true

        layout.search_edit.setText("")
        layout.search_edit.requestFocus()

        Thread.sleep(250)

        layout.search_edit.requestFocus()
        keyboard.showSoftInput(layout.search_edit, 0)
    }

    fun hide() =
        keyboard.hideSoftInputFromWindow(layout.search_edit.windowToken, 0)

}