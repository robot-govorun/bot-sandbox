package bot.events.services.telegram.model

case class Message(message_id: Long, chat: Chat, from: Option[User], text: Option[String], entities: Option[List[MessageEntity]])
