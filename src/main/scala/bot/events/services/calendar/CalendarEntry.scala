package bot.events.services.calendar

import java.time.LocalDateTime

case class CalendarEntry(id: String, userId: Long, title: String, when: LocalDateTime)
