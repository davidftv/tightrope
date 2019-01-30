package network;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import models.ClientInfo;
import models.Server;
import models.ServerPool;
import models.Statistics;

public class FrontendHandler extends SimpleChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(FrontendHandler.class);    
    private static final int TIMEOUT_IN_MILLIS = 100;
    private volatile Channel outboundChannel;

    private final ClientSocketChannelFactory factory;
    private final ChannelGroup channelGroup;
    private final ServerPool serverPool;
    private final Statistics statistics;

    public FrontendHandler(ChannelGroup channelGroup, ClientSocketChannelFactory factory, ServerPool serverPool, Statistics statistics) {
        this.channelGroup = channelGroup;
        this.factory = factory;
        this.serverPool = serverPool;
        this.statistics = statistics;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
        this.channelGroup.add(e.getChannel());
        final Channel inboundChannel = e.getChannel();
        inboundChannel.setReadable(false);
        ClientBootstrap cb = new ClientBootstrap(factory);
        cb.setOption("tcpNoDelay", true);
        cb.setOption("connectTimeMillis", TIMEOUT_IN_MILLIS);
        cb.getPipeline().addLast("handler", new BackendHandler(e.getChannel()));
        ClientInfo cinfo = new ClientInfo();
        cinfo.remoteAddr = inboundChannel.getRemoteAddress().toString();
        InetSocketAddress isa = (InetSocketAddress)inboundChannel.getRemoteAddress();
        cinfo.ip = isa.getHostString();
        final Server server = this.serverPool.selectServer(cinfo);


        ChannelFuture f = cb.connect(new InetSocketAddress(server.getHostname(), server.getPort()));


        this.outboundChannel = f.getChannel();
        f.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("Connection attempt succeeded: begin to accept incoming traffic.");
                    statistics.addFrontendConnection();
                    inboundChannel.setReadable(true);
                } else {
                    log.info("Unable to connect to {}:{}", server.getHostname(), server.getPort());
                    log.info("Setting {}:{} availability to false", server.getHostname(), server.getPort());
                    server.setAvailable(new AtomicBoolean(false));
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                }
            }
        });
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        ChannelBuffer msg = (ChannelBuffer) e.getMessage();
        ChannelBuffer m2 = msg.copy();
        this.statistics.addBytes(msg.readableBytes());
        this.outboundChannel.write(msg);
        byte[] a = new byte[1024];
        m2.getBytes(0, a, 0, m2.readableBytes());
       log.info(new String(a)+":"+ctx.getChannel().getRemoteAddress());
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (this.outboundChannel != null) {
            if (this.outboundChannel.isConnected()) {
                this.outboundChannel.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
        this.statistics.removeFrontendConnection();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        Channel channel = e.getChannel();
        if (channel.isConnected()) {
            log.error("Exception caught connecting to {}. {}", channel.getRemoteAddress(), e.getCause());
            channel.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
