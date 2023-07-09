package io.vepo.ring.protocol.io;

import static io.vepo.ring.protocol.io.RingSyncService.INSTANCE_ID;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import io.vepo.ring.protocol.io.protocol.DistributedId;
import io.vepo.ring.protocol.io.protocol.DoMathRequest;
import io.vepo.ring.protocol.io.protocol.DoMathResponse;
import io.vepo.ring.protocol.io.protocol.GreetingRequest;
import io.vepo.ring.protocol.io.protocol.GreetingResponse;
import io.vepo.ring.protocol.io.protocol.MathOperation;
import io.vepo.ring.protocol.io.protocol.Protocol;
import io.vepo.ring.protocol.io.protocol.ProtocolMessage;

public class SyncClient implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SyncClient.class);

    private final int port;
    private final AtomicBoolean running;
    private final String name;
    private final Map<DistributedId, Consumer<DoMathResponse>> callbacks;
    private final Deque<DoMathRequest> mathRequests;

    public SyncClient(int port, AtomicBoolean running, String name) {
        requireNonNull(running, "Running cannot be null!");
        requireNonNull(name, "Name cannot be null!");
        this.port = port;
        this.running = running;
        this.name = name;
        this.callbacks = synchronizedMap(new HashMap<>());
        this.mathRequests = new ConcurrentLinkedDeque<>();
    }

    public void calculate(DoMathRequest request, Consumer<DoMathResponse> callback) {
        if (request.id().instanceId().equals(INSTANCE_ID)) {
            logger.info("FINISHED Calc! {}", request);
            callback.accept(new DoMathResponse(request.id(), request.number()));
        } else {
            this.mathRequests.offer(request);
            this.callbacks.put(request.id(), callback);
        }
    }

    private void handleResponses(InputStream inputStream) {
        MDC.put("PID", RingSyncService.INSTANCE_ID);
        try {
            while (running.get()) {
                if (inputStream.available() > 0) {

                    byte messageId = (byte) inputStream.read();
                    if (messageId != -1) {
                        (switch (ProtocolMessage.from(messageId)) {
                            case GREETING -> Optional.of(GreetingResponse.from(inputStream));
                            case DO_MATH -> Optional.of(DoMathResponse.from(inputStream));
                            case UNKNOWN -> Optional.empty();
                        }).ifPresentOrElse(response -> {
                            logger.debug("Answer received! answer={}", response);

                            if (response instanceof GreetingResponse gr) {
                                logger.debug("Greetings from {}: {}", gr.name(), gr.message());
                            } else if (response instanceof DoMathResponse dmr) {
                                logger.debug("Math response! DoMathResponse={}", dmr);
                                if (dmr.id().instanceId().equals(INSTANCE_ID)) {
                                    logger.info("RESPONSE GOT! math={}", dmr);
                                } else {
                                    this.callbacks.remove(dmr.id()).accept(dmr);
                                }
                            }

                        }, () -> logger.warn("Invalid message! id={}", messageId));
                    }

                } else {
                    try {
                        sleep(Protocol.TICK);
                    } catch (InterruptedException e) {
                        currentThread().interrupt();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("Handling response is done!");
    }

    @Override
    public void run() {
        MDC.put("PID", RingSyncService.INSTANCE_ID);
        while (running.get()) {
            try {
                logger.debug("Waiting for server startup!");
                sleep(Protocol.TICK * 5);
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }

            Future<?> responseExecutor = null;
            logger.debug("Trying to connect with port={}", this.port);
            try (var socket = new Socket("localhost", this.port);
                    var outputStream = socket.getOutputStream();
                    var inputStream = socket.getInputStream();) {

                responseExecutor = newSingleThreadExecutor().submit(() -> handleResponses(inputStream));
                logger.debug("Connected started! Sending greeting...");
                outputStream.write(new GreetingRequest(String.format("Hello, I'm %s!", this.name),
                                                       this.name).serialize());
                outputStream.flush();
                logger.debug("Greeting request sent!");

                var random = new SecureRandom();
                var idGenerator = new AtomicLong(0);
                while (running.get()) {
                    logger.debug("Sending math challenge!");
                    var selectNumber = random.nextInt(10_000);
                    if (!this.mathRequests.isEmpty()) {
                        var request = this.mathRequests.pop();
                        logger.debug("Challenge from {}", request.id());
                        outputStream.write(new DoMathRequest(request.id(),
                                                             switch (request.operation()) {
                                                                 case SUM -> request.number() + selectNumber;
                                                                 case SUB -> request.number() - selectNumber;
                                                                 case MUL -> request.number() * selectNumber;
                                                                 case DIV -> request.number() / selectNumber;
                                                             },
                                                             MathOperation.random(),
                                                             String.format("(%s) %c %d",
                                                                           request.history(),
                                                                           request.operation().getOperation(),
                                                                           selectNumber)).serialize());
                        outputStream.flush();
                        logger.debug("Challenge sent!");
                    } else if (random.nextInt(100) > 30) {
                        logger.debug("New random challenge...");
                        outputStream.write(new DoMathRequest(new DistributedId(INSTANCE_ID,
                                                                               idGenerator.incrementAndGet()),
                                                             selectNumber,
                                                             MathOperation.random(),
                                                             Integer.toString(selectNumber)).serialize());
                        outputStream.flush();
                        logger.debug("Challenge sent!");
                    }
                    try {
                        sleep(Protocol.TICK);
                    } catch (InterruptedException e) {
                        currentThread().interrupt();
                    }
                }
                logger.info("Client is done!");
            } catch (IOException ex) {
                logger.warn("Error connecting with the server! trying again...", ex);

                try {
                    sleep(Protocol.TICK);
                } catch (InterruptedException e) {
                    currentThread().interrupt();
                }
            } catch (RuntimeException re) {
                logger.error("Error", re);
            } finally {
                logger.debug("Closing client...");
                if (nonNull(responseExecutor)) {
                    try {
                        responseExecutor.get();
                    } catch (ExecutionException ee) {
                        logger.error("Error on client!", ee);
                    } catch (InterruptedException ie) {
                        currentThread().interrupt();
                    }
                }
            }
        }
        logger.debug("Done!");
    }

}
