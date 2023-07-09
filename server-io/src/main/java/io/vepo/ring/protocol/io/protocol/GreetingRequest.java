package io.vepo.ring.protocol.io.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record GreetingRequest(String message, String name) implements ProtocolRequest {

    @Override
    public byte[] serialize() {
        var messageContent = message.getBytes(Charset.defaultCharset());
        var nameContent = name.getBytes(Charset.defaultCharset());
        return ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + Integer.BYTES + message.length() + name.length())
                         .put(ProtocolMessage.GREETING.getId())
                         .putInt(messageContent.length)
                         .put(messageContent)
                         .putInt(nameContent.length)
                         .put(nameContent)
                         .array();
    }

    public static GreetingRequest from(InputStream inputStream) {
        try {
            return new GreetingRequest(Protocol.readString(inputStream), Protocol.readString(inputStream));
        } catch (IOException e) {
            throw new IllegalStateException("Invalid response!", e);
        }
    }
}
