package strategies;

import models.IClientInfo;
import models.Server;
import models.ServerPool;

import java.util.List;
import java.util.Random;

public class RandomStrategy implements LoadBalancerStrategy {

    private final String NAME = "Random";

    public Server selectServer(final ServerPool serverPool,IClientInfo cinfo) {
        List<Server> serverList = serverPool.getAvailableServers();
        return serverList.get(new Random().nextInt(serverList.size()));
    }

    public String getName() {
        return NAME;
    }
}
