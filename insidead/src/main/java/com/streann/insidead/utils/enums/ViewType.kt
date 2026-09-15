package com.streann.insidead.utils.enums

import java.util.Locale

/**
 * The placement "slot" an ad belongs to, as sent by the backend in `Placement.viewType`.
 *
 * This is orthogonal to [AdType]: [AdType] decides *how* an ad renders (VAST, banner, image...),
 * [ViewType] decides *where* it is eligible to be served. An ad of any [AdType] can fill any slot.
 */
enum class ViewType(val value: String) {
    PREROLL("PREROLL"),
    MULTIVIEW_CANVAS("MULTIVIEW_CANVAS"),
    MULTIVIEW_RIGHT_BAR("MULTIVIEW_RIGHT_BAR");

    companion object {

        /**
         * Slots that are reserved for a dedicated ad request and must NEVER be served to a plain
         * [com.streann.insidead.InsideAdView.requestAd] call.
         *
         * Every new dedicated view type must be added here; forgetting to do so silently leaks
         * that inventory into regular ad traffic.
         */
        val DEDICATED_SLOTS: Set<ViewType> =
            setOf(PREROLL, MULTIVIEW_CANVAS, MULTIVIEW_RIGHT_BAR)

        /**
         * Backend casing is not contractual, so comparisons are normalised rather than exact:
         * case, whitespace, underscores and hyphens are all ignored. "MULTIVIEW_CANVAS",
         * "multiview_canvas", " Multiview-Canvas " and "multiview canvas" all canonicalize alike.
         */
        private fun canonicalize(raw: String?): String? =
            raw?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.uppercase(Locale.ROOT)
                ?.replace(NON_ALPHANUMERIC, "")
                ?.takeIf { it.isNotEmpty() }

        private val NON_ALPHANUMERIC = Regex("[^A-Z0-9]")

        /**
         * Resolves a raw backend value to a known [ViewType].
         *
         * Returns null for null, empty, blank and unrecognised values. The parser writes "" when
         * the key is absent (see HttpRequestsUtil), so all of those collapse to "no dedicated
         * slot", which keeps such placements eligible for regular ad requests.
         */
        fun fromRaw(raw: String?): ViewType? {
            val key = canonicalize(raw) ?: return null
            return values().firstOrNull { canonicalize(it.value) == key }
        }

        /** True when the raw value names a slot reserved for a dedicated ad request. */
        fun isDedicatedSlot(raw: String?): Boolean = fromRaw(raw) in DEDICATED_SLOTS
    }
}
