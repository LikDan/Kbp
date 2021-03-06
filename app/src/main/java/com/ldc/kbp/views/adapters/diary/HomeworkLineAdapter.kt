package com.ldc.kbp.views.adapters.diary

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.core.app.ActivityCompat.startActivityForResult
import com.ldc.kbp.*
import com.ldc.kbp.models.Homeworks
import com.ldc.kbp.models.Schedule
import com.ldc.kbp.views.adapters.Adapter
import com.ldc.kbp.views.dialogs.HomeworkSetDialog
import kotlinx.android.synthetic.main.item_homework_line.view.*
import org.threeten.bp.LocalDate
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.concurrent.thread

class HomeworkLineAdapter(
    private val activity: Activity,
    private val homeworksDay: Homeworks.Day,
    items: List<Schedule.Subjects?>,
    private val date: LocalDate,
    var imageAddListener: ((Int, Bitmap, File) -> Unit) = { _, _, _ -> },
    var onHomeworkChangeListener: (Schedule.Subject, Homeworks.Homework) -> Unit = { _, _ -> }
) : Adapter<Schedule.Subjects?>(activity, items.filterNotNull(), R.layout.item_homework_line) {

    override fun onBindViewHolder(view: View, item: Schedule.Subjects?, position: Int) {
        item ?: return

        val subject = item.subjects[0]

        view.item_homework_line_index.text = (item.rowIndex + 1).toString()
        view.item_homework_line_subject.text = subject.subject
        view.item_homework_line_text.text = homeworksDay.subjects[subject.subject]?.homework

        view.item_homework_line_text.setOnClickListener {
            HomeworkSetDialog(context, subject, homeworksDay.subjects[subject.subject]) {
                onHomeworkChangeListener(subject, it)
            }.show()
        }

        view.item_homework_line_take_photo.setOnClickListener {
            checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, activity) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val file = getFile(
                    Environment.DIRECTORY_PICTURES,
                    "${item.rowIndex}. $date ${subject.subject} ${getFilesInMedia(item)?.size ?: 0}",
                    "jpg"
                )

                intent.putExtra(MediaStore.EXTRA_OUTPUT, getUrlViaProvider(context, file))

                startActivityForResult(activity, intent, 0, null)
            }
        }
        view.item_homework_line_show_photo.setOnClickListener {
            val photos = getFilesInMedia(item)
            if (!photos.isNullOrEmpty()) {
                shortSnackbar(view, R.string.loading_photo)
                photos.forEachIndexed { index, file ->
                    thread {
                        imageAddListener(index, FileInputStream(file).getBitmap(), file)
                    }
                }
            } else shortSnackbar(view, R.string.load_photo)
        }
    }

    private fun getFilesInMedia(subjects: Schedule.Subjects): Array<File>? =
        getDir(Environment.DIRECTORY_PICTURES).listFiles { _, s ->
            s.contains("${subjects.rowIndex}. $date ${subjects.subjects[0].subject}")
        }


    private fun InputStream.getBitmap(): Bitmap = BitmapFactory.decodeStream(this).run {
        Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(90F) }, true)
    }

}