package tech.iooo.proxy.handler;

import java.net.InetSocketAddress;
import java.util.Objects;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import lombok.extern.slf4j.Slf4j;
import tech.iooo.boot.core.utils.NetUtils;

/**
 * @author 龙也
 * @date 2020/7/21 4:53 下午
 */
@Slf4j
public class ProxyChannelTrafficShapingHandler extends ChannelTrafficShapingHandler {

    public static final String PROXY_TRAFFIC = "ProxyChannelTrafficShapingHandler";

    private final ChannelListener channelListener;

    private long beginTime;
    private long endTime;
    private String username = "anonymous";

    public ProxyChannelTrafficShapingHandler(long checkInterval, ChannelListener channelListener) {
        super(checkInterval);
        this.channelListener = channelListener;
    }

    public static ProxyChannelTrafficShapingHandler get(ChannelHandlerContext ctx) {
        return (ProxyChannelTrafficShapingHandler)ctx.pipeline().get(PROXY_TRAFFIC);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        beginTime = System.currentTimeMillis();
        if (Objects.nonNull(channelListener)) {
            channelListener.active(ctx);
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        endTime = System.currentTimeMillis();
        if (Objects.nonNull(channelListener)) {
            channelListener.inActive(ctx);
        }
        log(ctx);
        super.channelInactive(ctx);
    }

    public long getBeginTime() {
        return beginTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public static void username(ChannelHandlerContext ctx, String username) {
        get(ctx).username = username;
    }

    public String getUsername() {
        return username;
    }

    private void log(ChannelHandlerContext ctx) {
        ProxyChannelTrafficShapingHandler trafficShapingHandler = ProxyChannelTrafficShapingHandler.get(ctx);
        InetSocketAddress localAddress = (InetSocketAddress)ctx.channel().localAddress();
        InetSocketAddress remoteAddress = (InetSocketAddress)ctx.channel().remoteAddress();

        long readByte = trafficShapingHandler.trafficCounter().cumulativeReadBytes();
        long writeByte = trafficShapingHandler.trafficCounter().cumulativeWrittenBytes();

        log.info("{},{}ms,{}:{},{}:{},read:{},write:{},total:{}",
                 trafficShapingHandler.getUsername(),
                 trafficShapingHandler.getEndTime() - trafficShapingHandler.getBeginTime(),
                 NetUtils.getLocalHost(),
                 localAddress.getPort(),
                 remoteAddress.getAddress().getHostAddress(),
                 remoteAddress.getPort(),
                 readByte,
                 writeByte,
                 (readByte + writeByte));
    }
}
