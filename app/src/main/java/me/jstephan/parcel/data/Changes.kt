package me.jstephan.parcel.data

/** What changed between two checks, from each delivery's [Delivery.signature]. */
object Changes {
    /**
     * The deliveries Parcel has news about: a new status or a new latest event, or a delivery that wasn't there
     * before (Parcel also adds deliveries by itself, from e-mails). Nothing on the very first check ([seen] null),
     * or every delivery already there would count as news.
     */
    fun detect(seen: Map<String, String>?, deliveries: List<Delivery>): List<Delivery> =
        if (seen == null) emptyList() else deliveries.filter { seen[it.key] != it.signature }

    fun signatures(deliveries: List<Delivery>): Map<String, String> = deliveries.associate { it.key to it.signature }
}
