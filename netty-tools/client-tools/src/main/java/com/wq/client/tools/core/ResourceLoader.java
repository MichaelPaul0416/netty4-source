package com.wq.client.tools.core;

public interface ResourceLoader<T> {

    T loadSourceFromClassPath(String name);
}
