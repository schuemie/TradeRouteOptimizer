import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * Class for downloading the data from eddb.io
 */
public class DataLoader {

	public static String	baseUrl		= "http://eddb.io/archive/v3/";
	public static String[]	fileNames	= new String[] { "commodities.json", "systems.json", "stations.json" };

	/**
	 * Download all necessary files from eddb.io, and store in the working folder.
	 * 
	 * @param folder
	 *            Absolute path to the working folder
	 */
	public static void download(String folder) {
		for (String fileName : fileNames) {
			System.out.format("Downloading %s\n", fileName);
			downloadFile(baseUrl + fileName, folder + "/" + fileName);
		}
		System.out.println("Finished downloading");
	}

	private static void downloadFile(String url, String filename) {
		try {
			FileOutputStream out = new FileOutputStream(filename);
			URL sourceURL = new URL(url);
			HttpURLConnection sourceConnection = (HttpURLConnection) sourceURL.openConnection();
			sourceConnection.setRequestProperty("Accept-Encoding", "gzip, deflate, sdch");
			sourceConnection.connect();
			GZIPInputStream resultingInputStream = new GZIPInputStream(sourceConnection.getInputStream());
			BufferedInputStream in = new BufferedInputStream(resultingInputStream);
			byte data[] = new byte[1024];
			int count;
			while ((count = in.read(data, 0, 1024)) != -1) {
				out.write(data, 0, count);
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
