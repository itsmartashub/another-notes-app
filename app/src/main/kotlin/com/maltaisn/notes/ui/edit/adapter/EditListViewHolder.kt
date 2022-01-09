/*
 * Copyright 2022 Nicolas Maltais
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maltaisn.notes.ui.edit.adapter

import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.text.style.CharacterStyle
import android.text.util.Linkify
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.core.text.getSpans
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.maltaisn.notes.hideKeyboard
import com.maltaisn.notes.model.PrefsManager
import com.maltaisn.notes.model.entity.Label
import com.maltaisn.notes.model.entity.Reminder
import com.maltaisn.notes.showKeyboard
import com.maltaisn.notes.strikethroughText
import com.maltaisn.notes.sync.R
import com.maltaisn.notes.sync.databinding.ItemEditContentBinding
import com.maltaisn.notes.sync.databinding.ItemEditDateBinding
import com.maltaisn.notes.sync.databinding.ItemEditHeaderBinding
import com.maltaisn.notes.sync.databinding.ItemEditItemAddBinding
import com.maltaisn.notes.sync.databinding.ItemEditItemBinding
import com.maltaisn.notes.sync.databinding.ItemEditLabelsBinding
import com.maltaisn.notes.sync.databinding.ItemEditTitleBinding
import com.maltaisn.notes.ui.edit.BulletTextWatcher
import com.maltaisn.notes.ui.edit.undo.TextUndoAction
import com.maltaisn.notes.ui.edit.undo.TextUndoActionType
import com.maltaisn.notes.utils.RelativeDateFormatter
import java.text.DateFormat

/**
 * Interface implemented by any item that can have its focus position changed.
 */
sealed interface EditFocusableViewHolder {
    fun setFocus(pos: Int)
}

class EditDateViewHolder(binding: ItemEditDateBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private val dateEdt = binding.dateEdt

    private val dateFormatter = RelativeDateFormatter(dateEdt.resources) { date ->
        DateUtils.formatDateTime(dateEdt.context, date, DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL)
    }

    fun bind(item: EditDateItem) {
        dateEdt.text = dateFormatter.format(item.date, System.currentTimeMillis(),
            PrefsManager.MAXIMUM_RELATIVE_DATE_DAYS)
    }
}

class EditTitleViewHolder(binding: ItemEditTitleBinding, callback: EditAdapter.Callback) :
    RecyclerView.ViewHolder(binding.root), EditFocusableViewHolder {

    private val titleEdt = binding.titleEdt
    private var item: EditTitleItem? = null

    private var ignoreChanges = false

    init {
        titleEdt.setOnClickListener {
            callback.onNoteClickedToEdit()
        }
        titleEdt.addTextChangedListener(clearSpansTextWatcher)
        titleEdt.addTextChangedListener(object : TextWatcher {
            private var oldText: String = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                oldText = s?.substring(start, start + count).orEmpty()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!ignoreChanges) {
                    callback.onTextChanged(TextUndoAction(bindingAdapterPosition, TextUndoActionType.TITLE,
                        start, start + before, oldText, s?.substring(start, start + count) ?: ""))
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // If editable has changed vs model, update model.
                if (s != item?.text?.text) {
                    item?.text = AndroidEditableText(s ?: return)
                }
            }
        })
        titleEdt.addOnAttachStateChangeListener(PrepareCursorControllersListener())
        titleEdt.setHorizontallyScrolling(false)
        titleEdt.maxLines = Integer.MAX_VALUE
    }

    fun bind(item: EditTitleItem) {
        this.item = item
        titleEdt.isFocusable = item.editable
        titleEdt.isFocusableInTouchMode = item.editable
        ignoreChanges = true
        titleEdt.setText(item.text.text)
        ignoreChanges = false
    }

    override fun setFocus(pos: Int) {
        titleEdt.requestFocus()
        titleEdt.setSelection(pos)
        titleEdt.showKeyboard()
    }
}

