package software.coley.recaf.launcher.util;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ResettableDelayTask {
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> taskFuture;
	private final Runnable task;
	private final long delay;
	private final TimeUnit unit;

	public ResettableDelayTask(@Nonnull Runnable task, long delay, @Nonnull TimeUnit unit) {
		this.task = task;
		this.delay = delay;
		this.unit = unit;
	}

	public synchronized void startOrReset() {
		if (taskFuture != null && !taskFuture.isDone())
			taskFuture.cancel(false);
		taskFuture = scheduler.schedule(task, delay, unit);
	}

	public void stop() {
		if (taskFuture != null && !taskFuture.isDone())
			taskFuture.cancel(true);
		scheduler.shutdownNow();
	}
}