package org.testng.eclipse.ui;

import org.eclipse.swt.dnd.Clipboard;

/**
 * Copy the failure trace to the clipboard.
 */
public class TraceCopyAction extends AbstractTraceAction {

    /**
     * Constructor for CopyTraceAction.
     * @param view 
     * @param clipboard 
     */
    public TraceCopyAction(FailureTrace view, Clipboard clipboard) {
        super(view, clipboard, "Copy TestNG trace");  
    }

    /*
     * Copy the failure trace to the clipboard.
     * @see IAction#run()
     */
    public void run() {
        transfer(getFView().getTrace());
    }

}
