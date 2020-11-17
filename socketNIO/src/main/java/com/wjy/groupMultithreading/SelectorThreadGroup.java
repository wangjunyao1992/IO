package com.wjy.groupMultithreading;

import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wangjunyao
 * @version 1.0.0
 * @Description TODO
 * @createTime 2020年11月17日 10:01:00
 */
public class SelectorThreadGroup {

    private ServerSocketChannel serverSocketChannel;

    //主线程
    private SelectorThread[] selectorThreads;

    //工作线程
    private SelectorThreadGroup workGroup = this;

    /**
     * 线程数量
     */
    private int selectorThreadNum;

    private AtomicInteger seq = new AtomicInteger(0);

    public SelectorThreadGroup(int selectorThreadNum){
        this.selectorThreadNum = selectorThreadNum;
        selectorThreads = new SelectorThread[selectorThreadNum];
        for (int i = 0; i < selectorThreadNum; i++) {
            selectorThreads[i] = new SelectorThread(workGroup);
            new Thread(selectorThreads[i]).start();
        }
    }

    public void setWorkGroup(SelectorThreadGroup workGroup){
        this.workGroup = workGroup;
    }

    public void bind(int port) throws Exception {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(port));
        register(serverSocketChannel);
    }

    public void register(Channel channel) throws InterruptedException {
        if (channel instanceof  ServerSocketChannel){
            SelectorThread selectorThread = voteBossSelectorThread();
            selectorThread.setWorkGroup(workGroup);
            selectorThread.queue().put(channel);
            selectorThread.wakeup();
        }else if (channel instanceof SocketChannel){
            SelectorThread selectorThread = voteWorkerSelectorThread();
            selectorThread.queue().add(channel);
            selectorThread.wakeup();
        }
    }

    //选主
    private SelectorThread voteBossSelectorThread() {
        int threadIndex = seq.incrementAndGet() % selectorThreadNum;
        return selectorThreads[threadIndex];
    }

    //选工作线程
    private SelectorThread voteWorkerSelectorThread(){
        int threadIndex = workGroup.seq.getAndIncrement() % workGroup.selectorThreadNum;
        return workGroup.selectorThreads[threadIndex];
    }

}
