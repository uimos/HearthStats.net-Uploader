package net.hearthstats.ui.deckoverlay

import java.io.{ BufferedWriter, File, FileWriter }
import com.softwaremill.macwire.MacwireMacros._
import net.hearthstats.config.UserConfig
import net.hearthstats.companion.CompanionState
import net.hearthstats.game.MatchState
import net.hearthstats.hstatsapi.DeckUtils
import net.hearthstats.util.Translation
import net.hearthstats.util.TranslationConfig
import net.hearthstats.hstatsapi.CardUtils
import net.hearthstats.util.Updater
import net.hearthstats.hstatsapi.API
import net.hearthstats.ui.log.Log
import net.hearthstats.config.TestEnvironment
import net.hearthstats.companion.DeckOverlayModule
import net.hearthstats.game.HearthstoneLogMonitor
import net.hearthstats.util.FileObserver
import net.hearthstats.game.LogParser

object LogMonitorDeckOverlayMain extends App {
  val translationConfig = TranslationConfig("net.hearthstats.resources.Main", "en")
  val uiLog = wire[Log]
  val translation = wire[Translation]
  val config = wire[UserConfig]

  val api = wire[API]

  val environment = TestEnvironment
  val cardUtils = wire[CardUtils]
  val deckUtils: DeckUtils = wire[DeckUtils]

  val presenter = wire[DeckOverlaySwing]
  val tempLogFile = File.createTempFile("hssample", "log")
  val fileObserver = wire[FileObserver]
  val logParser = wire[LogParser]
  val monitor = wire[HearthstoneLogMonitor]
  val deckOverlay = wire[DeckOverlayModule]

  println(s"monitoring $tempLogFile ")
  val deck = deckUtils.getDeck(20034)

  def runIt: Thread = new Thread {
    override def run() = {
      deckOverlay.show(deck)
      deckOverlay.startMonitoringCards(1)
      val writer = new BufferedWriter(new FileWriter(tempLogFile))
      Thread.sleep(500)
      writer.write(initialHand)
      writer.flush()
      Thread.sleep(5000)
      writer.write(mulligan)
      writer.close()
      Thread.sleep(500)
    }
  }

  val runOnce = runIt
  runOnce.start()
  runOnce.join()
  val runSecond = runIt
  runSecond.start()

  def initialHand =
    """
[Zone] ZoneChangeList.ProcessChanges() - id=2 local=False [name=Soulfire id=17 zone=HAND zonePos=0 cardId=EX1_308 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
[Zone] ZoneChangeList.ProcessChanges() - id=2 local=False [name=Sunfury Protector id=31 zone=HAND zonePos=0 cardId=EX1_058 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
[Zone] ZoneChangeList.ProcessChanges() - id=2 local=False [name=Twilight Drake id=13 zone=HAND zonePos=0 cardId=EX1_043 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
    """

  def mulligan =
    """
[Zone] ZoneChangeList.ProcessChanges() - id=4 local=False [name=Soulfire id=17 zone=DECK zonePos=1 cardId=EX1_308 player=1] zone from FRIENDLY HAND -> FRIENDLY DECK
[Zone] ZoneChangeList.ProcessChanges() - id=4 local=False [name=Sunfury Protector id=31 zone=DECK zonePos=2 cardId=EX1_058 player=1] zone from FRIENDLY HAND -> FRIENDLY DECK
[Zone] ZoneChangeList.ProcessChanges() - id=4 local=False [name=Twilight Drake id=13 zone=DECK zonePos=3 cardId=EX1_043 player=1] zone from FRIENDLY HAND -> FRIENDLY DECK

[Zone] ZoneChangeList.ProcessChanges() - id=4 local=False [name=Ancient Watcher id=20 zone=HAND zonePos=0 cardId=EX1_045 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
[Zone] ZoneChangeList.ProcessChanges() - id=4 local=False [name=Twilight Drake id=13 zone=HAND zonePos=3 cardId=EX1_043 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
    """
}