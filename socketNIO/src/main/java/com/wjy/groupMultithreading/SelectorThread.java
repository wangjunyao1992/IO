package com.wjy.groupMultithreading;

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author wangjunyao
 * @version 1.0.0
 * @Description TODO
 * @createTime 2020年11月17日 10:02:00
 */
public class SelectorThread extends ThreadLocal<LinkedBlockingQueue<Channel>> implements Runnable {

    private Selector selector;

    //工作线程
    private SelectorThreadGroup workGroup;

    //每个线程独有的队列
    private final LinkedBlockingQueue<Channel> queue = get();

    @Override
    protected LinkedBlockingQueue<Channel> initialValue() {
        return new LinkedBlockingQueue<>();
    }

    public SelectorThread(SelectorThreadGroup workGroup){
        this.workGroup = workGroup;
        try {
            selector = Selector.open();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public LinkedBlockingQueue queue(){
        return queue;
    }

    public void setWorkGroup(SelectorThreadGroup workGroup){
        this.workGroup = workGroup;
    }

    public void wakeup(){
        selector.wakeup();
    }

    @Override
    public void run() {
        while (true){
            try {
                int select = selector.select();
                if (select > 0){
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()){
                        SelectionKey selectionKey = iterator.next();
                        iterator.remove();
                        if (selectionKey.isAcceptable()){
                            acceptHandler(selectionKey);
                        }else if (selectionKey.isReadable()){
                            readHandler(selectionKey);
                        }else if (selectionKey.isWritable()){

                        }
                    }
                }
                if (!queue.isEmpty()){
                    Channel channel = queue.take();
                    if (channel instanceof ServerSocketChannel){
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) channel;
                        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                        System.out.println(Thread.currentThread().getName()+" register listen");
                    }else if (channel instanceof SocketChannel){
                        SocketChannel socketChannel = (SocketChannel) channel;
                        ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
                        socketChannel.register(selector, SelectionKey.OP_READ, buffer);
                        System.out.println(Thread.currentThread().getName()+" register client: " + socketChannel.getRemoteAddress());
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void readHandler(SelectionKey selectionKey) {
        System.out.println(Thread.currentThread().getName()+" read......");
        ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        buffer.clear();
        while (true){
            try {
                int read = socketChannel.read(buffer);
                if (read > 0){
                    buffer.flip();
                    while (buffer.hasRemaining()){
                        socketChannel.write(buffer);
                    }
                }else if (read == 0){
                    break;
                }else if (read < 0){
                    //客户端断开了
                    System.out.println("client: " + socketChannel.getRemoteAddress()+"closed......");
                    selectionKey.cancel();
                    break;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void acceptHandler(SelectionKey selectionKey) throws Exception {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        workGroup.register(socketChannel);
    }
}
