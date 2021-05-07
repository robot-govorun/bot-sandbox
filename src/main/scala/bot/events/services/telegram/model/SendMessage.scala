package bot.events.services.telegram.model

final case class SendMessage(chat_id: Long, text: String)
