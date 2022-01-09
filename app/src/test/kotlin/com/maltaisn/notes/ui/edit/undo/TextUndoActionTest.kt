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

package com.maltaisn.notes.ui.edit.undo

import org.junit.Test
import kotlin.test.assertEquals

class TextUndoActionTest {

    @Test
    fun `should create without common sequence (start)`() {
        // "abcdef" -> "abcd"
        assertEquals(testAction(4..5, "ef", ""),
            testAction(0..5, "abcdef", "abcd"))
    }

    @Test
    fun `should create without common sequence (end)`() {
        // "abcdef" -> "cdef"
        assertEquals(testAction(0..1, "ab", ""),
            testAction(0..5, "abcdef", "cdef"))
    }

    @Test
    fun `should create without common sequence (start + end)`() {
        // "abcdef" -> "abef"
        assertEquals(testAction(2..3, "cd", ""),
            testAction(0..5, "abcdef", "abef"))
    }

    @Test
    fun `should create without common sequence (all)`() {
        // "abcdef" -> "abcdef" (note: range doesn't matter)
        assertEquals(testAction(6 until 6, "", ""),
            testAction(0..5, "abcdef", "abcdef"))
    }

    @Test
    fun `should merge action (outside 0)`() {
        // "abcdef" -> "abyef" -> "abxyzef"
        assertEquals(testAction(2 until 4, "cd", "xyz"),
            testAction(2 until 4, "cd", "y").mergeWith(
                testAction(2 until 3, "y", "xyz")))
    }

    @Test
    fun `should merge action (outside 1)`() {
        // "abcdef" -> "abef" -> "abxyzef"
        assertEquals(testAction(0 until 6, "abcdef", ""),
            testAction(2 until 4, "cd", "").mergeWith(
                testAction(0 until 4, "abef", "")))
    }

    @Test
    fun `should merge action (outside 2)`() {
        // "xyz" -> "bcyz" -> "abxyz"
        assertEquals(testAction(0 until 0, "", "ab"),
            testAction(0 until 1, "x", "bc").mergeWith(
                testAction(0 until 2, "bc", "abx")))
    }

    @Test
    fun `should merge action (inside 0)`() {
        // "abcdef" -> "abxzef" -> "abxyzef"
        assertEquals(testAction(2 until 4, "cd", "xyz"),
            testAction(2 until 4, "cd", "xz").mergeWith(
                testAction(3 until 3, "", "y")))
    }

    @Test
    fun `should merge action (inside 1)`() {
        // "abcdef" -> "abxyzef" -> "abxyzef"
        assertEquals(testAction(2 until 4, "cd", "xyz"),
            testAction(2 until 4, "cd", "xyz").mergeWith(
                testAction(3 until 3, "", "")))
    }

    @Test
    fun `should merge action (inside 2)`() {
        // "abcdef" -> "abwyef" -> "abwxyzef"
        assertEquals(testAction(2 until 4, "cd", "wxyz"),
            testAction(2 until 4, "cd", "wy").mergeWith(
                testAction(3 until 4, "y", "xyz")))
    }

    @Test
    fun `should merge action (inside 3)`() {
        // "abcdef" -> "abxzef" -> "abwxyzef"
        assertEquals(testAction(2 until 4, "cd", "wxyz"),
            testAction(2 until 4, "cd", "xz").mergeWith(
                testAction(2 until 3, "x", "wxy")))
    }

    @Test
    fun `should merge action (before 0)`() {
        // "abcdef" -> "abcdefg" -> "abcdefgh"
        assertEquals(testAction(6 until 6, "", "gh"),
            testAction(6 until 6, "", "h").mergeWith(
                testAction(6 until 6, "", "g")))
    }

    @Test
    fun `should merge action (before 1)`() {
        // "xyz" -> "xbcz" -> "acz"
        assertEquals(testAction(0 until 2, "xy", "ac"),
            testAction(1 until 2, "y", "bc").mergeWith(
                testAction(0 until 2, "xb", "a")))
    }

    @Test
    fun `should merge action (before 2)`() {
        // "xyz" -> "xbcdz" -> "adz"
        assertEquals(testAction(0 until 2, "xy", "ad"),
            testAction(1 until 2, "y", "bcd").mergeWith(
                testAction(0 until 3, "xbc", "a")))
    }

