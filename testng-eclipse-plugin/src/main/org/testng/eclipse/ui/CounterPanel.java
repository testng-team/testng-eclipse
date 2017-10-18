package org.testng.eclipse.ui;

import org.testng.eclipse.TestNGPlugin;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * A panel presenting report info:
 * 1st row: suite counters, test counters, method counters
 * 2nd row: passed, failed, skipped and successPercentageFailed counters
 * <P/>
 * Original idea from org.eclipse.jdt.internal.junit.ui.CounterPanel.
 *
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class CounterPanel extends Composite {
  private static final String TOTAL_MESSAGE_FORMAT = "{0}/{1}  ";
  private final Image m_successIcon = TestNGPlugin.getImageDescriptor("ovr16/success_ovr.png").createImage(); //$NON-NLS-1$
  private final Image m_failureIcon = TestNGPlugin.getImageDescriptor("ovr16/failed_ovr.png").createImage(); //$NON-NLS-1$
  private final Image m_skipIcon = TestNGPlugin.getImageDescriptor("ovr16/skip.gif").createImage(); //$NON-NLS-1$
//  private final Image m_failPercentIcon = TestNGPlugin.getImageDescriptor("ovr16/failureOnPercentage.gif").createImage(); //$NON-NLS-1$

//  protected Text m_suiteCountText;
//  protected Text m_testCountText;
//  protected Text m_methodCountText;

  protected Text m_passedText;
  protected CLabel m_passedLabel;
  protected Text m_skippedText;
  protected CLabel m_skippedLabel;
  protected Text m_failedText;
  protected CLabel m_failedLabel;
//  protected Text m_successPercentageFailedText;

  protected int  m_suiteTotalCount;
  protected int  m_suiteCount = 0;
  protected int  m_testTotalCount;
  protected int  m_testCount = 0;
  protected int  m_methodTotalCount;
  protected int  m_methodCount = 0;

  public CounterPanel(Composite parent) {
    super(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().margins(0, 0).spacing(0, 0).applyTo(this);

    createReportUpperRow();

    addDisposeListener(new DisposeListener() {
        public void widgetDisposed(DisposeEvent e) {
          disposeIcons();
        }
      });
  }

  private void disposeIcons() {
    m_successIcon.dispose();
    m_failureIcon.dispose();
    m_skipIcon.dispose();
//    m_failPercentIcon.dispose();
  }

  private void createReportUpperRow() {
    Composite  upperRow = new Composite(this, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    gridLayout.makeColumnsEqualWidth = true;
    gridLayout.marginWidth = 0;
    gridLayout.verticalSpacing= 0;
//    gridLayout.horizontalSpacing= 5;
    upperRow.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    upperRow.setLayout(gridLayout);

    Color backgroundColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

//    m_suiteCountText = createLabel(upperRow, "Suites:", null, "0/0          ", backgroundColor);
//    m_testCountText = createLabel(upperRow, "Tests:", null, "0/0         ", backgroundColor);
//    m_methodCountText = createLabel(upperRow, "Methods:", null, "0/0          ", backgroundColor);
//    m_passedText = createLabel(upperRow, "Passed:", m_successIcon, "  0  ", backgroundColor); //$NON-NLS-1$ //$NON-NLS-2$
//    m_failedText = createLabel(upperRow, "Failed:", m_failureIcon, "  0  ", backgroundColor); //$NON-NLS-1$ //$NON-NLS-2$
//    m_skippedText = createLabel(upperRow, "Skipped:", m_skipIcon, "  0  ", backgroundColor); //$NON-NLS-1$ //$NON-NLS-2$
    m_passedLabel = createLabel(upperRow, "Passed: 0", m_successIcon, "  0  ", backgroundColor); //$NON-NLS-1$ //$NON-NLS-2$
    m_failedLabel = createLabel(upperRow, "Failed: 0", m_failureIcon, "  0  ", backgroundColor); //$NON-NLS-1$ //$NON-NLS-2$
    m_skippedLabel = createLabel(upperRow, "Skipped: 0", m_skipIcon, "  0  ", backgroundColor); //$NON-NLS-1$ //$NON-NLS-2$

  }

  private Text createReportFor(Composite parent,
                               String labelText,
                               Color backColor,
                               String initialText) {
    Composite  cell = new Composite(parent, SWT.BORDER);
    GridLayout gl = new GridLayout();
    gl.numColumns = 2;
    gl.makeColumnsEqualWidth = false;
    gl.horizontalSpacing = 1;
    cell.setLayout(gl);
    cell.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Label label = new Label(cell, SWT.NONE);
    label.setText(labelText);
    label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
    label.setFont(JFaceResources.getBannerFont());

    Text text = new Text(cell, SWT.READ_ONLY);
    text.setText(initialText);
    text.setBackground(backColor);
    text.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.GRAB_HORIZONTAL));

    return text;
  }

 /* private void createReportLowerRow() {
    Composite  lowerRow = new Composite(this, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    gridLayout.makeColumnsEqualWidth = false;
    gridLayout.marginWidth = 0;
    gridLayout.verticalSpacing= 0;
    gridLayout.horizontalSpacing= 5;
    lowerRow.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    lowerRow.setLayout(gridLayout);

    Color backgroundColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

//    m_passedText = createLabel(lowerRow, "Passed: 0", m_successIcon, "0", backgroundColor); //$NON-NLS-1$ //$NON-NLS-2$
//    m_failedText = createLabel(lowerRow, "Failed: 0", m_failureIcon, "0", backgroundColor); //$NON-NLS-1$ //$NON-NLS-2$
//    m_skippedText = createLabel(lowerRow, "Skipped: 0", m_skipIcon, "0", backgroundColor); //$NON-NLS-1$ //$NON-NLS-2$
//    m_successPercentageFailedText = createLabel(lowerRow,
//                                                "SPF:", //$NON-NLS-1$
//                                                m_failPercentIcon,
//                                                " 0 ", //$NON-NLS-1$
//                                                backgroundColor);
  }*/

  private CLabel createLabel(Composite parent,
                           String name,
                           Image image,
                           String init,
                           Color backColor) {
//    Label label = null; 
//    if(image != null) {
//      label= new Label(parent, SWT.NONE);
//      image.setBackground(label.getBackground());
//      label.setImage(image);
//      label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
//    }

    CLabel label= new CLabel(parent, SWT.NONE);
    if(null != image) {
      label.setImage(image);
    }
    label.setText(name);
    label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING 
        | GridData.FILL_HORIZONTAL));
    label.setFont(JFaceResources.getDialogFont());

