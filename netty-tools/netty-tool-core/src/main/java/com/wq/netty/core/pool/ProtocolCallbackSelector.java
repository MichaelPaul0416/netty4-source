package com.wq.netty.core.pool;

import com.wq.netty.core.proto.AbstractProtocol;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:协议定制选择器，根据协议返回唯一的ID，使其能与能选择到最初注入的CallBack进行对应的回调
 * @Resource:
 */
public interface ProtocolCallbackSelector<T extends AbstractProtocol> {

    CallBackProcessor<T> select(String uuid);

    void registerCallback(CallBackProcessor<T> callback, boolean covered);

    void registerCallback(CallBackProcessor<T> callBackProcessor);
}
