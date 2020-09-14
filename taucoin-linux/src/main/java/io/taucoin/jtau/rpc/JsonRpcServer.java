package io.taucoin.jtau.rpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import com.thetransactioncompany.jsonrpc2.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.controller.TauController;
import io.taucoin.jtau.rpc.method.*;

import java.net.InetAddress;

/**
 * Taucoin blockchain json rpc server.
 * You can use any json rpc client to access this service.
 */
public final class JsonRpcServer {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    // TauController through which all blockchain components can be accessed.
    private TauController tauController;

    // method dispatcher.
    private Dispatcher dispatcher;

    // netty evnent loop group.
    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;

    // http server listening port.
    private int port;

    /**
     * JsonRpcServer constructor.
     *
     * @param controller TauController
     */
    public JsonRpcServer(TauController controller) {
        this.tauController = controller;

        this.dispatcher = new Dispatcher();

        // register all rpc methods.
        // methods about dht
        this.dispatcher.register(new dht_nodesCount(this.tauController));
        this.dispatcher.register(new dht_getImmutableItem(this.tauController));
        this.dispatcher.register(new dht_getMutableItem(this.tauController));

		// methods about chain
        this.dispatcher.register(new chain_generateNewSeed(this.tauController));

        this.dispatcher.register(new chain_getBestBlock(this.tauController));
        this.dispatcher.register(new chain_getBlockByHash(this.tauController));
        this.dispatcher.register(new chain_getBlockByNumber(this.tauController));

        this.dispatcher.register(new chain_getTransactionByHash(this.tauController));
        this.dispatcher.register(new chain_getTransactionsInPool(this.tauController));

        this.dispatcher.register(new chain_getAccountState(this.tauController));
        this.dispatcher.register(new chain_updateKey(this.tauController));

        this.dispatcher.register(new chain_followChain(this.tauController));
        this.dispatcher.register(new chain_unfollowChain(this.tauController));

        this.dispatcher.register(new chain_sendTransaction(this.tauController));
        this.dispatcher.register(new chain_sendRawBlock(this.tauController));

        this.dispatcher.register(new chain_createNewCommunity(this.tauController));
    }

    /**
     * Start http json rcp server.
     *
     * @param port http server listening port
     */
    public void start(int port) throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        this.port = port;
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.localAddress(port);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new JsonRpcServerInitializer());

            Channel ch = b.bind().sync().channel();

            logger.info("Json rpc server is starting, listen port: {}", this.port);

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * Stop http json rcp server.
     */
    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    class JsonRpcServerInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        public void initChannel(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();
            p.addLast(new HttpServerCodec());
            p.addLast(new JsonRpcServerHandler(dispatcher));
        }
    }
}
