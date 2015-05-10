import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

/**
 * The user interface of the application
 */
public class TradeOptimizerUi {

	private static String			DATA_FILE_NAME		= "/EdDataProcessor.bin";

	private JFrame					frame;
	private String					folder;
	private DataProcessor			dataProcessor;
	private Console					console;
	private AutocompleteJComboBox	startStationComboBox;
	private JTextField				maxHopDistanceField;
	private JTextField				maxStarDistanceField;
	private JSpinner				hopCountSpinner;
	private JCheckBox				noLoopsCheckbox;
	private JRadioButton			lPadRadioButton;
	private JRadioButton			mPadRadioButton;
	private JRadioButton			sPadRadioButton;
	private JCheckBox				viaCheckbox;
	private AutocompleteJComboBox	viaSystemComboBox;
	private JButton					computeRouteButton;
	private List<String>			stations;
	private List<String>			systems;

	private ComputeRouteThread		computeRouteThread	= null;

	/**
	 * The application takes one argument.
	 * 
	 * @param args
	 *            First and only argument is a string representing the working folder (where the data is stored)
	 */
	public static void main(String[] args) {
		if (args.length != 0)
			new TradeOptimizerUi(args[0]);
		else
			new TradeOptimizerUi(null);
	}

	public TradeOptimizerUi(String folder) {
		if (folder == null)
			folder = new File("").getAbsolutePath();
		this.folder = folder;

		setLookAndFeel();

		frame = new JFrame("Multi-hop trade optimizer");

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		frame.setLayout(new BorderLayout());

		frame.setJMenuBar(createMenuBar());

		frame.add(createConsolePanel(), BorderLayout.CENTER);

		if (new File(folder + DATA_FILE_NAME).exists()) {
			dataProcessor = DataProcessor.loadFromFile(folder + DATA_FILE_NAME);
			buildStationList();
			buildSystemList();
		} else {
			dataProcessor = null;
			stations = new ArrayList<String>();
			systems = new ArrayList<String>();
			System.out.println("Preprocessed data not found. Please download and preprocess the data (see the Data menu option)");
		}

		frame.add(createOptionPanel(), BorderLayout.NORTH);

		frame.pack();

		frame.setVisible(true);

	}

