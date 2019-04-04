package li.cil.oc.server.component

import li.cil.oc.api.network.EnvironmentHost

abstract class IpNetworkCard(host : EnvironmentHost) extends NetworkCard(host) {
  protected var ipAddress : String

  def getIpAddress(): String = {
    ipAddress
  }
}
