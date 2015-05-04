public class Launcher {
	private final static int	MIN_HEAP	= 1024;

	public static void main(String[] args) throws Exception {

		float heapSizeMegs = (Runtime.getRuntime().maxMemory() / 1024) / 1024;

		if (heapSizeMegs > MIN_HEAP) {
			System.out.println("Launching with current VM");
			TradeOptimizerUi.main(args);
		} else {
			System.out.println("Starting new VM");
			String pathToJar = TradeOptimizerUi.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			ProcessBuilder pb = new ProcessBuilder("java", "-Xmx" + MIN_HEAP + "m", "-classpath", pathToJar, "TradeOptimizerUi");
			pb.start();
		}
	}
}
