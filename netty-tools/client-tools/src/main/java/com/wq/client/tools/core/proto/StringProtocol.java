package com.wq.client.tools.core.proto;

import com.wq.netty.core.proto.AbstractProtocol;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:
 * @Resource:
 */
public class StringProtocol extends AbstractProtocol {

    private String body;

    public StringProtocol(String body){
        this.body = body;
    }


    @Override
    public String protocol() {
        return "String";
    }

    @Override
    public <T> T getProtocolBody() {
        return (T) this.body;
    }
}
