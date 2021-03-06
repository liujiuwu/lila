package controllers

import lila._
import http.Context

import play.api._
import mvc._

import play.api.libs.concurrent._
import play.api.Play.current
import akka.dispatch.Future

object Ai extends LilaController {

  private def stockfishServer = env.ai.stockfishServer
  private def isServer = env.ai.isServer
  private implicit def executor = Akka.system.dispatcher

  def playStockfish = Action { implicit req ⇒
    implicit val ctx = Context(req, None)
    IfServer {
      Async {
        stockfishServer.play(
          pgn = getOr("pgn", ""),
          initialFen = get("initialFen"),
          level = getIntOr("level", 1)
        ).asPromise map (_.fold(
            err ⇒ BadRequest(err.shows),
            Ok(_)
          ))
      }
    }
  }

  def analyseStockfish = Action { implicit req ⇒
    implicit val ctx = Context(req, None)
    IfServer {
      Async {
        stockfishServer.analyse(
          pgn = getOr("pgn", ""),
          initialFen = get("initialFen")
        ).asPromise map (_.fold(
            err ⇒ InternalServerError(err.shows),
            analyse ⇒ Ok(analyse.encode)
          ))
      }
    }
  }

  def reportStockfish = Action { implicit req ⇒
    IfServer {
      Async {
        env.ai.stockfishServerReport.fold(
          _ map { Ok(_) },
          Future(NotFound)
        ).asPromise
      }
    }
  }

  private def IfServer(result: ⇒ Result) =
    isServer.fold(result, BadRequest("Not an AI server"))
}
