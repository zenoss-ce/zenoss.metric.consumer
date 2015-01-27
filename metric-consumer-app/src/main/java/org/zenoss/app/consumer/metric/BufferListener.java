package org.zenoss.app.consumer.metric;

public interface BufferListener {
    void onBufferUpdate(String clientId, long remainingBuffer);
}
