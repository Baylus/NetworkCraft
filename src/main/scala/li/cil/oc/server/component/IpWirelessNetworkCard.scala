package li.cil.oc.server.component

import li.cil.oc.api.network.EnvironmentHost

abstract class IpWirelessNetworkCard(host : EnvironmentHost) extends WirelessNetworkCard(host) {
  protected var ipAddress : String

  def getIpAddress(): String = {
    ipAddress
  }
}
