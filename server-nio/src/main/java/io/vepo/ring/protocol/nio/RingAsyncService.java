package io.vepo.ring.protocol.nio;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "ring-protocol-server", mixinStandardHelpOptions = true, version = "ring-protocol 1.0", description = "Start the ring protocol server")
public class RingAsyncService implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(RingAsyncService.class);

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
        int exitCode = new CommandLine(new RingAsyncService()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        logger.info("My names is                      : {}", name);
        logger.info("Starting server at port          : {}", serverPort);
        logger.info("Communicating with server at port: {}", clientPort);
        return 0;
    }
}
