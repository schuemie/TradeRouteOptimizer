import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class PreprocessDialog extends JDialog {

	private static final long	serialVersionUID	= 5059758971577395462L;

	private JTextArea			ignoreCommoditiesArea;
	private JTextField			maxHopDistanceField;
	private JTextField			minProfitField;
	private JTextField			minSupplyField;
	private JTextField			minDemandField;

	private int					result;

	public PreprocessDialog() {
		setModal(true);
		Box box = Box.createVerticalBox();

		JPanel optionPanel = new JPanel();
		optionPanel.setBorder(BorderFactory.createTitledBorder("Preprocess options"));
		optionPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.weightx = 1;

		JPanel ignoreCommoditiesPanel = new JPanel();
		ignoreCommoditiesPanel.setBorder(BorderFactory.createTitledBorder("Ignore commodities"));
		ignoreCommoditiesArea = new JTextArea("Slaves\nImperial Slaves\nTobacco");
		ignoreCommoditiesArea.setPreferredSize(new Dimension(300, 150));
		ignoreCommoditiesPanel.add(new JScrollPane(ignoreCommoditiesArea));
		optionPanel.add(ignoreCommoditiesPanel, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		optionPanel.add(new JLabel("Max hop distance:"), c);

		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 1;
		maxHopDistanceField = new JTextField("100");
		optionPanel.add(maxHopDistanceField, c);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		optionPanel.add(new JLabel("Min profit per unit:"), c);

		c.gridx = 1;
		c.gridy = 2;
		c.gridwidth = 1;
		minProfitField = new JTextField("500");
		optionPanel.add(minProfitField, c);

		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 1;
		optionPanel.add(new JLabel("Min supply:"), c);

		c.gridx = 1;
		c.gridy = 3;
		c.gridwidth = 1;
		minSupplyField = new JTextField("100");
		optionPanel.add(minSupplyField, c);

		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 1;
		optionPanel.add(new JLabel("Min demand:"), c);

		c.gridx = 1;
		c.gridy = 4;
		c.gridwidth = 1;
		minDemandField = new JTextField("100");
		optionPanel.add(minDemandField, c);

		box.add(optionPanel);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(Box.createHorizontalGlue());
		JButton okButton = new JButton("Ok");
		buttonPanel.add(okButton);
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				result = JOptionPane.OK_OPTION;
				setVisible(false);
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				result = JOptionPane.CANCEL_OPTION;
				setVisible(false);
			}
		});
		buttonPanel.add(cancelButton);

		box.add(buttonPanel);

		add(box);
		pack();
	}

	public int getResult() {
		return result;
	}

	public int getMaxDistance() {
		return Integer.parseInt(maxHopDistanceField.getText());
	}

	public String[] getIgnoreCommodities() {
		String text = ignoreCommoditiesArea.getText();
		List<String> commodities = new ArrayList<String>();
		for (String commodity : text.split("\n")) {
			commodity = commodity.trim();
			if (commodity.length() > 0)
				commodities.add(commodity);
		}
		return commodities.toArray(new String[commodities.size()]);
	}

	public int getMinProfit() {
		return Integer.parseInt(minProfitField.getText());
	}

	public int getMinSupply() {
		return Integer.parseInt(minSupplyField.getText());
	}

	public int getMinDemand() {
		return Integer.parseInt(minDemandField.getText());
	}
}
