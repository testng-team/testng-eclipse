package org.testng.eclipse.ui;

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.UIJob;
import org.testng.ITestResult;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.CustomSuite;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.LaunchUtil;
import org.testng.eclipse.util.PreferenceStoreUtil;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.remote.strprotocol.GenericMessage;
import org.testng.remote.strprotocol.IRemoteSuiteListener;
import org.testng.remote.strprotocol.IRemoteTestListener;
import org.testng.remote.strprotocol.SuiteMessage;
import org.testng.remote.strprotocol.TestMessage;
import org.testng.remote.strprotocol.TestResultMessage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * A ViewPart that shows the results of a test run.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class TestRunnerViewPart extends ViewPart 
implements IPropertyChangeListener, IRemoteSuiteListener, IRemoteTestListener {

  /** used by IWorkbenchSiteProgressService */
  private static final Object FAMILY_RUN = new Object();

  /** set from IPartListener2 part lifecycle listener. */
  protected boolean m_partIsVisible = false;

  /** store the state. */ 
  private IMemento m_stateMemento;
  
  /** 
   * The launcher that has started the test.
   * May be used for reruns. 
   */
  private ILaunch m_LastLaunch;

  /** The launched project */
  private IJavaProject m_workingProject;
  
  /** status text. */
  protected volatile String  m_statusMessage;
  
  // view components
  private Composite   m_parentComposite;
  private CTabFolder m_tabFolder;
  
  /** The collection of TestRunTab. */
  protected Vector<TestRunTab> m_tabsList = new Vector<TestRunTab>();

  /** The currently active run tab. */
  private TestRunTab m_activeRunTab;

  // Orientations
  static final int VIEW_ORIENTATION_VERTICAL = 0;
  static final int VIEW_ORIENTATION_HORIZONTAL = 1;
  static final int VIEW_ORIENTATION_AUTOMATIC = 2;

  /**
   * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
   * <code>VIEW_ORIENTATION_VERTICAL</code>, or <code>VIEW_ORIENTATION_AUTOMATIC</code>.
   */
  private int fOrientation = VIEW_ORIENTATION_AUTOMATIC;

  /**
   * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
   * <code>VIEW_ORIENTATION_VERTICAL</code>.
   */
  private int fCurrentOrientation;

  protected CounterPanel     m_counterPanel;
  private Composite   m_counterComposite;
  
  final Image m_viewIcon = TestNGPlugin.getImageDescriptor("main16/testng_noshadow.gif").createImage();//$NON-NLS-1$
//  final Image fStackViewIcon = TestNGPlugin.getImageDescriptor("eview16/stackframe.gif").createImage(); //$NON-NLS-1$

  /**
   * Actions
   */
  private Action fNextAction;
  private Action fPrevAction;
  private ToggleOrientationAction[] fToggleOrientationActions;
  private Action m_rerunAction;
  private Action m_rerunFailedAction;
  private Action m_openReportAction;
  private boolean m_hasFailures;
  
  private long m_startTime;
  private long m_stopTime;
  
  /**
   * Whether the output scrolls and reveals tests as they are executed.
   */
  protected boolean fAutoScroll = true;

  protected JUnitProgressBar fProgressBar;

  private Color fOKColor;
  private Color fFailureColor;

  
  private boolean m_isDisposed = false;
  
  // JOBS
  private UpdateUIJob m_updateUIJob;
  /**
   * A Job that runs as long as a test run is running. 
   * It is used to get the progress feedback for running jobs in the view.
   */
  private IsRunningJob m_isRunningJob;
  private ILock        m_runLock;
  private boolean      m_testIsRunning = false;
  
  /**
   * Queue used for processing Tree Entries
   */
  private List m_treeEntriesQueue = new ArrayList();
  
  /**
   * Indicates an instance of TreeEntryQueueDrainer is already running, or scheduled to
   */
  private boolean fQueueDrainRequestOutstanding;
  
  public static final String NAME = "org.testng.eclipse.ResultView"; //$NON-NLS-1$
  public static final String ID_EXTENSION_POINT_TESTRUN_TABS = TestNGPlugin.PLUGIN_ID + "." //$NON-NLS-1$
      + "internal_testRunTabs";  //$NON-NLS-1$

  static final int REFRESH_INTERVAL = 200;

  // Persistence tags.
  static final String TAG_PAGE = "page"; //$NON-NLS-1$
  static final String TAG_ORIENTATION = "orientation"; //$NON-NLS-1$

  //~ counters
  protected int m_suitesTotalCount;
  protected int m_testsTotalCount;
  protected int m_methodTotalCount;
  protected volatile int m_suiteCount;
  protected volatile int m_testCount;
  protected volatile int m_methodCount;
  protected volatile int m_passedCount;
  protected volatile int m_failedCount;
  protected volatile int m_skippedCount;
  protected volatile int m_successPercentageFailed;
  
  /**
   * The client side of the remote test runner
   */
  private EclipseTestRunnerClient fTestRunnerClient;
  
  // Stores any test descriptions of failed tests. For any test class 
  // that implements ITest, this will be the returned value of getTestName().
  private Set testDescriptions;
  private Text m_searchText;
  

  @Override
  public void init(IViewSite site, IMemento memento) throws PartInitException {
    ppp("Init, memento:" + memento);
    super.init(site, memento);
    m_stateMemento = memento;

    IWorkbenchSiteProgressService progressService = getProgressService();
    if(progressService != null) {
      progressService.showBusyForFamily(TestRunnerViewPart.FAMILY_RUN);
    }
  }

  private IWorkbenchSiteProgressService getProgressService() {
    Object siteService = getSite().getAdapter(IWorkbenchSiteProgressService.class);
    if(siteService != null) {
      return (IWorkbenchSiteProgressService) siteService;
    }

    return null;
  }

  private void restoreLayoutState(IMemento memento) {
    Integer page = memento.getInteger(TAG_PAGE);
    if(page != null) {
      int p = page.intValue();
      m_tabFolder.setSelection(p);
      m_activeRunTab = m_tabsList.get(p);
    }

    for (TestRunTab trt : m_tabsList) {
      trt.restoreState(memento);
    }

    Integer orientation = memento.getInteger(TAG_ORIENTATION);
    if(orientation != null) {
      fOrientation = orientation.intValue();
    }

    computeOrientation();
  }

  void computeOrientation() {
    if (fOrientation != VIEW_ORIENTATION_AUTOMATIC) {
      fCurrentOrientation = fOrientation;
      setOrientation(fCurrentOrientation);
    }
    else {
      Point size = m_parentComposite.getSize();
      if((size.x != 0) && (size.y != 0)) {
        if(size.x > size.y) {
          setOrientation(VIEW_ORIENTATION_HORIZONTAL);
        }
        else {
          setOrientation(VIEW_ORIENTATION_VERTICAL);
        }
      }
    }
  }

  private void setOrientation(int orientation) {

    boolean horizontal = orientation == VIEW_ORIENTATION_HORIZONTAL;
    for (TestRunTab trt : m_tabsList) {
      trt.setOrientation(horizontal);
    }

    for(int i = 0; i < fToggleOrientationActions.length; ++i) {
      fToggleOrientationActions[i].setChecked(
          fOrientation == fToggleOrientationActions[i].getOrientation());
    }
    fCurrentOrientation = orientation;

    GridLayout layout = (GridLayout) m_counterComposite.getLayout();
//    layout.numColumns = 1;
    setCounterColumns(layout);

    try {
      m_parentComposite.layout();
    }
    catch(Throwable cause) {
      cause.printStackTrace();
    }
  }

  /**
   * Stops the currently running test and shuts down the RemoteTestRunner.
   */
  private void stopTest() {
    if(null != fTestRunnerClient) {
      fTestRunnerClient.stopTest();
    }
    stopUpdateJobs();
  }

  public void selectNextFailure() {
    m_activeRunTab.selectNext();
  }

  public void selectPreviousFailure() {
    m_activeRunTab.selectPrevious();
  }

  public void showTest(RunInfo test) {
    m_activeRunTab.setSelectedTest(test.getId());
    new OpenTestAction(this, test.getClassName(), test.getMethodName(), false).run();
  }


  public void reset() {
    reset(0, 0);
    clearStatus();
  }

  private void stopUpdateJobs() {
    if(m_updateUIJob != null) {
      m_updateUIJob.stop();
      m_updateUIJob = null;
    }
    if((m_isRunningJob != null) && (m_runLock != null)) {
      m_runLock.release();
      m_isRunningJob = null;
    }
  }

  protected void selectFirstFailure() {
    // TODO
  }

  private boolean hasErrors() {
    return m_failedCount > 0 || m_successPercentageFailed > 0;
  }

  private int getStatus() {
    if (hasErrors()) return ITestResult.FAILURE;
    else if (m_skippedCount > 0) return ITestResult.SKIP;
    else return ITestResult.SUCCESS;
  }

  public void startTestRunListening(IJavaProject project, 
                                    String subName, 
                                    int port, 
                                    ILaunch launch) {
    m_LastLaunch = launch;
    m_workingProject = project;
    m_hasFailures= false;
    
    aboutToLaunch(subName);
    
    if(null != fTestRunnerClient) {
      stopTest();
    }
    fTestRunnerClient = new EclipseTestRunnerClient();
    fTestRunnerClient.startListening(this, this, port);
    
    m_rerunAction.setEnabled(true);
    m_rerunFailedAction.setEnabled(false);
    m_openReportAction.setEnabled(true);
//    getViewSite().getActionBars().updateActionBars();
  }

  protected void aboutToLaunch(final String message) {
    String msg = ResourceUtil.getFormattedString("TestRunnerViewPart.message.launching", message); //$NON-NLS-1$
    firePropertyChange(IWorkbenchPart.PROP_TITLE);
  }

  @Override
  public synchronized void dispose() {
    m_isDisposed = true;
    stopTest();

    TestNGPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
    getViewSite().getPage().removePartListener(fPartListener);
//    fStackViewIcon.dispose();
    m_viewIcon.dispose();
    fOKColor.dispose();
    fFailureColor.dispose();
  }

  private void postSyncRunnable(Runnable r) {
    if(!isDisposed()) {
      getDisplay().syncExec(r);
    }
  }

  private void refreshCounters() {
    m_counterPanel.setMethodCount(m_methodCount);
    m_counterPanel.setPassedCount(m_passedCount);
    m_counterPanel.setFailedCount(m_failedCount);
    m_counterPanel.setSkippedCount(m_skippedCount);
    String msg= "";
    if(m_startTime != 0L && m_stopTime != 0L) {
      msg= " (" + (m_stopTime - m_startTime) + " ms)";
    }
    
    fProgressBar.refresh(getStatus(), msg);
  }

  protected void postShowTestResultsView() {
    postSyncRunnable(new Runnable() {
      public void run() {
        if(isDisposed()) {
          return;
        }
        showTestResultsView();
      }
    });
  }

  /**
   * Show the result view.
   */
  public void showTestResultsView() {
    IWorkbenchWindow   window = getSite().getWorkbenchWindow();
    IWorkbenchPage     page = window.getActivePage();
    TestRunnerViewPart testRunner = null;

    if(page != null) {
      try { 
        testRunner = (TestRunnerViewPart) page.findView(TestRunnerViewPart.NAME);
        if(testRunner == null) {

          IWorkbenchPart activePart = page.getActivePart();
          testRunner = (TestRunnerViewPart) page.showView(TestRunnerViewPart.NAME);

          //restore focus
          page.activate(activePart);
        }
        else {
          page.bringToTop(testRunner);
        }
      }
      catch(PartInitException pie) {
        TestNGPlugin.log(pie);
      }
    }
  }

  /**
   * Can display addition infos.
   * FIXME
   */
//  protected void doShowStatus() {
//    setContentDescription(m_statusMessage);
//  }

  /**
   * FIXME
   */
  protected void setInfoMessage(final String message) {
    m_statusMessage = message;
  }

  /**
   * FIXME
   */
//  private void showMessage(String msg) {
//    postError(msg);
//  }

  /**
   * FIXME
   */
//  protected void postError(final String message) {
//    m_statusMessage = message;
//  }

  /**
   * FIXME
   */
  private void clearStatus() {
    getStatusLine().setMessage(null);
    getStatusLine().setErrorMessage(null);
  }


  protected CTabFolder createTestRunTabs(Composite parent) {
    CTabFolder tabFolder = new CTabFolder(parent, SWT.TOP);
    tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    loadTestRunTabs(tabFolder);
    tabFolder.setSelection(0);
    m_activeRunTab = m_tabsList.firstElement();

    tabFolder.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          testTabChanged(event);
        }
      });

    return tabFolder;
  }

  private void loadTestRunTabs(CTabFolder tabFolder) {

    IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(ID_EXTENSION_POINT_TESTRUN_TABS);
    if(extensionPoint == null) {
      return;
    }

    IConfigurationElement[] configs = extensionPoint.getConfigurationElements();
    MultiStatus status = new MultiStatus(TestNGPlugin.PLUGIN_ID,
                                         IStatus.OK,
                                         "Could not load some testRunTabs extension points", //$NON-NLS-1$
                                         null); 

    for(int i = 0; i < configs.length; i++) {
      try {

        TestRunTab testRunTab = (TestRunTab) configs[i].createExecutableExtension("class"); //$NON-NLS-1$
        createTabControl(testRunTab, tabFolder, this);
        m_tabsList.addElement(testRunTab);
      }
      catch(CoreException e) {
        status.add(e.getStatus());
      }
    }
    if(!status.isOK()) {
      TestNGPlugin.log(status);
    }
  }

  private void createTabControl(TestRunTab testRunTab, CTabFolder tabFolder,
      TestRunnerViewPart testRunnerViewPart) {
    Composite composite = testRunTab.createTabControl(tabFolder, this);

    CTabItem tab = new CTabItem(tabFolder, SWT.NONE);
    tab.setText(ResourceUtil.getString(testRunTab.getNameKey()));
    tab.setImage(testRunTab.getImage());
    tab.setToolTipText(ResourceUtil.getString(testRunTab.getTooltipKey())); //$NON-NLS-1$

    tab.setControl(composite);
  }

  private void testTabChanged(SelectionEvent event) {
    String selectedTestId = m_activeRunTab.getSelectedTestId();

    for (TestRunTab tab : m_tabsList) {
      tab.setSelectedTest(selectedTestId);

      String name = ResourceUtil.getString(tab.getNameKey());
      if(((CTabFolder) event.widget).getSelection().getText() == name) {
        m_activeRunTab = tab;
        m_activeRunTab.activate();
      }
    }
  }

  private void reset(final int suiteCount, final int testCount) {
    m_suitesTotalCount = suiteCount;
    m_testsTotalCount = testCount;
    m_methodTotalCount = 0;
    m_suiteCount = 0;
    m_testCount = 0;
    m_methodCount = 0;
    m_passedCount = 0;
    m_failedCount = 0;
    m_skippedCount = 0;
    m_successPercentageFailed = 0;
    m_startTime= 0L;
    m_stopTime= 0L;
    
    postSyncRunnable(new Runnable() {
      public void run() {
        if(isDisposed()) {
          return;
        }
        
        m_counterPanel.reset();
//        m_failureTraceComponent.clear();
        fProgressBar.reset(testCount);
        clearStatus();
        
        for (TestRunTab tab : m_tabsList) {
          tab.aboutToStart();
        }
      }
    });
  }

  @Override
  public void setFocus() {
    if(m_activeRunTab != null) {
      m_activeRunTab.setFocus();
    }
  }

  @Override
  public void createPartControl(Composite parent) {
    ppp("createPartControl");
    m_parentComposite = parent;
//    addResizeListener(parent);

    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    parent.setLayout(gridLayout);

    configureToolBar();

    createProgressCountPanel(parent);

    m_tabFolder = createTestRunTabs(parent);

//    m_tabFolder.setLayoutData(new Layout() {
//      @Override
//      protected Point computeSize (Composite composite, int wHint, int hHint, boolean flushCache) {
//          if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT)
//              return new Point(wHint, hHint);
//              
//          Control [] children = composite.getChildren ();
//          int count = children.length;
//          int maxWidth = 0, maxHeight = 0;
//          for (int i=0; i<count; i++) {
//              Control child = children [i];
//              Point pt = child.computeSize (SWT.DEFAULT, SWT.DEFAULT, flushCache);
//              maxWidth = Math.max (maxWidth, pt.x);
//              maxHeight = Math.max (maxHeight, pt.y);
//          }
//          
//          if (wHint != SWT.DEFAULT)
//              maxWidth= wHint;
//          if (hHint != SWT.DEFAULT)
//              maxHeight= hHint;
//          
//          return new Point(maxWidth, maxHeight);
//      }
//      
//      @Override
//      protected void layout(Composite composite, boolean flushCache) {
//          Rectangle rect= composite.getClientArea();
//          Control[] children = composite.getChildren();
//          for (int i = 0; i < children.length; i++) {
//              children[i].setBounds(rect);
//          }
//      }
//  });
//    SashForm sashForm = createSashForm(parent);
//    sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

    TestNGPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);

    getViewSite().getPage().addPartListener(fPartListener);

    if (m_stateMemento != null) {
      restoreLayoutState(m_stateMemento);
    }
    m_stateMemento = null;
  }

  @Override
  public void saveState(IMemento memento) {
    int activePage = m_tabFolder.getSelectionIndex();
    memento.putInteger(TAG_PAGE, activePage);
    memento.putInteger(TAG_ORIENTATION, fOrientation);

    for (TestRunTab tab : m_tabsList) {
      tab.saveState(memento);
    }
  }

  private void configureToolBar() {
    IActionBars     actionBars = getViewSite().getActionBars();
    IToolBarManager toolBar = actionBars.getToolBarManager();
    IMenuManager    viewMenu = actionBars.getMenuManager();

    fToggleOrientationActions = new ToggleOrientationAction[] {
        new ToggleOrientationAction(this, VIEW_ORIENTATION_VERTICAL),
        new ToggleOrientationAction(this, VIEW_ORIENTATION_HORIZONTAL),
        new ToggleOrientationAction(this, VIEW_ORIENTATION_AUTOMATIC)
    };
    fNextAction = new ShowNextFailureAction(this);
    fPrevAction = new ShowPreviousFailureAction(this);
    m_rerunAction= new RerunAction();
    m_rerunFailedAction= new RerunFailedAction();
    m_openReportAction= new OpenReportAction();
    
    
    fNextAction.setEnabled(false);
    fPrevAction.setEnabled(false);
    m_rerunAction.setEnabled(false);
    m_rerunFailedAction.setEnabled(false);
    m_openReportAction.setEnabled(false);
    
    actionBars.setGlobalActionHandler(ActionFactory.NEXT.getId(), fNextAction);
    actionBars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), fPrevAction);

    toolBar.add(fNextAction);
    toolBar.add(fPrevAction);
    toolBar.add(new Separator());
    toolBar.add(m_rerunAction);
    toolBar.add(m_rerunFailedAction);
    toolBar.add(new Separator());
    toolBar.add(m_openReportAction);
    
    for(int i = 0; i < fToggleOrientationActions.length; ++i) {
      viewMenu.add(fToggleOrientationActions[i]);
    }

    actionBars.updateActionBars();
  }

  private IStatusLineManager getStatusLine() {

    // we want to show messages globally hence we
    // have to go through the active part
    IViewSite      site = getViewSite();
    IWorkbenchPage page = site.getPage();
    IWorkbenchPart activePart = page.getActivePart();

    if(activePart instanceof IViewPart) {

      IViewPart activeViewPart = (IViewPart) activePart;
      IViewSite activeViewSite = activeViewPart.getViewSite();

      return activeViewSite.getActionBars().getStatusLineManager();
    }

    if(activePart instanceof IEditorPart) {
      IEditorPart activeEditorPart = (IEditorPart) activePart;
      IEditorActionBarContributor contributor = activeEditorPart.getEditorSite()
                                                                .getActionBarContributor();
      if(contributor instanceof EditorActionBarContributor) {
        return ((EditorActionBarContributor) contributor).getActionBars().getStatusLineManager();
      }
    }

    // no active part
    return getViewSite().getActionBars().getStatusLineManager();
  }

  protected void createProgressCountPanel(Composite parent) {
    Display display= parent.getDisplay();
    fFailureColor= new Color(display, 159, 63, 63);
    fOKColor= new Color(display, 95, 191, 95);

    {
      m_counterComposite = new Composite(parent, SWT.NONE);
      m_counterComposite.setLayoutData(
          new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
      GridLayout layout = new GridLayout();
      m_counterComposite.setLayout(layout);
      setCounterColumns(layout);

      fProgressBar = new JUnitProgressBar(m_counterComposite);
      fProgressBar.setLayoutData(
          new GridData(GridData.GRAB_HORIZONTAL| GridData.HORIZONTAL_ALIGN_FILL));
    }

    {
      Composite line2 = new Composite(parent, SWT.NONE);
      line2.setLayoutData(
          new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
      GridLayout layout = new GridLayout();
      layout.numColumns = 3;
      line2.setLayout(layout);
      new Label(line2, SWT.NONE).setText("Search:");
      m_searchText = new Text(line2, SWT.SINGLE | SWT.BORDER);
      m_searchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
      m_searchText.addKeyListener(new KeyListener() {

        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
          for (TestRunTab tab : m_tabsList) {
            tab.updateSearchFilter(m_searchText.getText());
          }
        }

      });

      m_counterPanel = new CounterPanel(line2);
//      m_counterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
//                                               | GridData.HORIZONTAL_ALIGN_FILL));
    }
  }

  /*private static class ProgressBarTextPainter implements PaintListener {
    ProgressBar m_bar;
    Color m_fontColor;
    TestRunnerViewPart parentComponent;
    
    public ProgressBarTextPainter(TestRunnerViewPart parent) {
      m_bar= parent.m_progressBar;
      parentComponent= parent;
      m_fontColor= m_bar.getDisplay().getSystemColor(SWT.COLOR_BLACK);
    }
    
    public void paintControl(PaintEvent e) {
      // string to draw. 
      String string = "Tests: " + parentComponent.m_testCount + "/" + parentComponent.m_testsTotalCount
        + "  Methods: " + parentComponent.m_methodCount + "/" + parentComponent.m_methodTotalCount;
      Point point = m_bar.getSize();
      e.gc.setForeground(m_fontColor);
      FontMetrics fontMetrics = e.gc.getFontMetrics();
      int stringWidth = fontMetrics.getAverageCharWidth() * string.length();
      int stringHeight = fontMetrics.getHeight();
      e.gc.drawString(string, (point.x-stringWidth)/2 , (point.y-stringHeight)/2, true);
    }
  }*/
  
//  public void handleTestSelected(final RunInfo testInfo) {
//    postSyncRunnable(new Runnable() {
//      public void run() {
//        if(!isDisposed()) {
//          m_failureTraceComponent.showFailure(testInfo);
//        }
//      }
//    });
//  }

  public IJavaProject getLaunchedProject() {
    return m_workingProject;
  }

  public ILaunch getLastLaunch() {
    return m_LastLaunch;
  }

  private boolean isDisposed() {
    return m_isDisposed || m_counterPanel.isDisposed();
  }

  private Display getDisplay() {
    return getViewSite().getShell().getDisplay();
  }

  public boolean isCreated() {
    return m_counterPanel != null;
  }

  public void warnOfContentChange() {

    IWorkbenchSiteProgressService service = getProgressService();
    if(service != null) {
      service.warnOfContentChange();
    }
  }

  public boolean lastLaunchIsKeptAlive() {
    return false;
  }

  private void setCounterColumns(GridLayout layout) {
    if(fCurrentOrientation == VIEW_ORIENTATION_HORIZONTAL) {
      layout.numColumns = 2;
    }
    else {
      layout.numColumns = 1;
    }
  }
  
  private class ToggleOrientationAction extends Action {

    private final int fActionOrientation;

    public ToggleOrientationAction(TestRunnerViewPart v, int orientation) {
      super("", AS_RADIO_BUTTON); //$NON-NLS-1$
      if(orientation == TestRunnerViewPart.VIEW_ORIENTATION_HORIZONTAL) {
        setText(ResourceUtil.getString("TestRunnerViewPart.toggle.horizontal.label")); //$NON-NLS-1$
        setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/th_horizontal.gif")); //$NON-NLS-1$
      }
      else if(orientation == TestRunnerViewPart.VIEW_ORIENTATION_VERTICAL) {
        setText(ResourceUtil.getString("TestRunnerViewPart.toggle.vertical.label")); //$NON-NLS-1$
        setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/th_vertical.gif")); //$NON-NLS-1$
      }
      else if(orientation == TestRunnerViewPart.VIEW_ORIENTATION_AUTOMATIC) {
        setText(ResourceUtil.getString("TestRunnerViewPart.toggle.automatic.label")); //$NON-NLS-1$
        setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/th_automatic.gif")); //$NON-NLS-1$
      }
      fActionOrientation = orientation;
    }

    public int getOrientation() {
      return fActionOrientation;
    }

    @Override
    public void run() {
      if(isChecked()) {
        fOrientation = fActionOrientation;
        computeOrientation();
      }
    }
  }

  /**
   * Background job running in UI thread for updating components info. 
   */
  class UpdateUIJob extends UIJob {
    private volatile boolean fRunning = true;

    public UpdateUIJob(String name) {
      super(name);
      setSystem(true);
    }

    @Override
    public IStatus runInUIThread(IProgressMonitor monitor) {
      if(!isDisposed()) {
//        doShowStatus();
        refreshCounters();
//        m_progressBar.redraw();
      }
      schedule(REFRESH_INTERVAL);

      return Status.OK_STATUS;
    }

    public void stop() {
      fRunning = false;
    }

    @Override
    public boolean shouldSchedule() {
      return fRunning;
    }
  }

  class IsRunningJob extends Job {
    public IsRunningJob(String name) {
      super(name);
      setSystem(true);
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {
      // wait until the test run terminates
      m_runLock.acquire();

      return Status.OK_STATUS;
    }

    @Override
    public boolean belongsTo(Object family) {
      return family == TestRunnerViewPart.FAMILY_RUN;
    }
  }

  private static void ppp(final Object message) {
    if (false) {
      System.out.println("[TestRunnerViewPart] " + message);
    }
  }

  /**
   * @see IWorkbenchPart#getTitleImage()
   */
  @Override
  public Image getTitleImage() {
    return m_viewIcon;
  }
  
  /**
   * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent event) {
  }

  private void postTestResult(final RunInfo runInfo, final int progressStep) {
    postSyncRunnable(new Runnable() {
      public void run() {
        if(isDisposed()) {
          return;
        }
//        for(int i = 0; i < m_tabsList.size(); i++) {
//          ((TestRunTab) m_tabsList.elementAt(i)).newTreeEntry(runInfo);
//        }

        fProgressBar.step(progressStep);
//        updateProgressBar(m_progressBar.getSelection() + 1, (progressStep == 0));

        for (TestRunTab tab : m_tabsList) {
          tab.updateTestResult(runInfo);
        }
      }
    });
  }

  ///~ [CURRENT WORK] ~///
  private IPartListener2 fPartListener = new IPartListener2() {
    public void partActivated(IWorkbenchPartReference ref) {
    }

    public void partBroughtToTop(IWorkbenchPartReference ref) {
    }

    public void partInputChanged(IWorkbenchPartReference ref) {
    }

    public void partClosed(IWorkbenchPartReference ref) {
    }

    public void partDeactivated(IWorkbenchPartReference ref) {
    }

    public void partOpened(IWorkbenchPartReference ref) {
    }

    public void partVisible(IWorkbenchPartReference ref) {
      if(getSite().getId().equals(ref.getId())) {
        m_partIsVisible = true;
      }
    }

    public void partHidden(IWorkbenchPartReference ref) {
      if(getSite().getId().equals(ref.getId())) {
        m_partIsVisible = false;
      }
    }
  };
  
  private class RerunAction extends Action {
    public RerunAction() {
      setText(ResourceUtil.getString("TestRunnerViewPart.rerunaction.label")); //$NON-NLS-1$
      setToolTipText(ResourceUtil.getString("TestRunnerViewPart.rerunaction.tooltip")); //$NON-NLS-1$
      setDisabledImageDescriptor(TestNGPlugin.getImageDescriptor("dlcl16/relaunch.gif")); //$NON-NLS-1$
      setHoverImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/relaunch.gif")); //$NON-NLS-1$
      setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/relaunch.gif")); //$NON-NLS-1$
    }
      
    @Override
    public void run() {
      if(null != m_LastLaunch) {
        DebugUITools.launch(m_LastLaunch.getLaunchConfiguration(), m_LastLaunch.getLaunchMode());
      }
    }
  }
  
  private class OpenReportAction extends Action {
    public OpenReportAction() {
      setText(ResourceUtil.getString("TestRunnerViewPart.openreport.label")); //$NON-NLS-1$
      setToolTipText(ResourceUtil.getString("TestRunnerViewPart.openreport.tooltip")); //$NON-NLS-1$
      setDisabledImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/report.gif")); //$NON-NLS-1$
      setHoverImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/report.gif")); //$NON-NLS-1$
      setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/report.gif")); //$NON-NLS-1$
    }

    private void openEditor(IFile file) {
      final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if(window == null) {
        return;
      }

      final IWorkbenchPage page = window.getActivePage();
      if(page == null) {
        return;
      }
      try {
        IDE.openEditor(page, file);
      } 
      catch(final PartInitException e) {
        TestNGPlugin.log(e);
      }
    }

    @Override
    public void run() {
      Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
      IJavaProject javaProject= m_workingProject != null ? m_workingProject : JDTUtil.getJavaProjectContext();
      if(null == javaProject) {
        return;
      }
      PreferenceStoreUtil storage= TestNGPlugin.getPluginPreferenceStore();
      IPath filePath= new Path(storage.getOutputDirectoryPath(javaProject).toOSString() + "/index.html");
      boolean isAbsolute= storage.isOutputAbsolutePath(javaProject.getElementName(), false);
      
      IProgressMonitor progressMonitor= new NullProgressMonitor();
      if(isAbsolute) {
        IFile file = javaProject.getProject().getFile("temp-testng-index.html");
        try {
          file.createLink(filePath, IResource.NONE, progressMonitor);
          if(null == file) return;
          try {
            openEditor(file);
          }
          finally {
            file.delete(true, progressMonitor);
          }
        }
        catch(CoreException cex) {
          ; // TODO: is there any other option?
        }
      }
      else {
        IFile file= (IFile) workspace.newResource(filePath, IResource.FILE);
        if(null == file) return;
        try {
          file.refreshLocal(IResource.DEPTH_ZERO, progressMonitor);
          openEditor(file);
        }
        catch(CoreException cex) {
          ; // nothing I can do about it
        }
      }
    }
  }
  
  private class RerunFailedAction extends Action {
    public RerunFailedAction() {
      setText(ResourceUtil.getString("TestRunnerViewPart.rerunfailedsaction.label")); //$NON-NLS-1$
      setToolTipText(ResourceUtil.getString("TestRunnerViewPart.rerunfailedsaction.tooltip")); //$NON-NLS-1$
      setDisabledImageDescriptor(TestNGPlugin.getImageDescriptor("dlcl16/relaunchf.gif")); //$NON-NLS-1$
      setHoverImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/relaunchf.gif")); //$NON-NLS-1$
      setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/relaunchf.gif")); //$NON-NLS-1$
    }
    
    @Override
    public void run() {
      if(null != m_LastLaunch && hasErrors()) {
        LaunchUtil.launchFailedSuiteConfiguration(m_workingProject, 
        		m_LastLaunch.getLaunchMode(), 
        		m_LastLaunch.getLaunchConfiguration(),
        		getTestDescriptions());
      }
    }    
  }

  /// ~ ITestNGRemoteEventListener
  public void onInitialization(GenericMessage genericMessage) {
    final int suiteCount = Integer.parseInt(genericMessage.getProperty("suiteCount")); //$NON-NLS-1$
    final int testCount = Integer.parseInt(genericMessage.getProperty("testCount")); //$NON-NLS-1$
    reset(suiteCount, testCount);
    stopUpdateJobs();
    m_updateUIJob= new UpdateUIJob("Update TestNG"); //$NON-NLS-1$ 
    m_isRunningJob = new IsRunningJob("TestNG run wrapper job"); //$NON-NLS-1$
    m_runLock = Platform.getJobManager().newLock();
    // acquire lock while a test run is running the lock is released when the test run terminates
    // the wrapper job will wait on this lock.
    m_runLock.acquire();
    getProgressService().schedule(m_isRunningJob);
    m_updateUIJob.schedule(REFRESH_INTERVAL);
    m_startTime= System.currentTimeMillis();
  }

//  public void onStart(SuiteMessage suiteMessage) {
//    RunInfo ri= new RunInfo(suiteMessage.getSuiteName());
//    ri.m_methodCount= suiteMessage.getTestMethodCount();
//
//    postNewTreeEntry(ri);
//  }

  public void onFinish(SuiteMessage suiteMessage) {
    m_suiteCount++;
    
//    postSyncRunnable(new Runnable() {
//      public void run() {
//        if(isDisposed()) {
//          return;
//        }
//        for(int i = 0; i < m_tabsList.size(); i++) {
//          ((TestRunTab) m_tabsList.elementAt(i)).updateEntry(entryId);
//        }
//      }
//    });

    if(m_suitesTotalCount == m_suiteCount) {
      fNextAction.setEnabled(hasErrors());
      fPrevAction.setEnabled(hasErrors());
      m_rerunFailedAction.setEnabled(hasErrors());
      m_hasFailures= true;
      postShowTestResultsView();
      stopTest();
      m_stopTime= System.currentTimeMillis();
      postSyncRunnable(new Runnable() {
        public void run() {
          if(isDisposed()) {
            return;
          }
          refreshCounters();
//          m_progressBar.redraw();
        }
      });
      
    }
  }

  public void onStart(TestMessage tm) {
    RunInfo ri= new RunInfo(tm.getSuiteName(), tm.getTestName());
    ri.m_methodCount= tm.getTestMethodCount();
    m_methodTotalCount += tm.getTestMethodCount();
    
//    postNewTreeEntry(ri);
    
    postSyncRunnable(new Runnable() {
      public void run() {
        if(isDisposed()) {
          return;
        }

        updateProgressBar();
//        m_progressBar.setMaximum(newMaxBar);
//        System.out.println("se maresteeee");
      }
    });
  }

  private void updateProgressBar() {
    postSyncRunnable(new Runnable() {
      public void run() {
        int newMaxBar = (m_methodTotalCount * m_testsTotalCount) / (m_testCount + 1);
        fProgressBar.setMaximum(newMaxBar, m_methodTotalCount);
      }
    });
  }

  public void onFinish(TestMessage tm) {
    m_testCount++;

    // The method count is more accurate than m_methodTotalCount since it also takes
    // data providers and other dynamic invocations into account.
    if (m_methodCount != m_methodTotalCount) {
        m_methodTotalCount= m_methodCount; // trust the methodCount
    }

    updateProgressBar();

    final String entryId = new RunInfo(tm.getSuiteName(), tm.getTestName()).getId();
    
    postSyncRunnable(new Runnable() {
      public void run() {
        if(isDisposed()) {
          return;
        }
//        for(int i = 0; i < m_tabsList.size(); i++) {
//          ((TestRunTab) m_tabsList.elementAt(i)).updateEntry(entryId);
//        }
        
        fProgressBar.stepTests();
      }
    });
  }

  private RunInfo createRunInfo(TestResultMessage trm, String stackTrace, int type) {
    String testName = trm.getName();
    if (testName == null) {
      testName = CustomSuite.DEFAULT_TEST_TAG_NAME;
    }
    return new RunInfo(trm.getSuiteName(),
                       testName,
                       trm.getTestClass(),
                       trm.getMethod(),
                       trm.getTestDescription(),
                       trm.getParameters(),
                       trm.getParameterTypes(),
                       trm.getEndMillis() - trm.getStartMillis(),
                       stackTrace,
                       type);
                       
  }
  
  public void onTestSuccess(TestResultMessage trm) {
    m_passedCount++;
    m_methodCount++;
    
    postTestResult(createRunInfo(trm, null, ITestResult.SUCCESS), 0 /*no error*/);
  }

  public void onTestFailure(TestResultMessage trm) {
    m_failedCount++;
    m_methodCount++;
    String desc = trm.getTestDescription();
    if (desc != null) {
    	getTestDescriptions().add (desc);
    }	
    //    System.out.println("[INFO:onTestFailure]:" + trm.getMessageAsString());
    postTestResult(createRunInfo(trm, trm.getStackTrace(), ITestResult.FAILURE), 1 /*error*/);
  }

  public void onTestSkipped(TestResultMessage trm) {
    m_skippedCount++;
    m_methodCount++;
//    System.out.println("[INFO:onTestSkipped]:" + trm.getMessageAsString());
    postTestResult(createRunInfo(trm, trm.getStackTrace(), ITestResult.SKIP), 1 /*error*/
    );
  }

  public void onTestFailedButWithinSuccessPercentage(TestResultMessage trm) {
    m_successPercentageFailed++;
    m_methodCount++;
    
    postTestResult(createRunInfo(trm, trm.getStackTrace(), ITestResult.SUCCESS_PERCENTAGE_FAILURE),
                   1 /*error*/
    );
  }

  public Set getTestDescriptions() {
  	if (testDescriptions == null) {
  		testDescriptions = new HashSet();
  	}
  	return testDescriptions;
  }
  
    /**
	 * If any test descriptions of failed tests have been saved, pass them along
	 * as a jvm argument. They can they be used by
	 * @Factory methods to select which parameters to use for creating the set
	 * of test instances to re-run.
	 */
	public void run() {
		if (null != m_LastLaunch && hasErrors()) {
			ILaunchConfiguration config = m_LastLaunch.getLaunchConfiguration();
			
			try {
				ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
				Set descriptions = getTestDescriptions();
				if (!descriptions.isEmpty()) { // String.join is not
					// available in jdk 1.4
					StringBuffer buf = new StringBuffer();
					Iterator it = descriptions.iterator();
					boolean first = true;
					while (it.hasNext()) {
						if (first) {
							first = false;
						} else {
							buf.append(",");
						}
						buf.append(it.next());
					}
					config = LaunchUtil.addJvmArg(TestNGPlugin
							.getFailedTestsKey(), buf.toString(), wc);
				}
			} catch (CoreException ce) {
				ce.printStackTrace();
			}
			LaunchUtil.launchFailedSuiteConfiguration(m_workingProject,
					m_LastLaunch.getLaunchMode());
		}
	}

	/**
   * FIXME: currently not used; it should be use to mark the currently running
   * tests.
   */
  public void onTestStart(TestResultMessage trm) {
////    System.out.println("[INFO:onTestStart]:" + trm.getMessageAsString());
//    postTestStarted(createRunInfo(trm, null, ITestResult.SUCCESS));
  }

  public void onStart(SuiteMessage suiteMessage) {
  }

}
