package com.wq.client.tools;

import com.wq.client.tools.core.ClassLoaderUtil;
import com.wq.client.tools.core.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class DefaultResourceLoader implements ResourceLoader<String> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String charset ;

    public DefaultResourceLoader(){
        this.charset = Charset.defaultCharset().displayName();
    }

    public DefaultResourceLoader(String charset){
        this.charset = charset;
    }

    @Override
    public String loadSourceFromClassPath(String name) {
        ClassLoader classLoader = ClassLoaderUtil.getClassLoader(this.getClass());

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(classLoader.getResourceAsStream(name),this.charset))){

            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null){
                builder.append(line);
            }

            return builder.toString();
        }catch (IOException e) {
            logger.error(e.getMessage(),e);
        }

        return null;
    }
}
