package approaches.symbolic.api;

import supplementary.experiments.eval.EvalGames;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Scanner;

public abstract class Endpoint {
    private static final PrintStream systemOut = System.out;
    private static final PrintStream systemErr = System.err;

    public String rawInput;

    String logFile = "cached-log.txt";
    public boolean logToFile = true;
    int maxLogSize = 100000;
    String oldLogId;

    public abstract String respond();

    public void start() {
        // Override System.out and System.err to log everything written to them
        if (logToFile) {
            System.setOut(createLoggingPrintStream("Print"));
            System.setErr(createLoggingPrintStream("Error"));
        }

        EvalGames.debug = false;

        Scanner sc = new Scanner(System.in);
        systemOut.println("Ready");

        while (sc.hasNextLine()) {
            rawInput = sc.nextLine().replace("\\n", "\n");

            String response;

            if (logToFile) {
                try {
                    response = respond();
                } catch (Exception e) {
                    response = "";
                    System.err.println("Crashed: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                response = respond();
            }

            // Output
            systemOut.println(response.replace("\n", "\\n"));
        }
        sc.close();
    }

    private PrintStream createLoggingPrintStream(String title) {
        return new PrintStream(new OutputStream() {
            final StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                char c = (char) b;
                if (c == '\n') {
                    log(title, buffer.toString());
                    buffer.setLength(0);
                } else {
                    buffer.append(c);
                }
            }
        });
    }

    public void log(String title, String message) {
        if (!logToFile) {
            System.out.println(title + ":" + message);
            return;
        }

        Path path = Paths.get(logFile);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {

            // Check if the log file exists and read the content if it does.
            String content = "";
            if (Files.exists(path)) {
                content = new String(Files.readAllBytes(path));

                while (content.length() > maxLogSize) {
                    content = content.substring(maxLogSize/10);
                }
            }

            // Prepare new log entry.
            String id = this.getClass().getSimpleName() + " - " + title + rawInput;
            StringBuilder newEntry = new StringBuilder();
            if (!id.equals(oldLogId)) {
                oldLogId = id;
                newEntry.append(System.lineSeparator());
                newEntry.append(System.lineSeparator());
                newEntry.append(this.getClass().getSimpleName() + " - " + System.currentTimeMillis() + " - " + title + ":");
                newEntry.append(System.lineSeparator());
                newEntry.append("Raw Input:");
                newEntry.append(System.lineSeparator());
                newEntry.append(rawInput);
                newEntry.append(System.lineSeparator());
                newEntry.append("Message:");
                newEntry.append(System.lineSeparator());
                newEntry.append(message);
            } else {
                newEntry.append(System.lineSeparator());
                newEntry.append(message);
            }

            // Combine old content (if any, and trimmed) with new log entry, then write to file.
            Files.write(path, (content + newEntry).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
