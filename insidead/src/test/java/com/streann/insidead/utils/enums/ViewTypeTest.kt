package com.streann.insidead.utils.enums

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewTypeTest {

    @Test
    fun `canvas resolves regardless of casing separators or padding`() {
        listOf(
            "MULTIVIEW_CANVAS",
            "multiview_canvas",
            "MultiView_Canvas",
            "  multiview_canvas  ",
            "multiview-canvas",
            "multiview canvas",
            "multiviewcanvas"
        ).forEach {
            assertEquals("failed for '$it'", ViewType.MULTIVIEW_CANVAS, ViewType.fromRaw(it))
        }
    }

    @Test
    fun `right bar resolves regardless of casing separators or padding`() {
        listOf(
            "MULTIVIEW_RIGHT_BAR",
            "multiview_right_bar",
            "MultiView_Right_Bar",
            " multiview-right-bar ",
            "multiview right bar",
            "MULTIVIEWRIGHTBAR"
        ).forEach {
            assertEquals("failed for '$it'", ViewType.MULTIVIEW_RIGHT_BAR, ViewType.fromRaw(it))
        }
    }

    @Test
    fun `preroll still resolves including lowercase`() {
        listOf("PREROLL", "preroll", " PreRoll ").forEach {
            assertEquals("failed for '$it'", ViewType.PREROLL, ViewType.fromRaw(it))
        }
    }

    @Test
    fun `absent blank and unknown values resolve to null`() {
        listOf(null, "", "   ", "\t\n", "___", "BANNER", "MIDROLL", "multiview", "canvas").forEach {
            assertNull("expected null for '$it'", ViewType.fromRaw(it))
        }
    }

    @Test
    fun `canvas and right bar are distinct`() {
        assertTrue(ViewType.fromRaw("multiview_canvas") != ViewType.fromRaw("multiview_right_bar"))
    }

    /**
     * Guard: a new view type added to the enum must be a deliberate decision about whether it is
     * reserved. If it is not dedicated it would be served to regular requestAd() traffic.
     */
    @Test
    fun `every view type is accounted for in DEDICATED_SLOTS`() {
        assertEquals(ViewType.values().toSet(), ViewType.DEDICATED_SLOTS)
    }

    @Test
    fun `isDedicatedSlot matches raw backend strings`() {
        assertTrue(ViewType.isDedicatedSlot("multiview_canvas"))
        assertTrue(ViewType.isDedicatedSlot("PREROLL"))
        assertTrue(!ViewType.isDedicatedSlot(""))
        assertTrue(!ViewType.isDedicatedSlot("SOMETHING_NEW"))
    }
}
