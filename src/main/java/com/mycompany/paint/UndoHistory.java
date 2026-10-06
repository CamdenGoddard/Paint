package com.mycompany.paint;

import java.util.ArrayDeque;
import java.util.Deque;

import javafx.scene.image.WritableImage;

/**
 * The undo/redo history for one tab, built from two stacks.
 *
 * <p>Each entry is a full snapshot of the canvas. Before any edit changes
 * the canvas, the tab {@linkplain #record records} a snapshot of how it
 * looked; that snapshot is pushed onto the <em>undo stack</em>. Undoing
 * pops the most recent snapshot off that stack (to put back on the canvas)
 * and pushes the canvas's current look onto the <em>redo stack</em> so the
 * undo can itself be undone. Making a brand-new edit throws the redo stack
 * away, since redoing past a new edit would no longer make sense.</p>
 *
 * <p>Both stacks are {@link Deque}s used strictly through {@code push},
 * {@code pop} and {@code peek} - the stack operations - which is what the
 * Java documentation recommends instead of the older {@code Stack} class.
 * The undo stack is capped at {@link #MAX_UNDO_STEPS} entries; when it
 * overflows, the oldest snapshot (the bottom of the stack) is dropped so
 * memory use stays bounded.</p>
 */
final class UndoHistory {

    /** How many edits can be undone before the oldest ones are forgotten. */
    static final int MAX_UNDO_STEPS = 40;

    private final Deque<WritableImage> undoStack = new ArrayDeque<>();
    private final Deque<WritableImage> redoStack = new ArrayDeque<>();

    /**
     * Remembers how the canvas looked just before an edit changed it.
     * Also forgets everything that could previously have been redone.
     *
     * @param before a snapshot of the canvas taken before the edit
     */
    void record(WritableImage before) {
        undoStack.push(before);
        if (undoStack.size() > MAX_UNDO_STEPS) {
            undoStack.removeLast(); // the oldest entry sits at the bottom of the stack
        }
        redoStack.clear();
    }

    /**
     * Steps back one edit.
     *
     * @param current a snapshot of the canvas as it looks right now; it is
     *                kept on the redo stack so this undo can be redone
     * @return the snapshot to put back on the canvas, or null if there is
     *         nothing left to undo
     */
    WritableImage undo(WritableImage current) {
        if (undoStack.isEmpty()) {
            return null;
        }
        redoStack.push(current);
        return undoStack.pop();
    }

    /**
     * Steps forward again after an undo.
     *
     * @param current a snapshot of the canvas as it looks right now; it is
     *                put back on the undo stack so this redo can be undone
     * @return the snapshot to put back on the canvas, or null if there is
     *         nothing to redo
     */
    WritableImage redo(WritableImage current) {
        if (redoStack.isEmpty()) {
            return null;
        }
        undoStack.push(current);
        return redoStack.pop();
    }

    /**
     * Reports whether {@link #undo} would do anything.
     *
     * @return true if at least one edit can be undone
     */
    boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Reports whether {@link #redo} would do anything.
     *
     * @return true if at least one undone edit can be redone
     */
    boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /** Forgets all history. Used when a tab is reset to a fresh blank canvas. */
    void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
