package models;

public class ClientInfo implements IClientInfo {
	public String ip;
	public String port;
	public String remoteAddr;
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getRemoteAddr() {
		return remoteAddr;
	}
	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}
	@Override
	public String toString() {
		return "ClientInfo [ip=" + ip + ", port=" + port + ", remoteAddr=" + remoteAddr + "]";
	}
	public String getKey() {
		return this.ip;
	}
}
