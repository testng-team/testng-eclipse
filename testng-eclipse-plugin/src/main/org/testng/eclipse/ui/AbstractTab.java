package org.testng.eclipse.ui;

import static org.testng.eclipse.ui.Images.IMG_TEST_HIERARCHY;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IMemento;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.ui.tree.BaseTreeItem;
import org.testng.eclipse.ui.tree.ClassTreeItem;
import org.testng.eclipse.ui.tree.ITreeItem;
import org.testng.eclipse.ui.tree.SuiteTreeItem;
import org.testng.eclipse.ui.tree.TestMethodParametersTreeItem;
import org.testng.eclipse.ui.tree.TestMethodTreeItem;
import org.testng.eclipse.ui.tree.TestTreeItem;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.StringUtils;

import com.google.common.collect.ArrayListMultimap;

/**
 * This class is responsible for the tree display in the runner view part. It
 * has two subclasses, SuccessTab and FailureTab. Whenever a new test result
 * message arrives from RemoteTestNG, updateTestResult() is invoked, which then
 * creates or updates the corresponding node in the tree.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
abstract public class AbstractTab extends TestRunTab implements IMenuListener {
  private Image m_testHierarchyIcon; 
  private Image m_stackViewIcon;

  // Persistence tags
  static final String TAG_RATIO = "ratio"; //$NON-NLS-1$

  /** Used to persist the state of the layout */
  private IMemento m_stateMemento;

  /** The component that displays the stack trace when an item is selected */
  private FailureTrace m_failureTraceComponent;

  private Tree m_tree;
  private TreeViewer m_treeViewer;

  private TestRunnerViewPart m_testRunnerPart;
  private SashForm m_sashForm;
  private Composite m_parentComposite;

  private Clipboard fClipboard;

  //
  // Keeping track of what's in the tree
  //
  private Map<String, ITreeItem> m_treeItemMap = new HashMap<>();
  private Set<RunInfo> m_runInfos = new HashSet<>();
  private String m_searchFilter = "";
  private Map<String, ITreeItem> m_suites = new HashMap<>();
  private Map<String, ITreeItem> m_tests = new HashMap<>();
  private Map<String, ITreeItem> m_classes = new HashMap<>();
  // bug/167: use ArrayListMultimap which allows duplicate keys to store m_methods
  //          since the instanceName (which is used as key) might be same
  private ArrayListMultimap<String, ITreeItem> m_methods = ArrayListMultimap.create();
  // Don't forget to .clear() all these maps beween each run ^^

  @Override
  public String getSelectedTestId() {
    TreeItem[] treeItems = m_tree.getSelection();
    if (treeItems == null || treeItems.length == 0) {
      return null;
    } else {
      return BaseTreeItem.getTreeItem(treeItems[0]).getRunInfo().getMethodId();
    }
  }

  @Override
  public void setSelectedTest(String testId) {
    if (testId == null) return;
    ITreeItem node = m_treeItemMap.get(testId);
    if (node != null) {
      m_tree.select(node.getTreeItem());
    } else {
      m_tree.deselectAll();
    }
  }

  @Override
  public Image getImage() {
    return m_testHierarchyIcon;
  }

  @Override
  public Composite createTabControl(Composite parent, TestRunnerViewPart runner) {
    m_testRunnerPart = runner;

    Composite result = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    gridLayout.numColumns = 3;
    result.setLayout(gridLayout);

    GridData gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
    result.setLayoutData(gridData);

    // The sash is the parent of both the tree and the stack trace component
    m_sashForm = new SashForm(result, SWT.HORIZONTAL);
    m_sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    //
    // Tree
    //
    m_tree = new Tree(m_sashForm, SWT.SINGLE | SWT.VIRTUAL);
//    m_treeViewer = new TreeViewer(m_sashForm, SWT.SINGLE);

    //
    // Stack trace (FailureComponent)
    //
    ViewForm stackTraceForm = new ViewForm(m_sashForm, SWT.NONE);
    stackTraceForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    CLabel label = new CLabel(stackTraceForm, SWT.NONE);
    label.setText(ResourceUtil.getString("TestRunnerViewPart.label.failure")); //$NON-NLS-1$
    label.setImage(m_stackViewIcon);
    stackTraceForm.setTopLeft(label);

    ToolBar failureToolBar = new ToolBar(stackTraceForm, SWT.FLAT | SWT.WRAP);
    stackTraceForm.setTopCenter(failureToolBar);
    m_failureTraceComponent = new FailureTrace(stackTraceForm, m_testRunnerPart, failureToolBar);
    stackTraceForm.setContent(m_failureTraceComponent.getComposite());

    m_sashForm.setWeights(new int[] { 50, 50 });

    initImages();
    initMenu();
    addListeners();

    m_parentComposite = result;

    fClipboard= new Clipboard(parent.getDisplay());

    return result;
  }

  private void initMenu() {
    MenuManager menuMgr = new MenuManager();
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(this);

    Menu menu = menuMgr.createContextMenu(m_tree);
    m_tree.setMenu(menu);
  }
  
  private void addListeners() {
    m_tree.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        activate();
      }

      public void widgetDefaultSelected(SelectionEvent e) {
        activate();
      }
    });

    m_tree.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        disposeIcons();
      }
    });

    m_tree.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDoubleClick(MouseEvent e) {
        handleDoubleClick(e);
      }
    });
  }

  void disposeIcons() {
//    if (m_suiteIcon != null) m_suiteIcon.dispose();
//    if (m_suiteOkeIcon != null) m_suiteOkeIcon.dispose();
//    if (m_suiteFailIcon != null) m_suiteFailIcon.dispose();
//    if (m_suiteSkipIcon != null) m_suiteSkipIcon.dispose();
//    if (m_suiteRunIcon != null) m_suiteRunIcon.dispose();
//
//    if (m_testHierarchyIcon != null) m_testHierarchyIcon.dispose();
//    if (m_testIcon != null) m_testIcon.dispose();
//    if (m_testOkeIcon != null) m_testOkeIcon.dispose();
//    if (m_testFailIcon != null) m_testFailIcon.dispose();
//    if (m_testSkipIcon != null) m_testSkipIcon.dispose();
//    if (m_testRunIcon != null) m_testRunIcon.dispose();
//
//    if (m_stackViewIcon != null) m_stackViewIcon.dispose();
  }

  void handleDoubleClick(MouseEvent e) {
    ITreeItem testInfo = getSelectedTreeItem();

    if (null == testInfo) {
      return;
    }

    if (testInfo instanceof TestMethodTreeItem) {
      OpenTestAction action = new OpenTestAction(m_testRunnerPart, testInfo.getRunInfo());
      
      if(action.isEnabled()) {
        action.run();
      }
    }
  }

  /**
   * @return the RunInfo associated with the current selection, or null it not applicable.
   */
  private ITreeItem getSelectedTreeItem() {
    TreeItem[] treeItems= m_tree.getSelection();
    
    return treeItems.length == 0 ? null
        : BaseTreeItem.getTreeItem(treeItems[0]);
  }

  /**
   * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
   */
  public void menuAboutToShow(IMenuManager manager) {
    if(m_tree.getSelectionCount() > 0) {
      TreeItem treeItem = m_tree.getSelection()[0];
      RunInfo testInfo = BaseTreeItem.getTreeItem(treeItem).getRunInfo();

      manager.add(new CopyAction(treeItem));
      manager.add(new OpenTestAction(m_testRunnerPart, testInfo));
      manager.add(new Separator());
      manager.add(new QuickRunAction(m_testRunnerPart.getLaunchedProject(), 
          m_testRunnerPart.getLastLaunch(),
          testInfo,
          ILaunchManager.RUN_MODE));
      manager.add(new QuickRunAction(m_testRunnerPart.getLaunchedProject(),
          m_testRunnerPart.getLastLaunch(),
          testInfo,
          ILaunchManager.DEBUG_MODE));
      manager.add(new Separator());
      manager.add(new ExpandAllAction());
    }
  }

  /**
   * This method is invoked whenever a new test result arrives. Before adding the
   * corresponding method node, it makes sure that all its ancestors (class, test, suite)
   * exist and create them if they don't. Then it adds the time of the new test method
   * to all these nodes.
   */
  @Override
  public void updateTestResult(RunInfo runInfo, boolean expand) {
    m_runInfos.add(runInfo);
    if (acceptTestResult(runInfo) && matchesSearchFilter(runInfo)) {
      ITreeItem suite = m_suites.get(runInfo.getSuiteName());
      if (suite == null) {
        suite = new SuiteTreeItem(m_tree, runInfo);
        m_suites.put(runInfo.getSuiteName(), suite);
      }
      String pathToTest = runInfo.getSuiteName() + "#" + runInfo.getTestName();
      ITreeItem test = m_tests.get(pathToTest);
      if (test == null) {
        test = new TestTreeItem(suite.getTreeItem(), runInfo);
        m_tests.put(pathToTest, test);
      }
      String pathToClass = pathToTest + "#" + runInfo.getInstanceName();
      ITreeItem cls = m_classes.get(pathToClass);
      if (cls == null) {
        cls = new ClassTreeItem(test.getTreeItem(), runInfo);
        m_classes.put(pathToClass, cls);
      }
      String pathToMethod = pathToClass + "#" + runInfo.getMethodName();
      ITreeItem method = new TestMethodTreeItem(cls.getTreeItem(), runInfo);
      m_methods.put(pathToMethod, method);

      // Create a node for the parameter values, if applicable
      ITreeItem methodParam = null;
      if (!StringUtils.isEmptyString(runInfo.getParametersDisplay())) {
        methodParam = new TestMethodParametersTreeItem(method.getTreeItem(),
            runInfo);
        methodParam.addToCumulatedTime(runInfo);
      }
      if (expand) {
        suite.getTreeItem().setExpanded(true);
        test.getTreeItem().setExpanded(true);
        cls.getTreeItem().setExpanded(true);
        method.getTreeItem().setExpanded(true);
        if (methodParam != null) {
          methodParam.getTreeItem().setExpanded(true);
          focus(methodParam.getTreeItem());
        } else {
          focus(method.getTreeItem());
        }
      }
      suite.addToCumulatedTime(runInfo);
      test.addToCumulatedTime(runInfo);
      cls.addToCumulatedTime(runInfo);
      method.addToCumulatedTime(runInfo);
    }
  }

  private void focus(TreeItem treeItem) {
    // how to remove the annoying selection background?
    treeItem.getParent().setSelection(treeItem);
  }

  @Override
  public void updateTestResult(List<RunInfo> results) {
    if (results.size() > 0) {
      reset();
      for (RunInfo ri : results) {
        updateTestResult(ri, false /* don't expand, we'll do that at the end */);
      }
    }
    postExpandAll();
  }

  private void reset() {
    m_suites.clear();
    m_tests.clear();
    m_classes.clear();
    m_methods.clear();
    m_tree.removeAll();
  }

  @Override
  public void updateSearchFilter(String text) {
    if (text.equals(m_searchFilter)) return;

    m_searchFilter = text;
    reset();
    m_treeItemMap.clear();
    for (final RunInfo runInfo : m_runInfos) {
      if (matchesSearchFilter(runInfo)) {
        updateTestResult(runInfo, true /* expand */);
      }
    }

  }

  private boolean matchesSearchFilter(RunInfo runInfo) {
    if ("".equals(m_searchFilter)) return true;
    else {
      return Pattern.matches(".*" + m_searchFilter.toLowerCase() + ".*",
          runInfo.getMethodDisplay().toLowerCase());
    }
  }

  /**
   * Override in subclasses to filter out nodes.
   */
  protected boolean acceptTestResult(RunInfo runInfo) {
    return true;
  }

  @Override
  public void aboutToStart() {
    reset();
    m_runInfos.clear();
    m_tree.removeAll();
    m_treeItemMap = new Hashtable<String, ITreeItem>();
    m_failureTraceComponent.clear();
  }

  @Override
  public void activate() {
    testSelected();
  }

  @Override
  public void setFocus() {
    m_tree.setFocus();
  }

  private class CopyAction extends Action {
    private TreeItem treeItem;
    public CopyAction(TreeItem treeItem) {
      this.treeItem = treeItem;
      setText(ResourceUtil.getString("CopyAction.text")); //$NON-NLS-1$
    }

    @Override
    public void run() {
      fClipboard.setContents(
          new String[]{ treeItem.getText() }, 
          new Transfer[]{ TextTransfer.getInstance() });
    }
  }

  /**
   * Expand all the nodes in the tree.
   */
  private class ExpandAllAction extends Action {
    public ExpandAllAction() {
      setText(ResourceUtil.getString("ExpandAllAction.text")); //$NON-NLS-1$
      setToolTipText(ResourceUtil.getString("ExpandAllAction.tooltip")); //$NON-NLS-1$
    }

    @Override
    public void run() {
      expandAll();
    }
  }

  private void expandAll() {
    m_tree.setRedraw(false);
    for (TreeItem treeItem : m_tree.getItems()) {
      expandAll(treeItem);
    }
    m_tree.setRedraw(true);
  }

  private void expandAll(TreeItem item) {
    item.setExpanded(true);

    for (TreeItem subItem : item.getItems()) {
      expandAll(subItem);
    }
  }

  private RunInfo getSelectedRunInfo() {
    TreeItem[] treeItems = m_tree.getSelection();
    if (treeItems.length == 0) {
      return null;
    } else {
      return BaseTreeItem.getTreeItem(treeItems[0]).getRunInfo();
    }
  }

  private void testSelected() {
    postSyncRunnable(new Runnable() {
    public void run() {
      m_failureTraceComponent.showFailure(getSelectedRunInfo());
    }
  });
  }

  private void postSyncRunnable(Runnable r) {
    m_tree.getDisplay().syncExec(r);
  }

  private void registerTreeItem(String id, ITreeItem treeItem) {
    m_treeItemMap.put(id, treeItem);
  }

  /**
   * Called after an item has been updated, meant to be overridden by subclasses
   */
  protected void onPostUpdate(TreeItem ti, int state) {
  }

  private void postExpandAll() {
    Runnable expandRunnable = new Runnable() {
      public void run() {
        m_tree.selectAll();
        expandAll();
      };
    };

    m_tree.getDisplay().syncExec(expandRunnable);
  }

  private void initImages() {
    m_testHierarchyIcon = Images.getImage(IMG_TEST_HIERARCHY); 
    m_stackViewIcon = TestNGPlugin.getImageDescriptor("eview16/stackframe.gif")
        .createImage(); //$NON-NLS-1$
  }

  private String getRatioTag() {
    return getNameKey() + "." + TAG_RATIO;
  }

  @Override
  public void saveState(IMemento memento) {
    if(m_sashForm == null) {
      // part has not been created
      if(m_stateMemento != null) { //Keep the old state;
        memento.putMemento(m_stateMemento);
      }

      return;
    }

    int[] weigths = m_sashForm.getWeights();
    int   ratio = (weigths[0] * 1000) / (weigths[0] + weigths[1]);
    memento.putInteger(getRatioTag(), ratio);
  }

  @Override
  public void restoreState(IMemento memento) {
    if (memento == null) return;

    Integer ratio = memento.getInteger(getRatioTag());
    if (ratio != null) {
      m_sashForm.setWeights(new int[] { ratio.intValue(), 1000 - ratio.intValue() });
    }

  }

  @Override
  public void setOrientation(boolean horizontal) {
    m_sashForm.setOrientation(horizontal ? SWT.HORIZONTAL : SWT.VERTICAL);
  }

//  private void addResizeListener(Composite parent) {
//    parent.addControlListener(new ControlListener() {
//      public void controlMoved(ControlEvent e) {
//      }
//
//      public void controlResized(ControlEvent e) {
//        computeOrientation();
//      }
//    });
//  }

}