class EditContentViewHolder(binding: ItemEditContentBinding, callback: EditAdapter.Callback) :
    RecyclerView.ViewHolder(binding.root), EditFocusableViewHolder {

    private val contentEdt = binding.contentEdt
    private var item: EditContentItem? = null

    private var ignoreChanges = false

    init {
        contentEdt.addTextChangedListener(BulletTextWatcher())
        contentEdt.addTextChangedListener(clearSpansTextWatcher)
        contentEdt.movementMethod = LinkMovementMethod.getInstance()  // Clickable links
        contentEdt.addTextChangedListener(object : TextWatcher {
            private var oldText: String = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                oldText = s?.substring(start, start + count).orEmpty()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!ignoreChanges) {
                    callback.onTextChanged(TextUndoAction(bindingAdapterPosition, TextUndoActionType.CONTENT,
                        start, start + before, oldText, s?.substring(start, start + count) ?: ""))
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Add new links
                LinkifyCompat.addLinks(s ?: return, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)

                // If editable has changed vs model, update model.
                if (s != item?.text?.text) {
                    item?.text = AndroidEditableText(s)
                }

            }
        })

        contentEdt.setOnClickListener {
            callback.onNoteClickedToEdit()
        }

        // Fixes broken long press after holder has been recycled (#34).
        // See https://stackoverflow.com/q/54833004
        contentEdt.addOnAttachStateChangeListener(PrepareCursorControllersListener())
    }

    fun bind(item: EditContentItem) {
        this.item = item
        contentEdt.isFocusable = item.editable
        contentEdt.isFocusableInTouchMode = item.editable
        ignoreChanges = true
        contentEdt.setText(item.text.text)
        ignoreChanges = false
    }

    override fun setFocus(pos: Int) {
        contentEdt.requestFocus()
        contentEdt.setSelection(pos)
        contentEdt.showKeyboard()
    }
}

class EditItemViewHolder(binding: ItemEditItemBinding, callback: EditAdapter.Callback) :
    RecyclerView.ViewHolder(binding.root), EditFocusableViewHolder {

    val dragImv = binding.dragImv
    private val itemCheck = binding.itemChk
    private val itemEdt = binding.contentEdt
    private val deleteImv = binding.deleteImv

    private var item: EditItemItem? = null

    private var ignoreChanges = false

    val isChecked: Boolean
        get() = itemCheck.isChecked

    init {
        itemCheck.setOnCheckedChangeListener { _, isChecked ->
            itemEdt.clearFocus()
            itemEdt.hideKeyboard()
            itemEdt.strikethroughText = isChecked && callback.strikethroughCheckedItems
            itemEdt.isActivated = !isChecked // Controls text color selector.
            dragImv.isInvisible = isChecked && callback.moveCheckedToBottom

            val pos = bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                callback.onNoteItemCheckChanged(pos, isChecked)
            }
        }

        itemEdt.addTextChangedListener(clearSpansTextWatcher)

        itemEdt.addTextChangedListener(object : TextWatcher {
            private var oldText: String = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                oldText = s?.substring(start, start + count).orEmpty()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!ignoreChanges) {
                    callback.onTextChanged(TextUndoAction(bindingAdapterPosition, TextUndoActionType.LIST_ITEM,
                        start, start + before, oldText, s?.substring(start, start + count) ?: ""))
                }

                // This is used to detect when user enters line breaks into the input, so the
                // item can be split into multiple items. When user enters a single line break,
                // selection is set at the beginning of new item. On paste, i.e. when more than one
                // character is entered, selection is set at the end of last new item.
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    callback.onNoteItemChanged(pos, count > 1)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (s == null) return

                // Add new links
                LinkifyCompat.addLinks(s, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)

                // If editable has changed vs model, update model.
                if (s != item?.text?.text) {
                    item?.text = AndroidEditableText(s)
                }
            }
        })
        itemEdt.movementMethod = LinkMovementMethod.getInstance()  // Clickable links
        itemEdt.setOnFocusChangeListener { _, hasFocus ->
            // Only show delete icon for currently focused item.
            deleteImv.isInvisible = !hasFocus
        }
        itemEdt.setOnKeyListener { _, _, event ->
            val isCursorAtStart = itemEdt.selectionStart == 0 && itemEdt.selectionStart == itemEdt.selectionEnd
            if (isCursorAtStart && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                // If user presses backspace at the start of an item, current item
                // will be merged with previous.
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    callback.onNoteItemBackspacePressed(pos)
                }
            }
            false
        }
        itemEdt.setOnClickListener {
            callback.onNoteClickedToEdit()
        }
        itemEdt.addOnAttachStateChangeListener(PrepareCursorControllersListener())

        deleteImv.setOnClickListener {
            val pos = bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                callback.onNoteItemDeleteClicked(pos)
            }
        }
    }

    fun bind(item: EditItemItem) {
        this.item = item

        itemEdt.isFocusable = item.editable
        itemEdt.isFocusableInTouchMode = item.editable
        itemEdt.isActivated = !item.checked
        ignoreChanges = true
        itemEdt.setText(item.text.text)
        ignoreChanges = false

        itemCheck.isChecked = item.checked
        itemCheck.isEnabled = item.editable
    }

    override fun setFocus(pos: Int) {
        itemEdt.requestFocus()
        itemEdt.setSelection(pos)
        itemEdt.showKeyboard()
    }

    fun clearFocus() {
        itemEdt.clearFocus()
    }
}

