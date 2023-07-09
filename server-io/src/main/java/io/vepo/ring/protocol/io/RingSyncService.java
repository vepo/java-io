package io.vepo.ring.protocol.io;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "ring-service-sync", mixinStandardHelpOptions = true, version = "ring-protocol 1.0", description = "Ring Sync Service")
public class RingSyncService implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(RingSyncService.class);
    public static final String INSTANCE_ID = UUID.randomUUID().toString();

    @Option(names = {
        "-s",
        "--server" }, description = "Server port", required = true)
    private int serverPort;

    @Option(names = {
        "-c",
        "--client" }, description = "Client port", required = true)
    private int clientPort;

    @Option(names = {
        "-n",
        "--name" }, required = true)
    private String name;

    public static void main(String[] args) {
        MDC.put("PID", RingSyncService.INSTANCE_ID);
        int exitCode = new CommandLine(new RingSyncService()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        var threadPool = Executors.newFixedThreadPool(10);
        var running = new AtomicBoolean(true);
        var latch = new CountDownLatch(1);
        logger.info("My names is                      : {}", name);
        logger.info("Starting server at port          : {}", serverPort);
        logger.info("Communicating with server at port: {}", clientPort);
        SyncClient client = new SyncClient(clientPort, running, this.name);
        threadPool.submit(client);
        try (ServerSocket server = new ServerSocket(serverPort)) {

            Runtime.getRuntime()
                   .addShutdownHook(new Thread("shutdown-hook") {
                       @Override
                       public void run() {
                           logger.info("Shutdown signal received!");
                           running.set(false);
                           try {
                               try {
                                   server.close();
                               } catch (IOException e) {
                                   logger.info("Error!", e);
                               }
                               logger.info("Waiting for service shutdown!");
                               latch.await();
                           } catch (InterruptedException e) {
                               // nothing to do kill this thread
                               Thread.currentThread().interrupt();
                           }
                           logger.info("Finished!");
                       }
                   });
            while (running.get()) {
                try {
                    Socket clientSocket = server.accept();
                    threadPool.submit(new SyncServer(clientSocket, running, this.name, client::calculate));
                } catch (SocketTimeoutException ste) {
                    logger.debug("No connection accepted!");
                }
            }
        } catch (SocketException ex) {
            if (running.get()) {
                logger.error("Socket closed!", ex);
            }
        }
        logger.info("Done!");
        latch.countDown();
        return 0;
    }
}
