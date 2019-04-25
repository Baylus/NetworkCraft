package li.cil.oc.server.network

import li.cil.oc.api.network

object IpNetwork {
  // uuid to ip address mapping
  protected var ipAddresses : Map[String, String] = Map.empty

  //uuid to mac address mapping
  protected var macAddresses : Map[String, String] = Map.empty

  //bad way to create more ip and mac addresses
  private var nextIpAddress : Int = 0

  private var nextMacAddress : Int = 0

  //get the ip address of the node passed in. Don't know why it cant be a Node
  def getIpAddress(node:network.Component) = {
    uuidToIp(node.address)
  }

  //get the ip address for a uuid and vice versa if such a string exists
  def uuidToIp(address: String) = {
    ipAddresses(address)
  }

  //remove all mappings associated with mac and ipaddresses to uuid
  def freeAddress(address: String) = {
    val mac = macAddresses(address)
    val ip = ipAddresses(address)
    macAddresses -= (mac, address)
    ipAddresses -= (ip, address)
  }

  //get an ip address for a uuid
  def receiveIpAddress(address: String) = {
    val mapping = "0.0.0."+nextIpAddress
    nextIpAddress += 1
    //allows for easy lookup between addresses and makes sure they are unique
    ipAddresses += (address -> mapping)
    ipAddresses += (mapping -> address)
    ipAddresses(address)
  }

  //get a mac address for a uuid
  def receiveMacAddress(address: String) = {
    //really bad way of creating addresses... but a place holder for now
    val mapping = "00:00:00:00:00:0"+nextMacAddress
    nextMacAddress += 1
    macAddresses += (address -> mapping)
    macAddresses += (mapping -> address)
    macAddresses(address)
  }

  //things to do
  //set your own ip address

}