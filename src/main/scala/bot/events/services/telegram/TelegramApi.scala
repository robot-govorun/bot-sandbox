package bot.events.services.telegram

import bot.events.services.telegram.model._
import io.circe.generic.auto._
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

object TelegramApi extends TelegramCodecs {

  val sendMessage: Endpoint[(SendMessage, BotToken), Unit, Unit, Any] =
    endpoint
      .post
      .in(jsonBody[SendMessage])
      .in(path[BotToken]("botToken") / "sendMessage")

  val getUpdates: Endpoint[(GetUpdates, BotToken), Unit, TelegramResponse[List[Update]], Any] =
    endpoint
      .get
      .in(query[Option[Long]]("offset"))
      .in(query[Option[Long]]("limit"))
      .in(query[Option[Int]]("timeout"))
      .mapIn((GetUpdates.apply _).tupled)(GetUpdates.unapply(_).get)
      .in(path[BotToken]("botToken") / "getUpdates")
      .out(jsonBody[TelegramResponse[List[Update]]])
}

trait TelegramCodecs {
  private [telegram] implicit val botTokenCodec: Codec[String, BotToken, CodecFormat.TextPlain] =
    Codec.string.mapDecode[BotToken](x => if (x.length > 3) DecodeResult.Value(BotToken(x.substring(3))) else DecodeResult.InvalidValue(Nil))(x => s"bot${x.tokenValue}")
}
