package com.wjy;

import java.io.Closeable;

public interface ISocketServer extends Closeable {

    void start() throws Exception;

}
