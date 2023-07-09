package io.vepo.ring.protocol.io;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import io.vepo.ring.protocol.io.protocol.DoMathRequest;
import io.vepo.ring.protocol.io.protocol.DoMathResponse;
import io.vepo.ring.protocol.io.protocol.GreetingRequest;
import io.vepo.ring.protocol.io.protocol.GreetingResponse;
import io.vepo.ring.protocol.io.protocol.Protocol;
import io.vepo.ring.protocol.io.protocol.ProtocolMessage;

public class SyncServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SyncServer.class);

    private final Socket client;
    private final AtomicBoolean running;
    private final String name;
    private final BiConsumer<DoMathRequest, Consumer<DoMathResponse>> calculate;

    public SyncServer(Socket client, AtomicBoolean running, String name,
            BiConsumer<DoMathRequest, Consumer<DoMathResponse>> calculate) {
        this.client = client;
        this.running = running;
        this.name = name;
        this.calculate = calculate;
    }

    @Override
    public void run() {
        MDC.put("PID", RingSyncService.INSTANCE_ID);
        try (var inputStream = this.client.getInputStream();
                var outputStream = this.client.getOutputStream();) {
            logger.debug("Connection open! Reading requests");
            while (running.get()) {
                if (inputStream.available() > 0) {
                    logger.debug("Reading request...");
                    (switch (ProtocolMessage.from((byte) inputStream.read())) {
                        case GREETING -> Optional.of(GreetingRequest.from(inputStream));
                        case DO_MATH -> Optional.of(DoMathRequest.from(inputStream));
                        case UNKNOWN -> Optional.empty();
                    }).ifPresentOrElse(request -> {
                        try {
                            logger.debug("Request received! {}", request);
                            if (request instanceof GreetingRequest gr) {
                                outputStream.write(new GreetingResponse(String.format("Hi, %s. I'm %s.", gr.name(),
                                                                                      this.name),
                                                                        this.name).serialize());
                                outputStream.flush();
                            } else if (request instanceof DoMathRequest dmr) {
                                this.calculate.accept(dmr, response -> {
                                    try {
                                        outputStream.write(response.serialize());
                                        outputStream.flush();
                                    } catch (IOException e) {
                                        logger.error("ERROR", e);
                                    }
                                });
                            }
                            logger.debug("Request processed!");
                        } catch (IOException ex) {
                            logger.error("Error", ex);
                        } catch (RuntimeException re) {
                            logger.error("ERROR", re);
                        }
                    }, () -> logger.warn("Invalid request!"));
                } else {
                    try {
                        sleep(Protocol.TICK);
                    } catch (InterruptedException e) {
                        currentThread().interrupt();
                    }
                }
            }
            logger.debug("Server is not running anymore...");
        } catch (IOException ioe) {
            logger.error("Error reading response!", ioe);
        } catch (RuntimeException re) {
            logger.error("ERROR", re);
        }
        logger.debug("Done!");
    }

}
