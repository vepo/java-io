package io.vepo.ring.protocol.io.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record DistributedId(String instanceId, Long sequentialId) implements Protocol {

    @Override
    public byte[] serialize() {
        byte[] instanceIdContent = instanceId.getBytes(Charset.defaultCharset());
        return ByteBuffer.allocate(Integer.BYTES + instanceId.length() + Long.BYTES)
                         .putInt(instanceIdContent.length)
                         .put(instanceIdContent)
                         .putLong(sequentialId)
                         .array();
    }

    public int bytes() {
        return Integer.BYTES + instanceId.getBytes(Charset.defaultCharset()).length + Long.BYTES;
    }

    public static DistributedId from(InputStream inputStream) throws IOException {
        return new DistributedId(Protocol.readString(inputStream), Protocol.readLong(inputStream));
    }
}
