package gov.nasa.jpl.aerie.timeline.payloads.activities

/** An activity ID, referencing either a directive or instance. */
sealed interface ActivityId {
  /***/ data class InstanceId(/***/ val id: Long): ActivityId
  /***/ data class DirectiveId(/***/ val id: Long): ActivityId
}
