package bot.events.services.calendar

import cats.Applicative
import cats.effect.concurrent.{MVar, MVar2}
import cats.effect.{Concurrent, Resource}

/**
 * Provides data access layer
 */
trait CalendarStoreService[F[_]] {
  def createCalendarEntry(calendarEntry: CalendarEntry): F[Unit]
  def findAllEntries(userId: Long): F[List[CalendarEntry]]
}

object CalendarStoreService {

  def apply[F[_]: Concurrent]: Resource[F, CalendarStoreService[F]] = inMemory[F]

  // It can be Doobie and postgresql
  def inMemory[F[_]: Concurrent]: Resource[F, CalendarStoreService[F]] = {
    Resource.eval(MVar.of(List.empty[CalendarEntry]))
      .map(inMemoryImpl(_))
  }

  private def inMemoryImpl[F[_]: Applicative](entries: MVar2[F, List[CalendarEntry]]): CalendarStoreService[F] = new CalendarStoreService[F] {
    override def createCalendarEntry(calendarEntry: CalendarEntry): F[Unit] = {
      entries.modify_(x => Applicative[F].pure(calendarEntry :: x))
    }

    override def findAllEntries(userId: Long): F[List[CalendarEntry]] = {
      entries.use(x => Applicative[F].pure(x.filter(_.userId == userId)))
    }
  }
}