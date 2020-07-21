package tech.iooo.proxy.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 龙也
 * @date 2020/7/21 4:37 下午
 */
@Slf4j
@RequiredArgsConstructor
public class Socks5InitialRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5InitialRequest> {

    private final boolean auth;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5InitialRequest msg) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("[initial]:{}", msg);
        }
        if (msg.decoderResult().isFailure()) {
            log.error("not ss5 protocol");
            ctx.fireChannelRead(msg);
        } else {
            if (msg.version().equals(SocksVersion.SOCKS5)) {
                Socks5InitialResponse initialResponse;
                if (auth) {
                    initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD);
                } else {
                    initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH);
                }
                ctx.writeAndFlush(initialResponse);
            }
        }
    }
}
