package tech.iooo.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import tech.iooo.boot.core.utils.NetUtils;
import tech.iooo.proxy.config.ApplicationProperties;
import tech.iooo.proxy.config.ApplicationProperties.Auth;
import tech.iooo.proxy.handler.ChannelListener;
import tech.iooo.proxy.handler.DefaultErrorHandler;
import tech.iooo.proxy.handler.ProxyChannelTrafficShapingHandler;
import tech.iooo.proxy.handler.ProxyIdleHandler;
import tech.iooo.proxy.handler.Socks5CommandRequestHandler;
import tech.iooo.proxy.handler.Socks5InitialRequestHandler;
import tech.iooo.proxy.handler.Socks5PasswordAuthRequestHandler;

/**
 * @author 龙也
 * @date 2020/7/21 3:40 下午
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationLifecycle implements SmartLifecycle {

    private final ApplicationProperties applicationProperties;

    private boolean running;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Setter
    private ChannelListener channelListener;

    @Override
    public void start() {
        bossGroup = new NioEventLoopGroup(2);
        workerGroup = new NioEventLoopGroup();

        final ServerBootstrap serverBootstrap = new ServerBootstrap();

        try {
            serverBootstrap.group(bossGroup, workerGroup)
                           .channel(NioServerSocketChannel.class)
                           .option(ChannelOption.SO_BACKLOG, 1024)
                           .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                           .handler(new LoggingHandler("SERVER", LogLevel.DEBUG))
                           .childOption(ChannelOption.SO_KEEPALIVE, true)
                           .childOption(ChannelOption.TCP_NODELAY, true)
                           .childHandler(new ChannelInitializer<SocketChannel>() {
                               @Override
                               protected void initChannel(SocketChannel channel) throws Exception {
                                   ChannelPipeline pipeline = channel.pipeline();

                                   if (applicationProperties.getLog()) {
                                       //流量统计
                                       pipeline.addLast(
                                           ProxyChannelTrafficShapingHandler.PROXY_TRAFFIC,
                                           new ProxyChannelTrafficShapingHandler(3000, channelListener));
                                   }

                                   //channel超时处理
                                   pipeline.addLast(new IdleStateHandler(3, 30, 0));
                                   pipeline.addLast(new ProxyIdleHandler());

                                   pipeline.addLast("CLIENT_REQUEST", new LoggingHandler(LogLevel.DEBUG));

                                   pipeline.addLast(Socks5ServerEncoder.DEFAULT);
                                   pipeline.addLast(new Socks5InitialRequestDecoder());

                                   Auth auth = applicationProperties.getAuth();
                                   boolean isAuth = auth.isAuth();
                                   pipeline.addLast(new Socks5InitialRequestHandler(isAuth));

                                   if (isAuth) {
                                       pipeline.addLast(new Socks5PasswordAuthRequestDecoder());
                                       pipeline.addLast(new Socks5PasswordAuthRequestHandler(auth));
                                   }

                                   //socks connection
                                   pipeline.addLast(new Socks5CommandRequestDecoder());
                                   //Socks connection
                                   pipeline.addLast(new Socks5CommandRequestHandler(bossGroup));

                                   pipeline.addLast(new DefaultErrorHandler());
                               }
                           });

            int port = NetUtils.getAvailablePort(applicationProperties.getPort());
            ChannelFuture future = serverBootstrap.bind(port).sync();
            log.info("server listening on port {}", port);
            this.running = true;
            future.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }
}
