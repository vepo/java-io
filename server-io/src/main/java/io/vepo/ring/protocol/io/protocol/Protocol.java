package io.vepo.ring.protocol.io.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public interface Protocol {
    public static long TICK = 500;

    static int readInteger(InputStream stream) throws IOException {
        var content = new byte[4];
        stream.read(content);
        return ByteBuffer.wrap(content)
                         .getInt();
    }

    static long readLong(InputStream stream) throws IOException {
        var content = new byte[8];
        stream.read(content);
        return ByteBuffer.wrap(content)
                         .getLong();
    }

    static String readString(InputStream stream) throws IOException {
        var sLength = readInteger(stream);
        byte[] sContent = new byte[sLength];
        stream.read(sContent);
        return new String(sContent, Charset.defaultCharset());
    }

    byte[] serialize();
}
