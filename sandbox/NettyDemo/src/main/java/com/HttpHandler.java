package com;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;

public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private AsciiString contentType = HttpHeaderValues.TEXT_PLAIN;

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("test".getBytes()));

        HttpHeaders headers = response.headers();

        headers.add(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
        headers.add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        channelHandlerContext.write(response);


    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        ctx.flush();
    }
}
