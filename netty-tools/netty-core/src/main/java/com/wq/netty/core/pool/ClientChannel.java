package com.wq.netty.core.pool;

import com.wq.netty.core.proto.AbstractProtocol;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:
 * @Resource:
 */
public interface ClientChannel<T extends AbstractProtocol> {

    boolean submit(Object param,CallBackProcessor<T> processor);
}
