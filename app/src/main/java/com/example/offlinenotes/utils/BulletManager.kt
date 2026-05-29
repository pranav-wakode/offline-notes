package com.example.offlinenotes.utils

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.widget.EditText

enum class BulletType {
    NONE, STANDARD, NUMBERED, ALPHABETICAL, ROMAN, CHECKBOX
}

class BulletManager(private val editText: EditText) {

    var currentBulletType = BulletType.NONE
    private var isFormatting = false
    private var listCounter = 1

    init {
        setupTextWatcher()
        setupCheckboxClicker()
    }

    private fun setupTextWatcher() {
        editText.addTextChangedListener(object : TextWatcher {
            private var previousText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousText = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null || currentBulletType == BulletType.NONE) return

                val text = s.toString()
                if (text.length > previousText.length && text.endsWith("\n") && !previousText.endsWith("\n")) {
                    isFormatting = true
                    try {
                        val cursorPosition = editText.selectionStart
                        val textBeforeCursor = text.substring(0, cursorPosition - 1)
                        val lastLineIndex = textBeforeCursor.lastIndexOf('\n') + 1
                        val lastLine = textBeforeCursor.substring(lastLineIndex)

                        if (isOnlyBullet(lastLine)) {
                            s.delete(lastLineIndex, cursorPosition - 1)
                            currentBulletType = BulletType.NONE
                            listCounter = 1
                            return
                        }

                        if (lastLine.isNotBlank()) {
                            val nextBullet = getNextBullet()
                            s.insert(cursorPosition, nextBullet)
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isFormatting = false
                    }
                }
            }
        })
    }

    private fun isOnlyBullet(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed == "•" || trimmed.matches(Regex("^[0-9]+\\.$")) ||
                trimmed.matches(Regex("^[a-zA-Z]\\.$")) || trimmed == "☐" || trimmed == "☑"
    }

    private fun getNextBullet(): String {
        return when (currentBulletType) {
            BulletType.STANDARD -> "• "
            BulletType.NUMBERED -> {
                listCounter++
                "$listCounter. "
            }
            BulletType.ALPHABETICAL -> {
                val char = ('A' + (listCounter % 26)).toString()
                listCounter++
                "$char. "
            }
            BulletType.ROMAN -> "• "
            BulletType.CHECKBOX -> "☐ "
            BulletType.NONE -> ""
        }
    }

    fun insertBulletAtCursor(type: BulletType) {
        currentBulletType = type
        if (type == BulletType.NONE) return

        listCounter = 1
        val cursorPosition = editText.selectionStart
        val editable = editText.text

        isFormatting = true
        val prefix = if (cursorPosition > 0 && editable[cursorPosition - 1] != '\n') "\n" else ""

        val bullet = getNextBullet()
        editable.insert(cursorPosition, prefix + bullet)
        isFormatting = false
    }

    fun insertDateOrReminder(tag: String) {
        val cursorPosition = editText.selectionStart
        val editable = editText.text
        isFormatting = true
        val prefix = if (cursorPosition > 0 && editable[cursorPosition - 1] != '\n') "\n" else ""
        editable.insert(cursorPosition, prefix + tag + " ")
        isFormatting = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCheckboxClicker() {
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val layout = editText.layout
                if (layout != null) {
                    val x = event.x - editText.totalPaddingLeft + editText.scrollX
                    val y = event.y - editText.totalPaddingTop + editText.scrollY
                    val line = layout.getLineForVertical(y.toInt())
                    val offset = layout.getOffsetForHorizontal(line, x)

                    val text = editText.text
                    if (offset < text.length) {
                        val char = text[offset]
                        if (char == '☐') {
                            text.replace(offset, offset + 1, "☑")
                            return@setOnTouchListener true
                        } else if (char == '☑') {
                            text.replace(offset, offset + 1, "☐")
                            return@setOnTouchListener true
                        }
                    }
                }
            }
            false
        }
    }
}