//    Text value = new Text(parent, SWT.READ_ONLY);
//    value.setText(init);
//    value.setBackground(backColor);
//    value.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
//                                     | GridData.HORIZONTAL_ALIGN_BEGINNING));
//
//    return value;
    return label;
  }

  /**
   * FIXME: too much redraw().
   */
  public void reset() {
    ppp("reset()");
    m_suiteTotalCount = 0;
    m_suiteCount = 0;
    setSuiteTotalCount(0);
    m_testTotalCount = 0;
    m_testCount = 0;
    setTestTotalCount(0);
    m_methodTotalCount = 0;
    m_methodCount = 0;
    setMethodTotalCount(0);
    setPassedCount(0);
    setFailedCount(0);
    setSkippedCount(0);
//    setSuccessPercentageFailedCount(0);
    redraw();
  }

  public void setSuiteTotalCount(int no) {
//    m_suiteTotalCount = no;
//    m_suiteCountText.setText(MessageFormat.format(TOTAL_MESSAGE_FORMAT,
//                                                  new Object[] {
//                                                    new Integer(m_suiteCount),
//                                                    new Integer(m_suiteTotalCount)
//                                                  }));
//    m_suiteCountText.redraw();
//    redraw();
  }

  public void setSuiteCount(int no) {
//    m_suiteCount = no;
//    m_suiteCountText.setText(MessageFormat.format(TOTAL_MESSAGE_FORMAT,
//                                                  new Object[] {
//                                                    new Integer(m_suiteCount),
//                                                    new Integer(m_suiteTotalCount)
//                                                  }));
//    m_suiteCountText.redraw();
  }

  public void setTestTotalCount(int no) {
//    m_testTotalCount = no;
//    m_testCountText.setText(MessageFormat.format(TOTAL_MESSAGE_FORMAT,
//                                                 new Object[] {
//                                                   new Integer(m_testCount),
//                                                   new Integer(m_testTotalCount)
//                                                 }));
//    m_testCountText.redraw();
//    redraw();
  }

  public void setTestCount(int no) {
//    m_testCount = no;
//    m_testCountText.setText(MessageFormat.format(TOTAL_MESSAGE_FORMAT,
//                                                 new Object[] {
//                                                   new Integer(m_testCount),
//                                                   new Integer(m_testTotalCount)
//                                                 }));
//    m_testCountText.redraw();
  }

  public void setMethodTotalCount(int no) {
//    m_methodTotalCount = no;
//    m_methodCountText.setText(MessageFormat.format(TOTAL_MESSAGE_FORMAT,
//                                                   new Object[] {
//                                                     new Integer(m_methodCount),
//                                                     new Integer(m_methodTotalCount)
//                                                   }));
//    m_methodCountText.redraw();
//    redraw();
  }

  public void setMethodCount(int no) {
//    m_methodCount = no;
//    m_methodCountText.setText(MessageFormat.format(TOTAL_MESSAGE_FORMAT,
//                                                   new Object[] {
//                                                     new Integer(m_methodCount),
//                                                     new Integer(m_methodTotalCount)
//                                                   }));
//    m_methodCountText.redraw();
  }

  public void setPassedCount(int no) {
//    m_passedText.setText(String.valueOf(no));
//    m_passedText.redraw();
//    redraw();
    m_passedLabel.setText("Passed: " + no);
    m_passedLabel.redraw();
  }

  public void setFailedCount(int no) {
//    m_failedText.setText(String.valueOf(no));
//    m_failedText.redraw();
//    redraw();
    m_failedLabel.setText("Failed: " + no);
    m_failedLabel.redraw();
  }

  public void setSkippedCount(int no) {
//    m_skippedText.setText(String.valueOf(no));
//    m_skippedText.redraw();
//    redraw();
    m_skippedLabel.setText("Skipped: " + no);
    m_skippedLabel.redraw();
  }

  /*public void setSuccessPercentageFailedCount(int no) {
    m_successPercentageFailedText.setText(String.valueOf(no));
    m_successPercentageFailedText.redraw();
    redraw();
  }*/

  private static void ppp(Object msg) {
//    System.out.println("[CounterPanel]:- " + msg);
  }
}
