package com.streann.insidead.utils

import com.streann.insidead.InsideAdSdk
import com.streann.insidead.models.AdProperties
import com.streann.insidead.models.Campaign
import com.streann.insidead.models.InsideAd
import com.streann.insidead.models.Placement
import com.streann.insidead.models.TargetingFilters
import com.streann.insidead.utils.enums.ViewType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Exercises the pure selection path. The important property is that two requests for different
 * slots produce independent results, since that is what allows the multiview canvas and right bar
 * to be on screen at the same time.
 */
class CampaignsFilterUtilTest {

    private val screen = "Multiview"

    private fun ad(id: String, duration: Int? = null) = InsideAd(
        id = id,
        name = id,
        weight = 1,
        adType = "LOCAL_IMAGE",
        url = "https://example.test/$id.png",
        properties = AdProperties(durationInSeconds = duration)
    )

    private fun placement(
        name: String,
        viewType: String?,
        ads: List<InsideAd>,
        startAfterSeconds: Int? = null,
        showCloseButtonAfterSeconds: Int? = null,
        intervalForReels: Int? = null
    ) = Placement(
        id = name,
        name = name,
        viewType = viewType,
        tags = arrayListOf(screen),
        ads = ArrayList(ads),
        properties = buildMap {
            startAfterSeconds?.let { put("startAfterSeconds", it) }
            showCloseButtonAfterSeconds?.let { put("showCloseButtonAfterSeconds", it) }
            intervalForReels?.let { put("intervalForReels", it) }
        }
    )

    private fun campaign(
        placements: List<Placement>,
        intervalInMinutes: Int? = null
    ) = Campaign(
        id = "c1",
        name = "c1",
        timePeriods = arrayListOf(),
        weight = 1,
        placements = ArrayList(placements),
        properties = intervalInMinutes?.let { mapOf<String, Number>("intervalInMinutes" to it) }
            ?: mapOf(),
        targeting = arrayListOf()
    )

    private val canvasAd = ad("canvasAd", duration = 15)
    private val rightBarAd = ad("rightBarAd", duration = 30)

    private fun campaigns() = arrayListOf(
        campaign(
            listOf(
                placement("canvas", "multiview_canvas", listOf(canvasAd), startAfterSeconds = 3),
                placement("rightBar", "MULTIVIEW_RIGHT_BAR", listOf(rightBarAd), startAfterSeconds = 9),
                placement("regular", null, listOf(ad("regularAd")))
            ),
            intervalInMinutes = 5
        )
    )

    @Test
    fun `each slot selects only its own ad`() {
        val canvas = CampaignsFilterUtil.selectAd(campaigns(), screen, ViewType.MULTIVIEW_CANVAS, null)
        val rightBar = CampaignsFilterUtil.selectAd(campaigns(), screen, ViewType.MULTIVIEW_RIGHT_BAR, null)

        assertEquals("canvasAd", canvas?.insideAd?.id)
        assertEquals("rightBarAd", rightBar?.insideAd?.id)
    }

    @Test
    fun `concurrent slot selections carry independent timings`() {
        val all = campaigns()
        // Interleave, the way two in-flight requests would.
        val canvas = CampaignsFilterUtil.selectAd(all, screen, ViewType.MULTIVIEW_CANVAS, null)
        val rightBar = CampaignsFilterUtil.selectAd(all, screen, ViewType.MULTIVIEW_RIGHT_BAR, null)

        assertEquals(3_000L, canvas?.startAfterSecondsMillis)
        assertEquals(9_000L, rightBar?.startAfterSecondsMillis)
        // The first result must not have been mutated by the second selection.
        assertEquals("canvasAd", canvas?.insideAd?.id)
    }

    @Test
    fun `campaign interval is returned per selection so slots can repeat independently`() {
        val selection = CampaignsFilterUtil.selectAd(campaigns(), screen, ViewType.MULTIVIEW_CANVAS, null)
        assertEquals(300_000L, selection?.intervalInMinutesMillis)
    }

    @Test
    fun `regular selection never returns multiview inventory`() {
        val selection = CampaignsFilterUtil.selectAd(campaigns(), screen, null, null)
        assertEquals("regularAd", selection?.insideAd?.id)
    }

