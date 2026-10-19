
package com.karar.karar

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : Activity() {

    private val bg = Color.rgb(13, 13, 16)
    private val panel = Color.rgb(25, 25, 30)
    private val red = Color.rgb(225, 35, 55)
    private val white = Color.WHITE
    private val muted = Color.LTGRAY

    private lateinit var editor: EditText
    private lateinit var fileLabel: TextView
    private lateinit var status: TextView

    private var currentUri: Uri? = null
    private var currentName = "ملف جديد.txt"
    private var dirty = false

    private val openCode = 100
    private val saveCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(panel, 16)
        }

        val title = TextView(this).apply {
            text = "📂 مستكشف الملفات"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(red)
        }
        header.addView(title)

        fileLabel = TextView(this).apply {
            text = "لم يتم فتح ملف"
            textSize = 13f
            setTextColor(muted)
            setPadding(0, dp(6), 0, 0)
        }
        header.addView(fileLabel)

        root.addView(
            header,
            LinearLayout.LayoutParams(-1, -2)
        )

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        addButton(row1, "📂 فتح ملف") { openFile() }
        addButton(row1, "💾 حفظ") { saveFile() }

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        addButton(row2, "🔍 بحث") { searchText() }
        addButton(row2, "📋 نسخ الكل") { copyAll() }
        addButton(row2, "＋ ملف جديد") { newFile() }

        root.addView(row1)
        root.addView(row2)

        editor = EditText(this).apply {
            setTextColor(white)
            setHintTextColor(Color.GRAY)
            hint = "اكتب النص أو افتح ملفاً..."
            textSize = 15f
            gravity = Gravity.TOP or Gravity.START
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(19, 19, 23))
            typeface = Typeface.MONOSPACE
            setSingleLine(false)
            isVerticalScrollBarEnabled = true
        }

        val editorParams = LinearLayout.LayoutParams(-1, 0, 1f)
        editorParams.setMargins(0, dp(8), 0, dp(8))
        root.addView(editor, editorParams)

        status = TextView(this).apply {
            text = "جاهز"
            setTextColor(red)
            textSize = 12f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
        root.addView(status)

        editor.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int,
                    count: Int, after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?, start: Int,
                    before: Int, count: Int
                ) {
                    dirty = true
                    updateStatus()
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {}
            }
        )

        setContentView(root)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), Color.rgb(55, 55, 62))
        }
    }

    private fun addButton(
        row: LinearLayout,
        text: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            this.text = text
            textSize = 11f
            isAllCaps = false
            setTextColor(white)
            background = rounded(panel, 12)
            setPadding(dp(3), 0, dp(3), 0)
            setOnClickListener { action() }
        }

        val params = LinearLayout.LayoutParams(0, dp(46), 1f)
        params.setMargins(dp(3), dp(6), dp(3), dp(6))
        row.addView(button, params)
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        try {
            startActivityForResult(intent, openCode)
        } catch (e: Exception) {
            toast("تعذر فتح منتقي الملفات")
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK || data?.data == null) return

        val uri = data.data!!

        when (requestCode) {
            openCode -> {
                if (dirty && editor.text.isNotEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle("فتح ملف آخر")
                        .setMessage("توجد تغييرات غير محفوظة. هل تريد المتابعة؟")
                        .setPositiveButton("متابعة") { _, _ ->
                            readFile(uri)
                        }
                        .setNegativeButton("إلغاء", null)
                        .show()
                } else {
                    readFile(uri)
                }
            }

            saveCode -> writeFile(uri)
        }
    }

    private fun readFile(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use {
                    it.readText()
                }
            } ?: ""

            currentUri = uri
            currentName = queryName(uri) ?: "ملف نصي"
            editor.setText(text)
            editor.setSelection(0)
            dirty = false
            fileLabel.text = "📄 $currentName"
            updateStatus()
            toast("تم فتح الملف")
        } catch (e: Exception) {
            toast("تعذر قراءة الملف: ${e.message}")
        }
    }

    private fun queryName(uri: Uri): String? {
        return try {
            contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveFile() {
        val uri = currentUri

        if (uri == null) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, currentName)
            }

            try {
                startActivityForResult(intent, saveCode)
            } catch (e: Exception) {
                toast("تعذر إنشاء الملف")
            }
        } else {
            writeFile(uri)
        }
    }

    private fun writeFile(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.use { output ->
                OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                    writer.write(editor.text.toString())
                    writer.flush()
                }
            } ?: throw Exception("تعذر الوصول إلى الملف")

            currentUri = uri
            currentName = queryName(uri) ?: currentName
            fileLabel.text = "📄 $currentName"
            dirty = false
            updateStatus()
            toast("✅ تم حفظ الملف")
        } catch (e: Exception) {
            toast("فشل الحفظ: ${e.message}")
        }
    }

    private fun searchText() {
        val input = EditText(this).apply {
            hint = "اكتب النص الذي تريد البحث عنه"
            setSingleLine(true)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        AlertDialog.Builder(this)
            .setTitle("🔍 البحث داخل الملف")
            .setView(input)
            .setPositiveButton("بحث") { _, _ ->
                val query = input.text.toString()
                if (query.isBlank()) return@setPositiveButton

                val content = editor.text.toString()
                val index = content.indexOf(
                    query,
                    startIndex = (editor.selectionStart.coerceAtLeast(0))
                        .coerceAtMost(content.length),
                    ignoreCase = true
                ).let {
                    if (it >= 0) it else content.indexOf(
                        query, ignoreCase = true
                    )
                }

                if (index >= 0) {
                    editor.requestFocus()
                    editor.setSelection(index, index + query.length)
                    status.text = "تم العثور على النص عند الموضع $index"
                } else {
                    toast("لم يتم العثور على النص")
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun copyAll() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE)
                as android.content.ClipboardManager

        clipboard.setPrimaryClip(
            android.content.ClipData.newPlainText(
                "نص الملف",
                editor.text.toString()
            )
        )

        toast("تم نسخ النص")
    }

    private fun newFile() {
        if (dirty && editor.text.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("ملف جديد")
                .setMessage("التغييرات الحالية غير محفوظة. هل تريد المتابعة؟")
                .setPositiveButton("متابعة") { _, _ -> resetEditor() }
                .setNegativeButton("إلغاء", null)
                .show()
        } else {
            resetEditor()
        }
    }

    private fun resetEditor() {
        editor.setText("")
        currentUri = null
        currentName = "ملف جديد.txt"
        fileLabel.text = currentName
        dirty = false
        updateStatus()
    }

    private fun updateStatus() {
        val text = editor.text.toString()
        val lines = if (text.isEmpty()) 0 else text.count { it == '\n' } + 1
        status.text =
            "الأسطر: $lines  |  الأحرف: ${text.length}" +
            if (dirty) "  • غير محفوظ" else "  • محفوظ"
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
