package strategies;


import models.IClientInfo;
import models.Server;
import models.ServerPool;

public interface LoadBalancerStrategy {
    Server selectServer(ServerPool serverPool,IClientInfo cinfo);
    String getName();
}
