package software.coley.recaf.launcher.util;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper around a task that can be delayed repeatedly.
 */
public class ResettableDelayTask {
	private final ScheduledExecutorService scheduler;
	private final Runnable task;
	private final long delay;
	private final TimeUnit unit;
	private ScheduledFuture<?> taskFuture;

	/**
	 * @param name
	 * 		Task name.
	 * @param task
	 * 		Task to run.
	 * @param delay
	 * 		Delay amount.
	 * @param unit
	 * 		Delay amount time units.
	 */
	public ResettableDelayTask(@Nonnull String name, @Nonnull Runnable task, long delay, @Nonnull TimeUnit unit) {
		this.task = task;
		this.delay = delay;
		this.unit = unit;

		scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread thread = new Thread(r);
			thread.setName(name);
			thread.setDaemon(true);
			return thread;
		});
	}

	/**
	 * Start, or delay the pending task.
	 */
	public synchronized void startOrReset() {
		if (taskFuture != null && !taskFuture.isDone())
			taskFuture.cancel(false);
		taskFuture = scheduler.schedule(task, delay, unit);
	}

	/**
	 * Cancel running the pending task.
	 */
	public void stop() {
		if (taskFuture != null && !taskFuture.isDone())
			taskFuture.cancel(true);
		scheduler.shutdownNow();
	}
}