package com.wq.client.tools.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassLoaderUtil {

    private static final Logger logger = LoggerFactory.getLogger(ClassLoaderUtil.class);

    public static ClassLoader getClassLoader(Class clazz){
        if(clazz == null){
            logger.warn("empty class and return current thread classloader...");
            return Thread.currentThread().getContextClassLoader();
        }

        return clazz.getClassLoader();
    }
}
