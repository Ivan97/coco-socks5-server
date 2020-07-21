package tech.iooo.proxy.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tech.iooo.proxy.config.ApplicationProperties.Auth;

/**
 * @author 龙也
 * @date 2020/7/21 4:40 下午
 */
@Slf4j
@RequiredArgsConstructor
public class Socks5PasswordAuthRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5PasswordAuthRequest> {

    private final Auth auth;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5PasswordAuthRequest msg) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("auth [username:{} password:{}]", msg.username(), msg.password());
        }
        if (auth(msg.username(), msg.password())) {
            ProxyChannelTrafficShapingHandler.username(ctx, msg.username());
            Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(
                Socks5PasswordAuthStatus.SUCCESS);
            ctx.writeAndFlush(passwordAuthResponse);
        } else {
            ProxyChannelTrafficShapingHandler.username(ctx, "unauthorized");
            Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(
                Socks5PasswordAuthStatus.FAILURE);
            //发送鉴权失败消息，完成后关闭channel
            ctx.writeAndFlush(passwordAuthResponse).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private boolean auth(String username, String password) {
        return StringUtils.equals(username, auth.getUsername())
            && StringUtils.equals(password, auth.getPassword());
    }
}
