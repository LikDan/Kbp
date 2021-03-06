package com.ldc.kbp.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.ldc.kbp.*
import com.ldc.kbp.models.Journal
import com.ldc.kbp.models.JournalTeacherSelector
import com.ldc.kbp.views.PinnedScrollView
import com.ldc.kbp.views.adapters.RoundButtonsAdapter
import com.ldc.kbp.views.adapters.journal.*
import com.ldc.kbp.views.adapters.search.CategoryAdapter
import com.ldc.kbp.views.itemdecoritions.BottomOffsetDecoration
import com.ldc.kbp.views.itemdecoritions.SpaceDecoration
import kotlinx.android.synthetic.main.fragment_journal.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.concurrent.thread


class JournalFragment : Fragment() {

    private lateinit var root: View

    private val httpRequests = HttpRequests()

    @Throws(IllegalStateException::class)
    fun loginStudent(surname: String, groupId: String, birthday: String): Document {
        val sCode = httpRequests.get("$JOURNAL_URL/templates/login_parent.php")
            .substringAfter("value=\"")
            .substringBefore("\">")

        val result = httpRequests.post(
            "$JOURNAL_URL/ajax.php",
            "action" to "login_parent",
            "student_name" to surname,
            "group_id" to groupId,
            "birth_day" to birthday,
            "S_Code" to sCode
        )

        when (result) {
            "Попытка подмены токена, повторите попытку отправки формы!" -> error(R.string.token_error)
            "Неверные данные!" -> error(R.string.incorrect_data)
            "good" -> {
            }
            else -> error(R.string.unknown_response)
        }

        return Jsoup.connect("$JOURNAL_URL/templates/parent_journal.php").get()
    }

    @Throws(IllegalStateException::class)
    fun loginTeacher(surname: String, password: String): Document {
        val sCode = httpRequests.get("$JOURNAL_URL/templates/login_teacher.php")
            .substringAfter("value=\"")
            .substringBefore("\">")

        val result = httpRequests.post(
            "$JOURNAL_URL/ajax.php",
            "action" to "login_teather",
            "login" to surname,
            "password" to password,
            "S_Code" to sCode
        )

        when (result) {
            "Попытка подмены токена, повторите попытку отправки формы!" -> error(R.string.token_error)
            "Неверный данные!" -> error(R.string.incorrect_data)
            "good" -> {
            }
            else -> error(R.string.unknown_response)
        }

        return Jsoup.connect("$JOURNAL_URL/templates/teacher_journal.php").get()
    }

    private lateinit var info: JournalTeacherSelector.Subject
    private var pairType = 0

    private lateinit var journal: Journal

