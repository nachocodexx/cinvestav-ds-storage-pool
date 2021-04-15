package mx.cinvestav

import mx.cinvestav.config.{DefaultConfig, Node}

import scala.util.Random

object LoadBalancer {
  private val rand = new Random()
  def apply(x:String,counter:collection.mutable.Map[String,Int])(implicit C:DefaultConfig): Node = {
    val total = counter.foldLeft(0)((x, y) => y._2 + x)
    x match {
      case "RB" => roundRobin(total)
      case "RND" => random()
      case "2C" => twoChoices(counter)
      case _ => roundRobin(total)
    }
  }
  def roundRobin(counter:Int)(implicit C:DefaultConfig): Node = C.nodes(counter % C.nodes.length)
  def random()(implicit C:DefaultConfig): Node = C.nodes(rand.nextInt(C.nodes.length))
  def twoChoices(counter:collection.mutable.Map[String,Int])(implicit C:DefaultConfig): Node = {
    val nodesLen = C.nodes.length
    val a =C.nodes(rand.nextInt(nodesLen))
    val b =C.nodes(rand.nextInt(nodesLen))
    if (counter(a.nodeId) < counter(b.nodeId)) a else b
  }


}
