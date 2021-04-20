package mx.cinvestav.config

case class Node(nodeId:String,url:String)
case class DefaultConfig(nodeId:String,host:String,port:Int,nodes:List[Node],loadBalancer:String,
                         replicationFactor:Int,metadataUrl:String)