    @Test
    fun `selection does not depend on the global targeting filters`() {
        InsideAdSdk.targetingFilters = TargetingFilters(vodId = "some-unrelated-vod")
        try {
            // Passing no filters must behave as untargeted regardless of the global value.
            val selection =
                CampaignsFilterUtil.selectAd(campaigns(), screen, ViewType.MULTIVIEW_CANVAS, null)
            assertNotNull(selection)
            assertEquals("canvasAd", selection?.insideAd?.id)
        } finally {
            InsideAdSdk.targetingFilters = null
        }
    }

    @Test
    fun `no inventory for a slot returns null`() {
        val onlyRegular = arrayListOf(campaign(listOf(placement("regular", null, listOf(ad("a"))))))
        assertNull(CampaignsFilterUtil.selectAd(onlyRegular, screen, ViewType.MULTIVIEW_CANVAS, null))
    }

    @Test
    fun `deprecated getInsideAd still publishes timings to the globals`() {
        @Suppress("DEPRECATION")
        val ad = CampaignsFilterUtil.getInsideAd(campaigns(), screen, "multiview_canvas")

        assertEquals("canvasAd", ad?.id)
        assertEquals(3_000L, InsideAdSdk.startAfterSeconds)
        assertEquals(300_000L, InsideAdSdk.intervalInMinutes)
    }

    @Test
    fun `zero weights do not crash selection`() {
        val zeroWeighted = arrayListOf(
            campaign(
                listOf(
                    placement(
                        "canvas", "multiview_canvas",
                        listOf(
                            ad("a").apply { weight = 0 },
                            ad("b").apply { weight = 0 }
                        )
                    )
                )
            )
        )
        assertNotNull(CampaignsFilterUtil.selectAd(zeroWeighted, screen, ViewType.MULTIVIEW_CANVAS, null))
    }

    // ---- production payload shape ---------------------------------------------------------------

    /**
     * Mirrors the live "Test Local Video Apps" campaign on the VIX la-casa account: the multiview
     * placements carry NO tags, so they only match an empty screen. Getting this wrong means the
     * slots silently never fill, which is why it is pinned here.
     */
    private fun liveShapedCampaigns(): ArrayList<Campaign> {
        fun taglessPlacement(name: String, viewType: String, adId: String) = Placement(
            id = name,
            name = name,
            viewType = viewType,
            tags = arrayListOf(), // the parser produces an empty list when "tags" is absent
            ads = arrayListOf(
                InsideAd(
                    id = adId, name = adId, weight = 100,
                    adType = "LOCAL_VIDEO", url = "https://example.test/$adId.mp4",
                    properties = AdProperties()
                )
            ),
            properties = mapOf("startAfterSeconds" to 5, "showCloseButtonAfterSeconds" to 0)
        )

        return arrayListOf(
            Campaign(
                id = "live", name = "Test Local Video Apps",
                timePeriods = arrayListOf(), weight = 100,
                placements = arrayListOf(
                    taglessPlacement("test mw canvas", "MULTIVIEW_CANVAS", "canvasVideo"),
                    taglessPlacement("test mw side menu", "MULTIVIEW_RIGHT_BAR", "sideMenuVideo")
                ),
                properties = mapOf<String, Number>("intervalInMinutes" to 3),
                targeting = arrayListOf()
            )
        )
    }

    @Test
    fun `live payload fills each multiview slot with an empty screen`() {
        val canvas =
            CampaignsFilterUtil.selectAd(liveShapedCampaigns(), "", ViewType.MULTIVIEW_CANVAS, null)
        val rightBar =
            CampaignsFilterUtil.selectAd(liveShapedCampaigns(), "", ViewType.MULTIVIEW_RIGHT_BAR, null)

        assertEquals("canvasVideo", canvas?.insideAd?.id)
        assertEquals("sideMenuVideo", rightBar?.insideAd?.id)
        assertEquals(5_000L, canvas?.startAfterSecondsMillis)
        assertEquals(180_000L, canvas?.intervalInMinutesMillis)
    }

    @Test
    fun `live payload yields nothing for a regular request`() {
        assertNull(CampaignsFilterUtil.selectAd(liveShapedCampaigns(), "", null, null))
    }

    @Test
    fun `live payload does not match a non-empty screen`() {
        assertNull(
            CampaignsFilterUtil.selectAd(
                liveShapedCampaigns(), "Multiview", ViewType.MULTIVIEW_CANVAS, null
            )
        )
    }
}
