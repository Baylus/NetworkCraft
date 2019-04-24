package li.cil.oc.server.component

import java.util

import com.google.common.base.Charsets
import li.cil.oc.Constants
import li.cil.oc.common.Tier
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.component.RackBusConnectable
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.internal.Rack
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network._
import li.cil.oc.server.network.IpNetwork
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.nbt._

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class IpNetworkCard(host : EnvironmentHost) extends NetworkCard(host)
{
  val ipAddress : String = "0.0.0.0"
  val macAddress : String = "00:00:00:00"

  @Callback(doc = """function() -- get the IP address for your IP Network Card""")
  def getIpAddress(context: Context, args: Arguments): Array[AnyRef] = {
    result(IpNetwork.getIpAddress(node))

  }

  @Callback(doc = """function() -- get the MAC address for your IP Network Card""")
  def getMacAddress(context: Context, args: Arguments): Array[AnyRef] = {
    result(macAddress)
  }

  @Callback(doc = """function() -- ask for an IP Address from the network""")
  def retrieveIpAddress(context: Context, args: Arguments): Array[AnyRef] = {
    result(ipAddress)
  }
}

/*
class IpNetworkCard(host : EnvironmentHost) extends prefab.ManagedEnvironment with RackBusConnectable with DeviceInfo with traits.WakeMessageAware {
  val ipAddress : String = ""

  def getIpAddress(): String = {
    ipAddress
  }
  protected val visibility = host match {
    case _: Rack => Visibility.Neighbors
    case _ => Visibility.Network
  }

  override val node = Network.newNode(this, visibility).
    withComponent("modem", Visibility.Neighbors).
    create()

  protected val openPorts = mutable.Set.empty[Int]

  // wired network card is the 1st in the max ports list (before both wireless cards)
  protected def maxOpenPorts = Settings.get.maxOpenPorts(Tier.One)

  // ----------------------------------------------------------------------- //

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Network,
    DeviceAttribute.Description -> "Ethernet controller with IP/MAC addresses",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "UI001",
    DeviceAttribute.Version -> "1.0",
    DeviceAttribute.Capacity -> Settings.get.maxNetworkPacketSize.toString,
    DeviceAttribute.Size -> maxOpenPorts.toString,
    DeviceAttribute.Width -> Settings.get.maxNetworkPacketParts.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(port:number):boolean -- Opens the specified port. Returns true if the port was opened.""")
  def open(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    if (openPorts.contains(port)) result(false)
    else if (openPorts.size >= maxOpenPorts) {
      throw new java.io.IOException("too many open ports")
    }
    else result(openPorts.add(port))
  }



  @Callback(doc = """function([port:number]):boolean -- Closes the specified port (default: all ports). Returns true if ports were closed.""")
  def close(context: Context, args: Arguments): Array[AnyRef] = {
    if (args.count == 0) {
      val closed = openPorts.nonEmpty
      openPorts.clear()
      result(closed)
    }
    else {
      val port = checkPort(args.checkInteger(0))
      result(openPorts.remove(port))
    }
  }

  @Callback(direct = true, doc = """function(port:number):boolean -- Whether the specified port is open.""")
  def isOpen(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    result(openPorts.contains(port))
  }

  @Callback(direct = true, doc = """function():boolean -- Whether this card has wireless networking capability.""")
  def isWireless(context: Context, args: Arguments): Array[AnyRef] = result(false)

  @Callback(direct = true, doc = """function():boolean -- Whether this card has wired networking capability.""")
  def isWired(context: Context, args: Arguments): Array[AnyRef] = result(true)

  @Callback(doc = """function(address:string, port:number, data...) -- Sends the specified data to the specified target.""")
  def send(context: Context, args: Arguments): Array[AnyRef] = {
    val address = args.checkString(0)
    val port = checkPort(args.checkInteger(1))
    val packet = api.Network.newPacket(node.address, address, port, args.drop(2).toArray)
    doSend(packet)
    networkActivity()
    result(true)
  }

  @Callback(doc = """function(port:number, data...) -- Broadcasts the specified data on the specified port.""")
  def broadcast(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    val packet = api.Network.newPacket(node.address, null, port, args.drop(1).toArray)
    doBroadcast(packet)
    networkActivity()
    result(true)
  }

  //Removed in MC 1.11
  @Callback(direct = true, doc = """function():number -- Gets the maximum packet size (config setting).""")
  def maxPacketSize(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.maxNetworkPacketSize)

  protected def doSend(packet: Packet) = visibility match {
    case Visibility.Neighbors => node.sendToNeighbors("network.message", packet)
    case Visibility.Network => node.sendToReachable("network.message", packet)
    case _ => // Ignore.
  }

  protected def doBroadcast(packet: Packet) = visibility match {
    case Visibility.Neighbors => node.sendToNeighbors("network.message", packet)
    case Visibility.Network => node.sendToReachable("network.message", packet)
    case _ => // Ignore.
  }

  // ----------------------------------------------------------------------- //

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      openPorts.clear()
    }
  }

  override def onMessage(message: Message) = {
    super.onMessage(message)
    if ((message.name == "computer.stopped" || message.name == "computer.started") && node.isNeighborOf(message.source))
      openPorts.clear()
    if (message.name == "network.message") message.data match {
      case Array(packet: Packet) => receivePacket(packet)
      case _ =>
    }
  }

  override protected def isPacketAccepted(packet: Packet, distance: Double): Boolean = {
    if (super.isPacketAccepted(packet, distance)) {
      if (openPorts.contains(packet.port)) {
        networkActivity()
        return true
      }
    }
    false
  }

  override def receivePacket(packet: Packet): Unit = receivePacket(packet, 0, host)

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    assert(openPorts.isEmpty)
    openPorts ++= nbt.getIntArray("openPorts")
    loadWakeMessage(nbt)
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setIntArray("openPorts", openPorts.toArray)
    saveWakeMessage(nbt)
  }

  // ----------------------------------------------------------------------- //

  protected def checkPort(port: Int) =
    if (port < 1 || port > 0xFFFF) throw new IllegalArgumentException("invalid port number")
    else port

  private def networkActivity() {
    host match {
      case h: EnvironmentHost => ServerPacketSender.sendNetworkActivity(node, h)
      case _ =>
    }
  }
}
*/