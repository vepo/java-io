package io.vepo.ring.protocol.io.protocol;

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ProtocolMessage {
    GREETING((byte) 1), DO_MATH((byte) 2), UNKNOWN(Byte.MAX_VALUE);

    private static final Logger logger = LoggerFactory.getLogger(ProtocolMessage.class);

    private byte id;

    ProtocolMessage(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static ProtocolMessage from(byte id) {
        return Stream.of(ProtocolMessage.values())
                     .filter(m -> m.id == id).findFirst()
                     .orElseGet(() -> {
                         logger.warn("Invalid protocol message id! id={}", id);
                         return UNKNOWN;
                     });
    }
}
