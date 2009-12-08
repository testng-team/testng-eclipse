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

	private Button radioButton;
	private Text text;
	private Button searchButton;
	private ModifyListener textAdapter;
	private Composite comp;
	private TestNGMainTab callback;
	private ButtonHandler buttonHandler;
	private String labelKey;

	private LaunchType testngType;

	TestngTestSelector() {
	}

	TestngTestSelector(TestNGMainTab callback, ButtonHandler buttonHandler,
			LaunchType testngType, Composite comp, String labelKey) {

		init(callback, buttonHandler, testngType, comp, labelKey);
	}
	
	

	public void attachModificationListener() {
		text.addModifyListener(textAdapter);
	}

	public void detachModificationListener() {
		text.removeModifyListener(textAdapter);
	}

	public void enableRadio(boolean state) {
		radioButton.setEnabled(state);
	}

	public void setTextEditable(boolean editable) {
		text.setEditable(editable);
	}

	public void setRadioSelected(boolean selected) {
		radioButton.setSelection(selected);
	}

	public void setText(String string) {
		text.setText(string);
	}

	public String getText() {
		return text.getText();
	}

	public LaunchType getTestngType() {
		return testngType;
	}

	public Button getRadioButton() {
		return radioButton;
	}

	public TestNGMainTab getCallback() {
		return callback;
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
			LaunchType testngType, Composite comp, String labelKey) {

		this.callback = callback;
		this.buttonHandler = buttonHandler;
		this.testngType = testngType;
		this.comp = comp;
		this.labelKey = labelKey;

		textAdapter = new TextAdapter(testngType);
		SelectionAdapter radioAdapter = new RadioAdapter(testngType);
		SelectionAdapter buttonAdapter = new ButtonAdapter(testngType,
				buttonHandler);
		Utils.Widgets wt = Utils.createWidgetTriple(comp, labelKey,
				radioAdapter, buttonAdapter, textAdapter);

		radioButton = wt.radio;
		text = wt.text;
		searchButton = wt.button;
	}

	/////
	// RadioAdapter
	//

	class RadioAdapter extends SelectionAdapter {
		private LaunchType m_type;

		public RadioAdapter(LaunchType type) {
			m_type = type;
		}

		public void widgetSelected(SelectionEvent evt) {
			if (((Button) evt.widget).getSelection()) {
				callback.setType(m_type);
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
			callback.setType(m_type);
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

		public void widgetSelected(SelectionEvent evt) {
			callback.setType(m_type);
			try {
				callback.setEnabledRadios(false);
				m_handler.handleButton();
			} finally {
				callback.setEnabledRadios(true);
			}
		};

		//
		// ButtonAdapter
		/////

	};

}
