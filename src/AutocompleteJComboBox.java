import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

public class AutocompleteJComboBox extends JComboBox<String> {
	private static final long	serialVersionUID	= 742581679282073435L;
	private List<String>		items;

	public void setItems(List<String> items) {
		this.items = items;
	}

	public AutocompleteJComboBox(List<String> s) {
		super();
		this.items = s;
		setEditable(true);
		Component c = getEditor().getEditorComponent();
		if (c instanceof JTextComponent) {
			final JTextComponent tc = (JTextComponent) c;
			tc.getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void changedUpdate(DocumentEvent arg0) {
				}

				@Override
				public void insertUpdate(DocumentEvent arg0) {
					update();
				}

				@Override
				public void removeUpdate(DocumentEvent arg0) {
					update();
				}

				public void update() {

					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							List<String> founds = search(items, tc.getText());
							Set<String> foundSet = new HashSet<String>();
							for (String s : founds)
								foundSet.add(s.toLowerCase());
							setEditable(false);
							removeAllItems();

							//if (!foundSet.contains(tc.getText().toLowerCase())) {
								addItem(tc.getText());
							//}

							for (String s : founds) {
								addItem(s);
							}
							setEditable(true);
							setPopupVisible(true);
							tc.requestFocus();
						}

						private List<String> search(List<String> searchable, String text) {
							List<String> result = new ArrayList<String>();
							for (String string : searchable)
								if (string.toLowerCase().contains(text.toLowerCase()))
									result.add(string);
							return result;
						}
					});
				}
			});

			tc.addFocusListener(new FocusListener() {

				@Override
				public void focusGained(FocusEvent arg0) {
					if (tc.getText().length() > 0) {
						setPopupVisible(true);
					}
				}

				@Override
				public void focusLost(FocusEvent arg0) {
				}
			});

		} else {
			throw new IllegalStateException("Editing component is not a JTextComponent!");
		}
	}
}