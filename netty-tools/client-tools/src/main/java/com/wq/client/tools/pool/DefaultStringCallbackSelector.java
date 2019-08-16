package com.wq.client.tools.pool;

import com.wq.netty.core.pool.CallBackProcessor;
import com.wq.netty.core.pool.ProtocolCallbackSelector;
import com.wq.client.tools.core.proto.StringProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:
 * @Resource:
 */
public class DefaultStringCallbackSelector implements ProtocolCallbackSelector<StringProtocol> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String,CallBackProcessor<StringProtocol>> registerMap = new ConcurrentHashMap<>();

    private boolean preCheck(CallBackProcessor<StringProtocol> callback) {
        if(registerMap.containsKey(callback.uuid())){
            logger.warn("已经注册了uuid为{}的callback");
            return true;
        }

        return false;
    }

    @Override
    public CallBackProcessor<StringProtocol> select(String uuid) {
        return this.registerMap.get(uuid);
    }

    @Override
    public void registerCallback(CallBackProcessor<StringProtocol> callback, boolean covered) {
        if(preCheck(callback)){
            if(covered){
                logger.info("覆盖key:{}的callback",callback.uuid());
                this.registerMap.put(callback.uuid(),callback);
            }

            return;
        }

        this.registerMap.put(callback.uuid(),callback);
    }

    @Override
    public void registerCallback(CallBackProcessor<StringProtocol> callBackProcessor) {
        if(preCheck(callBackProcessor)){
            return;
        }

        this.registerMap.put(callBackProcessor.uuid(),callBackProcessor);
    }

    public static String generatorUuid(){
        return UUID.randomUUID().toString();
    }
}
