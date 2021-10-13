package com.ldc.kbp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.ldc.kbp.*
import com.ldc.kbp.models.Files
import com.ldc.kbp.models.Groups
import com.ldc.kbp.models.Timetable
import com.ldc.kbp.views.fragments.SearchFragment
import kotlinx.android.synthetic.main.fragment_settings.view.*
import kotlin.concurrent.thread

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        with(inflater.inflate(R.layout.fragment_settings, container, false)) {
            val bottomSheetBehavior = BottomSheetBehavior.from(settings_bottom_sheet)

            if (!config.isStudent) {
                name_layout.isVisible = false
                password_tv.setText(R.string.password)
            }

            val searchFragment = SearchFragment { timetableInfo ->
                thread { mainTimetable = Timetable.loadTimetable(timetableInfo) }

                group_name_tv.text = timetableInfo.group
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

                shortToast(requireContext(), R.string.loading)

                config.link = timetableInfo.link

                fun noGroupFound() = Snackbar.make(confirm_button, R.string.logins_not_match, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.enter) {
                        journal_id_layout.isVisible = true
                    }.show()


                when (Groups.categories[timetableInfo.categoryIndex]) {
                    "преподаватель" -> {
                        getUrlFromGroup(
                            "https://nehai.by/ej/t.php",
                            timetableInfo.group
                        ) {
                            if (it == null) {
                                noGroupFound()
                                return@getUrlFromGroup
                            }
                            config.groupId = it
                            Files.saveConfig(requireContext())
                        }

                        config.isStudent = false
                        config.group = ""
                        config.surname = timetableInfo.group
                        Files.saveConfig(requireContext())

                        name_layout.isVisible = false
                        password_tv.setText(R.string.password)
                    }
                    "группа" -> {
                        getUrlFromGroup(
                            "https://nehai.by/ej",
                            timetableInfo.group
                        ) {
                            if (it == null) {
                                noGroupFound()
                                return@getUrlFromGroup
                            }
                            config.groupId = it
                            Files.saveConfig(requireContext())
                        }

                        config.isStudent = true
                        config.group = timetableInfo.group
                        Files.saveConfig(requireContext())

                        name_layout.isVisible = true
                        password_tv.setText(R.string.birthday)
                    }
                }

                journal_id.setText(config.groupId)

                shortToast(requireContext(), R.string.load_end)
            }

            bottomSheetBehavior.addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_COLLAPSED)
                            searchFragment.hide()
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                }
            )

            requireActivity().supportFragmentManager.beginTransaction()
                .add(R.id.groups_selector_fragment, searchFragment).commit()

            search_image.setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                searchFragment.show()
            }

            department_auto.setAdapter(
                ArrayAdapter(
                    context,
                    android.R.layout.simple_dropdown_item_1line,
                    resources.getStringArray(R.array.departments)
                )
            )

            multi_week_mode_switcher.setOnCheckedChangeListener { _, isChecked ->
                config.multiWeek = isChecked
                Files.saveConfig(requireContext())
            }

            name_et.setText(config.surname)
            journal_id.setText(config.groupId)
            password_et.setText(config.password)
            department_auto.setText(config.department)
            multi_week_mode_switcher.isChecked = config.multiWeek
            sex_switcher.isChecked = config.isFemale

            group_name_tv.text = mainTimetable.info?.group

            confirm_button.setOnClickListener {
                if (config.isStudent) config.surname = name_et.text.toString()

                config.groupId = journal_id.text.toString()
                config.password = password_et.text.toString()
                config.department = department_auto.text.toString()
                config.isFemale = sex_switcher.isChecked

                Files.saveConfig(requireContext())
            }
            return this
        }


    private fun getUrlFromGroup(link: String, group: String, onLoad: (String?) -> Unit) {
        WebController(requireContext(), link, scriptName = "journalLogins.js") { _, it ->
            val row = it.split("|")
            val id = row.find { line -> line.substringAfter(":").lowercase().contains(group.lowercase()) }

            onLoad(id?.substringBefore(":"))
        }.load()
    }
}