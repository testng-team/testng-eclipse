package org.testng.eclipse.ui;

import org.eclipse.swt.dnd.Clipboard;

/**
 * Copy the first line of the failure trace to the clipboard.
 */
public class MessageCopyAction extends AbstractTraceAction {

    /**
     * Constructor for CopyTraceAction.
     * @param view 
     * @param clipboard 
     */
    public MessageCopyAction(FailureTrace view, Clipboard clipboard) {
        super(view, clipboard, "Copy TestNG message");  
    }

    /*
     * Copy the first line of the failure trace to the clipboard.
     * @see IAction#run()
     */
    public void run() {
        transfer(getFView().getMessage());
    }

}
