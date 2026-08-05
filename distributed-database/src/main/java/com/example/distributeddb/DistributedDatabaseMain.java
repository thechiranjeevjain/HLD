package com.example.distributeddb;

import java.util.concurrent.CountDownLatch;

public final class DistributedDatabaseMain {
    private DistributedDatabaseMain() {
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0 || hasHelp(args)) {
            printUsage();
            return;
        }
        NodeConfig config = NodeConfig.fromArgs(args);
        ClusterNode node = new ClusterNode(config);
        node.start();
        Runtime.getRuntime().addShutdownHook(new Thread(node::close, "shutdown-" + config.nodeId()));
        System.out.println("Started " + config.nodeId() + " on " + config.bindHost() + ":" + config.port());
        new CountDownLatch(1).await();
    }

    private static boolean hasHelp(String[] args) {
        for (String arg : args) {
            if (arg.equals("--help") || arg.equals("-h")) {
                return true;
            }
        }
        return false;
    }

    private static void printUsage() {
        System.out.println("""
                Usage:
                  java -jar target/distributed-database-0.1.0-SNAPSHOT.jar \\
                    --node-id node1 \\
                    --bind-host 127.0.0.1 \\
                    --port 9101 \\
                    --peers node1=127.0.0.1:9101,node2=127.0.0.1:9102,node3=127.0.0.1:9103 \\
                    --data-dir data/node1

                TCP commands:
                  PUT <key> <value>
                  GET <key>
                  DELETE <key>
                  STATUS
                  RING
                  RECOVER
                """);
    }
}
