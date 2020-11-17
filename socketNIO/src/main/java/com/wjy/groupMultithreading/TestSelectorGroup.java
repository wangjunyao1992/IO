package com.wjy.groupMultithreading;

/**
 * @author wangjunyao
 * @version 1.0.0
 * @Description TODO
 * @createTime 2020年11月17日 15:13:00
 */
public class TestSelectorGroup {

    public static void main(String[] args) throws Exception {
        SelectorThreadGroup bossGroup = new SelectorThreadGroup(3);
        SelectorThreadGroup workGroup = new SelectorThreadGroup(3);
        bossGroup.setWorkGroup(workGroup);
        bossGroup.bind(7777);
        bossGroup.bind(8888);
        bossGroup.bind(9999);
    }
}
