package com.wjy;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @Description TODO 多路复用器多线程
 * 主线程只负责accept，而read和write分别创建了新的线程处理
 *
 * 1.readHandler()被重复调起
 *   因为，当有可读事件发生时，readHandler()被第一次调起，在readHandler()中另起了一个线程去处理读事件，
 *   这个线程读是需要一个时间的，主线程只要是发现还有数据未被读物完，就会调起readHandler()，会看到如下日志打印：
 *   server start...
 *   read handler.....
 *   read handler.....
 *   read handler.....
 *   read handler.....
 *   read handler.....
 *   read handler.....
 *   write handler...
 *   write handler...
 *   write handler...
 * @author wangjunyao
 * @version 1.0.0
 * @createTime 2020年11月10日 17:24:00
 */
public class ServerSocketMultiplexingSingleThread3 implements IServerSocketNIO, ILiftCycle{

    //服务端监听的端口
    private final int port;

    //服务端
    private ServerSocketChannel serverSocketChannel;

    //多路复用器
    private Selector selector;

    public ServerSocketMultiplexingSingleThread3(int port) {
        this.port = port;
    }


    @Override
    public void init() throws Exception {
        //得到serverSocketChannel  的文件描述符   fd4
        serverSocketChannel = ServerSocketChannel.open();
        //绑定端口  否则会自动分配
        serverSocketChannel.bind(new InetSocketAddress(port));
        //设置为非阻塞模式
        serverSocketChannel.configureBlocking(false);
        //在epoll模型下：Selector.open(); 相当于 epoll_create  得到fd3
        selector = Selector.open();
        //注册到多路复用器
        /*
         * select, poll：jvm里开辟一个数组，把fd4放进去
         * epoll：epoll_ctl(fd3,ADD,fd4,EPOLLIN
         */
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void stop() throws Exception {
        if (selector != null){
            selector.close();
        }
        if (serverSocketChannel != null){
            serverSocketChannel.close();
        }
    }

    @Override
    public void start() throws Exception {
        System.out.println("server start...");
        while (true){
            /*
             * select()
             * 1：select, poll 其实内核  select(fd4)  poll(fd4)
             * 2：epoll   其实是内核的epoll_wait();
             * 没有时间参数时：阻塞，直到有返回
             * 有时间参数：设置一个超时时间
             */

            /*
             * 这里有个坑，selector.select(50)，在参数中增加了超时时间，因为这个版本是在另外一个线程中注册的write事件，
             * 在多线程模型下，selector.select()是拿着已经注册的fd去发现事件是否有到达，
             * 假如此时主线程先行阻塞，后来在另外一个线程中注册了write事件，那么主线程阻塞先发生，是不会往下执行的，
             * wakeup()可以解决此问题
             */
            if (selector.select(50) > 0){
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();
                    if (selectionKey.isAcceptable()){
                        acceptHandler(selectionKey);
                    }else if (selectionKey.isReadable()){
                        /*
                         * 在readHandler中只处理了read并注册 关心这个key的write事件
                         */
                        /*
                         * 解决readHandler重复调用
                         * 解决方案1：从selector中取出key   selectionKey.cancel();
                         */
                        selectionKey.cancel();
                        readHandler(selectionKey);//这里不在阻塞，即便是抛出线程读取，但是在读取的时差里，还是会重复调用
                    }else if (selectionKey.isWritable()){
                        /*
                         * 写事件，只要是 send-queue 是空的，就一定会给你返回可以写的事件，就会调writeHandler，
                         * 什么时候写，不是依赖 send-queue 是不是有空间（多路复用器能不能写是参考 send-queue(本机的发送队列)有没有空间）
                         *
                         * 第一步：准备好要写什么，
                         * 第二部：才关心send-queue是否有空间
                         *
                         * 因此，read事件一开始就要注册，但是write依赖以上关系，什么时候用什么时候注册
                         * 如果一开始就注册了write事件，就进入死循环，一直调起
                         */
                        /*
                         * 解决writeHandler重复调用
                         * 解决方案1：从selector中取出key   selectionKey.cancel();
                         *
                         * 解决方案2：
                         * 但是重复多次的cancel、register都会造成系统调用，最终解决方案：
                         * 当有n个fd有R/W处理的时候，
                         * 将n个fd分组，每一组一个selector，将一个selector压到一个线程上
                         * 最好的线程数量是：cpu核数  或者  cpu核数 * 2
                         * 单看一个线程：里面有一个selector，有一部分fd，且他们是线性的
                         * 多个线程：他们在自己的cpu上执行，代表会有多个selector在并行，且线程内是线性的，最终是并行的fd被处理
                         *
                         * 是否还需要一个selector中的fd要放到不同的线程并行，从而造成cancel调用吗？？？？
                         * 答案是：  不用
                         *
                         *
                         * 那为什么会有方案1的解决模型呢？？？？？？？
                         * 需要尽量的利用资源，利用cpu核数，假如有一个fd执行耗时，在一个线性里阻塞后续的fd的处理，
                         * 因此提出了多线程模型，但是这个版本的多线程，在不加cancel的情况下，会造成readHandler和
                         * writeHandler重复调用，因此提出了cancel，先从selector中剔除这个fd，但是由因为cancel
                         * 会造成系统调用，导致频繁的用户态内核态切换，性能下降，所以有了解决方案2
                         *
                         *
                         * 方案2其实就是分治，假如有100W个连接，如果有4个线程（selector），每个线程处理250000个fd
                         *
                         * 那么，可以拿出一个线程的selector就只关注accept，然后把接收到的客户端fd，分配
                         * 给其他线程的selector处理
                         *
                         */
                        selectionKey.cancel();//会发生系统调用
                        writeHandler(selectionKey);
                    }
                }
            }
        }
    }

