package li.cil.oc.server.component

import li.cil.oc.server.network.IpNetwork
import li.cil.oc.api
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.{EnvironmentHost, Node}
import li.cil.oc.server.{PacketSender => ServerPacketSender}

import scala.collection.convert.WrapAsScala._


class IpNetworkCard(host : EnvironmentHost) extends NetworkCard(host)
{
  var ipAddress : String = "0.0.0.0"
  var macAddress : String = "00:00:00:00:00:00"

  @Callback(doc = """function() -- get the IP address for your IP Network Card""")
  def getIpAddress(context: Context, args: Arguments): Array[AnyRef] = {
    result(ipAddress)
  }

  @Callback(doc = """function() -- get the MAC address for your IP Network Card""")
  def getMacAddress(context: Context, args: Arguments): Array[AnyRef] = {
    result(macAddress)
  }

  @Callback(doc = """function() -- ask for an IP Address from the network""")
  def receiveIpAddress(context: Context, args: Arguments): Array[AnyRef] = {
    var address = IpNetwork.receiveIpAddress(node.address())
    ipAddress = address
    address = IpNetwork.receiveMacAddress(node.address())
    macAddress = address
    result(ipAddress, macAddress)
  }


  @Callback(doc = """function(address:string, port:number, data...) -- Sends data to the specified IP Address and port number.""")
  def sendViaIp(context: Context, args: Arguments): Array[AnyRef] = {
    val address = IpNetwork.uuidToIp(args.checkString(0))
    val port = checkPort(args.checkInteger(1))
    val packet = api.Network.newPacket(node.address, address, port, args.drop(2).toArray)
    doSend(packet)
    networkActivity()
    result(true)
  }

  @Callback(doc = """function() -- free the IP address associated with your IP Network Card""")
  def freeAddress(context: Context, args: Arguments): Array[AnyRef] = {
    IpNetwork.freeAddress(node.address)
    result("addresses have been freed!")
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      openPorts.clear()
    }
    IpNetwork.freeAddress(node.address)
  }

  private def networkActivity() {
    host match {
      case h: EnvironmentHost => ServerPacketSender.sendNetworkActivity(node, h)
      case _ =>
    }
  }
}