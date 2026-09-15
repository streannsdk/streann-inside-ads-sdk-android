package com.streann.insidead.utils

import com.streann.insidead.models.Placement
import com.streann.insidead.utils.enums.ViewType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the placement predicate that decides which inventory a given ad request may serve.
 * The headline case is that a regular requestAd() must never receive dedicated-slot inventory.
 */
class PlacementFilterTest {

    private val screen = "Video Player"

    private fun placement(name: String, viewType: String?, tag: String = screen) =
        Placement(
            id = name,
            name = name,
            viewType = viewType,
            tags = arrayListOf(tag),
            ads = arrayListOf(),
            properties = mapOf()
        )

    private fun filter(placements: List<Placement>, viewType: ViewType?) =
        CampaignsFilterUtil.getFilteredPlacements(ArrayList(placements), screen, viewType)

    private fun names(placements: List<Placement>?) = placements?.map { it.name }?.toSet()

    // ---- the headline regression -------------------------------------------------------------

    @Test
    fun `regular request never serves preroll or multiview inventory in any casing`() {
        val placements = listOf(
            placement("regular", null),
            placement("regularEmpty", ""),
            placement("preroll", "PREROLL"),
            placement("prerollLower", "preroll"),
            placement("canvas", "MULTIVIEW_CANVAS"),
            placement("canvasLower", "multiview_canvas"),
            placement("rightBar", "MULTIVIEW_RIGHT_BAR"),
            placement("rightBarLower", "multiview_right_bar")
        )

        assertEquals(setOf("regular", "regularEmpty"), names(filter(placements, null)))
    }

    @Test
    fun `canvas request serves only canvas inventory`() {
        val placements = listOf(
            placement("regular", null),
            placement("preroll", "PREROLL"),
            placement("canvas", "multiview_canvas"),
            placement("rightBar", "multiview_right_bar")
        )

        assertEquals(setOf("canvas"), names(filter(placements, ViewType.MULTIVIEW_CANVAS)))
    }

    @Test
    fun `right bar request serves only right bar inventory`() {
        val placements = listOf(
            placement("regular", null),
            placement("preroll", "PREROLL"),
            placement("canvas", "MULTIVIEW_CANVAS"),
            placement("rightBar", "MULTIVIEW_RIGHT_BAR")
        )

        assertEquals(setOf("rightBar"), names(filter(placements, ViewType.MULTIVIEW_RIGHT_BAR)))
    }

    @Test
    fun `preroll request is unchanged and excludes multiview inventory`() {
        val placements = listOf(
            placement("regular", null),
            placement("preroll", "PREROLL"),
            placement("canvas", "MULTIVIEW_CANVAS")
        )

        assertEquals(setOf("preroll"), names(filter(placements, ViewType.PREROLL)))
    }

    // ---- unknown / absent values -------------------------------------------------------------

    @Test
    fun `unrecognised view types stay eligible for regular requests only`() {
        val placements = listOf(
            placement("nullType", null),
            placement("emptyType", ""),
            placement("blankType", "   "),
            placement("futureType", "SOMETHING_NEW")
        )

        assertEquals(
            setOf("nullType", "emptyType", "blankType", "futureType"),
            names(filter(placements, null))
        )
        assertTrue(filter(placements, ViewType.MULTIVIEW_CANVAS)!!.isEmpty())
        assertTrue(filter(placements, ViewType.PREROLL)!!.isEmpty())
    }

    // ---- screen / tag matching is preserved ---------------------------------------------------

    @Test
    fun `screen tag must also match`() {
        val placements = listOf(
            placement("rightScreen", "multiview_canvas", tag = screen),
            placement("wrongScreen", "multiview_canvas", tag = "Splash")
        )

        assertEquals(setOf("rightScreen"), names(filter(placements, ViewType.MULTIVIEW_CANVAS)))
    }

    @Test
    fun `empty screen matches only placements with no tags`() {
        val untagged = Placement(id = "untagged", name = "untagged", viewType = null, tags = arrayListOf())
        val tagged = placement("tagged", null)

        val result = CampaignsFilterUtil.getFilteredPlacements(arrayListOf(untagged, tagged), "", null)
        assertEquals(setOf("untagged"), names(result))
    }

    @Test
    fun `a canvas placement tagged for a screen is excluded from that screens regular request`() {
        val placements = listOf(placement("canvasOnReels", "multiview_canvas", tag = "Reels"))
        val result = CampaignsFilterUtil.getFilteredPlacements(ArrayList(placements), "Reels", null)
        assertTrue(result!!.isEmpty())
    }

    // ---- degenerate inputs --------------------------------------------------------------------

    @Test
    fun `null placements returns null and empty returns empty`() {
        assertNull(CampaignsFilterUtil.getFilteredPlacements(null, screen, null))
        assertTrue(CampaignsFilterUtil.getFilteredPlacements(arrayListOf(), screen, null)!!.isEmpty())
    }
}
