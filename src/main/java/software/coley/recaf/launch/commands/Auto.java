package software.coley.recaf.launch.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Command for checking the currently installed version of Recaf.
 */
@Command(name = "auto", description = {
		"Runs the suggested commands in order: ",
		" - compatibility",
		" - update-jfx -maxc 30 -maxs 60000000 -k",
		" - update",
		" - run",
		"If one of the commands fails, the following ones are skipped."
})
public class Auto implements Callable<Void> {
	@Override
	public Void call() {
		// Ensure compatibility
		if (!Compatibility.isCompatible(false, false))
			return null;

		// Update JavaFX when possible, clearing outdated cache entries when it gets too cluttered
		UpdateJavaFX.checkClearCache(false, true, 30, 64_000_000);
		if (UpdateJavaFX.update(false) == null)
			return null;

		// Update Recaf.
		UpdateRecafFromCI.update("dev4");
		// TODO: When released, replace with - UpdateRecaf.update(true);

		// Run recaf.
		Run.run(true, null);
		return null;
	}
}
