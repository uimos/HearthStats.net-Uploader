package net.hearthstats.hstatsapi

import java.io.IOException

import net.hearthstats.core.{ Card, Deck, HeroClass }
import net.hearthstats.ui.log.Log
import org.apache.commons.lang3.StringUtils
import org.json.simple.JSONObject

import scala.collection.JavaConversions.seqAsJavaList

class DeckUtils(api: API, uiLog: Log, cardUtils: CardUtils) {

  private var _decks: List[JSONObject] = _

  def updateDecks() {
    try {
      _decks = api.getDecks
      if (_decks.isEmpty) {
        uiLog.warn("no deck were returned from Hearthstats.net. Either you have not created a deck or the site is down")
      }
    } catch {
      case e: IOException => uiLog.warn("Error occurred while loading deck list from HearthStats.net", e)
    }
  }

  def getDeckFromSlot(slotNum: java.lang.Integer): Option[Deck] = {
    getDecks
    for (
      i <- 0 until _decks.size if _decks.get(i).get("slot") != null &&
        _decks.get(i).get("slot").toString == slotNum.toString
    ) return Some(fromJson(_decks.get(i)))
    None
  }

  def getDecks: List[JSONObject] = {
    if (_decks == null) updateDecks()
    _decks
  }

  def getDeckLists: List[Deck] =
    for (deck <- getDecks)
      yield fromJson(deck)

  def getDeck(id: Int): Deck =
    getDeckLists.find(_.id == id) match {
      case Some(d) => d
      case None => throw new IllegalArgumentException("No deck found for id " + id)
    }

  def fromJson(json: JSONObject): Deck = {
    val id = Integer.parseInt(json.get("id").toString)
    val cardList: List[Card] =
      Option(json.get("cardstring")) match {
        case Some(cs) if StringUtils.isNotBlank(cs.toString) =>
          parseCardString(cs.toString.trim)
        case _ => Nil
      }

    val klassId = json.get("klass_id")
    val heroString = if (klassId == null) "" else HeroClass.stringWithId(klassId.toString.toInt)

    Deck(id = id,
      slug = json.get("slug").toString,
      name = json.get("name").toString,
      cards = cardList,
      hero = HeroClass.byName(heroString),
      activeSlot = Option(json.get("slot")).map(_.toString.toInt))
  }

  def parseCardString(ds: String): List[Card] = {
    val cardData = cardUtils.cards
    val cards = for {
      card <- ds.split(",").toList
      u = card.indexOf('_')
      count = card.substring(u + 1)
      id = Integer.parseInt(card.substring(0, u))
      cd = cardData(id)
    } yield cd.copy(count = Integer.parseInt(count))
    cards.sorted
  }

  def parseDeckString(ds: String): List[Card] = {
    val cardMap = cardUtils.cards.values.map(c => c.name -> c).toMap
    val cards = for {
      card <- ds.split("\n").toList
      count = try {
        Integer.parseInt(card.substring(0, 1))
      } catch {
        case e: NumberFormatException => 0
      }
      cd = cardMap.get(card.substring(2))
    } yield cd match {
      case Some(c) =>
        c.copy(count = count)
      case None =>
        // If the card is invalid, then return a made-up card. This might be better implemented as a different class
        // to indicate that the card couldn't be identified
        new Card(id = 0, originalName = card, collectible = false)
    }
    cards.sorted
  }

}
