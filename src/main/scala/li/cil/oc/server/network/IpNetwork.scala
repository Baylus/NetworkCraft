package li.cil.oc.server.network

import li.cil.oc.api.network



object IpNetwork {
                              //UUID    IP      MAC
  protected var addresses : Map[String, Array[String]] = null

  def getIpAddress(node:network.Component) = {
    uuidToIp(node.address)
  }

  def uuidToIp(address: String) = {
    //addresses(address)(0)
    "0.0.0.0"
  }

}