package tech.iooo.proxy.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 龙也
 * @date 2020/7/21 4:46 下午
 */
@Slf4j
@RequiredArgsConstructor
public class Socks5CommandRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

    private final EventLoopGroup bossGroup;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("dest [{},{}:{}]", msg.type(), msg.dstAddr(), msg.dstPort());
        }
        if (msg.type().equals(Socks5CommandType.CONNECT)) {
            if (log.isTraceEnabled()) {
                log.trace("准备连接目标服务器");
            }
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(bossGroup)
                     .channel(NioSocketChannel.class)
                     .option(ChannelOption.TCP_NODELAY, true)
                     .handler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) throws Exception {
                             ch.pipeline().addLast(new Dest2ClientHandler(ctx));
                         }
                     });
            if (log.isTraceEnabled()) {
                log.trace("连接目标服务器");
            }
            ChannelFuture channelFuture = bootstrap.connect(msg.dstAddr(), msg.dstPort());
            channelFuture.addListener((ChannelFutureListener)future -> {
                if (future.isSuccess()) {
                    if (log.isTraceEnabled()) {
                        log.trace("成功连接目标服务器");
                    }
                    ctx.pipeline().addLast(new Client2DestHandler(future));
                    Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(
                        Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4);
                    ctx.writeAndFlush(commandResponse);
                } else {
                    Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(
                        Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
                    ctx.writeAndFlush(commandResponse);
                }
            });
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * 将目标服务器信息转发给客户端
     *
     * @author huchengyi
     */
    @RequiredArgsConstructor
    private static class Dest2ClientHandler extends ChannelInboundHandlerAdapter {

        private final ChannelHandlerContext ctx;

        @Override
        public void channelRead(ChannelHandlerContext ctx2, Object destMsg) throws Exception {
            if (log.isTraceEnabled()) {
                log.trace("将目标服务器信息转发给客户端");
            }
            ctx.writeAndFlush(destMsg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx2) throws Exception {
            if (log.isTraceEnabled()) {
                log.trace("目标服务器断开连接");
            }
            ctx.channel().close();
        }
    }

    /**
     * 将客户端的消息转发给目标服务器端
     *
     * @author huchengyi
     */
    @RequiredArgsConstructor
    private static class Client2DestHandler extends ChannelInboundHandlerAdapter {

        private final ChannelFuture destChannelFuture;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (log.isTraceEnabled()) {
                log.trace("将客户端的消息转发给目标服务器端");
            }
            destChannelFuture.channel().writeAndFlush(msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (log.isTraceEnabled()) {
                log.trace("客户端断开连接");
            }
            destChannelFuture.channel().close();
        }
    }
}
