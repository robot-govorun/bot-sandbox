package bot.events.services.telegram


import bot.events.services.telegram.model.{BotToken, GetUpdates, SendMessage, Update}
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Resource}
import cats.syntax.functor._
import fs2.Chunk
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import sttp.tapir.client.http4s.Http4sClientInterpreter

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

trait TelegramClient[F[_]] {
  def sendMessage(chatId: Long, messageText: String): F[Unit]
  def fetchUpdates(lastOffset: Option[Long]): fs2.Stream[F, Update]
}

object TelegramClient {

  def apply[F[_] : ConcurrentEffect : ContextShift](botToken: BotToken): Resource[F, TelegramClient[F]] = {
    BlazeClientBuilder[F](ExecutionContext.global)
      .resource.map(telegramServiceImpl(botToken, _))
  }

  private def telegramServiceImpl[F[_] : ConcurrentEffect : ContextShift](botToken: BotToken, client: Client[F]): TelegramClient[F] = new TelegramClient[F] {
    private val telegramBaseUri = "https://api.telegram.org"
    private val tapirInterpreter = Http4sClientInterpreter[F]
    private implicit val blocker: Blocker = Blocker.liftExecutorService(Executors.newSingleThreadExecutor())

    private val sendMessageF = tapirInterpreter.toRequestUnsafe(TelegramApi.sendMessage, Some(telegramBaseUri))
    private val fetchUpdatesF = tapirInterpreter.toRequestUnsafe(TelegramApi.getUpdates, Some(telegramBaseUri))

    override def sendMessage(chatId: Long, messageText: String): F[Unit] = {
      val (req, respF) = sendMessageF.apply(SendMessage(chatId, messageText) -> botToken)
      client.run(req).use(x => respF(x).map(_.merge))
    }

    override def fetchUpdates(lastOffset: Option[Long]): fs2.Stream[F, Update] = {
      fs2.Stream.eval(fetchUpdatesOne(lastOffset, Some(100))).flatMap { updates =>
        val chunk = Chunk.seq(updates)
        fs2.Stream.chunk(chunk) ++ fetchUpdates(chunk.last.map(_.update_id + 1L).orElse(lastOffset))
      }
    }

    private def fetchUpdatesOne(offset: Option[Long], timeout: Option[Int]): F[List[Update]] = {
      val (req, respF) = fetchUpdatesF.apply(GetUpdates(offset, None, timeout) -> botToken)
      client.run(req).use(x => respF(x).map(_.map(_.result).getOrElse(Nil)))
    }
  }
}