    @Test
    fun `should merge action (before 3)`() {
        // "abcdef" -> "abcdxyz" -> "wxyz"
        assertEquals(testAction(0 until 6, "abcdef", "wxyz"),
            testAction(4 until 6, "ef", "xyz").mergeWith(
                testAction(0 until 4, "abcd", "w")))
    }

    @Test
    fun `should merge action (insert after 0)`() {
        // "abcdef" -> "abcdefg" -> "abcdefgh"
        assertEquals(testAction(6 until 6, "", "gh"),
            testAction(6 until 6, "", "g").mergeWith(
                testAction(7 until 7, "", "h")))
    }

    @Test
    fun `should merge action (insert after 1)`() {
        // "abcdef" -> "abcxf" -> "abcxyz"
        assertEquals(testAction(3 until 6, "def", "xyz"),
            testAction(3 until 5, "de", "x").mergeWith(
                testAction(4 until 5, "f", "yz")))
    }

    @Test
    fun `should merge action (insert after 2)`() {
        // "abcde" -> "abwxye" -> "abwze"
        assertEquals(testAction(0 until 0, "cd", "wz"),
            testAction(2 until 4, "cd", "wxy").mergeWith(
                testAction(3 until 5, "xy", "z")))
    }

//    @Test
//    fun `should merge action (replace 1)`() {
//        // "xyz" -> "xyz___" -> "xyzabc"
//        assertEquals(testAction(0 until 0, "", "abc"),
//            testAction(3 until 3, "", "___").mergeWith(
//                testAction(3 until 6, "___", "abc")))
//    }
//
//    @Test
//    fun `should merge action (replace 2)`() {
//        // "xyz" -> "_cdxyz" -> "abcdxyz"
//        assertEquals(testAction(0 until 0, "", "abcd"),
//            testAction(0 until 0, "", "_cd").mergeWith(
//                testAction(0 until 1, "_", "ab")))
//    }
//
//    @Test
//    fun `should merge action (replace 3)`() {
//        // "xyz" -> "bcyz" -> "abcdyz"
//        assertEquals(testAction(0 until 1, "", "abcd"),
//            testAction(0 until 0, "x", "bc").mergeWith(
//                testAction(0 until 2, "bc", "abcd")))
//    }
//
//    @Test
//    fun `should merge action (replace 4)`() {
//        // "xyz" -> "bdyz" -> "abcdyz"
//        assertEquals(testAction(0 until 1, "", "abcd"),
//            testAction(0 until 1, "x", "bd").mergeWith(
//                testAction(0 until 1, "b", "abc")))
//    }
//
//    @Test
//    fun `should merge action (replace 5)`() {
//        // "xyz" -> "xyac" -> "xyabcd"
//        assertEquals(testAction(0 until 1, "", "abcd"),
//            testAction(2 until 3, "z", "ac").mergeWith(
//                testAction(3 until 1, "c", "bcd")))
//    }
//
//    @Test
//    fun `should merge action (replace 6)`() {
//        // "abcdef" -> "af" -> "abcdf"
//        assertEquals(testAction(4 until 5, "e", ""),
//            testAction(1 until 5, "bcde", "").mergeWith(
//                testAction(1 until 1, "", "bcd")))
//    }
//
//    @Test
//    fun `should merge action (common sequence 1)`() {
//        // "abcdef" -> "abcxf" -> "abcxyz"
//        assertEquals(testAction(3 until 6, "def", "xyz"),
//            testAction(1 until 5, "bcde", "bcx").mergeWith(
//                testAction(2 until 5, "cxf", "cxyz")))
//    }
//
//    @Test
//    fun `should merge action (common sequence 2)`() {
//        // "abcdef" -> "axcdef" -> "axyzef"
//        assertEquals(testAction(1 until 4, "bcd", "xyz"),
//            testAction(0 until 3, "abc", "axc").mergeWith(
//                testAction(1 until 5, "xcde", "xyze")))
//    }
//
//    @Test
//    fun `should merge action (common sequence 3)`() {
//        // "abcdef" -> "abcde" -> "abcd"
//        assertEquals(testAction(4 until 6, "ef", ""),
//            testAction(3 until 6, "def", "de").mergeWith(
//                testAction(1 until 5, "bcde", "bcd")))
//    }

    private fun testAction(range: IntRange, old: String, new: String) =
        TextUndoAction.create(0, TextUndoActionType.CONTENT, range.first, range.last + 1, old, new)
}
