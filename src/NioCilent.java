package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * Package: nio
 * Date: 2020/1/12
 * Time: 11:47
 * Description:
 */
public class NioCilent {

    /**
     * 启动客户端
     */
    public void start() throws IOException {
        System.out.println(">>> 客户端启动成功");
        // 1. 连接服务器端
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8000));

        // 2. 用于接收服务器端响应,需要新开一个线程，专门接收服务器端响应数据
        Selector selector = Selector.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        new Thread(new NioClientHandler(selector)).start();

        // 3. 向服务器发送数据
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()){
            String request = scanner.nextLine();
            socketChannel.write(StandardCharsets.UTF_8.encode(request));
        }
    }

    public static void main(String[] args) {
        NioCilent nioCilent = new NioCilent();
        try {
            nioCilent.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 客户端线程，专门接收服务器端响应信息
     */
    private static class NioClientHandler implements Runnable{

        private Selector selector;

        public NioClientHandler(Selector selector) {
            this.selector = selector;
        }

        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        @Override
        public void run() {
            try {
                while (true) {
                    // 获取就绪channel数
                    int readyChannels = selector.select();
                    if (readyChannels == 0) continue;

                    // 获取就绪channel集合
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        // 检查就绪状态
                        if (selectionKey.isReadable()) {
                            this.readHandler(selectionKey, selector);
                        }
                        iterator.remove();
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }

        }

        /**
         * 处理可读事件
         * @param selectionKey
         * @param selector
         * @throws IOException
         */
        private void readHandler(SelectionKey selectionKey, Selector selector) throws IOException {
            // 获取到就绪到channel
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

            // 创建buffer
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

            // 读取服务器广播过来的数据
            StringBuilder builder = new StringBuilder();
            while (socketChannel.read(byteBuffer) > 0){
                byteBuffer.flip();
                builder.append(StandardCharsets.UTF_8.decode(byteBuffer));
            }
            if (builder.length() > 0){
                // 本地客户端打印
                System.out.println(builder.toString());
            }
        }
    }
}
