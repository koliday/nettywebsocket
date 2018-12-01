package server;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

//当有新的客户端连接到服务器时，创建并初始化一个channel
public class ChattingServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        //一个channel对应一个pipeline管道
        ChannelPipeline pipeline=socketChannel.pipeline();


        pipeline.addLast("http-codec", new HttpServerCodec());//http解码器，它可能将一个http请求解析成多个消息对象
        pipeline.addLast("aggregator", new HttpObjectAggregator(65536)); // Http消息聚合器，将多个消息聚合成一个完整的http请求/回应
        pipeline.addLast("http-chunked", new ChunkedWriteHandler()); // 对数据进行分块写入，防止大数据传输时撑爆内存
//        pipeline.addLast("websocket-handler",new WebSocketServerProtocolHandler("/ws"));//处理websocket协议，/ws为访问websocket时的uri

        pipeline.addLast(new ChattingServerHandler());//自定义数据处理类



    }
}
