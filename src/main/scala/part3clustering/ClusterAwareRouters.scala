package part3clustering
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.routing.FromConfig
import com.typesafe.config.ConfigFactory

case class SimpleTask(contents: String)
case object StartWork

class MasterWithRouter extends Actor with ActorLogging {

  val router = context.actorOf(FromConfig.props(Props[SimpleRoutee]), "clusterAwareRouter")
  override def receive: Receive = {
    case StartWork => {
      log.info("Starting work")
      (1 to 100).foreach { id =>
        router ! SimpleTask(s"Simple Task $id")
      }
    }
  }
}

class SimpleRoutee extends Actor with ActorLogging {
  override def receive: Receive = {
    case SimpleTask(contents) => {
      log.info(s"Processing $contents")
    }
  }
}

object RouteesApp extends App {
  def startRouteeNode(port: Int) = {
    val config = ConfigFactory.parseString(
      s"""
        |akka.remote.artery.canonical.port = $port
      """.stripMargin)
      .withFallback(ConfigFactory.load("part3clustering/ClusterAwareRouters.conf"))

    val system = ActorSystem("RTJVMCluster", config)
  }

  startRouteeNode(2551)
  startRouteeNode(2552)
}

object MasterWithRouter extends App {
  val mainConfig = ConfigFactory.load("part3clustering/ClusterAwareRouters.conf")
  val config = mainConfig.getConfig("masterWithRouterApp").withFallback(mainConfig)
  val system = ActorSystem("RTJVMCluster", config)

  val masterActor = system.actorOf(Props[MasterWithRouter], "mater")

  Thread.sleep(1000)
  masterActor ! StartWork
}