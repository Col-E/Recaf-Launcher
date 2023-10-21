package software.coley.recaf.launch.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import software.coley.recaf.launch.info.RecafVersion;

import java.util.concurrent.Callable;

/**
 * Command for checking the currently installed version of Recaf.
 */
@Command(name = "version", description = "Checks what version of Recaf is installed locally")
public class GetVersion implements Callable<RecafVersion> {
	private static final Logger logger = LoggerFactory.getLogger(GetVersion.class);

	@Override
	public RecafVersion call() {
		RecafVersion installedVersion = RecafVersion.getInstalledVersion();
		if (installedVersion != null)
			logger.info(installedVersion.getVersion());
		return installedVersion;
	}
}
