package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * Package: nio
 * Date: 2020/1/12
 * Time: 11:47
 * Description: 服务器端，接受连接、请求并响应
 */
public class NioServer {

    /**
     * 启动服务器
     */
    public void start() throws IOException {
        // 1.创建Selector
        Selector selector = Selector.open();
        
        // 2.通过ServerSocketChannel创建Channel通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        // 3.为channel通道绑定监听端口,并设置为非阻塞
        serverSocketChannel.bind(new InetSocketAddress(8000));
        serverSocketChannel.configureBlocking(false);
        
        // 4.将channel注册到selector上，监听连接事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println(">>> 服务器启动成功！");
        
        // 5.循环等待新接入到连接
        while (true) {
            // 获取可用channel就绪数量
            int readyChannels = selector.select();
            if (readyChannels == 0) continue;

            // 获取可用channel的集合
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();

                if (selectionKey.isAcceptable()) {
                    // 只有serverSocketChannel才会注册连接事件，监听到连接就给该客户端创建一个socketChannel
                    this.acceptHandler(serverSocketChannel, selector);
                } else if (selectionKey.isReadable()) {
                    this.readHandler(selectionKey, selector);
                }

                // 移除当前的selectionKey
                iterator.remove();
            }
        }
        
        // 6.根据就绪状态，调用对应方法处理业务逻辑
    }

    /**
     * 接入事件处理
     * 如果客户端建立连接,创建socketChannel与它建立连接并注册到selector
     * @param serverSocketChannel   用于端口监听的被动channel
     * @param selector
     * @throws IOException
     */
    private void acceptHandler(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        // 1. 获取与客户端建立的socketChannel,如果没有连接建立返回null
        SocketChannel socketChannel = serverSocketChannel.accept();

        // 2. 将socket设置成非阻塞，并注册到selector，注册可读事件
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);// 此时该socketChannel归selector管理，并且监听socketChannel的可读事件

        // 3. 回复客户端
        socketChannel.write(StandardCharsets.UTF_8.encode("-----你已与聊天室服务器建立连接-----"));
    }

    /**
     * 可读事件处理
     * @param selectionKey
     * @param selector
     * @throws IOException
     */
    private void readHandler(SelectionKey selectionKey, Selector selector) throws IOException {
        // 1. 获取到已就绪的channel
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        // 2. 创建Buffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        // 3. 读取客户端发来的数据
        StringBuilder builder = new StringBuilder();
        while (socketChannel.read(byteBuffer) > 0){ // 从channel中读取的字节大于0
            byteBuffer.flip();
            builder.append(StandardCharsets.UTF_8.decode(byteBuffer));
        }

        // channel再次注册到selector中，如果注册的事件没有发生变化，那么是不需要重新注册的
            //socketChannel.register(selector, SelectionKey.OP_READ);

        // 4. 将客户端发送的请求信息，广播给其他客户端
        if (builder.length() > 0){
            String request = builder.toString();
            System.out.println("::" + request);
            this.broadCast(selector, socketChannel, request);
        }
    }


    private void broadCast(Selector selector, SocketChannel sourceChannel, String sourceRequest){
        // 1. 获取到所有已接入到客户端Channel
        Set<SelectionKey> selectionKeySet = selector.keys();

        // 2. 循环向所有channel广播信息
        selectionKeySet.forEach( selectionKey -> {
            Channel targetChannel = selectionKey.channel();
            // 剔除ServerSocketChannel和发送该消息的Channel
            if (targetChannel != sourceChannel && targetChannel instanceof SocketChannel){
                try {
                    // 将消息发送到客户端
                    ((SocketChannel) targetChannel).write(StandardCharsets.UTF_8.encode(sourceRequest));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    public static void main(String[] args) {
        NioServer nioServer = new NioServer();
        try {
            nioServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
