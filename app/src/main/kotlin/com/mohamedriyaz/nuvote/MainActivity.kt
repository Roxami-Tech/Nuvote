package com.mohamedriyaz.nuvote

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var searchBox: EditText

    private val prefs by lazy {
        getSharedPreferences("nuvote_storage", Context.MODE_PRIVATE)
    }

    private val notes = mutableListOf<JSONObject>()
    private val deletedNotes = mutableListOf<JSONObject>()
    private val tasks = mutableListOf<JSONObject>()
    private val folders = mutableListOf<String>()

    private var currentPage = "Notes"
    private var darkMode = false
    private var gridMode = true
    private var sortOrder = "Recently updated"
    private var fontSize = 16

    private var bg = Color.rgb(248, 248, 250)
    private var surface = Color.WHITE
    private var foreground = Color.rgb(30, 30, 35)
    private var muted = Color.rgb(130, 130, 140)
    private var accent = Color.rgb(105, 85, 220)
    private var outline = Color.rgb(225, 225, 232)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        darkMode = prefs.getBoolean("dark_mode", false)
        gridMode = prefs.getBoolean("grid_mode", true)
        fontSize = prefs.getInt("font_size", 16)
        sortOrder = prefs.getString("sort_order", "Recently updated")
            ?: "Recently updated"

        updateThemeColors()
        loadData()
        buildApp()
    }

    private fun updateThemeColors() {
        if (darkMode) {
            bg = Color.rgb(18, 18, 22)
            surface = Color.rgb(31, 31, 38)
            foreground = Color.rgb(245, 245, 248)
            muted = Color.rgb(165, 165, 175)
            accent = Color.rgb(160, 140, 255)
            outline = Color.rgb(60, 60, 70)
        } else {
            bg = Color.rgb(248, 248, 250)
            surface = Color.WHITE
            foreground = Color.rgb(30, 30, 35)
            muted = Color.rgb(130, 130, 140)
            accent = Color.rgb(105, 85, 220)
            outline = Color.rgb(225, 225, 232)
        }

        window.statusBarColor = bg
        window.navigationBarColor = bg

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility =
                if (darkMode) 0
                else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                     View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun rounded(
        color: Int,
        radius: Int = 16,
        stroke: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != 0) {
                setStroke(dp(1), stroke)
            }
        }
    }

    private fun makeText(
        value: String,
        size: Float = 16f,
        color: Int = foreground,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    }

    private fun addSpace(
        parent: LinearLayout,
        height: Int = 10
    ) {
        parent.addView(View(this), LinearLayout.LayoutParams(
            1, dp(height)
        ))
    }
        private fun loadData() {
        notes.clear()
        deletedNotes.clear()
        tasks.clear()
        folders.clear()

        try {
            val savedNotes = JSONArray(
                prefs.getString("saved_notes", "[]")
            )
            for (i in 0 until savedNotes.length()) {
                notes.add(savedNotes.getJSONObject(i))
            }

            val savedDeleted = JSONArray(
                prefs.getString("deleted_notes", "[]")
            )
            for (i in 0 until savedDeleted.length()) {
                deletedNotes.add(savedDeleted.getJSONObject(i))
            }

            val savedTasks = JSONArray(
                prefs.getString("saved_tasks", "[]")
            )
            for (i in 0 until savedTasks.length()) {
                tasks.add(savedTasks.getJSONObject(i))
            }

            val savedFolders = JSONArray(
                prefs.getString("saved_folders", "[]")
            )
            for (i in 0 until savedFolders.length()) {
                folders.add(savedFolders.getString(i))
            }
        } catch (_: Exception) {
        }
    }

    private fun saveData() {
        val noteArray = JSONArray()
        notes.forEach { noteArray.put(it) }

        val deletedArray = JSONArray()
        deletedNotes.forEach { deletedArray.put(it) }

        val taskArray = JSONArray()
        tasks.forEach { taskArray.put(it) }

        val folderArray = JSONArray()
        folders.forEach { folderArray.put(it) }

        prefs.edit()
            .putString("saved_notes", noteArray.toString())
            .putString("deleted_notes", deletedArray.toString())
            .putString("saved_tasks", taskArray.toString())
            .putString("saved_folders", folderArray.toString())
            .putBoolean("dark_mode", darkMode)
            .putBoolean("grid_mode", gridMode)
            .putInt("font_size", fontSize)
            .putString("sort_order", sortOrder)
            .apply()
    }

    private fun buildApp() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        setContentView(root)
        showPage(currentPage)
    }

    private fun showPage(page: String) {
        currentPage = page
        root.removeAllViews()
        root.setBackgroundColor(bg)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(8))
        }

        val titleRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }

        val title = makeText(page, 26f, foreground, true)
        titleRow.addView(title, LinearLayout.LayoutParams(
            0, -2, 1f
        ))

        if (page == "Notes") {
            val layoutButton = makeText(
                if (gridMode) "☷" else "▦",
                25f, accent, true
            )
            layoutButton.setPadding(dp(12), dp(4), dp(12), dp(4))
            layoutButton.setOnClickListener {
                gridMode = !gridMode
                saveData()
                showPage("Notes")
            }
            titleRow.addView(layoutButton)
        }

        header.addView(titleRow)

        if (page == "Notes") {
            addSpace(header, 12)

            searchBox = EditText(this).apply {
                hint = "Search notes..."
                textSize = 15f
                setSingleLine(false)
                setTextColor(this@MainActivity.foreground)
                setHintTextColor(muted)
                background = rounded(surface, 14, outline)
                setPadding(dp(15), 0, dp(15), 0)
            }

            header.addView(searchBox, LinearLayout.LayoutParams(
                -1, dp(48)
            ))

            searchBox.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) {
                    refreshNotes(s?.toString() ?: "")
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

        root.addView(header)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }

        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        scroll.addView(content)

        root.addView(scroll, LinearLayout.LayoutParams(
            -1, 0, 1f
        ))

        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(surface)
            setPadding(dp(5), dp(9), dp(5), dp(9))
        }

        root.addView(bottomBar, LinearLayout.LayoutParams(
            -1, dp(66)
        ))

        buildBottomNavigation()

        when (page) {
            "Notes" -> refreshNotes()
            "Tasks" -> refreshTasks()
            "Folders" -> showFolders()
            "Settings" -> showSettings()
            "Trash" -> showTrash()
        }
    }

    private fun buildBottomNavigation() {
        val items = listOf("Notes", "Tasks", "+", "Folders", "Settings")

        items.forEach { item ->
            val label = when (item) {
                "+" -> "＋"
                else -> item
            }

            val button = TextView(this).apply {
                text = label
                textSize = if (item == "+") 27f else 12f
                gravity = Gravity.CENTER
                setTextColor(
                    if (item == currentPage || item == "+") accent
                    else muted
                )
                setTypeface(
                    null,
                    if (item == currentPage || item == "+")
                        Typeface.BOLD else Typeface.NORMAL
                )
                setOnClickListener {
                    if (item == "+") {
                        openNoteEditor()
                    } else {
                        showPage(item)
                    }
                }
            }

            bottomBar.addView(button, LinearLayout.LayoutParams(
                0, -1, 1f
            ))
        }
    }
        private fun refreshNotes(query: String = "") {
        if (!::content.isInitialized) return

        content.removeAllViews()

        val matching = notes.filter {
            val title = it.optString("title")
            val body = it.optString("content")

            title.contains(query, ignoreCase = true) ||
                body.contains(query, ignoreCase = true)
        }

        // Apply selected sorting and keep pinned notes at the top.
        val filtered = sortNotes(matching).sortedByDescending {
            it.optBoolean("pinned", false)
        }

        if (filtered.isEmpty()) {
            val empty = makeText(
                if (query.isEmpty()) {
                    "No notes yet.\nTap + to create your first note."
                } else {
                    "No matching notes found."
                },
                15f,
                muted
            )

            empty.gravity = Gravity.CENTER
            empty.setPadding(0, dp(70), 0, dp(70))
            content.addView(empty)
            return
        }

        if (gridMode) {
            var index = 0

            while (index < filtered.size) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                for (column in 0..1) {
                    if (index >= filtered.size) break

                    val note = filtered[index]

                    row.addView(
                        createNoteCard(note),
                        LinearLayout.LayoutParams(
                            0,
                            dp(175),
                            1f
                        ).apply {
                            if (column == 0) {
                                marginEnd = dp(8)
                            } else {
                                marginStart = dp(8)
                            }
                        }
                    )

                    index++
                }

                content.addView(row)
                addSpace(content, 12)
            }
        } else {
            filtered.forEach { note ->
                content.addView(createNoteCard(note, true))
                addSpace(content, 10)
            }
        }
    }

    private fun createNoteCard(
        note: JSONObject,
        fullWidth: Boolean = false
    ): LinearLayout {

        val color = note.optInt("color", Color.WHITE)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
            )

            background = rounded(color, 18, outline)

            isClickable = true
            isFocusable = true

            setOnClickListener {
                openNoteEditor(note)
            }
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = makeText(
            note.optString("title", "Untitled"),
            16f,
            readableText(color),
            true
        )

        titleRow.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        if (note.optBoolean("pinned", false)) {
            titleRow.addView(
                makeText(
                    "📌",
                    14f,
                    readableText(color)
                )
            )
        }

        card.addView(titleRow)

        addSpace(card, 8)

        val body = makeText(
            note.optString("content"),
            13f,
            readableText(color)
        )

        body.maxLines = if (fullWidth) 5 else 6
        card.addView(body)

        addSpace(card, 8)

        val options = makeText(
            "•••",
            18f,
            readableText(color),
            true
        )

        options.gravity = Gravity.END

        options.setOnClickListener {
            showNoteOptions(note)
        }

        card.addView(options)

        return card
    }

    private fun readableText(backgroundColor: Int): Int {
        val darkness = (
            Color.red(backgroundColor) * 299 +
            Color.green(backgroundColor) * 587 +
            Color.blue(backgroundColor) * 114
        ) / 1000

        return if (darkness < 140) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    private fun openNoteEditor(existing: JSONObject? = null) {

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        var selectedColor = existing?.optInt(
            "color",
            Color.WHITE
        ) ?: Color.WHITE

        var selectedFolder = existing?.optString(
            "folder",
            ""
        ) ?: ""

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(20),
                dp(18),
                dp(20),
                dp(18)
            )

            background = rounded(surface, 22)
        }

        val titleInput = EditText(this).apply {
            hint = "Title"
            textSize = 21f
            setTextColor(this@MainActivity.foreground)
            setHintTextColor(muted)
            setSingleLine(false)

            setText(
                existing?.optString("title", "") ?: ""
            )

            background = null
        }

        val bodyInput = EditText(this).apply {
            hint = "Start writing..."
            textSize = fontSize.toFloat()
            setTextColor(this@MainActivity.foreground)
            setHintTextColor(muted)
            gravity = Gravity.TOP
            minLines = 8

            setText(
                existing?.optString("content", "") ?: ""
            )

            background = null
        }

        layout.addView(titleInput)

        addSpace(layout, 10)

        layout.addView(
            bodyInput,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        addSpace(layout, 12)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val colorButton = makeActionText("Color")
        val folderButton = makeActionText("Folder")
        val saveButton = makeActionText("Save")

        colorButton.setOnClickListener {
            chooseNoteColor {
                selectedColor = it
            }
        }

        folderButton.setOnClickListener {
            chooseFolder(selectedFolder) {
                selectedFolder = it
            }
        }

        saveButton.setOnClickListener {

            val titleText = titleInput.text.toString().trim()
            val bodyText = bodyInput.text.toString().trim()

            if (titleText.isEmpty() && bodyText.isEmpty()) {
                Toast.makeText(
                    this,
                    "Write something first",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val note = existing ?: JSONObject()

            note.put(
                "title",
                if (titleText.isEmpty()) {
                    "Untitled"
                } else {
                    titleText
                }
            )

            note.put("content", bodyText)
            note.put("color", selectedColor)
            note.put("folder", selectedFolder)
            note.put("updatedAt", System.currentTimeMillis())

            if (existing == null) {
                note.put("pinned", false)
                notes.add(0, note)
            }

            saveData()

            dialog.dismiss()
            showPage("Notes")
        }

        actions.addView(colorButton)
        actions.addView(folderButton)
        actions.addView(saveButton)

        layout.addView(actions)

        dialog.setContentView(layout)

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(
                android.R.color.transparent
            )

            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                dp(520)
            )
        }

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            dp(520)
        )
    }

    private fun makeActionText(value: String): TextView {
        return makeText(
            value,
            14f,
            accent,
            true
        ).apply {
            setPadding(
                dp(10),
                dp(10),
                dp(10),
                dp(10)
            )

            isClickable = true
        }
    }
        private fun chooseNoteColor(onSelected: (Int) -> Unit) {
        val colors = intArrayOf(
            Color.WHITE,
            Color.rgb(255, 236, 179),
            Color.rgb(255, 205, 210),
            Color.rgb(225, 245, 254),
            Color.rgb(220, 237, 200),
            Color.rgb(232, 234, 246),
            Color.rgb(248, 187, 208),
            Color.rgb(215, 204, 200)
        )

        val names = arrayOf(
            "White", "Yellow", "Pink", "Blue",
            "Green", "Lavender", "Rose", "Brown"
        )

        AlertDialog.Builder(this)
            .setTitle("Choose note color")
            .setItems(names) { _, which ->
                onSelected(colors[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun chooseFolder(
        current: String,
        onSelected: (String) -> Unit
    ) {
        val choices = mutableListOf("No folder")
        choices.addAll(folders)

        AlertDialog.Builder(this)
            .setTitle("Move to folder")
            .setSingleChoiceItems(
                choices.toTypedArray(),
                choices.indexOf(current).coerceAtLeast(0)
            ) { dialog, which ->
                onSelected(
                    if (which == 0) "" else choices[which]
                )
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNoteOptions(note: JSONObject) {
        val options = arrayOf(
            if (note.optBoolean("pinned", false))
                "Unpin" else "Pin",
            "Move to folder",
            "Share",
            "Delete"
        )

        AlertDialog.Builder(this)
            .setTitle("Note options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        note.put(
                            "pinned",
                            !note.optBoolean("pinned", false)
                        )
                        note.put("updatedAt", System.currentTimeMillis())
                        saveData()
                        showPage("Notes")
                    }

                    1 -> {
                        chooseFolder(note.optString("folder", "")) {
                            note.put("folder", it)
                            saveData()
                            showPage("Notes")
                        }
                    }

                    2 -> shareNote(note)

                    3 -> confirmDeleteNote(note)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareNote(note: JSONObject) {
        val text = buildString {
            append(note.optString("title"))
            append("\n\n")
            append(note.optString("content"))
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        startActivity(Intent.createChooser(intent, "Share note"))
    }

    private fun confirmDeleteNote(note: JSONObject) {
        AlertDialog.Builder(this)
            .setTitle("Move to Recently Deleted?")
            .setMessage("You can restore this note later from Trash.")
            .setPositiveButton("Delete") { _, _ ->
                notes.remove(note)
                deletedNotes.add(note)
                saveData()
                showPage("Notes")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFolders() {
        content.removeAllViews()

        val create = makeText(
            "＋  Create new folder",
            16f, accent, true
        ).apply {
            setPadding(dp(14), dp(16), dp(14), dp(16))
            background = rounded(surface, 14, outline)
            setOnClickListener {
                createFolder()
            }
        }

        content.addView(create)
        addSpace(content, 14)

        if (folders.isEmpty()) {
            content.addView(makeText(
                "No folders created yet.",
                14f, muted
            ))
            return
        }

        folders.forEach { folder ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(surface, 14, outline)
                setPadding(dp(14), dp(14), dp(14), dp(14))
            }

            val name = makeText(
                "📁  $folder",
                16f, foreground, true
            )
            row.addView(name, LinearLayout.LayoutParams(
                0, -2, 1f
            ))

            val open = makeText("Open", 14f, accent, true)
            open.setOnClickListener {
                showFolderNotes(folder)
            }

            val more = makeText("•••", 18f, muted)
            more.setPadding(dp(12), 0, 0, 0)
            more.setOnClickListener {
                showFolderOptions(folder)
            }

            row.addView(open)
            row.addView(more)

            content.addView(row)
            addSpace(content, 10)
        }
    }

    private fun createFolder() {
        val input = EditText(this).apply {
            hint = "Folder name"
            setTextColor(this@MainActivity.foreground)
        }

        AlertDialog.Builder(this)
            .setTitle("New folder")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()

                if (name.isNotEmpty() && !folders.contains(name)) {
                    folders.add(name)
                    saveData()
                    showPage("Folders")
                } else {
                    Toast.makeText(
                        this, "Enter a unique folder name",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFolderOptions(folder: String) {
        val options = arrayOf("Rename", "Delete")

        AlertDialog.Builder(this)
            .setTitle(folder)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> renameFolder(folder)
                    1 -> deleteFolder(folder)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
        private fun renameFolder(oldName: String) {
        val input = EditText(this).apply {
            setText(oldName)
            setTextColor(this@MainActivity.foreground)
        }

        AlertDialog.Builder(this)
            .setTitle("Rename folder")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()

                if (newName.isNotEmpty() &&
                    (newName == oldName || !folders.contains(newName))
                ) {
                    val index = folders.indexOf(oldName)
                    if (index >= 0) folders[index] = newName

                    notes.forEach {
                        if (it.optString("folder") == oldName) {
                            it.put("folder", newName)
                        }
                    }

                    saveData()
                    showPage("Folders")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFolder(folder: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete folder?")
            .setMessage("Notes inside this folder will remain in Notes.")
            .setPositiveButton("Delete") { _, _ ->
                folders.remove(folder)

                notes.forEach {
                    if (it.optString("folder") == folder) {
                        it.put("folder", "")
                    }
                }

                saveData()
                showPage("Folders")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFolderNotes(folder: String) {
        content.removeAllViews()

        val back = makeText("←  Back to folders", 15f, accent, true)
        back.setPadding(0, 0, 0, dp(15))
        back.setOnClickListener {
            showPage("Folders")
        }
        content.addView(back)

        val folderNotes = notes.filter {
            it.optString("folder") == folder
        }

        if (folderNotes.isEmpty()) {
            content.addView(makeText(
                "This folder is empty.",
                14f, muted
            ))
        } else {
            folderNotes.forEach {
                content.addView(createNoteCard(it, true))
                addSpace(content, 10)
            }
        }
    }

    private fun refreshTasks() {
        content.removeAllViews()

        val add = makeText(
            "＋  Add task",
            16f, accent, true
        ).apply {
            setPadding(dp(14), dp(16), dp(14), dp(16))
            background = rounded(surface, 14, outline)
            setOnClickListener {
                openTaskEditor()
            }
        }

        content.addView(add)
        addSpace(content, 14)

        if (tasks.isEmpty()) {
            content.addView(makeText(
                "No tasks yet.",
                14f, muted
            ))
            return
        }

        tasks.forEach { task ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(surface, 14, outline)
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }

            val check = CheckBox(this).apply {
                isChecked = task.optBoolean("done", false)
                buttonTintList = android.content.res.ColorStateList.valueOf(
                    accent
                )
                setOnCheckedChangeListener { _, checked ->
                    task.put("done", checked)
                    saveData()
                    refreshTasks()
                }
            }

            row.addView(check)

            val title = makeText(
                task.optString("title"),
                15f, foreground
            )
            if (task.optBoolean("done", false)) {
                title.paintFlags =
                    title.paintFlags or
                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                title.setTextColor(muted)
            }

            row.addView(title, LinearLayout.LayoutParams(
                0, -2, 1f
            ))

            val more = makeText("•••", 18f, muted)
            more.setPadding(dp(10), dp(5), dp(5), dp(5))
            more.setOnClickListener {
                showTaskOptions(task)
            }

            row.addView(more)
            content.addView(row)
            addSpace(content, 9)
        }
    }

    private fun openTaskEditor(existing: JSONObject? = null) {
        val input = EditText(this).apply {
            hint = "Task name"
            setText(existing?.optString("title", "") ?: "")
            setTextColor(this@MainActivity.foreground)
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "New task" else "Edit task")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().trim()

                if (value.isNotEmpty()) {
                    val task = existing ?: JSONObject()

                    task.put("title", value)

                    if (existing == null) {
                        task.put("done", false)
                        tasks.add(task)
                    }

                    saveData()
                    refreshTasks()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTaskOptions(task: JSONObject) {
        val options = arrayOf("Edit", "Delete")

        AlertDialog.Builder(this)
            .setTitle("Task options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openTaskEditor(task)

                    1 -> {
                        AlertDialog.Builder(this)
                            .setTitle("Delete task?")
                            .setPositiveButton("Delete") { _, _ ->
                                tasks.remove(task)
                                saveData()
                                refreshTasks()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
        private fun showSettings() {
        content.removeAllViews()

        val themeRow = settingRow(
            "Dark mode",
            if (darkMode) "On" else "Off"
        ) {
            darkMode = !darkMode
            saveData()
            updateThemeColors()
            showPage("Settings")
        }

        content.addView(themeRow)
        addSpace(content, 10)

        content.addView(settingRow(
            "Notes layout",
            if (gridMode) "Grid" else "List"
        ) {
            gridMode = !gridMode
            saveData()
            showPage("Settings")
        })

        addSpace(content, 10)

        content.addView(settingRow(
            "Font size",
            "$fontSize"
        ) {
            val sizes = arrayOf("Small", "Medium", "Large")
            val values = intArrayOf(14, 16, 20)

            AlertDialog.Builder(this)
                .setTitle("Choose font size")
                .setItems(sizes) { _, which ->
                    fontSize = values[which]
                    saveData()
                    showPage("Settings")
                }
                .show()
        })

        addSpace(content, 10)

        content.addView(settingRow(
            "Sort notes",
            sortOrder
        ) {
            val choices = arrayOf(
                "Recently updated",
                "Oldest first",
                "Alphabetical"
            )

            AlertDialog.Builder(this)
                .setTitle("Sort notes")
                .setItems(choices) { _, which ->
                    sortOrder = choices[which]
                    saveData()
                    showPage("Notes")
                }
                .show()
        })

        addSpace(content, 10)

        content.addView(settingRow(
            "Recently Deleted",
            "${deletedNotes.size} items"
        ) {
            showPage("Trash")
        })

        addSpace(content, 10)

        content.addView(settingRow(
            "About Nuvote",
            "Version 1.0"
        ) {
            showAbout()
        })
    }

    private fun settingRow(
        title: String,
        subtitle: String,
        action: () -> Unit
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = rounded(surface, 14, outline)
            setOnClickListener { action() }
        }

        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        left.addView(makeText(title, 15f, foreground, true))
        addSpace(left, 4)
        left.addView(makeText(subtitle, 12f, muted))

        row.addView(left, LinearLayout.LayoutParams(
            0, -2, 1f
        ))

        row.addView(makeText("›", 25f, muted))

        return row
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("Nuvote")
            .setMessage(
                "Nuvote – Notes and Tasks\n\n" +
                "A simple place to organize your thoughts, " +
                "notes, folders and daily tasks.\n\n" +
                "Version 1.0"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showTrash() {
        content.removeAllViews()

        val back = makeText("←  Back to Settings", 15f, accent, true)
        back.setPadding(0, 0, 0, dp(15))
        back.setOnClickListener {
            showPage("Settings")
        }
        content.addView(back)

        if (deletedNotes.isEmpty()) {
            content.addView(makeText(
                "Recently Deleted is empty.",
                14f, muted
            ))
            return
        }

        deletedNotes.toList().forEach { note ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(surface, 14, outline)
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }

            row.addView(makeText(
                note.optString("title", "Untitled"),
                16f, foreground, true
            ))

            addSpace(row, 5)

            row.addView(makeText(
                note.optString("content"),
                13f, muted
            ))

            addSpace(row, 10)

            val buttons = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }

            val restore = makeActionText("Restore")
            restore.setOnClickListener {
                deletedNotes.remove(note)
                notes.add(note)
                saveData()
                showPage("Trash")
            }

            val delete = makeActionText("Delete forever")
            delete.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete permanently?")
                    .setMessage("This note cannot be recovered.")
                    .setPositiveButton("Delete") { _, _ ->
                        deletedNotes.remove(note)
                        saveData()
                        showPage("Trash")
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            buttons.addView(restore)
            buttons.addView(delete)

            row.addView(buttons)
            content.addView(row)
            addSpace(content, 10)
        }

        addSpace(content, 15)

        val emptyTrash = makeText(
            "Empty Recently Deleted",
            14f, Color.RED, true
        )
        emptyTrash.setPadding(dp(12), dp(12), dp(12), dp(12))
        emptyTrash.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Empty Recently Deleted?")
                .setMessage("All deleted notes will be permanently removed.")
                .setPositiveButton("Empty") { _, _ ->
                    deletedNotes.clear()
                    saveData()
                    showPage("Trash")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        content.addView(emptyTrash)
    }
        private fun sortNotes(list: List<JSONObject>): List<JSONObject> {
        return when (sortOrder) {
            "Oldest first" -> list.sortedBy {
                it.optLong("updatedAt", 0L)
            }

            "Alphabetical" -> list.sortedBy {
                it.optString("title").lowercase()
            }

            else -> list.sortedByDescending {
                it.optLong("updatedAt", 0L)
            }
        }
    }

    override fun onBackPressed() {
        when (currentPage) {
            "Trash" -> showPage("Settings")
            "Folders" -> showPage("Notes")
            "Tasks" -> showPage("Notes")
            "Settings" -> showPage("Notes")
            else -> super.onBackPressed()
        }
    }
}