    private lateinit var monthSelectorAdapter: RoundButtonsAdapter
    private lateinit var dateAdapter: JournalDateAdapter
    private lateinit var cellsAdapter: JournalCellsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_journal, container, false).apply {
        root = this

        val bottomSheetBehavior = BottomSheetBehavior.from(journal_bottom_sheet)

        val categoryAdapter =
            CategoryAdapter(requireContext(), listOf("Лекция", "Практика", "Лабораторная"), false)
        categoryAdapter.onItemClickListener = { pos, _ -> pairType = pos }
        categoryAdapter.selectionIndex = 0

        category_recycler.adapter = categoryAdapter

        val bottomOffsetDecoration = BottomOffsetDecoration(resources.getDimension(R.dimen.bottomSpace).toInt())
        journal_subjects_name_scroll.addItemDecoration(bottomOffsetDecoration)

        val datePickerPopup = createDatePicker(requireContext()) { date ->
            journal_add_date_layout.isVisible = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

            date_tv.text = date.toString()
        }

        journal_multi_month.isSelected = config.multiMonth

        monthSelectorAdapter = RoundButtonsAdapter(requireContext())
        month_selector_recycler.adapter = monthSelectorAdapter
        month_selector_recycler.addItemDecoration(SpaceDecoration(20))

        monthSelectorAdapter.onItemClickListener = { i, month ->
            if (journal_multi_month.isSelected) {
                if (i == 0) {
                    journal_marks_scroll.recyclerView.scrollToPosition(0)
                    journal_date_scroll.scrollToPosition(0)
                } else {
                    val scrollDates = journal.dates.subList(0, i).sumOf { it.dates.size }
                    journal_marks_scroll.recyclerView.scrollToPosition(scrollDates * journal.subjects.size)
                    journal_date_scroll.scrollToPosition(scrollDates)
                }
            } else {
                cellsAdapter.currentMonth = i
                dateAdapter.currentMonth = i
            }

            root.month_text.text =
                requireContext().resources.getStringArray(R.array.months)[month.toInt() - 1]
        }

        confirm_button.setOnClickListener {
            journal_add_date_layout.isVisible = false
            CoroutineScope(Dispatchers.IO).launch {
                val pairType = categoryAdapter.selectionIndex
                val description = description_edit.text.toString()

                val newHtml = httpRequests.post(
                    "$JOURNAL_URL/ajax.php",
                    "action" to "add_date",
                    "new_date" to "${date_tv.text}",
                    "subject_id" to info.subjectId,
                    "group_id" to info.groupId,
                    "pair_type" to "$pairType",
                    "pair_disc" to description
                )

                val laboratoryTableHtml = httpRequests.post(
                    "$JOURNAL_URL/ajax.php",
                    "action" to "show_labs",
                    "subject_id" to info.subjectId,
                    "group_id" to info.groupId
                )

                update(Journal.parseTeacherJournal(Jsoup.parse(newHtml), Jsoup.parse(laboratoryTableHtml)), bottomSheetBehavior, false)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val html: Document
                if (config.isStudent) {
                    html = loginStudent(
                        config.surname.substringBefore(" "),
                        config.groupId,
                        config.password
                    )
                    update(Journal.parseJournal(html), bottomSheetBehavior)
                } else {
                    html = loginTeacher(config.groupId, config.password)
                    updateSelector(
                        JournalTeacherSelector.parseTeacherSelector(html),
                        bottomSheetBehavior
                    )
                }
            } catch (ex: IllegalStateException) {
                shortSnackbar(root, ex.message!!.toInt())
            }
        }

        journal_multi_month.onSelectionChanged = {
            if (it) {
                cellsAdapter.currentMonth = monthSelectorAdapter.selectionIndex
                dateAdapter.currentMonth = monthSelectorAdapter.selectionIndex
            } else {
                cellsAdapter.currentMonth = null
                dateAdapter.currentMonth = null
            }
        }

        journal_average_img.setOnClickListener {
            journal_average_scroll.isVisible = !journal_average_scroll.isVisible
        }

        journal_add_date_img.setOnClickListener {
            datePickerPopup.show()
        }

        journal_marks_scroll.scrollContainers = listOf(
            PinnedScrollView.Container(journal_date_scroll, LinearLayout.HORIZONTAL, false),
            PinnedScrollView.Container(journal_subjects_name_scroll, LinearLayout.VERTICAL, false),
            PinnedScrollView.Container(journal_average_scroll, LinearLayout.VERTICAL, false)
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun update(
        journal: Journal,
        behavior: BottomSheetBehavior<*>,
        isStudent: Boolean = true
    ) = MainScope().launch {
        this@JournalFragment.journal = journal

        dateAdapter = JournalDateAdapter(requireContext(), journal.dates)

        root.journal_date_scroll.adapter = dateAdapter
        root.journal_date_scroll.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)

        root.journal_average_scroll.adapter =
            JournalAverageAdapter(requireContext(), journal.subjects)
        root.journal_average_scroll.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        root.journal_subjects_name_scroll.adapter =
            JournalSubjectsNameAdapter(requireContext(), journal.subjects.map { it.name })
        root.journal_subjects_name_scroll.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        monthSelectorAdapter.items = journal.dates.map { it.month.toString() }
        monthSelectorAdapter.selectionIndex = monthSelectorAdapter.items!!.lastIndex


        var selectedMark: Journal.Mark? = null

        val viewMarkAdapter = JournalViewMarkAdapter(requireContext()) { selectedMark = it }

        root.journal_view_marks_recycler.adapter = viewMarkAdapter

        if (!isStudent) root.journal_add_date_img.isVisible = !config.isStudent

        cellsAdapter = JournalCellsAdapter(
            requireContext(),
            journal,
            if (config.multiMonth) null else journal.dates.size - 1
        )
        cellsAdapter.onVisibleMonthChanged = { index, month ->
            root.month_text.text =
                requireContext().resources.getStringArray(R.array.months)[month - 1]

            monthSelectorAdapter.selectionIndex = index
        }

        var cells = listOf<Journal.Cell>()
        var updateIndexes: List<Int>? = null

        val marks = mutableListOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "X", "н", "з")
        root.journal_add_mark_recycler.adapter = JournalAddMarksAdapter(requireContext(), marks) { action, _ ->
            MainScope().launch(Dispatchers.IO) {
                cells.forEach { cell ->
                    if (action == "X") {
                        when {
                            selectedMark == null -> cell.marks.removeIf { it.remove(httpRequests, cell) == "0" }
                            selectedMark!!.remove(httpRequests, cell) == "0" -> cell.marks.remove(selectedMark)
                        }
                    } else {
                        val markId = httpRequests.post(
                            "$JOURNAL_URL/ajax.php",
                            "action" to "set_mark",
                            "student_id" to cell.studentId,
                            "pair_id" to cell.pairId,
                            "mark_id" to "${selectedMark?.markId ?: 0}",
                            "value" to action
                        ).lines().last()

                        if (selectedMark != null)
                            cell.marks.remove(selectedMark)

                        cell.marks.add(Journal.Mark(action, markId))
                    }
                }

                launch(Dispatchers.Main) {
                    updateIndexes?.forEach {
                        cellsAdapter.notifyItemChanged(it)
                    } ?: cellsAdapter.notifyDataSetChanged()
                    if (cells.isNotEmpty())
                        viewMarkAdapter.items = cells[0].marks
                }
            }
        }

        dateAdapter.onSelectionChanged = onClick@{ pos ->
            viewMarkAdapter.items = emptyList()
            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            if (isStudent) return@onClick

            cells = journal.subjects.map { s -> s.months.flatMap { it.cells }[pos!!] }
            updateIndexes = null
        }

        cellsAdapter.onClick = onClick@{ cell, pos ->
            viewMarkAdapter.items = cell.marks
            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            if (isStudent) return@onClick

            cells = listOf(cell)
            updateIndexes = listOf(pos)
        }

        root.journal_marks_scroll.spanCount = journal.subjects.size
        root.journal_marks_scroll.recyclerView.adapter = cellsAdapter
        root.journal_marks_scroll.recyclerView.scrollToPosition(cellsAdapter.items.size - 1)
        root.journal_date_scroll.scrollToPosition(dateAdapter.items.size - 1)
    }

    private fun updateSelector(
        selector: JournalTeacherSelector, behavior: BottomSheetBehavior<*>
    ) = MainScope().launch {
        JournalSubjectsNameAdapter(requireContext(), selector.groups.keys.map { it.name })

        val selectorAdapter = JournalSubjectsSelectorAdapter(requireContext(), selector)

        selectorAdapter.onClick = { _, subject ->
            info = subject

            behavior.isHideable = true
            behavior.state = BottomSheetBehavior.STATE_HIDDEN

            thread {
                val mainTableHtml = httpRequests.post(
                    "$JOURNAL_URL/ajax.php",
                    "action" to "show_table",
                    "subject_id" to subject.subjectId,
                    "group_id" to subject.groupId
                )

                val laboratoryTableHtml = httpRequests.post(
                    "$JOURNAL_URL/ajax.php",
                    "action" to "show_labs",
                    "subject_id" to subject.subjectId,
                    "group_id" to subject.groupId
                )

                root.journal_average_scroll.post {
                    root.journal_subjects_selector_recycler.isVisible = false
                    root.journal_add_mark_recycler.isVisible = true

                    journal = Journal.parseTeacherJournal(Jsoup.parse(mainTableHtml), Jsoup.parse(laboratoryTableHtml))

                    update(journal, behavior, false)
                }
            }
        }

        root.journal_subjects_selector_recycler.adapter = selectorAdapter
    }
}
