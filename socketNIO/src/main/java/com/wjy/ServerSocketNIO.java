package com.wjy;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * 非阻塞网络IO
 *
 * 非阻塞模型下，serverSocketChannel.accept();和channel.read(buffer);不会再阻塞，
 * 因此可以在一个线程里面处理多个客户端的请求
 *
 * 缺点：
 *  1)每次循环都会有一次：O(n)复杂度recv(系统调用，接收数据)，很多调用都是无意义的，浪费资源
 *      iter.hasNext()，加入有10000一个连接，只有其中一个客户端发来数据，此时还是需要遍历所有
 *
 * 多路复用
 *
 * @author wangjunyao
 * @version 1.0.0
 * @Description TODO
 * @createTime 2020年11月09日 14:08:00
 */
public class ServerSocketNIO implements IServerSocketNIO, ILiftCycle{

    private final int port;

    private ServerSocketChannel serverSocketChannel;

    private LinkedList<SocketChannel> socketChannels;

    public ServerSocketNIO(int port){
        this.port = port;
    }

    @Override
    public void init() throws Exception {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        //设置为非阻塞
        serverSocketChannel.configureBlocking(false);
        socketChannels = new LinkedList<>();
    }

    @Override
    public void stop() throws Exception {
        if (serverSocketChannel != null){
            serverSocketChannel.close();
        }
    }

    @Override
    public void start() throws Exception {
        System.out.println("server start...");
        while (true){
            /**
             * 非阻塞模式，没有连接时立即返回null
             * 阻塞模式，没有连接时将无限期阻塞
             *
             * 通过此方法返回的套接字默认将处于阻塞模式
             */
            SocketChannel socketChannel = serverSocketChannel.accept();
            if (socketChannel == null){
                //System.out.println("null......");
            }else {
                //设置为非阻塞模式
                socketChannel.configureBlocking(false);
                System.out.println("客户端" + socketChannel.getRemoteAddress() + "连接");
                socketChannels.add(socketChannel);
            }

            //处理每个客户端的业务数据
            //堆内
            //ByteBuffer buffer = ByteBuffer.allocate(4096);
            //堆外  直接内存缓冲区
            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
            Iterator<SocketChannel> iter = socketChannels.iterator();
            while (iter.hasNext()){
                SocketChannel channel = iter.next();
                //不会阻塞
                int read = channel.read(buffer);
                if (read > 0){
                    /**
                     * limit = position;position = 0;mark = -1;
                     * 翻转，也就是让flip之后的position到limit这块区域变成之前的0到position这块，
                     * 翻转就是将一个处于存数据状态的缓冲区变为一个处于准备取数据的状态
                     */
                    buffer.flip();
                    byte[] bytes = new byte[buffer.limit()];
                    buffer.get(bytes);
                    System.out.print(channel.getRemoteAddress() + " : " + new String(bytes));
                    /**
                     * 把从position到limit中的内容移到0到limit-position的区域内，
                     * position和limit的取值也分别变成limit-position、capacity。
                     * 如果先将positon设置到limit，再compact，那么相当于clear()
                     */
                    buffer.compact();
                    /**
                     * 相对写，向position的位置写入一个byte，并将postion+1，为下次读写作准备
                     */
                    buffer.put(bytes);
                    buffer.flip();
                    channel.write(buffer);
                    buffer.clear();
                }
            }
        }
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
