package li.cil.oc.server.network

import li.cil.oc.api
import li.cil.oc.api.network
import li.cil.oc.api.network._
import li.cil.oc.api.network.{Node => ImmutableNode}

abstract class IpNetwork {
  protected var addresses : Map[String, String]

  def getAddress(node:Node) = {
    node.address
  }

  def uuidToIp(address: String) = {
    addresses(address)
  }

}
