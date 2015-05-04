import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

public class Console extends OutputStream {

	private StringBuffer	buffer	= new StringBuffer();
	private JTextArea		textArea;

	public void println(String string) {
		textArea.append(string + "\n");
		textArea.repaint();
		System.out.println(string);
	}

	public void setTextArea(JTextArea textArea) {
		this.textArea = textArea;
	}

	public String getText() {
		try {
			return textArea.getDocument().getText(0, textArea.getDocument().getLength());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void write(int b) throws IOException {
		buffer.append((char) b);
		if ((char) b == '\n') {
			if (textArea != null) {
				textArea.append(buffer.toString());
				textArea.setCaretPosition(textArea.getDocument().getLength());
			}
			buffer = new StringBuffer();
		}
	}

	public void clear() {
		textArea.setText("");

	}

}
