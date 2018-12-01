package server;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;

import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;


import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class ChattingServerHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;
    //onlineusers用于存放在线用户的昵称和对应的通道
    private static Map<String, Channel> onlineUsers = new HashMap<String, Channel>();

    //当有新客户端连接到端口
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel newchannel = ctx.channel();
        System.out.println("新客户端连接到端口：" + newchannel.remoteAddress());

    }

    //当有客户端断开连接时，将该用户从在线用户中移除
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        System.out.println("客户端断开连接：" + channel.remoteAddress());
        for (Map.Entry<String, Channel> entry : onlineUsers.entrySet()) {
            if(entry.getValue()==channel){
                onlineUsers.remove(entry.getKey());
            }
        }
    }


    //当服务器读到数据时，判断是http请求还是websocket请求
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {// 如果是HTTP请求，进行HTTP操作
            System.out.println("收到http消息");// + (FullHttpRequest) msg);
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {// 如果是Websocket请求，则进行websocket操作
            System.out.println("收到websocket消息");// + (WebSocketFrame) msg);
            handleWebsocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    //对http请求的操作
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        // 如果HTTP解码失败，返回HHTP异常
        if (!req.decoderResult().isSuccess()
                || (!"websocket".equals(req.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
                    BAD_REQUEST));
            return;
        }

        // 如果是升级为websocket的http请求
        if("websocket".equals(req.headers().get("Upgrade"))){
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                    "ws://localhost:8888/websocket", null, false);
            handshaker = wsFactory.newHandshaker(req);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory
                        .sendUnsupportedVersionResponse(ctx.channel());
            } else {
                handshaker.handshake(ctx.channel(), req);//进行一次握手
                System.out.println("升级为Websocket");
            }
        }

    }

    //对websocket请求的操作
    private void handleWebsocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {//关闭连接消息
            for (Map.Entry<String, Channel> entry : onlineUsers.entrySet()) {
                if (entry.getValue() == ctx.channel()) {
                    onlineUsers.remove(entry.getKey());
                }
            }
            System.out.println("客户端关闭websocket连接");
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
//        } else if (frame instanceof PingWebSocketFrame) {//ping消息
//            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
//        }
        }else if (frame instanceof TextWebSocketFrame) {//文本消息
            String requestmsg = ((TextWebSocketFrame) frame).text();
            System.out.println("websocket收到文本消息：" + requestmsg);
            //对消息进行处理
            String[] array = requestmsg.split(",");
            if (array.length == 3) {//如果是发送消息
                String sender = array[0];
                String receiver = array[1];
                String message = array[2];
                sendMsg(sender, receiver, message);
            }else{//如果是登录消息
                onlineUsers.put(requestmsg,ctx.channel());
                System.out.println(requestmsg+"登录到聊天室！");
                ctx.channel().writeAndFlush(new TextWebSocketFrame("欢迎你进入聊天室！"));
            }
        }
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
               // 返回应答给客户端
                 if (res.getStatus().code() != 200) {
                        ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
                                         CharsetUtil.UTF_8);
                        res.content().writeBytes(buf);
                        buf.release();
                        HttpUtil.setContentLength(res, res.content().readableBytes());
                    }

               // 如果是非Keep-Alive，关闭连接
               ChannelFuture f = ctx.channel().writeAndFlush(res);
                if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
                        f.addListener(ChannelFutureListener.CLOSE);
                    }
            }

    private void sendMsg(String sender, String receiver, String message) {
        if (receiver.equals("全体")) {//群聊信息
            for (Map.Entry<String, Channel> entry : onlineUsers.entrySet()) {
                if(!entry.getKey().equals(sender)){
                    onlineUsers.get(entry.getKey()).writeAndFlush(new TextWebSocketFrame("[群聊 "+sender+"]:"+message));
                }
            }
        } else {//私聊信息
            System.out.println("私聊"+sender+receiver+message+onlineUsers.get(receiver));
            System.out.println(onlineUsers.entrySet());
            onlineUsers.get(receiver).writeAndFlush(new TextWebSocketFrame("[私聊 "+sender+"]:"+message));
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel newchannel = ctx.channel();
        System.out.println("[" + newchannel.remoteAddress() + "]：通讯异常");
        System.out.println(cause.getMessage());
        newchannel.close();
    }
}
