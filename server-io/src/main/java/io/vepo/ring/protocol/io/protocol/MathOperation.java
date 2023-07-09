package io.vepo.ring.protocol.io.protocol;

import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.Stream;

public enum MathOperation {
    SUM((byte) 1, '+'),
    SUB((byte) 2, '-'),
    MUL((byte) 3, 'x'),
    DIV((byte) 4, '÷');

    private byte id;
    private char operation;

    MathOperation(byte id, char operation) {
        this.id = id;
        this.operation = operation;
    }

    public byte getId() {
        return id;
    }

    public char getOperation() {
        return operation;
    }

    public static MathOperation from(byte id) {
        return Stream.of(MathOperation.values())
                     .filter(m -> m.id == id).findFirst()
                     .orElseThrow(() -> new IllegalStateException(String.format("No operation with id=%d!", id)));
    }

    private static Random random = new SecureRandom();

    public static MathOperation random() {
        var operations = values();
        return operations[random.nextInt(operations.length)];
    }
}
