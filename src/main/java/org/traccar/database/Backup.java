package org.traccar.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Backup {
  private static final Logger LOGGER = LoggerFactory.getLogger(Backup.class);

  private static class StreamGobbler implements Runnable {
    private InputStream inputStream;
    private Consumer<String> consumer;

    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
      this.inputStream = inputStream;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(inputStream)).lines()
          .forEach(consumer);
    }
  }

  public static File getDatabaseBackup(String outputPath, String username, String password, String host,
      String database,
      boolean ignoreCreate, List<String> ignoreTablesNames) throws IOException, InterruptedException {
    String outputFile = String.format("%s/db.sql", outputPath);
    File backupFile = null;
    Process process;
    StringBuilder command = new StringBuilder(
        String.format("mysqldump -h %s -u %s -p %s ", host, username, database));

    if (ignoreCreate) {
      command.append("--no-create-info ");
    }

    for (String name : ignoreTablesNames) {
      command.append(String.format("--ignore-table=%s.%s ", database, name));
    }

    command.append("--lock-tables ");
    command.append(String.format("-r %s ", outputFile));
    command.append(String.format("--password=%s ", password));

    LOGGER.warn("Running Backup with: " + command.toString());

    process = Runtime.getRuntime().exec(command.toString());

    StreamGobbler gobbler = new StreamGobbler(process.getInputStream(), LOGGER::warn);
    Future<?> future = Executors.newSingleThreadExecutor().submit(gobbler);

    int exitCode = process.waitFor();
    assert exitCode == 0;

    backupFile = new File(outputFile);

    try {
      future.get();
    } catch (ExecutionException e) {
      LOGGER.error(e.getMessage());
    }

    return backupFile;
  }
}
