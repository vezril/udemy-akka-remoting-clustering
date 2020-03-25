package part2remoting
import akka.actor.{ Actor, ActorIdentity, ActorLogging, ActorRef, ActorSystem, Identify, Props }
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object RemoteActors extends App {
  val localSystem      = ActorSystem("LocalSystem", ConfigFactory.load("part2remoting/remoteActors.conf"))
  val localSimpleActor = localSystem.actorOf(Props[SimpleActor], "localSimpleActor")
  localSimpleActor ! "hello, local actor"

  // send a message to the Remote simple actor

  // Method 1: actor selection
  val remoteActorSelection = localSystem.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteSimpleActor")
  remoteActorSelection ! "Hello from the local JVM"

  // Method 2: resolve the actor selection to an actor ref
  import localSystem.dispatcher
  implicit val timeout     = Timeout(3 seconds)
  val remoteActorRefFuture = remoteActorSelection.resolveOne()
  remoteActorRefFuture.onComplete {
    case Success(actorRef) => actorRef ! "I've resolved you in a future"
    case Failure(ex)       => println(s"Failed to resolve the remote actor: $ex")
  }

  // Method 3: Actor identification via messages
  /*
    - actor resolver will ask for an actor selection from the local actor system
    - actor resolver will send an Identify(42) to the actor selection
    - the remote actor will AUTOMATICALLY respond with ActorIdentity(42, actorRef)
    - the actor resolver is free to use the remote actorRef
   */
  class ActorResolver extends Actor with ActorLogging {
    override def preStart(): Unit = {
      val selection = context.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteSimpleActor")
      selection ! Identify(42)
    }
    override def receive: Receive = {
      case ActorIdentity(42, Some(actorRef)) => {
        actorRef ! "Thank you for identifying yourself"
      }
    }
  }

  val localActorResolver = localSystem.actorOf(Props[ActorResolver], "localActorResolver")
}

object RemoteActorsRemote extends App {
  val remoteSystem =
    ActorSystem("RemoteSystem", ConfigFactory.load("part2remoting/remoteActors.conf").getConfig("remoteSystem"))
  val remoteSimpleActor = remoteSystem.actorOf(Props[SimpleActor], "remoteSimpleActor")
  remoteSimpleActor ! "hello, remote actor"
}
