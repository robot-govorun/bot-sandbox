package bot.events

import bot.events.services.telegram.model.BotToken
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp with Program {
  override def run(args: List[String]): IO[ExitCode] = runProgram[IO](BotToken(sys.env("BOT_TOKEN")))
}

