package ascalo19.itemsync;

import java.util.List;


public class SynchronizationEngine implements Runnable {
	private SynchronizedSystem systemA;
	private SynchronizedSystem systemB;

	public SynchronizationEngine(SynchronizedSystem systemA, SynchronizedSystem systemB) {
		this.systemA = systemA;
		this.systemB = systemB;
	}

	@Override
	public void run() {

		try {
			sync(systemA, systemB);
			sync(systemB, systemA);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void sync(SynchronizedSystem source, SynchronizedSystem target) throws Exception {

		List<Item> created = target.getCreated();
		List<Item> updated = target.getUpdated();
		List<Item> deleted = target.getDeleted();

		for (Item i : source.getCreated()) {
			if (created.contains(i)) {
				java.lang.System.err.println("Error " + i);
			} else if (updated.contains(i)) {
				java.lang.System.err.println("Error " + i);
			} else if (deleted.contains(i)) {
				java.lang.System.err.println("Error " + i);
			} else {
				target.create(i);
			}
		}

		for (Item i : source.getUpdated()) {
			if (created.contains(i)) {
				java.lang.System.err.println("Error " + i);
			} else if (updated.contains(i)) {
				java.lang.System.err.println("Error " + i);
			} else if (deleted.contains(i)) {
				java.lang.System.err.println("Error " + i);
			} else {
				target.update(i);
			}
		}

		for (Item i : source.getDeleted()) {
			if (created.contains(i)) {
				java.lang.System.err.println("Error " + i);
			} else if (updated.contains(i)) {
				java.lang.System.err.println("Error " + i);
			} else if (deleted.contains(i)) {
				java.lang.System.err.println("Error " + i);
			} else {
				target.delete(i);
			}
		}

	}
}