    /**
     * 可写事件
     * @param selectionKey
     * @throws Exception
     */
    private void writeHandler(SelectionKey selectionKey) {
        new Thread(() -> {
            System.out.println("write handler...");
            SocketChannel clientSocket = (SocketChannel)selectionKey.channel();
            ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();
            buffer.flip();
            try{
                while (buffer.hasRemaining()){
                    clientSocket.write(buffer);
                }
                buffer.clear();
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 可读事件处理
     * @param selectionKey
     */
    private void readHandler(@NotNull SelectionKey selectionKey) throws Exception {
        new Thread(() -> {
            try{
                System.out.println("read handler.....");
                //得到客户端socket
                SocketChannel clientSocket = (SocketChannel)selectionKey.channel();
                //从 SelectionKey 中取出缓冲区
                ByteBuffer buffer = (ByteBuffer)selectionKey.attachment();
                //初始化缓冲区
                buffer.clear();
                int read;
                while (true){
                    read = clientSocket.read(buffer);
                    if (read > 0){
                        //注册写事件
                        clientSocket.register(selectionKey.selector(),SelectionKey.OP_WRITE,buffer);
                    }else if (read == 0){
                        break;
                    }else {
                        //关闭客户端
                        clientSocket.close();
                        //关闭客户端缓冲区
                        closeClientSocketBuffer(buffer);
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 连接
     * @param selectionKey
     * @throws Exception
     */
    private void acceptHandler(SelectionKey selectionKey) throws Exception {
        //得到服务端socket
        ServerSocketChannel serverSocket = (ServerSocketChannel) selectionKey.channel();
        //得到客户端client 的文件描述符  fd7
        SocketChannel client = serverSocket.accept();
        //设置为非阻塞模式
        client.configureBlocking(false);
        //注册到selector 并分配缓冲区大小
        /*
         * selector, poll：jvm里面开辟一个数组 fd7 放进去
         * epoll：epoll_ctl(fd3,ADD,fd7,EPOLLIN
         */
        client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocateDirect(8192));
    }

    private void closeClientSocketBuffer(ByteBuffer buffer) throws Exception {
        Method getCleanerMethod = buffer.getClass().getDeclaredMethod("cleaner");
        getCleanerMethod.setAccessible(true);
        Object cleaner = getCleanerMethod.invoke(buffer);
        Method cleanMethod = cleaner.getClass().getDeclaredMethod("clean");
        cleanMethod.invoke(cleaner);
    }

    @Override
    public void close() {
        try {
            stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
