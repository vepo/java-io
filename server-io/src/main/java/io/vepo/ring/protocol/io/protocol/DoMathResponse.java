package io.vepo.ring.protocol.io.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public record DoMathResponse(DistributedId id, int result) implements ProtocolResponse {

    @Override
    public byte[] serialize() {
        return ByteBuffer.allocate(Byte.BYTES + id.bytes() + Integer.BYTES)
                         .put(ProtocolMessage.DO_MATH.getId())
                         .put(id.serialize())
                         .putInt(result)
                         .array();
    }

    public static DoMathResponse from(InputStream inputStream) {
        try {
            return new DoMathResponse(DistributedId.from(inputStream),
                                      Protocol.readInteger(inputStream));
        } catch (IOException e) {
            throw new IllegalStateException("Invalid response!", e);
        }
    }

}
