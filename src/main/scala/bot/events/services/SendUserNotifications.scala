package bot.events.services

import bot.events.services.telegram.TelegramClient

/**
 * Encapsulates notification rendering logic
 */
object SendUserNotifications {
  def viaTelegram[F[_]](telegramClient: TelegramClient[F]): fs2.Pipe[F, UserNotification, Unit] = { in =>
    in.evalMap(x => telegramClient.sendMessage(x.chatId, x.render))
  }
}
