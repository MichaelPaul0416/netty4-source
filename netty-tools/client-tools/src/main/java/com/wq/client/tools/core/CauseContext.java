package com.wq.client.tools.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: wangqiang20995
 * @Date:2019/5/30
 * @Description:
 * @Resource:
 */
public class CauseContext {

    private Throwable e;

    private CauseContext next;

    private static Logger logger = LoggerFactory.getLogger(CauseContext.class);

    public CauseContext(Throwable e){
        this.e = e;
    }

    public void next(CauseContext context){
        this.next = next;
    }

    public boolean empty(){
        return e == null;
    }

    public void throwable(Throwable e){
        if(this.e != null){
            this.next = new CauseContext(e);
            return;
        }
        this.e = e;
    }

    public CauseContext nextContext(){
        return this.next;
    }

    public Throwable exception(){
        return this.e;
    }

    public static void printException(CauseContext causeContext){
        CauseContext handler = causeContext;
        while (handler != null && !handler.empty()){
            Throwable throwable = handler.exception();
            logger.error(throwable.getMessage(),throwable);
            handler = handler.nextContext();
        }
    }
}
