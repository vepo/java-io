package io.vepo.ring.protocol.io.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record DoMathRequest(DistributedId id, int number, MathOperation operation, String history) implements ProtocolRequest {

    @Override
    public byte[] serialize() {
        var historyContent = history.getBytes(Charset.defaultCharset());
        return ByteBuffer.allocate(Byte.BYTES + id.bytes() + Integer.BYTES + Byte.BYTES + Integer.BYTES
                + historyContent.length)
                         .put(ProtocolMessage.DO_MATH.getId())
                         .put(id.serialize())
                         .putInt(number)
                         .put(operation.getId())
                         .putInt(historyContent.length)
                         .put(historyContent)
                         .array();
    }

    public static DoMathRequest from(InputStream inputStream) {
        try {
            return new DoMathRequest(DistributedId.from(inputStream),
                                     Protocol.readInteger(inputStream),
                                     MathOperation.from((byte) inputStream.read()),
                                     Protocol.readString(inputStream));
        } catch (IOException e) {
            throw new IllegalStateException("Invalid response!", e);
        }
    }

}
