import pureconfig._
import pureconfig.generic.auto._
class PureConfigSpec extends munit.FunSuite {
  case class T(value:Int)
  case class Conf(name: String, age: Int,tests:List[T])
  test("test"){
    val source = ConfigSource.string("{ name = John, age = 33,tests:[{value:1}] }").load[Conf]
    println(source)

  }

}
