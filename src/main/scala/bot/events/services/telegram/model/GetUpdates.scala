package bot.events.services.telegram.model

final case class GetUpdates(offset: Option[Long], limit: Option[Long], timeout: Option[Int])
