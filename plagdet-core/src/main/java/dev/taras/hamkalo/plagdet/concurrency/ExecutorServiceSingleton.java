package dev.taras.hamkalo.plagdet.concurrency;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExecutorServiceSingleton {

  private static ExecutorServiceSingleton executorServiceSingleton;

  @Setter
  @Getter
  private static int numberOfProcessors = Runtime.getRuntime().availableProcessors();

  ExecutorService executorService;

  boolean isInitialized = false;

  public static synchronized ExecutorServiceSingleton getExecutorService() {
    if (executorServiceSingleton == null) {
      executorServiceSingleton = new ExecutorServiceSingleton();
    }

    return executorServiceSingleton;
  }

  private ExecutorServiceSingleton() {
  }

  public void init() {
    if (isInitialized) {
      return;
    }

    this.executorService = Executors.newFixedThreadPool(
      ExecutorServiceSingleton.numberOfProcessors
    );
    registerShutdownHook();
    this.isInitialized = true;
  }

  public void deInit() {
    if (!isInitialized) {
      return;
    }

    this.executorService.shutdownNow();
    this.isInitialized = false;
  }

  public <T> List<Future<T>> invokeAll(
    @NotNull List<Callable<T>> tasks
  ) throws InterruptedException {
    return this.executorService.invokeAll(tasks);
  }

  public <T> Future<T> submit(Callable<T> task) {
    return this.executorService.submit(task);
  }

  private void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (executorService.isTerminated()) {
        return;
      }
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (InterruptedException e) {
        executorService.shutdownNow();
      }
    }));
  }
}
