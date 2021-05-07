package bot.events.services

import bot.events.services.telegram.TelegramClient

/**
 * Transforms telegram updates into domain specific set of commands
 */
object ReadUserInput {

  def fromTelegram[F[_]](telegramClient: TelegramClient[F]): fs2.Stream[F, UserInput] = {
    telegramClient
      .fetchUpdates(None)
      .map { u =>
        u.message.text.map {
          case "/myevents" => UserCommand.GetEvents
          case "/createevent" => UserCommand.CreateEvent
          case text => UserCommand.Text(text)
        }.map(UserInput(u.message.chat.id, u.message.from.map(_.id), _))
      }.unNone
  }
}
