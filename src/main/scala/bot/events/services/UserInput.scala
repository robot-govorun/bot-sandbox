package bot.events.services

final case class UserInput(chatId: Long, userId: Option[Long], userInput: UserCommand)
