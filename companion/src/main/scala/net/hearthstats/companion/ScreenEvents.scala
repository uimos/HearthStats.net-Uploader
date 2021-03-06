package net.hearthstats.companion

import java.awt.image.BufferedImage
import scala.util.control.NonFatal
import grizzled.slf4j.Logging
import net.hearthstats.game.GameEvents.screenToObject
import net.hearthstats.game.Screen
import net.hearthstats.game.Screen.FINDING_OPPONENT
import net.hearthstats.game.Screen.PLAY_LOBBY
import net.hearthstats.game.ScreenEvent
import net.hearthstats.game.imageanalysis.IndividualPixelAnalyser
import net.hearthstats.game.imageanalysis.ScreenAnalyser
import net.hearthstats.game.imageanalysis.UniquePixel
import net.hearthstats.ProgramHelper
import net.hearthstats.util.ActorObservable
import scala.collection.mutable.Buffer
import net.hearthstats.modules.video.TimedImage
import net.hearthstats.modules.video.TimedImage

class ScreenEvents(
  companionState: CompanionState,
  individualPixelAnalyser: IndividualPixelAnalyser,
  screenAnalyser: ScreenAnalyser) extends ActorObservable with Logging { self =>

  def handleImage(bi: BufferedImage): Unit =
    if (bi != null) {
      import companionState._
      addImage(bi)
      if (iterationsSinceScreenMatched > 10) { lastScreen = None }
      Option(screenAnalyser.identifyScreen(bi, lastScreen.getOrElse(null))) match {
        case Some(screen) =>
          iterationsSinceScreenMatched = 0
          eventFromScreen(screen, bi) match {
            case Some(e) =>
              debug(s"screen $screen => event $e")
              self.notify(e)
            case _ =>
          }
        case None =>
          debug(s"no screen match on image, last match was $lastScreen $iterationsSinceScreenMatched iterations ago")
          iterationsSinceScreenMatched += 1
      }
    }

  private def eventFromScreen(newScreen: Screen, image: BufferedImage): Option[ScreenEvent] = {
    import companionState._
    import net.hearthstats.game.GameEvents._

    if (newScreen == PLAY_LOBBY && individualPixelAnalyser.testAllPixelsMatch(image, UniquePixel.allBackgroundPlay))
      //      Sometimes the OS X version captures a screenshot where, apparently, Hearthstone hasn't finished compositing the screen
      //    and so we only get the background. This can happen whenever there is something layered over the main screen, for example
      //    during the 'Finding Opponent', 'Victory' and 'Defeat' screens.</p>
      //   At the moment I haven't worked out how to ensure we always get the completed screen. So this method detects when
      //    we've received and incomplete play background instead of the 'Finding Opponent' screen, so we can reject it and try again.</p>
      None
    else
      Some(ScreenEvent(newScreen, image))

  }
}