package com.wjy;

/**
 * 生命周期
 * @author wangjunyao
 * @version 1.0.0
 * @Description TODO
 * @createTime 2020年11月09日 14:10:00
 */
public interface ILiftCycle {

    void init() throws Exception;

    void stop() throws Exception;

}
