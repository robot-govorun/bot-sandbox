package bot.events.services.telegram.model

final case class TelegramResponse[+T](ok: Boolean, result: T)
