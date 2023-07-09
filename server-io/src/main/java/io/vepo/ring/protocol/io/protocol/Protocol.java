package io.vepo.ring.protocol.io.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public interface Protocol {
    public static long TICK = 500;

    static int readInteger(InputStream stream) throws IOException {
        var content = new byte[4];
        int read = 0;
        while ((read += stream.read(content, read, 4 - read)) != 4)
            ;
        return ByteBuffer.wrap(content)
                         .getInt();
    }

    static long readLong(InputStream stream) throws IOException {
        var content = new byte[8];
        int read = 0;
        while ((read += stream.read(content, read, 8 - read)) != 8)
            ;
        return ByteBuffer.wrap(content)
                         .getLong();
    }

    static String readString(InputStream stream) throws IOException {
        var sLength = readInteger(stream);
        byte[] sContent = new byte[sLength];
        int read = 0;
        while ((read += stream.read(sContent, read, sLength - read)) != sLength)
            ;
        return new String(sContent, Charset.defaultCharset());
    }

    byte[] serialize();
}
