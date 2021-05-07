package bot.events.services

import bot.events.services.calendar.CalendarStoreService
import cats.data.ReaderT
import cats.effect.concurrent.{MVar, MVar2}
import cats.effect.{Concurrent, Resource, Sync}
import cats.syntax.functor._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object UserInputInterpreter {

  // userInput ~> ((interpreter)) ~> userNotification
  type UserInputInterpreter[F[_]] = fs2.Pipe[F, UserInput, UserNotification]

  def apply[F[_] : Concurrent]: Resource[F, UserInputInterpreter[F]] = {
    for {
      sessionStore <- Resource.eval(SessionStore[F])
      calendarService <- CalendarStoreService[F]
    } yield interpreterImpl(calendarService, sessionStore)
  }

  private def interpreterImpl[F[_] : Sync](calendarService: CalendarStoreService[F], states: MVar2[F, Map[Long, SessionState]]): UserInputInterpreter[F] =
    _.evalMap { input =>
      states.modify { currentStates =>
        userSessionFsm(currentStates.getOrElse(input.chatId, Empty), input).run(calendarService)
          .map {
            case (Empty, n) => (currentStates - input.chatId) -> n
            case (nonEmpty, n) => (currentStates + (input.chatId -> nonEmpty)) -> n
          }
      }
    }

  sealed trait SessionState
  sealed trait CreatingCalendarSessionState extends SessionState
  case object Empty extends SessionState
  case object ExpectTitle extends SessionState with CreatingCalendarSessionState
  case class ExpectDate(title: String) extends SessionState with CreatingCalendarSessionState

  /**
   * Stores current interaction state for all users
   */
  type SessionStore[F[_]] = MVar2[F, Map[Long, SessionState]]
  object SessionStore {
    def apply[F[_] : Concurrent]: F[SessionStore[F]] = MVar.of(Map.empty)
  }

  type UserSessionFsm[F[_]] = ReaderT[F, CalendarStoreService[F], (SessionState, UserNotification)]

  // The simplest possible interpreter. Could be decomposed into series of independent handlers per Command
  private def userSessionFsm[F[_] : Sync](state: SessionState, ui: UserInput): UserSessionFsm[F] = ReaderT { cal =>
    ui.userInput match {
      case UserCommand.GetEvents =>
        // TODO: Delegation notification rendering logic downstream
        cal.findAllEntries(ui.chatId).map(x => state -> UserNotification(ui.chatId, x.mkString("[", ",", "]")))
      case UserCommand.CreateEvent =>
        Sync[F].pure(ExpectTitle, UserNotification(ui.chatId, "Please enter title"))
      case UserCommand.Text(text) =>
        state match {
          case ExpectTitle =>
            Sync[F].pure(ExpectDate(text) -> UserNotification(ui.chatId, "Please enter date time in format 'YYYY-MM-DDTHH:mm'"))
          case ExpectDate(title) =>
            cal.createCalendarEntry(
              calendar.CalendarEntry(
                UUID.randomUUID().toString,
                ui.chatId,
                title,
                LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
              )
            ).as(Empty -> UserNotification(ui.chatId, "New calendar event has been created"))
          case _ =>
            Sync[F].pure(state -> UserNotification(ui.chatId, "User command has not been recognized"))
        }
    }
  }
}
