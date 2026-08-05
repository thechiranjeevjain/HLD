package com.example.distributeddb;

final class CommandHandler {
    private final ClusterNode node;

    CommandHandler(ClusterNode node) {
        this.node = node;
    }

    String handle(String rawLine) {
        String line = rawLine == null ? "" : rawLine.trim();
        if (line.isBlank()) {
            return "ERROR empty command";
        }
        String command = line.split("\\s+", 2)[0].toUpperCase();
        try {
            return switch (command) {
                case "PING" -> node.ping();
                case "HELP" -> node.help();
                case "STATUS" -> node.status();
                case "RING" -> node.ringSummary();
                case "RECOVER" -> "OK recovered=" + node.recoverFromPeers();
                case "GET" -> handleGet(line);
                case "PUT" -> handlePut(line);
                case "DELETE" -> handleDelete(line);
                case "COORD_PUT" -> handleCoordinatedPut(line);
                case "COORD_DELETE" -> handleCoordinatedDelete(line);
                case "INTERNAL_GET" -> handleInternalGet(line);
                case "INTERNAL_PUT" -> handleInternalPut(line);
                case "INTERNAL_DELETE" -> handleInternalDelete(line);
                case "INTERNAL_SYNC" -> handleInternalSync(line);
                default -> "ERROR unknown command=" + command;
            };
        } catch (RuntimeException ex) {
            return "ERROR " + ex.getMessage();
        }
    }

    private String handlePut(String line) {
        String[] fields = line.split("\\s+", 3);
        if (fields.length != 3) {
            return "ERROR usage=PUT <key> <value>";
        }
        return node.put(fields[1], fields[2]);
    }

    private String handleGet(String line) {
        String[] fields = line.split("\\s+", 2);
        if (fields.length != 2) {
            return "ERROR usage=GET <key>";
        }
        return node.get(fields[1]);
    }

    private String handleDelete(String line) {
        String[] fields = line.split("\\s+", 2);
        if (fields.length != 2) {
            return "ERROR usage=DELETE <key>";
        }
        return node.delete(fields[1]);
    }

    private String handleCoordinatedPut(String line) {
        String[] fields = line.split("\\s+", 3);
        if (fields.length != 3) {
            return "ERROR usage=COORD_PUT <base64-key> <base64-value>";
        }
        return node.coordinatePut(Codec.decode(fields[1]), Codec.decode(fields[2]));
    }

    private String handleCoordinatedDelete(String line) {
        String[] fields = line.split("\\s+", 2);
        if (fields.length != 2) {
            return "ERROR usage=COORD_DELETE <base64-key>";
        }
        return node.coordinateDelete(Codec.decode(fields[1]));
    }

    private String handleInternalGet(String line) {
        String[] fields = line.split("\\s+", 2);
        if (fields.length != 2) {
            return "ERROR usage=INTERNAL_GET <base64-key>";
        }
        return node.internalGet(Codec.decode(fields[1]));
    }

    private String handleInternalPut(String line) {
        String[] fields = line.split("\\s+", 4);
        if (fields.length != 4) {
            return "ERROR usage=INTERNAL_PUT <base64-key> <version> <base64-value>";
        }
        return node.applyInternalPut(Codec.decode(fields[1]), Long.parseLong(fields[2]), Codec.decode(fields[3]));
    }

    private String handleInternalDelete(String line) {
        String[] fields = line.split("\\s+", 3);
        if (fields.length != 3) {
            return "ERROR usage=INTERNAL_DELETE <base64-key> <version>";
        }
        return node.applyInternalDelete(Codec.decode(fields[1]), Long.parseLong(fields[2]));
    }

    private String handleInternalSync(String line) {
        String[] fields = line.split("\\s+", 2);
        if (fields.length != 2) {
            return "ERROR usage=INTERNAL_SYNC <target-node-id>";
        }
        return node.internalSync(fields[1]);
    }
}
