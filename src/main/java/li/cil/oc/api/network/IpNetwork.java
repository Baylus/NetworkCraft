package li.cil.oc.api.network;

import java.util.Map;

public interface IpNetwork extends Network{
    //UUID [IpAddress, MAC]
    Map<String, String[]> addresses();

    //get the IP address of the current node, which can be switched to uuid with another function
    String getAddress(Node node);

    //turn uuid address into IP address for displaying IP address
    String uuidToIp(String address);

    //turn IP address into uuid for compatibility with mod
    String ipToUuid(String address);
}
