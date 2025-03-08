package com.dpeng.gateway.core.helper;

import com.alibaba.nacos.common.utils.StringUtils;
import com.dpeng.gateway.config.pojo.ServiceDefinition;
import com.dpeng.gateway.core.request.GatewayRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.asynchttpclient.Request;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static com.dpeng.gateway.common.constant.HttpConstant.HTTP_FORWARD_SEPARATOR;

/**
 * Netty服务端、网关、Http客户端之间的请求转换
 */
public class RequestHelper {

    /**
     * 主要用于 将 Netty 服务端接收到的 HTTP 请求转换为网关内部请求 (GatewayRequest)，并且提供获取客户端 IP 的方法。
     */
    public static GatewayRequest buildGatewayRequest(ServiceDefinition serviceDefinition, FullHttpRequest fullHttpRequest, ChannelHandlerContext ctx) {
        HttpHeaders headers = fullHttpRequest.headers(); // 服务端的http请求头
        String host = headers.get(HttpHeaderNames.HOST); // host
        HttpMethod method = fullHttpRequest.method(); // http请求类型
        String uri = fullHttpRequest.uri(); // uri
        String clientIp = getClientIp(ctx, fullHttpRequest); // 客户端ip
        String contentType = HttpUtil.getMimeType(fullHttpRequest) == null ? null :
                HttpUtil.getMimeType(fullHttpRequest).toString(); // 请求的MIME类型
        Charset charset = HttpUtil.getCharset(fullHttpRequest, StandardCharsets.UTF_8); // 字符集

        return new GatewayRequest(serviceDefinition, charset, clientIp, host, uri, method,
                contentType, headers, fullHttpRequest);
    }

    public static Request buildHttpClientRequest(GatewayRequest gatewayRequest) {
        return gatewayRequest.build();
    }


    /**
     * X-Forwarded-For 是 HTTP 代理服务器（如 Nginx）常用的请求头，表示原始客户端的 IP 地址。
     * xForwardedValue = "192.168.1.100, 172.16.0.1";例如第一个是用户真实ip, 第二个是代理服务器ip
     */
    private static String getClientIp(ChannelHandlerContext ctx, FullHttpRequest request) {
        String xForwardedValue = request.headers().get(HTTP_FORWARD_SEPARATOR);

        String clientIp = null;
        if (StringUtils.isNotEmpty(xForwardedValue)) {
            List<String> values = Arrays.asList(xForwardedValue.split(", "));
            if (!values.isEmpty() && StringUtils.isNotBlank(values.get(0))) {
                clientIp = values.get(0);
            }
        }
        //请求未被代理, 则从netty中获取
        if (clientIp == null) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            clientIp = inetSocketAddress.getAddress().getHostAddress();
        }
        return clientIp;
    }

}
