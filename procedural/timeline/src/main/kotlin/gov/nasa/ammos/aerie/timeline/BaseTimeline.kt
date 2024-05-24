package gov.nasa.ammos.aerie.timeline

import gov.nasa.ammos.aerie.timeline.payloads.IntervalLike
import gov.nasa.ammos.aerie.timeline.util.listCollector

/**
 * The basic timeline container that all higher-level timeline collections ultimately delegate to.
 *
 * Only the most extreme power-users should ever need to construct this manually.
 */
data class BaseTimeline<V: IntervalLike<V>, TL: gov.nasa.ammos.aerie.timeline.Timeline<V, TL>>(
    override val ctor: (gov.nasa.ammos.aerie.timeline.Timeline<V, TL>) -> TL,
    private val collector: (gov.nasa.ammos.aerie.timeline.CollectOptions) -> List<V>
): gov.nasa.ammos.aerie.timeline.Timeline<V, TL> {
  private var cached: List<V>? = null
  private var cachedOptions: gov.nasa.ammos.aerie.timeline.CollectOptions? = null

  override fun cache(opts: gov.nasa.ammos.aerie.timeline.CollectOptions) {
    if (cachedOptions == null || !cachedOptions!!.contains(opts)) {
      cached = collect(opts)
      cachedOptions = opts
    }
  }

  override fun collect(opts: gov.nasa.ammos.aerie.timeline.CollectOptions) =
    if (cached == null || !cachedOptions!!.contains(opts)) collector(opts)
    else ctor(gov.nasa.ammos.aerie.timeline.BaseTimeline(ctor, listCollector(cached!!))).collect(opts)

  override fun <RESULT : gov.nasa.ammos.aerie.timeline.Timeline<V, RESULT>> unsafeCast(ctor: (gov.nasa.ammos.aerie.timeline.Timeline<V, RESULT>) -> RESULT) =
      gov.nasa.ammos.aerie.timeline.BaseTimeline(ctor, collector).specialize()
}
