package com.wjy;

import java.io.Closeable;

/**
 * @author wangjunyao
 * @version 1.0.0
 * @Description TODO
 * @createTime 2020年11月09日 14:09:00
 */
public interface IServerSocketNIO extends Closeable {

    void start() throws Exception;

}
