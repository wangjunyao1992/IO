package com.wjy.multithreading;

import com.wjy.ILiftCycle;
import com.wjy.IServerSocketNIO;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.TimeUnit;

/**
 * @Description TODO 多路复用器多线程线程
 *
 * 将n个fd分组，每一组一个selector，将一个selector压到一个线程上，最好的线程数量是：cpu核数   或者   cpu核数 * 2
 * 单看一个线程：里面有一个selector，有一部分fd，且他们是线性的
 * 多个线程：他们在自己的cpu上执行，代表会有多个selector在并行，且线程内是线性的，最终达到的是并行的fd被处理
 *
 * @author wangjunyao
 * @version 1.0.0
 * @createTime 2020年11月11日 10:40:00
 */
public class ServerSocketMultiplexingMultithreading implements IServerSocketNIO, ILiftCycle {

    private final int port;

    private ServerSocketChannel serverSocket;

    private Selector selector0;

    private Selector selector1;

    private Selector selector2;

    public ServerSocketMultiplexingMultithreading(int port) {
        this.port = port;
    }

    @Override
    public void init() throws Exception {
        serverSocket = ServerSocketChannel.open();
        serverSocket.configureBlocking(false);
        serverSocket.bind(new InetSocketAddress(port));
        selector0 = Selector.open();
        selector1 = Selector.open();
        selector2 = Selector.open();
        serverSocket.register(selector0, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void stop() throws Exception {
        if (selector0 != null){
            selector0.close();
        }
        if (selector1 != null){
            selector1.close();
        }
        if (selector1 != null){
            selector1.close();
        }
        if (serverSocket != null){
            serverSocket.close();
        }

    }

    @Override
    public void start() throws Exception {
        System.out.println("server start... ...");
        //服务端线程
        SelectorThread selectorThread0 = new SelectorThread(selector0, 3);
        SelectorThread selectorThread1 = new SelectorThread(selector1);
        SelectorThread selectorThread2 = new SelectorThread(selector2);
        new Thread(selectorThread0).start();
        //只是为了确保server能先运行，正式肯定不能这个样子处理
        TimeUnit.SECONDS.sleep(5);
        new Thread(selectorThread1).start();
        new Thread(selectorThread2).start();
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
