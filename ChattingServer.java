package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ChattingServer {
    //开放端口号8888
    private final int port = 8888;
    //Boss线程，用于接受、创建、连接、绑定、监听socket
    private  EventLoopGroup bossGroup = new NioEventLoopGroup();
    //Worker线程，用于处理所有IO操作
    private  EventLoopGroup workerGroup = new NioEventLoopGroup();
    //ServerBootstrap用于初始化并启动服务器，开始监听端口的socket请求
    private ServerBootstrap bootstrap=new ServerBootstrap(); ;


    public void startServer() {
        try {
            bootstrap.group(bossGroup, workerGroup)//将boss线程和worker线程进行绑定
                    .channel(NioServerSocketChannel.class)//创建通道
                    .option(ChannelOption.SO_BACKLOG, 1024)//对channel设定一些参数
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChattingServerInitializer());//初始化管道

            ChannelFuture channelFuture = bootstrap.bind(port).sync();

            System.out.println("服务器已启动！");
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
