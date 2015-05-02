package org.testng.eclipse.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.LaunchType;
import org.testng.eclipse.ui.util.Utils;

public abstract class TestngTestSelector {

	public interface ButtonHandler {
		void handleButton();
	}

	public abstract void initializeFrom(ILaunchConfiguration configuration);

	private Button m_radioButton;
	private Text m_text;
	private ModifyListener m_textAdapter;
	private TestNGMainTab m_callback;
	private LaunchType m_launchType;

	TestngTestSelector() {
	}

	TestngTestSelector(TestNGMainTab callback, ButtonHandler buttonHandler,
			LaunchType testngType, Composite comp, String labelKey) {

		init(callback, buttonHandler, comp, testngType, labelKey);
	}

	public void attachModificationListener() {
		m_text.addModifyListener(m_textAdapter);
	}

	public void detachModificationListener() {
		m_text.removeModifyListener(m_textAdapter);
	}

	public void enableRadio(boolean state) {
		m_radioButton.setEnabled(state);
	}

	public void setTextEditable(boolean editable) {
		m_text.setEditable(editable);
	}

	public void setRadioSelected(boolean selected) {
		m_radioButton.setSelection(selected);
	}

	public void setText(String string) {
		m_text.setText(string);
	}

	public String getText() {
		return m_text.getText();
	}

	public LaunchType getTestngType() {
		return m_launchType;
	}

	public Button getRadioButton() {
		return m_radioButton;
	}

	public TestNGMainTab getCallback() {
		return m_callback;
	}

	/**
	 * This method is final but non-private because it is called by the constructor
	 * but also available to callers who wish to use the no-arg constructor, 
	 * construct their buttonHandler, and only then do this initializing.
	 * @param callback
	 * @param buttonHandler
	 * @param testngType
	 * @param comp
	 */
	final void init(TestNGMainTab callback, ButtonHandler buttonHandler,
			Composite comp, LaunchType testngType, String labelKey) {

		m_callback = callback;
		m_launchType = testngType;

		m_textAdapter = new TextAdapter(testngType);
		SelectionAdapter radioAdapter = new RadioAdapter(testngType);
		SelectionAdapter buttonAdapter = new ButtonAdapter(testngType,
				buttonHandler);
		Utils.Widgets wt = Utils.createWidgetTriple(comp, labelKey,
				radioAdapter, buttonAdapter, m_textAdapter);

		m_radioButton = wt.radio;
		m_text = wt.text;
	}

	/////
	// RadioAdapter
	//

	class RadioAdapter extends SelectionAdapter {
		private LaunchType m_type;

		public RadioAdapter(LaunchType type) {
			m_type = type;
		}

		@Override
		public void widgetSelected(SelectionEvent evt) {
			if (((Button) evt.widget).getSelection()) {
				m_callback.setType(m_type);
			}
		}
	}

	//
	// RadioAdapter
	/////

	/////
	// TextAdapter
	//

	class TextAdapter implements ModifyListener {
		LaunchType m_type;

		public TextAdapter(LaunchType type) {
			m_type = type;
		}

		public void modifyText(ModifyEvent evt) {
			m_callback.setType(m_type);
		}
	}

	//
	// TextAdapter
	/////

	/////
	// ButtonAdapter
	//

	class ButtonAdapter extends SelectionAdapter {
		private LaunchType m_type;
		private ButtonHandler m_handler;

		public ButtonAdapter(LaunchType type, ButtonHandler handler) {
			m_type = type;
			m_handler = handler;
		}

		@Override
		public void widgetSelected(SelectionEvent evt) {
			m_callback.setType(m_type);
			try {
				m_callback.setEnabledRadios(false);
				m_handler.handleButton();
			} finally {
				m_callback.setEnabledRadios(true);
			}
		}

		//
		// ButtonAdapter
		/////

	};

}
