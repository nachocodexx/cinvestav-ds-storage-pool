import mx.cinvestav.LoadBalancer
import pureconfig.generic.auto._
import mx.cinvestav.config.DefaultConfig
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
//import mx.cinvestav.config.
class LoadBalancerSpec extends munit .FunSuite {
  private implicit val cfg: DefaultConfig = ConfigSource.default.load[DefaultConfig].getOrElse(DefaultConfig("","",0,
    Nil,"",1,""))
  val values: Seq[Int] = 0 until 99
  test("aa"){
    println(cfg)
  }
  test("Load balancer: RB"){

    val counter = scala.collection.mutable.Map("sn-00"->0,"sn-01"->0,"sn-02"->0)
    values.foreach{ i=>
      val node = LoadBalancer("RB",counter)
      counter.updateWith(node.nodeId)(_.map(1+_))
    }
    println(counter)
    assertEquals(true, true)
  }
  test("Load balancer: RND"){
    val counter = scala.collection.mutable.Map("sn-00"->0,"sn-01"->0,"sn-02"->0)
    values.foreach{ i=>
      val node = LoadBalancer("RND",counter)
      counter.updateWith(node.nodeId)(_.map(1+_))
    }
    println(counter)
    assertEquals(true, true)
  }
  test("Load balancer: 2C"){
    val counter = scala.collection.mutable.Map("sn-00"->0,"sn-01"->0,"sn-02"->0)
    values.foreach{ i=>
      val node = LoadBalancer("2C",counter)
      counter.updateWith(node.nodeId)(_.map(1+_))
    }
    println(counter)
    assertEquals(true, true)
  }


}
