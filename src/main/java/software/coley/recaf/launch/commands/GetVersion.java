package software.coley.recaf.launch.commands;

import picocli.CommandLine.Command;
import software.coley.recaf.launch.info.RecafVersion;

import java.util.concurrent.Callable;

/**
 * Command for checking the currently installed version of Recaf.
 */
@Command(name = "version", description = "Checks what version of Recaf is installed locally")
public class GetVersion implements Callable<RecafVersion> {
	@Override
	public RecafVersion call() {
		RecafVersion installedVersion = RecafVersion.getInstalledVersion(true);
		if (installedVersion != null)
			System.out.println(installedVersion.getVersion());
		return installedVersion;
	}
}
