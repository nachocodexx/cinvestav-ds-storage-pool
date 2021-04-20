package mx.cinvestav
import mx.cinvestav.config.{DefaultConfig, Node}
import org.http4s.{Header, Headers, HttpVersion, MediaType, Uri}
import fs2.Stream
import org.typelevel.ci.CIString

import java.net.URL
import java.util.UUID
import scala.collection.mutable
import scala.concurrent.ExecutionContext.global
import scala.util.Random
//
import cats.implicits._
import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
//
import org.http4s.client.blaze.{BlazeClient, BlazeClientBuilder}
import org.http4s.{HttpRoutes, Request, Response,Method}
import org.http4s.Uri._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.multipart.Multipart
import org.http4s.server.Router
import org.http4s.circe._
import org.http4s.headers._
import org.http4s.multipart._
import org.http4s.Uri.RegName
import org.http4s.circe.CirceEntityCodec._
//
import pureconfig._
//
import pureconfig.generic.auto._
//
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._


object Application extends IOApp{
  case class NodeHealthInfo(nodeId:String, status:Int, message:String, issuedAt:Long)
  case class NodeResponse(nodeId:String,node:Node,issuedAt:Long)
  case class NodeData(nodeId:String,amount:Int)
  case class NodesInfo(nodes:List[NodeData])
  case class MetadataBody(filename:String,size:Long,replicas:List[Node])
  case class MetadataResponse(fileId:String,issuedAt:Long)
//  implicit val metadataBodyEncoder =
  var counter: mutable.Map[String, Int] = mutable.Map[String,Int]()

  def buildRequest(filename:String,m:Multipart[IO],node:Node): Request[IO]#Self#Self =
    Request[IO](Method.POST,
      Uri.unsafeFromString(s"${node.url}/${node.nodeId}"))
      .withEntity(m)
//      .withHeaders(m.headers)
      .withHeaders(Headers( Header.Raw(CIString("filename"),filename)).headers.concat(m.headers.headers) )

  def services()(implicit C:DefaultConfig): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "health-check" =>
      IO.realTime.flatMap{ createdAt =>
        Ok(NodeHealthInfo(C.nodeId,1,"I'm ok :)",createdAt.toSeconds))
      }
    case GET -> Root / "nodes" =>
      val response = counter.foldLeft(List.empty[NodeData])((xs,x)=>xs:+(NodeData tupled x))
      Ok(response)

    case req@POST -> Root =>
      val clientBuilder  = BlazeClientBuilder[IO](global).resource
      req.decode[Multipart[IO]]{m =>
            clientBuilder.use{ client =>
              val node            = LoadBalancer(C.loadBalancer,counter)
              val replicasNodes   = Random.shuffle(C.nodes).filter(_.nodeId != node.nodeId).take(C.replicationFactor-1)
              val nodes           = node::replicasNodes
              val filename        = req.headers.get(CIString("filename")).map(_.head.value).getOrElse("sample")
              val size            = req.headers.get(CIString("size")).map(_.head.value).getOrElse("0").toLong
              val metadata        = MetadataBody(filename,size,nodes)
              val metadataRequest = Request[IO](Method.POST,Uri.unsafeFromString(C.metadataUrl))
                .withEntity(metadata)

              client.expect[MetadataResponse](metadataRequest).flatMap{ metadataResponse=>
                val requests        = nodes.map(buildRequest(metadataResponse.fileId,m,_))
                val responses       = requests.traverse(client.expect[Json])
//                Ok(s"OK ${metadataResponse.fileId}")
                  responses.flatMap{ nodeResponses=>
                    IO.realTime.flatMap{ issuedAt=>
                      counter.updateWith(node.nodeId)(x=>x.map(_+1))
                      Ok(NodeResponse(C.nodeId,node,issuedAt.toSeconds))
                    }
                  }
              }

//
            }
      }
  }
  def httpApp()(implicit C:DefaultConfig): Kleisli[IO, Request[IO], Response[IO]] = Router(
      s"/${C.nodeId}"-> services()
    ).orNotFound

  def runServer()(implicit C:DefaultConfig): IO[ExitCode] =
    BlazeServerBuilder[IO](global)
      .bindHttp(C.port, C.host)
      .withHttpApp(httpApp())
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  override def run(args: List[String]): IO[ExitCode] = {
    val config = ConfigSource.default.load[DefaultConfig]
    config match {
      case Left(value) =>
        println(value)
        println(value.head.description)
        IO.unit.as(ExitCode.Error)
      case Right(value) =>
        println(value)
        value.nodes.foreach(x=>counter(x.nodeId)=0)
        println(counter)
        runServer()(value)
    }
  }
}