class EditItemAddViewHolder(binding: ItemEditItemAddBinding, callback: EditAdapter.Callback) :
    RecyclerView.ViewHolder(binding.root) {

    init {
        itemView.setOnClickListener {
            callback.onNoteItemAddClicked(bindingAdapterPosition)
        }
    }
}

class EditHeaderViewHolder(binding: ItemEditHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private val titleTxv = binding.titleTxv

    fun bind(item: EditCheckedHeaderItem) {
        titleTxv.text = titleTxv.context.resources.getQuantityString(
            R.plurals.edit_checked_items, item.count, item.count)
    }
}

class EditItemLabelsViewHolder(binding: ItemEditLabelsBinding, callback: EditAdapter.Callback) :
    RecyclerView.ViewHolder(binding.root) {

    private val chipGroup = binding.chipGroup
    private val labelClickListener = View.OnClickListener {
        callback.onNoteLabelClicked()
    }
    private val reminderClickListener = View.OnClickListener {
        callback.onNoteReminderClicked()
    }

    private val reminderDateFormatter = RelativeDateFormatter(itemView.resources) { date ->
        DateFormat.getDateInstance(DateFormat.SHORT).format(date)
    }

    fun bind(item: EditChipsItem) {
        val layoutInflater = LayoutInflater.from(chipGroup.context)
        chipGroup.removeAllViews()
        for (chip in item.chips) {
            when (chip) {
                is Label -> {
                    val view = layoutInflater.inflate(R.layout.view_edit_chip_label, chipGroup, false) as Chip
                    chipGroup.addView(view)
                    view.text = chip.name
                    view.setOnClickListener(labelClickListener)
                }
                is Reminder -> {
                    val view = layoutInflater.inflate(R.layout.view_edit_chip_reminder, chipGroup, false) as Chip
                    chipGroup.addView(view)
                    view.text = reminderDateFormatter.format(chip.next.time,
                        System.currentTimeMillis(), PrefsManager.MAXIMUM_RELATIVE_DATE_DAYS)
                    view.strikethroughText = chip.done
                    view.isActivated = !chip.done
                    view.setChipIconResource(if (chip.recurrence != null) R.drawable.ic_repeat else R.drawable.ic_alarm)
                    view.setOnClickListener(reminderClickListener)
                }
                else -> error("Unknown chip type")
            }
        }
    }
}

// Wrapper around Editable to allow transparent access to text content from ViewModel.
// Editable items have a EditableText field which is set by a text watcher added to the
// EditText and called when text is set when item is bound.
// Note that the Editable instance can change during the EditText lifetime.
private class AndroidEditableText(override val text: Editable) : EditableText {

    override fun append(text: CharSequence) {
        this.text.append(text)
    }

    override fun replace(start: Int, end: Int, text: CharSequence) {
        this.text.replace(start, end, text)
    }

    override fun replaceAll(text: CharSequence) {
        this.text.replace(0, this.text.length, text)
    }
}

private val clearSpansTextWatcher = object : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // nothing
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // nothing
    }

    override fun afterTextChanged(s: Editable?) {
        if (s == null) return
        // Might not remove all spans but will work for most of them.
        val spansToRemove = s.getSpans<CharacterStyle>()
        for (span in spansToRemove) {
            s.removeSpan(span)
        }
    }
}

/**
 * Used to fix the issue described at [https://stackoverflow.com/q/54833004],
 * causing the EditText long press to fail after a view holder has been recycled.
 */
private class PrepareCursorControllersListener : View.OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(view: View) {
        if (view !is EditText) {
            return
        }
        view.isCursorVisible = false
        view.isCursorVisible = true
    }

    override fun onViewDetachedFromWindow(v: View) = Unit
}
