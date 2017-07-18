package savemgo.nomad;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NomadService {

	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private Future<?> future = null;
	private boolean running = false;
	private Callable<Boolean> callable;
	private int seconds;
	private boolean active = false;

	public NomadService(Callable<Boolean> callable, int seconds) {
		this.callable = callable;
		this.seconds = seconds;
	}

	public void start() {
		running = true;
		future = executor.submit(() -> {
			while (running && !active) {
				active = true;
				try {
					boolean result = callable.call();
					if (!result) {
						running = false;
						break;
					}
					Thread.sleep(seconds * 1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
				active = false;
			}
		});
	}

	public void stop() {
		running = false;
		if (future != null && !future.isDone()) {
			future.cancel(true);
			try {
				future.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
