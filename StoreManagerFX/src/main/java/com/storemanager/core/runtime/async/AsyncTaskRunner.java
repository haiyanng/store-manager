package com.storemanager.core.runtime.async;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AsyncTaskRunner {

    private static final ExecutorService EXECUTOR =
            Executors.newCachedThreadPool(runnable -> {
                Thread thread =
                        new Thread(
                                runnable,
                                "storemanager-async-task"
                        );

                thread.setDaemon(true);

                return thread;
            });

    public static <T> void run(
            Callable<T> task,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError,
            Runnable onFinally
    ) {

        AuditActorContext.Actor actor = AuditActorContext.capture();
        EXECUTOR.submit(() -> {
            try {

                T result =
                        AuditActorContext.callAs(actor, task);

                runOnUiThread(
                        () -> {
                            if (onSuccess != null) {
                                onSuccess.accept(result);
                            }
                        }
                );

            } catch (Throwable throwable) {

                runOnUiThread(
                        () -> {
                            if (onError != null) {
                                onError.accept(throwable);
                            }
                        }
                );

            } finally {

                runOnUiThread(
                        () -> {
                            if (onFinally != null) {
                                onFinally.run();
                            }
                        }
                );
            }
        });
    }

    private static void runOnUiThread(
            Runnable runnable
    ) {

        if (Platform.isFxApplicationThread()) {
            runnable.run();
            return;
        }

        Platform.runLater(runnable);
    }
}
