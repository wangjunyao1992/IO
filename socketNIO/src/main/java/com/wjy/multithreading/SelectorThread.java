package com.wjy.multithreading;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description TODO selector所在线程
 *
 *
 *
 * @author wangjunyao
 * @version 1.0.0
 * @createTime 2020年11月12日 14:30:00
 */
public class SelectorThread implements Runnable{

    /**
     * 线程id
     */
    private int threadId;

    /**
     * 线程持有的selector
     */
    private Selector selector;

    /**
     * 服务端fd得到客户端之后放到指定id的序列中
     * 队列索引即为threadId
     */
    private volatile static BlockingQueue<SocketChannel>[] queue;

    /**
     * 总的线程数量   线程间共享此遍历
     */
    private volatile static int selectors;

    /**
     * 一个自增的序列，用来计算 得到的客户端fd分配给哪个线程
     */
    private static AtomicInteger seq = new AtomicInteger(0);

    /**
     * 服务端调用这个构造方法
     * @param selector
     * @param selectors
     */
    public SelectorThread(Selector selector, int selectors){
        this.selector = selector;
        SelectorThread.selectors = selectors;
        threadId = threadIdGenerator();
        init();
        System.out.println("boss " + threadId + "启动... ...");
    }

    private void init(){
        queue =new LinkedBlockingQueue[SelectorThread.selectors];
        for (int i = 0; i < SelectorThread.selectors; i++) {
            queue[i] = new LinkedBlockingQueue<SocketChannel>();
        }
    }

    public SelectorThread(Selector selector){
        this.selector = selector;
        this.threadId = threadIdGenerator();
        System.out.println("worker " + this.threadId + "启动... ...");
    }

    @Override
    public void run() {
        try{
            while (true){
                if (selector.select(10) > 0){
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()){
                        SelectionKey selectionKey = iterator.next();
                        iterator.remove();
                        if (selectionKey.isAcceptable()){//接收事件
                            acceptHandler(selectionKey);
                        }else if (selectionKey.isReadable()){//可读事件
                            readHandler(selectionKey);
                        }
                    }
                }
                /*
                 * 各个线程从自己对应的队列中取出client，注册到自己线程的selector上
                 */
                BlockingQueue<SocketChannel> threadQueue = queue[threadId];
                if (!threadQueue.isEmpty()){
                    SocketChannel client = threadQueue.take();
                    client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocateDirect(8192));
                    System.out.println("客户端：" + client.getRemoteAddress() + "注册到线程" + threadId);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void readHandler(SelectionKey selectionKey) throws IOException {
        SocketChannel client = (SocketChannel) selectionKey.channel();
        System.out.println("客户端：" + client.getRemoteAddress() + "接受到消息");
        ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();
        buffer.clear();
        while (true){
            int read = client.read(buffer);
            if (read > 0){
                buffer.flip();
                if (buffer.hasRemaining()){
                    client.write(buffer);
                }
                buffer.clear();
            }else if (read == 0){
                break;
            }else {
                System.out.println("客户端：" + client.getRemoteAddress() + "断开连接");
                client.close();
                break;
            }
        }
    }

    /**
     * 只有注册了服务端fd的线程会执行到这个方法
     * 这个线程拿到客户端fd后，需要分配一个线程处理，这里就用到了队列，
     * 在new 服务端所在的线程是，指定了总的线程数量，初始化了BlockingQueue<SocketChannel>[] queue;
     * queue的索引和线程的 threadId对应，这样将客户端放到对应索引的数组中的队列中，
     * 在run()方法中，各个选出轮询的时候，在数组中取出对应的队列，注册到自己线程的selector上，消费队列
     *
     * @param selectionKey
     * @throws IOException
     */
    private void acceptHandler(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel serverSocket = (ServerSocketChannel) selectionKey.channel();
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        /*
         * 选取一个thread，并将其放到对应的队列中
         */
        int threadId = threadIdGenerator();
        System.out.println("客户端：" + client.getRemoteAddress() + "将被注册到线程" + threadId);
        queue[threadId].add(client);
    }

    private int threadIdGenerator(){
        return SelectorThread.seq.getAndIncrement() % SelectorThread.selectors;
    }
}
