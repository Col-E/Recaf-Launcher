package software.coley.recaf.launch.commands;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import software.coley.recaf.launch.util.CommonPaths;
import software.coley.recaf.launch.util.Web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Command for updating Recaf from whatever is on the CI's latest artifact.
 */
@Command(name = "update-ci", description = "Installs the latest artifact from CI", hidden = true)
public class UpdateRecafFromCI implements Callable<Boolean> {
	@Option(names = {"-b", "--branch"}, description = {
			"Branch name to pull from.",
			"By default, no branch is used.",
			"Whatever is found first on the CI will be grabbed."
	})
	private String branch;

	@Override
	public Boolean call() {
		Predicate<String> branchMatcher = branch == null ?
				null :
				name -> branch.equalsIgnoreCase(name);
		return update(branchMatcher);
	}

	/**
	 * @param branchMatcher
	 * 		Filter to whitelist only certain branches.
	 *
	 * @return {@code true} on update success.
	 */
	public static boolean update(Predicate<String> branchMatcher) {
		try {
			// Get artifacts.
			// They appear in sorted order by time.
			String artifactsJson = Web.getText("https://api.github.com/repos/Col-E/Recaf/actions/artifacts");
			JsonObject artifacts = Json.parse(artifactsJson).asObject();
			JsonArray listing = artifacts.get("artifacts").asArray();
			for (JsonValue artifactValue : listing) {
				JsonObject artifact = artifactValue.asObject();

				// Skip expired artifacts
				boolean expired = artifact.getBoolean("expired", false);
				if (expired)
					continue;

				// Skip non snapshot builds
				String name = artifact.getString("name", "?");
				if (!name.equals("snapshot-build"))
					continue;

				// Skip branches if predicate is given
				JsonObject workflowRun = artifact.get("workflow_run").asObject();
				if (branchMatcher != null) {
					String branch = workflowRun.getString("head_branch", "?");
					if (!branchMatcher.test(branch))
						continue;
				}

				// Size sanity check
				long size = artifact.getLong("size_in_bytes", -1);
				if (size <= 0)
					continue;

				// You can't just use the 'archive_download_url' value because we don't have permissions without
				// including an access token... which, I'm not going to do.
				// You also cannot reconstruct the URL: https://github.com/Col-E/Recaf/suites/<check_suite_id>/artifacts/<artifact_id>
				// as that is also locked behind requiring an account or access token.
				long workflowRunId = workflowRun.getLong("id", -1);

				// Instead we use a graciously hosted public service that will generate a link for you.
				// https://nightly.link/Col-E/Recaf/actions/runs/<run-id>/snapshot-build.zip
				//  - Feeling generous? You can sponsor the nightly link mirror service: https://github.com/sponsors/oprypin
				String downloadUrl = "https://nightly.link/Col-E/Recaf/actions/runs/" + workflowRunId + "/snapshot-build.zip";
				byte[] download = Web.getBytes(downloadUrl);
				try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(download))) {
					// Extract the jar from the zip
					while (true) {
						ZipEntry entry = zip.getNextEntry();
						if (entry == null) break;
						if (entry.getName().toLowerCase().contains(".jar"))
							Files.copy(zip, CommonPaths.getRecafJar(), StandardCopyOption.REPLACE_EXISTING);
					}
				}
				return true;
			}

			System.err.println("No matching CI artifacts");
			return false;
		} catch (IOException ex) {
			System.err.println("Failed to download/parse CI artifacts");
			ex.printStackTrace();
			return false;
		}
	}
}
