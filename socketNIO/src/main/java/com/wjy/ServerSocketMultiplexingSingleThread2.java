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
 * @Description TODO 多路复用器单线程
 * 1)增加了对写事件的处理，写事件是依赖本机的send-queue是否有空间，因此只要注册了写事件，
 *   只要是 send-queue 是空的，就一定会给你返回可以写的事件，就会调writeHandler
 *
 *   会打印如下日志
 *   server start...
 *   read handler.....
 *   write handler...
 *   write handler...
 *   write handler...
 *   write handler...
 *
 * 2)但是什么时候写，应该是依赖于业务处理，当准备好要写的数据时，才回去关注send-queue是否为空，
 *   因此写事件应该是什么时候用，什么时候注册
 * @author wangjunyao
 * @version 1.0.0
 * @createTime 2020年11月10日 17:24:00
 */
public class ServerSocketMultiplexingSingleThread2 implements IServerSocketNIO, ILiftCycle{

    //服务端监听的端口
    private final int port;

    //服务端
    private ServerSocketChannel serverSocketChannel;

    //多路复用器
    private Selector selector;

    public ServerSocketMultiplexingSingleThread2(int port) {
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
            if (selector.select() > 0){
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();
                    if (selectionKey.isAcceptable()){
                        acceptHandler(selectionKey);
                    }else if (selectionKey.isReadable()){
                        /*
                         *在readHandler中只处理了read并注册 关系这个key的write事件
                         */
                        readHandler(selectionKey);
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
    private void writeHandler(SelectionKey selectionKey) throws Exception {
        System.out.println("write handler...");
        SocketChannel clientSocket = (SocketChannel)selectionKey.channel();
        ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();
        buffer.flip();
        while (buffer.hasRemaining()){
            clientSocket.write(buffer);
        }
        buffer.clear();
    }

    /**
     * 可读事件处理
     * @param selectionKey
     */
    private void readHandler(@NotNull SelectionKey selectionKey) throws Exception {
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
