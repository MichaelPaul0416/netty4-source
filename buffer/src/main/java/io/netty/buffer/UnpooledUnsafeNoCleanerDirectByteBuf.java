/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.buffer;

import io.netty.util.internal.PlatformDependent;

import java.nio.ByteBuffer;

/**
 * without Instrumented
 */
class UnpooledUnsafeNoCleanerDirectByteBuf extends UnpooledUnsafeDirectByteBuf {

    UnpooledUnsafeNoCleanerDirectByteBuf(ByteBufAllocator alloc, int initialCapacity, int maxCapacity) {
        super(alloc, initialCapacity, maxCapacity);
    }

    @Override
    protected ByteBuffer allocateDirect(int initialCapacity) {
        return PlatformDependent.allocateDirectNoCleaner(initialCapacity);// 依赖于JDK提供的ByteBuffer
    }

    ByteBuffer reallocateDirect(ByteBuffer oldBuffer, int initialCapacity) {
        return PlatformDependent.reallocateDirectNoCleaner(oldBuffer, initialCapacity);
    }

    @Override
    protected void freeDirect(ByteBuffer buffer) {
        PlatformDependent.freeDirectNoCleaner(buffer);
    }

    @Override
    public ByteBuf capacity(int newCapacity) {
        checkNewCapacity(newCapacity);// 如果是大小<4M,那么最小的扩容大小是64，然后64 <<= 1的倍数来扩容

        int oldCapacity = capacity();
        if (newCapacity == oldCapacity) {
            return this;
        }

        ByteBuffer newBuffer = reallocateDirect(buffer, newCapacity);// 扩容directByteBuffer的时候，需要重新申请

        if (newCapacity < oldCapacity) {
            if (readerIndex() < newCapacity) {
                if (writerIndex() > newCapacity) {
                    // 如果readIndex小于新的容量，并且writeIndex大于新的容量，那么就只需要将writeIndex设置为新的容量即可
                    // readIndex<newCapacity && writeIndex<=newCapacity的情况，就不需要调整索引位置
                    writerIndex(newCapacity);
                }
            } else {
                // readIndex>=newCapacity，那就相当于数据已经全部读取完毕了，此时只需要将readIndex和writeIndex全部设置为newCapacity即可
                setIndex(newCapacity, newCapacity);
            }
        }
        // 当前实例对象内部持有一个ByteBuffer对象的引用
        setByteBuffer(newBuffer, false);
        return this;
    }
}
