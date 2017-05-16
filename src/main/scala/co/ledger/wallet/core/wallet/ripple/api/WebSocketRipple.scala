package co.ledger.wallet.core.wallet.ripple.api

import co.ledger.wallet.core.device.utils.EventEmitter
import co.ledger.wallet.core.net.{WebSocket, WebSocketFactory}
import co.ledger.wallet.core.wallet.ripple.events.{NewBlock, NewTransaction}
import co.ledger.wallet.web.ripple.wallet.RippleWalletClient
import org.json.JSONObject

import scala.scalajs.js.timers
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.timers.setTimeout

/**
  * Created by alix on 5/2/17.
  */
class WebSocketRipple(factory: WebSocketFactory,
                      addresses: Array[String],
                     wallet: RippleWalletClient) {

  def start(): Unit = {
    if (_socket.isEmpty) {
      connect()

    }
  }

  private def connect(): Unit = {
    println("connectiong socket")
    _socket  = Some(factory.connect("wss://s1.ripple.com"))
    _socket.get onComplete {
      case Success(ws) =>
        println("success socket")
        val subscribeMessage = js.Dynamic.literal(
          command = "subscribe",
          accounts = js.Array(addresses(0))) //TODO: change in case of multi account
        ws.send(js.JSON.stringify(subscribeMessage))
        ws.onJsonMessage(onMessage _)
        ws onClose {(ex) =>
          println("close websocket")
          ex.printStackTrace()
          if (isRunning)
            connect()
        }
      case Failure(ex) =>
        println("failure websocket")
        if (isRunning)
          connect()
    }
  }

  private def onMessage(json: JSONObject): Unit = {
    println("websocket triggered")
    if (json.getString("type") == "transaction") {
      setTimeout(1000) {
        wallet.synchronize()
      }
    }
  }

  def stop(): Unit = {
    _socket foreach {(s) =>
      _socket = None
      s.foreach(_.close())
    }
  }

  def isRunning = _socket.isDefined

  private var _socket: Option[Future[WebSocket]] = None
}