package com.wq.netty.core.pool;

import com.wq.netty.core.proto.AbstractProtocol;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:用于实际用于回调，接收server响应的处理器
 * @Resource:
 */
public interface CallBackProcessor<T extends AbstractProtocol> {

    //处理什么类型的对象
    void process(T param);

    String uuid();
}
