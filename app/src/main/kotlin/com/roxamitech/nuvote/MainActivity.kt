
package com.roxamitech.nuvote

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.MotionEvent
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
    private val folders = mutableListOf<JSONObject>()

    private var currentPage = "Notes"
    private var darkMode = true
    private var gridMode = true
    private var sortOrder = "Recently updated"
    private var fontSize = 16

    private var bg = Color.rgb(17, 18, 37)
    private var surface = Color.rgb(29, 30, 54)
    private var surface2 = Color.rgb(37, 38, 65)
    private var foreground = Color.rgb(246, 243, 255)
    private var muted = Color.rgb(165, 162, 190)
    private var accent = Color.rgb(177, 125, 255)
    private var outline = Color.rgb(57, 57, 86)

    private val categoryColors = mapOf(
        "Work" to Color.rgb(174, 133, 255),
        "Personal" to Color.rgb(91, 207, 183),
        "Study" to Color.rgb(246, 211, 112),
        "Ideas" to Color.rgb(239, 145, 198),
        "Finance" to Color.rgb(111, 166, 245),
        "Health" to Color.rgb(242, 139, 157),
        "Projects" to Color.rgb(242, 166, 111),
        "Other" to Color.rgb(169, 169, 190)
    )

    private val categoryIcons = mapOf(
        "Work" to "▣",
        "Personal" to "●",
        "Study" to "▤",
        "Ideas" to "✦",
        "Finance" to "◇",
        "Health" to "♥",
        "Projects" to "▰",
        "Other" to "▦"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        darkMode = prefs.getBoolean("dark_mode", true)
        gridMode = prefs.getBoolean("grid_mode", true)
        fontSize = prefs.getInt("font_size", 16)
        sortOrder = prefs.getString(
            "sort_order",
            "Recently updated"
        ) ?: "Recently updated"

        updateThemeColors()
        loadData()
        buildApp()
    }

    private fun updateThemeColors() {
        if (darkMode) {
            bg = Color.rgb(17, 18, 37)
            surface = Color.rgb(29, 30, 54)
            surface2 = Color.rgb(37, 38, 65)
            foreground = Color.rgb(246, 243, 255)
            muted = Color.rgb(165, 162, 190)
            accent = Color.rgb(177, 125, 255)
            outline = Color.rgb(57, 57, 86)
        } else {
            bg = Color.rgb(247, 245, 252)
            surface = Color.WHITE
            surface2 = Color.rgb(239, 235, 249)
            foreground = Color.rgb(36, 32, 52)
            muted = Color.rgb(125, 119, 145)
            accent = Color.rgb(119, 83, 211)
            outline = Color.rgb(225, 220, 238)
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
        radius: Int = 18,
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
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun horizontalSpace(
        parent: LinearLayout,
        width: Int = 10
    ) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(dp(width), 1)
        )
    }

    private fun makeCard(
        color: Int = surface,
        radius: Int = 18
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(color, radius, outline)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
    }

    private fun makeActionText(value: String): TextView {
        return makeText(value, 14f, accent, true).apply {
            setPadding(dp(12), dp(10), dp(12), dp(10))
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    // ==========================================
    // DATA LOADING AND SAVING
    // ==========================================

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

            // Supports both old string folders and new objects.
            val savedFolders = JSONArray(
                prefs.getString("saved_folders", "[]")
            )

            for (i in 0 until savedFolders.length()) {
                val item = savedFolders.get(i)

                if (item is JSONObject) {
                    folders.add(item)
                } else {
                    val oldName = item.toString()
                    val category = if (
                        categoryColors.containsKey(oldName)
                    ) oldName else "Other"

                    folders.add(
                        JSONObject().apply {
                            put("name", oldName)
                            put("category", category)
                            put(
                                "color",
                                categoryColors[category] ?: accent
                            )
                        }
                    )
                }
            }
        } catch (e: Exception) {
            showToast("Some saved data could not be loaded.")
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

    // ==========================================
    // MAIN APP
    // ==========================================

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
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = makeText(page, 25f, foreground, true)
        titleRow.addView(
            title,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        
        if (page == "Notes") {
            val sortButton = createSortButton()
            titleRow.addView(sortButton)
        
            val layoutButton = makeText(
                if (gridMode) "▤" else "▦",
                23f,
                accent,
                true
            )
        
            layoutButton.setPadding(
                dp(12), dp(4), dp(4), dp(4)
            )
        
            layoutButton.setOnClickListener {
                gridMode = !gridMode
                saveData()
                showPage("Notes")
            }
        
            titleRow.addView(layoutButton)
        }


        header.addView(titleRow)

        if (page == "Notes") {
            addSpace(header, 13)

            val searchRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            searchBox = EditText(this).apply {
                hint = "⌕  Search notes..."
                textSize = 15f
                setSingleLine(true)
                setTextColor(this@MainActivity.foreground)
                setHintTextColor(muted)
                background = rounded(surface2, 24)
                setPadding(dp(16), 0, dp(16), 0)
            }

            searchRow.addView(
                searchBox,
                LinearLayout.LayoutParams(0, dp(48), 1f)
            )


            header.addView(searchRow)

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
            setPadding(dp(16), dp(8), dp(16), dp(18))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(content)
        }

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(surface)
            setPadding(dp(5), dp(7), dp(5), dp(7))
            elevation = dp(12).toFloat()
        }

        root.addView(
            bottomBar,
            LinearLayout.LayoutParams(-1, dp(72))
        )

        buildBottomNavigation()

        when (page) {
            "Notes" -> refreshNotes()
            "Tasks" -> refreshTasks()
            "Folders" -> showFolders()
            "Settings" -> showSettings()
            "Trash" -> showTrash()
        }
    }

    // ==========================================
    // BOTTOM NAVIGATION
    // ==========================================

    
    
    private fun buildBottomNavigation() {
        bottomBar.removeAllViews()
    
        val items = listOf(
            Triple("Notes", R.drawable.ic_notes, "Notes"),
            Triple("Tasks", R.drawable.ic_tasks, "Tasks"),
            Triple("+", R.drawable.ic_add, "Create"),
            Triple("Folders", R.drawable.ic_folder, "Folders"),
            Triple("Settings", R.drawable.ic_settings, "Settings")
        )
    
        items.forEach { item ->
            val selected = item.first == currentPage
            val isCreate = item.first == "+"
    
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                contentDescription = item.third
            }
    
            if (isCreate) {
                val addButton = android.widget.ImageView(this).apply {
                    setImageResource(R.drawable.ic_add)
                    setColorFilter(Color.WHITE)
                    scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                    setPadding(dp(13), dp(13), dp(13), dp(13))
                    background = rounded(accent, 50)
                }
    
                container.addView(
                    addButton,
                    LinearLayout.LayoutParams(dp(52), dp(52))
                )
            } else {
                val icon = android.widget.ImageView(this).apply {
                    setImageResource(item.second)
                    setColorFilter(
                        if (selected) accent else muted
                    )
                    scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                }
    
                container.addView(
                    icon,
                    LinearLayout.LayoutParams(dp(25), dp(25))
                )
    
                addSpace(container, 4)
    
                val label = makeText(
                    item.third,
                    10f,
                    if (selected) accent else muted,
                    selected
                )
    
                label.gravity = Gravity.CENTER
    
                container.addView(label)
            }
    
            container.setOnClickListener {
                if (isCreate) {
                    showCreateOptions()
                } else {
                    showPage(item.first)
                }
            }
    
            bottomBar.addView(
                container,
                LinearLayout.LayoutParams(0, -1, 1f)
            )
        }
    }



        private fun showCreateOptions() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = rounded(surface, 24, outline)
        }

        layout.addView(makeText("Create new", 20f, foreground, true))
        addSpace(layout, 16)

        listOf(
            "▤   New note",
            "☑   New task",
            "▱   New folder"
        ).forEachIndexed { index, label ->
            val option = makeText(label, 16f, foreground, true).apply {
                background = rounded(surface2, 14)
                setPadding(dp(16), dp(17), dp(16), dp(17))
                setOnClickListener {
                    dialog.dismiss()
                    when (index) {
                        0 -> openNoteEditor()
                        1 -> openTaskEditor()
                        2 -> createFolder()
                    }
                }
            }

            layout.addView(option)
            addSpace(layout, 9)
        }

        dialog.setContentView(layout)
        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.90f).toInt(),
                -2
            )
            setGravity(Gravity.BOTTOM)
            attributes = attributes.apply {
                windowAnimations = android.R.style.Animation_Dialog
            }
        }
    }
    
        // ==========================================
    // NOTES
    // ==========================================



    private fun refreshNotes(query: String = "") {
        if (!::content.isInitialized) return
    
        content.removeAllViews()
    
        val matching = notes.filter {
            it.optString("title").contains(query, true) ||
            it.optString("content").contains(query, true)
        }
    
        val filtered = sortNotes(matching).sortedByDescending {
            it.optBoolean("pinned", false)
        }
    
        if (filtered.isEmpty()) {
            val empty = makeText(
                if (query.isEmpty())
                    "No notes yet.\nTap + to create your first note."
                else
                    "No matching notes found.",
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
    
                    row.addView(
                        createNoteCard(filtered[index], false),
                        LinearLayout.LayoutParams(
                            0,
                            dp(175),
                            1f
                        ).apply {
                            if (column == 0) marginEnd = dp(7)
                            else marginStart = dp(7)
                        }
                    )
    
                    index++
                }
    
                content.addView(row)
                addSpace(content, 12)
            }
        } else {
            filtered.forEach { note ->
                // Wrap the note in a fixed-height card.
                content.addView(
                    createNoteCard(note, true),
                    LinearLayout.LayoutParams(
                        -1,
                        dp(145)
                    )
                )
                addSpace(content, 10)
            }
        }
    }


    private fun createNoteCard(
        note: JSONObject,
        fullWidth: Boolean = false
    ): LinearLayout {
        val color = note.optInt("color", Color.rgb(239, 231, 255))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(12))
            background = rounded(color, 20)
            isClickable = true
            isFocusable = true
            setOnClickListener { openNoteEditor(note) }
        }

        val textColor = readableText(color)

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = makeText(
            note.optString("title", "Untitled"),
            16f,
            textColor,
            true
        )

        title.maxLines = 2
        titleRow.addView(title, LinearLayout.LayoutParams(0, -2, 1f))

        if (note.optBoolean("pinned", false)) {
            titleRow.addView(makeText("📌", 13f, textColor))
        }

        card.addView(titleRow)
        addSpace(card, 10)

        val body = makeText(
            note.optString("content"),
            if (fullWidth) fontSize.toFloat() else 13f,
            textColor
        )

        body.maxLines = if (fullWidth) 6 else 5
        card.addView(body)

        val spacer = View(this)
        card.addView(
            spacer,
            LinearLayout.LayoutParams(1, 0, 1f)
        )

        addSpace(card, 8)

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val date = makeText(
            if (note.has("updatedAt")) "Updated" else "",
            10f,
            textColor
        )
        date.alpha = 0.65f

        footer.addView(date, LinearLayout.LayoutParams(0, -2, 1f))

        val more = makeText("⋮", 21f, textColor, true)
        more.gravity = Gravity.CENTER
        more.setOnClickListener { showNoteOptions(note) }
        footer.addView(more)

        card.addView(footer)
        return card
    }

    private fun readableText(color: Int): Int {
        val darkness = (
            Color.red(color) * 299 +
                Color.green(color) * 587 +
                Color.blue(color) * 114
            ) / 1000

        return if (darkness < 145) Color.WHITE else Color.rgb(30, 28, 40)
    }

    // ==========================================
    // NOTE EDITOR
    // ==========================================

    private fun openNoteEditor(existing: JSONObject? = null) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        var selectedColor = existing?.optInt(
            "color",
            Color.rgb(239, 231, 255)
        ) ?: Color.rgb(239, 231, 255)

        var selectedFolder = existing?.optString("folder", "") ?: ""

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = rounded(surface, 24, outline)
        }

        layout.addView(
            makeText(
                if (existing == null) "New note" else "Edit note",
                21f,
                foreground,
                true
            )
        )

        addSpace(layout, 12)

        val titleInput = EditText(this).apply {
            hint = "Title"
            textSize = 21f
            setTextColor(this@MainActivity.foreground)
            setHintTextColor(muted)
            setSingleLine(false)
            setText(existing?.optString("title", "") ?: "")
            background = null
        }

        layout.addView(titleInput)

        addSpace(layout, 8)

        val bodyInput = EditText(this).apply {
            hint = "Start writing..."
            textSize = fontSize.toFloat()
            setTextColor(this@MainActivity.foreground)
            setHintTextColor(muted)
            gravity = Gravity.TOP
            minLines = 8
            setText(existing?.optString("content", "") ?: "")
            background = null
        }

        layout.addView(
            bodyInput,
            LinearLayout.LayoutParams(-1, 0, 1f)
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
            chooseNoteColor { selectedColor = it }
        }

        folderButton.setOnClickListener {
            chooseFolder(selectedFolder) { selectedFolder = it }
        }

        saveButton.setOnClickListener {
            val titleText = titleInput.text.toString().trim()
            val bodyText = bodyInput.text.toString().trim()

            if (titleText.isEmpty() && bodyText.isEmpty()) {
                showToast("Write something first")
                return@setOnClickListener
            }

            val note = existing ?: JSONObject()

            note.put("title", titleText.ifEmpty { "Untitled" })
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
        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.94f).toInt(),
                dp(520)
            )
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    private fun chooseNoteColor(onSelected: (Int) -> Unit) {
        val colors = listOf(
            Color.rgb(255, 255, 255),
            Color.rgb(255, 236, 179),
            Color.rgb(255, 205, 210),
            Color.rgb(225, 245, 254),
            Color.rgb(220, 237, 200),
            Color.rgb(232, 234, 246),
            Color.rgb(248, 187, 208),
            Color.rgb(215, 204, 200)
        )

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = rounded(surface, 22, outline)
        }

        layout.addView(makeText("Note color", 20f, foreground, true))
        addSpace(layout, 14)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        colors.forEach { color ->
            val swatch = View(this).apply {
                background = rounded(color, 50, outline)
                setOnClickListener {
                    onSelected(color)
                    dialog.dismiss()
                }
            }

            row.addView(
                swatch,
                LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                    marginEnd = dp(5)
                    marginStart = dp(5)
                }
            )
        }

        layout.addView(row)
        addSpace(layout, 14)
        layout.addView(
            popupButton("Close") { dialog.dismiss() }
        )

        dialog.setContentView(layout)
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.90f).toInt(),
                -2
            )
        }
    }
    
        // ==========================================
    // CUSTOM POPUP HELPERS
    // ==========================================

    private fun createPopup(title: String): Pair<Dialog, LinearLayout> {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = rounded(surface, 24, outline)
        }

        layout.addView(makeText(title, 20f, foreground, true))

        dialog.setContentView(layout)
        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.90f).toInt(),
                -2
            )
            setGravity(Gravity.CENTER)
        }

        return Pair(dialog, layout)
    }

    private fun popupButton(
        label: String,
        primary: Boolean = false,
        action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = label
            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (primary) Color.WHITE else accent)
            background = rounded(
                if (primary) accent else surface2,
                12
            )
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnClickListener { action() }
        }
    }

    private fun addPopupButtons(
        layout: LinearLayout,
        cancel: () -> Unit,
        save: () -> Unit,
        saveLabel: String = "Save"
    ) {
        addSpace(layout, 16)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        row.addView(popupButton("Cancel", false, cancel))

        row.addView(
            popupButton(saveLabel, true, save),
            LinearLayout.LayoutParams(-2, -2).apply {
                marginStart = dp(10)
            }
        )

        layout.addView(row)
    }

    private fun showCustomInput(
        title: String,
        hint: String,
        initialValue: String = "",
        saveLabel: String = "Save",
        onSave: (String) -> Unit
    ) {
        val (dialog, layout) = createPopup(title)

        addSpace(layout, 14)

        val input = EditText(this).apply {
            this.hint = hint
            setText(initialValue)
            textSize = 16f
            setTextColor(this@MainActivity.foreground)
            setHintTextColor(muted)
            background = rounded(surface2, 12, outline)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setSingleLine(true)
            selectAll()
        }

        layout.addView(input, LinearLayout.LayoutParams(-1, dp(52)))

        addPopupButtons(
            layout,
            cancel = { dialog.dismiss() },
            save = {
                val value = input.text.toString().trim()
                if (value.isEmpty()) {
                    showToast("Please enter a name")
                } else {
                    onSave(value)
                    dialog.dismiss()
                }
            },
            saveLabel = saveLabel
        )

        input.requestFocus()
    }

    // ==========================================
    // FOLDER CATEGORY SELECTION
    // ==========================================

    private fun createFolder() {
        showCategoryPopup { category ->
            showCustomInput(
                title = "Create $category folder",
                hint = "Enter folder name",
                saveLabel = "Create"
            ) { name ->

                val duplicate = folders.any {
                    it.optString("name").equals(name, true)
                }

                if (duplicate) {
                    showToast("Folder name already exists")
                } else {
                    val folder = JSONObject().apply {
                        put("name", name)
                        put("category", category)
                        put(
                            "color",
                            categoryColors[category] ?: accent
                        )
                    }

                    folders.add(folder)
                    saveData()
                    showPage("Folders")
                }
            }
        }
    }

    private fun showCategoryPopup(
        onSelected: (String) -> Unit
    ) {
        val (dialog, layout) = createPopup("Choose category")

        addSpace(layout, 12)

        val categories = categoryColors.keys.toList()

        categories.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            pair.forEach { category ->
                val color = categoryColors[category] ?: accent

                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = rounded(surface2, 14, outline)
                    setPadding(dp(10), dp(14), dp(10), dp(14))
                    isClickable = true
                    isFocusable = true
                }

                val icon = makeText(
                    categoryIcons[category] ?: "▱",
                    25f,
                    color,
                    true
                )
                icon.gravity = Gravity.CENTER

                card.addView(icon)
                addSpace(card, 6)

                val name = makeText(
                    category,
                    13f,
                    foreground,
                    true
                )
                name.gravity = Gravity.CENTER
                card.addView(name)

                card.setOnClickListener {
                    dialog.dismiss()
                    onSelected(category)
                }

                row.addView(
                    card,
                    LinearLayout.LayoutParams(0, dp(86), 1f).apply {
                        marginEnd = dp(5)
                        marginStart = dp(5)
                    }
                )
            }

            layout.addView(row)
            addSpace(layout, 9)
        }

        layout.addView(
            popupButton("Cancel") { dialog.dismiss() }
        )
    }

    // ==========================================
    // FOLDER LIST
    // ==========================================

    private fun showFolders() {
        content.removeAllViews()

        val create = makeText(
            "＋   Create new folder",
            16f,
            accent,
            true
        ).apply {
            background = rounded(surface2, 16, outline)
            setPadding(dp(16), dp(17), dp(16), dp(17))
            setOnClickListener { createFolder() }
        }

        content.addView(create)
        addSpace(content, 15)

        if (folders.isEmpty()) {
            val empty = makeText(
                "No folders yet.\nCreate a folder to organize your notes.",
                14f,
                muted
            )
            empty.gravity = Gravity.CENTER
            empty.setPadding(0, dp(60), 0, dp(60))
            content.addView(empty)
            return
        }

        folders.forEach { folder ->
            val name = folder.optString("name")
            val category = folder.optString("category", "Other")
            val color = folder.optInt(
                "color",
                categoryColors[category] ?: accent
            )

            val count = notes.count {
                it.optString("folder") == name
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(surface, 17, outline)
                setPadding(dp(14), dp(14), dp(12), dp(14))
            }

            val folderIcon = makeText(
                categoryIcons[category] ?: "▱",
                25f,
                color,
                true
            )
            row.addView(folderIcon)

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            info.addView(makeText(name, 15f, foreground, true))
            addSpace(info, 3)
            info.addView(
                makeText(
                    "$category  •  $count ${if (count == 1) "note" else "notes"}",
                    11f,
                    muted
                )
            )

            row.addView(
                info,
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    marginStart = dp(13)
                }
            )

            val open = makeText("›", 25f, muted)
            open.setPadding(dp(8), 0, dp(8), 0)
            open.setOnClickListener { showFolderNotes(name) }
            row.addView(open)

            val more = makeText("⋮", 21f, muted)
            more.setPadding(dp(7), 0, 0, 0)
            more.setOnClickListener { showFolderOptions(name) }
            row.addView(more)

            row.setOnClickListener { showFolderNotes(name) }

            content.addView(row)
            addSpace(content, 10)
        }
    }

    private fun showFolderOptions(folder: String) {
        val (dialog, layout) = createPopup(folder)

        addSpace(layout, 14)

        layout.addView(
            popupButton("Rename folder") {
                dialog.dismiss()
                renameFolder(folder)
            }
        )

        addSpace(layout, 9)

        layout.addView(
            popupButton("Delete folder") {
                dialog.dismiss()
                deleteFolder(folder)
            }
        )

        addSpace(layout, 9)

        layout.addView(
            popupButton("Cancel") { dialog.dismiss() }
        )
    }

    private fun renameFolder(oldName: String) {
        val oldFolder = folders.find {
            it.optString("name") == oldName
        } ?: return

        showCustomInput(
            title = "Rename folder",
            hint = "Folder name",
            initialValue = oldName
        ) { newName ->

            val duplicate = folders.any {
                it.optString("name").equals(newName, true) &&
                    it.optString("name") != oldName
            }

            if (duplicate) {
                showToast("Folder name already exists")
            } else {
                oldFolder.put("name", newName)

                notes.forEach {
                    if (it.optString("folder") == oldName) {
                        it.put("folder", newName)
                    }
                }

                saveData()
                showPage("Folders")
            }
        }
    }

    private fun deleteFolder(folder: String) {
        val (dialog, layout) = createPopup("Delete folder?")

        addSpace(layout, 10)
        layout.addView(
            makeText(
                "Notes inside this folder will remain in Notes.",
                14f,
                muted
            )
        )

        addPopupButtons(
            layout,
            cancel = { dialog.dismiss() },
            save = {
                folders.removeAll {
                    it.optString("name") == folder
                }

                notes.forEach {
                    if (it.optString("folder") == folder) {
                        it.put("folder", "")
                    }
                }

                saveData()
                dialog.dismiss()
                showPage("Folders")
            },
            saveLabel = "Delete"
        )
    }

    
    
    private fun showFolderNotes(folder: String) {
        content.removeAllViews()
    
        val back = makeText("←  Back to folders", 15f, accent, true)
        back.setPadding(0, 0, 0, dp(15))
        back.setOnClickListener {
            showPage("Folders")
        }
        content.addView(back)
    
        val folderNotes = sortNotes(
            notes.filter {
                it.optString("folder") == folder
            }
        )
    
        if (folderNotes.isEmpty()) {
            content.addView(
                makeText("This folder is empty.", 14f, muted)
            )
            return
        }
    
        if (gridMode) {
            val grid = android.widget.GridLayout(this).apply {
                columnCount = 2
                useDefaultMargins = false
            }
    
            folderNotes.forEachIndexed { index, note ->
                val card = createNoteCard(note, false)
                val params = android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(175)
                    columnSpec = android.widget.GridLayout.spec(
                        index % 2,
                        1f
                    )
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                }
    
                grid.addView(card, params)
            }
    
            content.addView(grid)
        } else {
            folderNotes.forEach { note ->
                content.addView(
                    createNoteCard(note, true).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(145)
                        ).apply {
                            bottomMargin = dp(10)
                        }
                    }
                )
            }
        }
    }



    private fun createSortButton(): TextView {
        return makeText("↕", 27f, accent, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(4), dp(4), dp(4))
            setOnClickListener {
                showSortPopup()
            }
        }
    }


    private fun chooseFolder(
        current: String,
        onSelected: (String) -> Unit
    ) {
        val choices = listOf("No folder") +
            folders.map { it.optString("name") }

        val (dialog, layout) = createPopup("Move to folder")

        addSpace(layout, 12)

        choices.forEach { name ->
            val selected = if (current.isEmpty()) {
                name == "No folder"
            } else {
                name == current
            }

            val row = makeText(
                (if (selected) "✓   " else "     ") + name,
                15f,
                if (selected) accent else foreground,
                selected
            ).apply {
                background = rounded(surface2, 12)
                setPadding(dp(14), dp(13), dp(14), dp(13))
                setOnClickListener {
                    onSelected(if (name == "No folder") "" else name)
                    dialog.dismiss()
                }
            }

            layout.addView(row)
            addSpace(layout, 7)
        }

        layout.addView(
            popupButton("Cancel") { dialog.dismiss() }
        )
    }
    
        // ==========================================
    // NOTE ACTIONS
    // ==========================================

    private fun showNoteOptions(note: JSONObject) {
        val (dialog, layout) = createPopup("Note options")

        addSpace(layout, 12)

        val options = listOf(
            if (note.optBoolean("pinned", false)) "Unpin note" else "Pin note",
            "Move to folder",
            "Share note",
            "Delete note"
        )

        options.forEachIndexed { index, option ->
            val action = popupButton(option) {
                dialog.dismiss()

                when (index) {
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

            layout.addView(action)
            addSpace(layout, 8)
        }

        layout.addView(
            popupButton("Cancel") { dialog.dismiss() }
        )
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

        startActivity(
            Intent.createChooser(intent, "Share note")
        )
    }

    private fun confirmDeleteNote(note: JSONObject) {
        val (dialog, layout) = createPopup("Move to Recently Deleted?")

        addSpace(layout, 8)

        layout.addView(
            makeText(
                "You can restore this note later from Recently Deleted.",
                14f,
                muted
            )
        )

        addPopupButtons(
            layout,
            cancel = { dialog.dismiss() },
            save = {
                notes.remove(note)
                deletedNotes.add(note)
                saveData()
                dialog.dismiss()
                showPage("Notes")
            },
            saveLabel = "Delete"
        )
    }

    // ==========================================
    // TASKS
    // ==========================================

    private fun refreshTasks() {
        content.removeAllViews()

        val add = makeText(
            "＋   Add a new task...",
            15f,
            muted
        ).apply {
            background = rounded(surface2, 18, outline)
            setPadding(dp(16), dp(17), dp(16), dp(17))
            setOnClickListener { openTaskEditor() }
        }

        content.addView(add)
        addSpace(content, 17)

        if (tasks.isEmpty()) {
            val empty = makeText(
                "No tasks yet.\nAdd your first task to get started.",
                14f,
                muted
            )
            empty.gravity = Gravity.CENTER
            empty.setPadding(0, dp(65), 0, dp(65))
            content.addView(empty)
            return
        }

        val pending = tasks.filter {
            !it.optBoolean("done", false)
        }

        val completed = tasks.filter {
            it.optBoolean("done", false)
        }

        if (pending.isNotEmpty()) {
            content.addView(
                makeText("TO DO", 12f, muted, true)
            )
            addSpace(content, 7)

            pending.forEach {
                content.addView(createTaskRow(it))
                addSpace(content, 8)
            }
        }

        if (completed.isNotEmpty()) {
            addSpace(content, 15)
            content.addView(
                makeText("COMPLETED", 12f, muted, true)
            )
            addSpace(content, 7)

            completed.forEach {
                content.addView(createTaskRow(it))
                addSpace(content, 8)
            }
        }
    }

    private fun createTaskRow(task: JSONObject): LinearLayout {
        val done = task.optBoolean("done", false)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(surface, 15, outline)
            setPadding(dp(12), dp(13), dp(10), dp(13))
        }

        val check = CheckBox(this).apply {
            isChecked = done
            buttonTintList = ColorStateList.valueOf(accent)
            setOnCheckedChangeListener { _, checked ->
                task.put("done", checked)
                saveData()
                refreshTasks()
            }
        }

        row.addView(check)

        val title = makeText(
            task.optString("title"),
            15f,
            if (done) muted else foreground
        )

        if (done) {
            title.paintFlags =
                title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        row.addView(
            title,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val more = makeText("⋮", 21f, muted, true)
        more.setPadding(dp(10), dp(5), dp(5), dp(5))
        more.setOnClickListener { showTaskOptions(task) }
        row.addView(more)

        return row
    }

    private fun openTaskEditor(existing: JSONObject? = null) {
        val (dialog, layout) = createPopup(
            if (existing == null) "Create task" else "Edit task"
        )

        addSpace(layout, 14)

        val input = EditText(this).apply {
            hint = "What do you need to do?"
            setText(existing?.optString("title", "") ?: "")
            textSize = 16f
            setTextColor(this@MainActivity.foreground)
            setHintTextColor(muted)
            background = rounded(surface2, 12, outline)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setSingleLine(true)
        }

        layout.addView(input, LinearLayout.LayoutParams(-1, dp(52)))

        addPopupButtons(
            layout,
            cancel = { dialog.dismiss() },
            save = {
                val value = input.text.toString().trim()

                if (value.isEmpty()) {
                    showToast("Enter a task name")
                } else {
                    val task = existing ?: JSONObject()

                    task.put("title", value)

                    if (existing == null) {
                        task.put("done", false)
                        tasks.add(task)
                    }

                    saveData()
                    dialog.dismiss()
                    refreshTasks()
                }
            },
            saveLabel = if (existing == null) "Create" else "Save"
        )
    }

    private fun showTaskOptions(task: JSONObject) {
        val (dialog, layout) = createPopup("Task options")

        addSpace(layout, 12)

        layout.addView(
            popupButton("Edit task") {
                dialog.dismiss()
                openTaskEditor(task)
            }
        )

        addSpace(layout, 8)

        layout.addView(
            popupButton("Delete task") {
                dialog.dismiss()
                confirmDeleteTask(task)
            }
        )

        addSpace(layout, 8)

        layout.addView(
            popupButton("Cancel") { dialog.dismiss() }
        )
    }

    private fun confirmDeleteTask(task: JSONObject) {
        val (dialog, layout) = createPopup("Delete task?")

        addSpace(layout, 8)
        layout.addView(
            makeText("This task will be permanently deleted.", 14f, muted)
        )

        addPopupButtons(
            layout,
            cancel = { dialog.dismiss() },
            save = {
                tasks.remove(task)
                saveData()
                dialog.dismiss()
                refreshTasks()
            },
            saveLabel = "Delete"
        )
    }
    
        // ==========================================
    // SETTINGS
    // ==========================================

    
    private fun showSettings() {
        content.removeAllViews()
    
        content.addView(
            settingRow("☾", "Dark mode", if (darkMode) "On" else "Off") {
                darkMode = !darkMode
                saveData()
                updateThemeColors()
                showPage("Settings")
            }
        )
    
        addSpace(content, 10)
    
        content.addView(
            settingRow("Aa", "Font size", "$fontSize sp") {
                showFontSizePopup()
            }
        )
    
        addSpace(content, 10)
    
        content.addView(
            settingRow("▤", "Recently Deleted", "${deletedNotes.size} items") {
                showPage("Trash")
            }
        )
    
        addSpace(content, 10)
    
        content.addView(
            settingRow("ⓘ", "About Nuvote", "Version 1.0") {
                showAbout()
            }
        )
    }


    private fun settingRow(
        icon: String,
        title: String,
        subtitle: String,
        action: () -> Unit
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(surface, 16, outline)
            setPadding(dp(14), dp(16), dp(14), dp(16))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

        val symbol = makeText(icon, 22f, accent, true)
        symbol.gravity = Gravity.CENTER

        row.addView(
            symbol,
            LinearLayout.LayoutParams(dp(38), dp(38))
        )

        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        left.addView(makeText(title, 15f, foreground, true))
        addSpace(left, 3)
        left.addView(makeText(subtitle, 12f, muted))

        row.addView(
            left,
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginStart = dp(12)
            }
        )

        row.addView(makeText("›", 25f, muted))

        return row
    }

    // ==========================================
    // SORT POPUP
    // ==========================================

    private fun showSortPopup() {
        val (dialog, layout) = createPopup("Sort notes")

        addSpace(layout, 12)

        val choices = listOf(
            "Recently updated",
            "Oldest first",
            "Alphabetical"
        )

        choices.forEach { choice ->
            val selected = sortOrder == choice

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(
                    if (selected) surface2 else surface,
                    12,
                    if (selected) accent else outline
                )
                setPadding(dp(14), dp(14), dp(14), dp(14))
            }

            row.addView(
                makeText(
                    choice,
                    14f,
                    if (selected) accent else foreground,
                    selected
                ),
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            row.addView(
                makeText(
                    if (selected) "✓" else "○",
                    18f,
                    if (selected) accent else muted,
                    true
                )
            )

            row.setOnClickListener {
                sortOrder = choice
                saveData()
                dialog.dismiss()
                if (currentPage == "Notes") {
                    refreshNotes(
                        if (::searchBox.isInitialized) {
                            searchBox.text.toString()
                        } else ""
                    )
                } else {
                    showPage("Notes")
                }
            }

            layout.addView(row)
            addSpace(layout, 8)
        }

        layout.addView(
            popupButton("Cancel") { dialog.dismiss() }
        )
    }

    // ==========================================
    // FONT SIZE
    // ==========================================

    private fun showFontSizePopup() {
        val (dialog, layout) = createPopup("Font size")

        addSpace(layout, 12)

        val preview = makeText(
            "Your ideas, neatly organized.",
            fontSize.toFloat(),
            foreground
        )

        preview.gravity = Gravity.CENTER
        preview.setPadding(dp(12), dp(20), dp(12), dp(20))
        preview.background = rounded(surface2, 14)

        layout.addView(preview)

        addSpace(layout, 12)

        val value = makeText("$fontSize sp", 15f, accent, true)
        value.gravity = Gravity.CENTER
        layout.addView(value)

        val slider = SeekBar(this).apply {
            max = 12
            progress = (fontSize - 12).coerceIn(0, 12)
            progressTintList = ColorStateList.valueOf(accent)
            thumbTintList = ColorStateList.valueOf(accent)
        }

        slider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val size = 12 + progress
                    preview.textSize = size.toFloat()
                    value.text = "$size sp"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        layout.addView(slider)

        addPopupButtons(
            layout,
            cancel = { dialog.dismiss() },
            save = {
                fontSize = 12 + slider.progress
                saveData()
                dialog.dismiss()
                showPage("Settings")
            },
            saveLabel = "Apply"
        )
    }

    // ==========================================
    // ABOUT NUVOTE
    // ==========================================

    private fun showAbout() {
        val (dialog, layout) = createPopup("About Nuvote")

        addSpace(layout, 14)

        val logo = TextView(this).apply {
            text = "N"
            textSize = 40f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = rounded(accent, 23)
        }

        val logoRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER
        }

        logoRow.addView(
            logo,
            LinearLayout.LayoutParams(dp(82), dp(82))
        )

        layout.addView(logoRow)
        addSpace(layout, 12)

        val name = makeText("NUVOTE", 25f, foreground, true)
        name.gravity = Gravity.CENTER
        layout.addView(name)

        val tagline = makeText(
            "Your ideas, neatly organized.",
            14f,
            muted
        )
        tagline.gravity = Gravity.CENTER
        layout.addView(tagline)

        addSpace(layout, 18)

        layout.addView(
            makeText("Version 1.0", 13f, muted)
        )

        addSpace(layout, 8)

        layout.addView(
            makeText("Developed by ROXAMI", 15f, foreground, true)
        )

        addSpace(layout, 7)

        layout.addView(
            makeText(
                "A simple place to organize your notes, " +
                    "ideas, folders and everyday tasks.",
                13f,
                muted
            )
        )

        addSpace(layout, 18)

        layout.addView(
            popupButton("Close", true) {
                dialog.dismiss()
            }
        )
    }
    
        // ==========================================
    // RECENTLY DELETED
    // ==========================================

    private fun showTrash() {
        content.removeAllViews()

        val back = makeText(
            "←  Back to Settings",
            15f,
            accent,
            true
        )

        back.setPadding(0, 0, 0, dp(15))
        back.setOnClickListener { showPage("Settings") }
        content.addView(back)

        if (deletedNotes.isEmpty()) {
            val empty = makeText(
                "Recently Deleted is empty.",
                14f,
                muted
            )

            empty.gravity = Gravity.CENTER
            empty.setPadding(0, dp(70), 0, dp(70))
            content.addView(empty)
            return
        }

        deletedNotes.toList().forEach { note ->
            val card = makeCard(surface, 16)

            card.addView(
                makeText(
                    note.optString("title", "Untitled"),
                    16f,
                    foreground,
                    true
                )
            )

            addSpace(card, 7)

            card.addView(
                makeText(
                    note.optString("content"),
                    13f,
                    muted
                )
            )

            addSpace(card, 12)

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }

            actions.addView(
                popupButton("Restore") {
                    deletedNotes.remove(note)
                    notes.add(note)
                    saveData()
                    showPage("Trash")
                }
            )

            actions.addView(
                popupButton("Delete forever") {
                    confirmPermanentDelete(note)
                },
                LinearLayout.LayoutParams(-2, -2).apply {
                    marginStart = dp(8)
                }
            )

            card.addView(actions)
            content.addView(card)
            addSpace(content, 10)
        }

        addSpace(content, 14)

        content.addView(
            popupButton("Empty Recently Deleted") {
                confirmEmptyTrash()
            }
        )
    }

    private fun confirmPermanentDelete(note: JSONObject) {
        val (dialog, layout) = createPopup("Delete permanently?")

        addSpace(layout, 8)

        layout.addView(
            makeText(
                "This note cannot be recovered.",
                14f,
                muted
            )
        )

        addPopupButtons(
            layout,
            cancel = { dialog.dismiss() },
            save = {
                deletedNotes.remove(note)
                saveData()
                dialog.dismiss()
                showPage("Trash")
            },
            saveLabel = "Delete"
        )
    }

    private fun confirmEmptyTrash() {
        val (dialog, layout) = createPopup("Empty Recently Deleted?")

        addSpace(layout, 8)

        layout.addView(
            makeText(
                "All deleted notes will be permanently removed.",
                14f,
                muted
            )
        )

        addPopupButtons(
            layout,
            cancel = { dialog.dismiss() },
            save = {
                deletedNotes.clear()
                saveData()
                dialog.dismiss()
                showPage("Trash")
            },
            saveLabel = "Empty"
        )
    }

    // ==========================================
    // SORTING
    // ==========================================

    private fun sortNotes(
        list: List<JSONObject>
    ): List<JSONObject> {
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

    // ==========================================
    // BACK NAVIGATION
    // ==========================================

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        when (currentPage) {
            "Trash" -> showPage("Settings")
            "Tasks" -> showPage("Notes")
            "Folders" -> showPage("Notes")
            "Settings" -> showPage("Notes")
            else -> super.onBackPressed()
        }
    }
}
