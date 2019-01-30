package strategies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import models.IClientInfo;
import models.Server;
import models.ServerPool;

public class IPStickStrategy implements LoadBalancerStrategy {

    private final String NAME = "Random";
    private Map<String,Server> ipMap = new HashMap<String,Server>();

    public Server selectServer(final ServerPool serverPool,IClientInfo cinfo) {
		System.out.println("select server based on:" + cinfo.toString());
    	
    	if(ipMap.containsKey(cinfo.getKey())) {
    		System.out.println("select previous server");
    		return ipMap.get(cinfo.getKey());
    	}
        List<Server> serverList = serverPool.getAvailableServers();
        Server s = serverList.get(new Random().nextInt(serverList.size()));
        ipMap.put(cinfo.getKey(), s);
        return s;
    }

    public String getName() {
        return NAME;
    }
}
