package com.wq.netty.core.proto;

import java.io.Serializable;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:
 * @Resource:
 */
public abstract class AbstractProtocol implements Protocol,Serializable {

    public abstract <T> T getProtocolBody();
}
