package bot.events.services

sealed trait UserCommand

object UserCommand {
  case object CreateEvent extends UserCommand
  case object GetEvents extends UserCommand
  case class Text(text: String) extends UserCommand
}
