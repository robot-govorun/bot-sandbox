package bot.events

import bot.events.services.telegram.TelegramClient
import bot.events.services.telegram.model.BotToken
import bot.events.services.{ReadUserInput, SendUserNotifications, UserInputInterpreter}
import cats.effect.{ConcurrentEffect, ContextShift, ExitCode}
import cats.syntax.functor._

trait Program {

  /**
   * This bot assumes that you have the following commands
   * /createevent - creates new calendar event (initiates interactive session)
   * /myevents - returns a list of all active events
   */
  def runProgram[F[_] : ConcurrentEffect : ContextShift](botToken: BotToken): F[ExitCode] = {
    (for {
      telegramClient <- TelegramClient[F](botToken)
      userInputInterpreter <- UserInputInterpreter[F]
    } yield {
      ReadUserInput.fromTelegram(telegramClient)
        .through(userInputInterpreter)
        .through(SendUserNotifications.viaTelegram(telegramClient))
    }).use(_.compile.drain).as(ExitCode.Success)
  }
}
