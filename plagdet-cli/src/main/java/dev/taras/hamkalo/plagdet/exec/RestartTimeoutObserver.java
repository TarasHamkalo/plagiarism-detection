package dev.taras.hamkalo.plagdet.exec;

import dev.taras.hamkalo.plagdet.Plagdet;
import dev.taras.hamkalo.plagdet.concurrency.ExecutorServiceSingleton;
import dev.taras.hamkalo.plagdet.io.session.SessionManager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.TimeoutObserver;
import org.apache.commons.exec.Watchdog;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,  makeFinal = true)
public class RestartTimeoutObserver implements TimeoutObserver {

  @NotNull Plagdet plagdet;

  @NotNull Thread interruptTarget;

  @NotNull String[] args;

  @Getter AtomicBoolean shouldRestart = new AtomicBoolean(false);

  @Override
  public void timeoutOccured(Watchdog w) {
    shouldRestart.set(true);
    log.debug("Interrupting");
    plagdet.getComparisonPipeline().getIsRunning().set(false);
    interruptTarget.interrupt();
//    try {
//      Thread.sleep(5000);
//    } catch (InterruptedException ignore) {
//    }


    SessionManager sessionManager = new SessionManager();
    sessionManager.saveSession(plagdet);

    ExecutorServiceSingleton.getExecutorService().deInit();
    restart();
  }

  @SneakyThrows
  private void restart() {
    log.debug("Restarting from PID " +  ProcessHandle.current().pid());

    String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

    List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();

    String jarPath = new File(RestartTimeoutObserver.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();

    List<String> command = new ArrayList<>();
    command.add(javaBin);
    command.addAll(vmArguments);
    command.add("-cp");
    command.add(System.getProperty("java.class.path"));

    if (jarPath.endsWith(".jar")) {
      command.clear();
      command.add(javaBin);
      command.addAll(vmArguments);
      command.add("-jar");
      command.add(jarPath);
    }

    command.addAll(Arrays.asList(args));

    ProcessBuilder builder = new ProcessBuilder(command);
    builder.inheritIO();
    Process process = builder.start();
    log.debug("New process PID " +  process.pid());

    log.debug("Exiting");
    System.exit(0);

  }
}
