package software.coley.recaf.launcher.util;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

/**
 * Basic web utils.
 */
public class Web {
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";

	/**
	 * @param url
	 * 		URL to read from.
	 *
	 * @return Text of web content at location.
	 *
	 * @throws IOException
	 * 		When the content cannot be read.
	 */
	@Nonnull
	public static String getText(@Nonnull String url) throws IOException {
		return mapContent(url, Web::toString);
	}

	/**
	 * @param url
	 * 		URL to read from.
	 *
	 * @return Raw bytes of content at location.
	 *
	 * @throws IOException
	 * 		When the content cannot be read.
	 */
	@Nonnull
	public static byte[] getBytes(@Nonnull String url) throws IOException {
		return mapContent(url, Web::toBytes);
	}

	/**
	 * @param url
	 * 		URL to read from.
	 * @param consumer
	 * 		Consumer to operate on the content at the given URL.
	 *
	 * @throws IOException
	 * 		When the content cannot be read.
	 */
	public static void consumeStream(@Nonnull String url, @Nonnull IOConsumer<InputStream> consumer) throws IOException {
		acceptContent(url, consumer);
	}

	@Nonnull
	private static <T> T mapContent(@Nonnull String url, @Nonnull IOFunction<InputStream, T> function) throws IOException {
		URLConnection conn = openConnection(url);
		return function.apply(conn.getInputStream());
	}

	private static void acceptContent(@Nonnull String url, @Nonnull IOConsumer<InputStream> consumer) throws IOException {
		URLConnection conn = openConnection(url);
		consumer.accept(conn.getInputStream());
	}

	@Nonnull
	private static URLConnection openConnection(@Nonnull String url) throws IOException {
		URL urlObject = new URL(url);
		URLConnection conn = urlObject.openConnection();
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept", "*/*");
		if (conn instanceof HttpURLConnection) {
			HttpURLConnection httpURLConnection = (HttpURLConnection) conn;
			httpURLConnection.setRequestMethod("GET");
			httpURLConnection.setInstanceFollowRedirects(true);
		}
		return conn;
	}

	@Nonnull
	private static byte[] toBytes(@Nonnull InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		Stream.transfer(65536, input, output);
		return output.toByteArray();
	}

	@Nonnull
	private static String toString(@Nonnull InputStream input) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			StringBuilder builder = new StringBuilder();
			while ((line = reader.readLine()) != null)
				builder.append(line);
			return builder.toString();
		}
	}
}