	private void buildSystemList() {
		systems = new ArrayList<String>();
		for (DataProcessor.System system : dataProcessor.systems)
			if (system != null)
				systems.add(system.name);
		Collections.sort(systems);
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu dataMenu = new JMenu("Data");
		menuBar.add(dataMenu);

		JMenuItem downloadMenuItem = new JMenuItem("Download data");
		downloadMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				downloadData();
			}
		});
		dataMenu.add(downloadMenuItem);

		JMenuItem preprocessMenuItem = new JMenuItem("Preprocess data");
		preprocessMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				preprocessData();
			}
		});
		dataMenu.add(preprocessMenuItem);

		return menuBar;
	}

	private void downloadData() {
		int result = JOptionPane
				.showOptionDialog(
						frame,
						"Are you sure you want to download the data from http://eddb.io?\nYou should only do this at most once a day to prevent overloading the server",
						"Download data", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (result == JOptionPane.OK_OPTION)
			new DownloadDataThread();
	}

	private void preprocessData() {
		PreprocessDialog dialog = new PreprocessDialog();
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
		if (dialog.getResult() == JOptionPane.OK_OPTION) {
			DataProcessor.IGNORE_COMMODITIES = dialog.getIgnoreCommodities();
			DataProcessor.MAX_DISTANCE = dialog.getMaxDistance();
			DataProcessor.MIN_PROFIT = dialog.getMinProfit();
			DataProcessor.MIN_DEMAND = dialog.getMinDemand();
			DataProcessor.MIN_SUPPLY = dialog.getMinSupply();
			new PreprocessThread();
		}
	}

	private void setLookAndFeel() {
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void buildStationList() {
		stations = new ArrayList<String>();
		for (DataProcessor.Station station : dataProcessor.stations)
			if (station != null) {
				DataProcessor.System system = dataProcessor.systems[station.systemId];
				stations.add(station.name + " - " + system.name);
			}
		Collections.sort(stations);
	}

	private JPanel createOptionPanel() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Search parameters"));
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 1;
		panel.add(new JLabel("Starting station:"), c);
		
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 3;
		c.weightx = 1;
		startStationComboBox = new AutocompleteJComboBox(stations);
		startStationComboBox.setToolTipText("The starting station");
		panel.add(startStationComboBox, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		panel.add(new JLabel("Max hop distance:"), c);

		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 1;
		maxHopDistanceField = new JTextField("30");
		maxHopDistanceField.setToolTipText("The maximum distance in lightyears per hop");
		panel.add(maxHopDistanceField, c);

		c.gridx = 2;
		c.gridy = 1;
		c.gridwidth = 1;
		panel.add(new JLabel("  Max distance to star:"), c);

		c.gridx = 3;
		c.gridy = 1;
		c.gridwidth = 1;
		maxStarDistanceField = new JTextField("1000");
		maxStarDistanceField.setToolTipText("The maximum distance in lightseconds from the star (where you jump in) to the station");
		panel.add(maxStarDistanceField, c);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		panel.add(new JLabel("Number of hops:"), c);

		c.gridx = 1;
		c.gridy = 2;
		c.gridwidth = 1;
		hopCountSpinner = new JSpinner();
		hopCountSpinner.setValue(6);
		hopCountSpinner.setToolTipText("The number of hops in the route");
		panel.add(hopCountSpinner, c);

		c.gridx = 2;
		c.gridy = 2;
		c.gridwidth = 1;
		panel.add(new JLabel("  Avoid loops:"), c);

		c.gridx = 3;
		c.gridy = 2;
		c.gridwidth = 1;
		noLoopsCheckbox = new JCheckBox();
		noLoopsCheckbox.setToolTipText("Should loops in the route be avoided?");
		panel.add(noLoopsCheckbox, c);

		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 4;
		JPanel landingPadPanel = new JPanel();
		landingPadPanel.setBorder(BorderFactory.createTitledBorder("Landing pad size"));
		landingPadPanel.setLayout(new BoxLayout(landingPadPanel, BoxLayout.X_AXIS));
		sPadRadioButton = new JRadioButton("Small");
		sPadRadioButton.setToolTipText("Require at least a small landing pad");
		mPadRadioButton = new JRadioButton("Medium");
		mPadRadioButton.setToolTipText("Require at least a medium landing pad");
		lPadRadioButton = new JRadioButton("Large");
		lPadRadioButton.setToolTipText("Require at least a large landing pad");
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(sPadRadioButton);
		buttonGroup.add(mPadRadioButton);
		buttonGroup.add(lPadRadioButton);
		mPadRadioButton.setSelected(true);
		landingPadPanel.add(sPadRadioButton);
		landingPadPanel.add(mPadRadioButton);
		landingPadPanel.add(lPadRadioButton);
		panel.add(landingPadPanel, c);

		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 1;

		viaCheckbox = new JCheckBox("Via system:");
		viaCheckbox.setToolTipText("Force the route to go through this system");
		panel.add(viaCheckbox, c);

		c.gridx = 1;
		c.gridy = 4;
		c.gridwidth = 3;

		viaSystemComboBox = new AutocompleteJComboBox(systems);
		viaSystemComboBox.setToolTipText("Force the route to go through this system");
		panel.add(viaSystemComboBox, c);

		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 1;
		computeRouteButton = new JButton("Compute route");
		computeRouteButton.setToolTipText("Compute the route");
		computeRouteButton.setBackground(new Color(151, 220, 141));
		computeRouteButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				computeRoute();
			}
		});
		panel.add(computeRouteButton, c);
		return panel;
	}

	private void computeRoute() {
		if (computeRouteThread == null)
			computeRouteThread = new ComputeRouteThread();
		else {
			computeRouteThread.abort();
		}
	}

	private class ComputeRouteThread extends Thread {

		private DataProcessor.MultiHopSettings	settings;

		public ComputeRouteThread() {
			console.clear();
			start();
		}

		public void run() {
			if (dataProcessor == null)
				System.out.println("Preprocessed data not found. Please download and preprocess the data (see the Data menu option)");

			computeRouteButton.setText("Abort computation");
			computeRouteButton.setBackground(new Color(220, 151, 141));
			try {
				settings = new DataProcessor.MultiHopSettings();
				settings.maxHopDistance = Double.parseDouble(maxHopDistanceField.getText());
				settings.maxDistanceFromStar = Integer.parseInt(maxStarDistanceField.getText());
				settings.numerOfHops = (Integer) hopCountSpinner.getValue();
				if (sPadRadioButton.isSelected())
					settings.requiredLandingPadSize = DataProcessor.S;
				else if (mPadRadioButton.isSelected())
					settings.requiredLandingPadSize = DataProcessor.M;
				else if (lPadRadioButton.isSelected())
					settings.requiredLandingPadSize = DataProcessor.L;
				settings.noLoops = noLoopsCheckbox.isSelected();
				if (viaCheckbox.isSelected()) {
					String viaSystem = (String) viaSystemComboBox.getSelectedItem();
					settings.viaSystem = dataProcessor.findSystem(viaSystem);
				} else
					settings.viaSystem = null;
				String startStation = (String) startStationComboBox.getSelectedItem();
				if (startStation == null) 
					throw new RuntimeException("Please enter a starting station"); 
				String[] stationSystem = startStation.split(" - ");
				if (stationSystem.length != 2) 
					throw new RuntimeException("Incorrect station or system name"); 
				settings.startStation = dataProcessor.findStation(stationSystem[1], stationSystem[0]);
				System.out.println("Computing route");
				dataProcessor.findMultiHops(settings);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(frame,
					    e.getMessage(),
					    "Error when computing route",
					    JOptionPane.ERROR_MESSAGE);
			} finally {
				computeRouteButton.setText("Compute route");
				computeRouteButton.setBackground(new Color(151, 220, 141));
				computeRouteThread = null;
			}
		}

		public void abort() {
			settings.abort = true;
		}
	}

	private class DownloadDataThread extends Thread {

		public DownloadDataThread() {
			console.clear();
			start();
		}

		public void run() {
			computeRouteButton.setEnabled(false);
			frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				DataLoader.download(folder);
			} finally {
				computeRouteButton.setEnabled(true);
				frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}

	private class PreprocessThread extends Thread {

		public PreprocessThread() {
			console.clear();
			start();
		}

		public void run() {
			computeRouteButton.setEnabled(false);
			frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				DataProcessor newDataProcessor = new DataProcessor(folder);
				newDataProcessor.saveToFile(folder + DATA_FILE_NAME);
				dataProcessor = newDataProcessor;
				buildStationList();
				buildSystemList();
				startStationComboBox.setItems(stations);
				viaSystemComboBox.setItems(systems);
			} finally {
				computeRouteButton.setEnabled(true);
				frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}

	private JComponent createConsolePanel() {
		JTextArea consoleArea = new JTextArea();
		consoleArea.setEditable(false);
		console = new Console();
		console.setTextArea(consoleArea);
		System.setOut(new PrintStream(console));
		System.setErr(new PrintStream(console));
		JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
		consoleScrollPane.setBorder(BorderFactory.createTitledBorder("Output"));
		consoleScrollPane.setPreferredSize(new Dimension(800, 500));
		consoleScrollPane.setAutoscrolls(true);
		return consoleScrollPane;
	}
}
