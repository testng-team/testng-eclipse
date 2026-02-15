package org.testng.eclipse.ui;

import static org.testng.eclipse.ui.Images.IMG_TEST_HIERARCHY;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IMemento;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;
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
 * Results are paginated: at most {@link PagedResultStore#DEFAULT_PAGE_SIZE}
 * results are rendered as tree items at any time. Navigation controls at the
 * bottom of the tree allow browsing through pages.
 *
 * @author Cedric Beust <cedric@beust.com>
 * @see <a href="https://github.com/testng-team/testng-eclipse/issues/578">Issue #578</a>
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

  private TestRunnerViewPart m_testRunnerPart;
  private SashForm m_sashForm;
  private Composite m_parentComposite;

  private Clipboard fClipboard;

  //
  // Keeping track of what's in the tree
  //
  private Map<String, ITreeItem> m_treeItemMap = new HashMap<>();
  private String m_searchFilter = "";
  private Map<String, ITreeItem> m_suites = new HashMap<>();
  private Map<String, ITreeItem> m_tests = new HashMap<>();
  private Map<String, ITreeItem> m_classes = new HashMap<>();
  // bug/167: use ArrayListMultimap which allows duplicate keys to store m_methods
  //          since the instanceName (which is used as key) might be same
  private ArrayListMultimap<String, ITreeItem> m_methods = ArrayListMultimap.create();
  // Don't forget to .clear() all these maps beween each run ^^

  // Pagination
  private PagedResultStore m_store = new PagedResultStore(getPageSize());
  private Composite m_paginationBar;
  private Button m_firstPageBtn;
  private Button m_prevPageBtn;
  private Label m_pageInfoLabel;
  private Button m_nextPageBtn;
  private Button m_lastPageBtn;

  private static int getPageSize() {
    try {
      return TestNGPlugin.getDefault().getPreferenceStore()
          .getInt(TestNGPluginConstants.S_PAGE_SIZE);
    } catch (Exception e) {
      return PagedResultStore.DEFAULT_PAGE_SIZE;
    }
  }

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

    // Left side: tree + pagination bar
    Composite treeComposite = new Composite(m_sashForm, SWT.NONE);
    GridLayout treeLayout = new GridLayout(1, false);
    treeLayout.marginHeight = 0;
    treeLayout.marginWidth = 0;
    treeComposite.setLayout(treeLayout);

    //
    // Tree
    //
    m_tree = new Tree(treeComposite, SWT.SINGLE | SWT.VIRTUAL);
    m_tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    //
    // Pagination bar
    //
    createPaginationBar(treeComposite);

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

  private void createPaginationBar(Composite parent) {
    m_paginationBar = new Composite(parent, SWT.NONE);
    GridData barGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    m_paginationBar.setLayoutData(barGridData);
    GridLayout barLayout = new GridLayout(5, false);
    barLayout.marginHeight = 2;
    barLayout.marginWidth = 2;
    m_paginationBar.setLayout(barLayout);

    m_firstPageBtn = new Button(m_paginationBar, SWT.PUSH);
    m_firstPageBtn.setText("<<"); //$NON-NLS-1$
    m_firstPageBtn.setToolTipText("First page");
    m_firstPageBtn.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        m_store.firstPage();
        renderCurrentPage();
      }
    });

    m_prevPageBtn = new Button(m_paginationBar, SWT.PUSH);
    m_prevPageBtn.setText("<"); //$NON-NLS-1$
    m_prevPageBtn.setToolTipText("Previous page");
    m_prevPageBtn.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        m_store.prevPage();
        renderCurrentPage();
      }
    });

    m_pageInfoLabel = new Label(m_paginationBar, SWT.CENTER);
    m_pageInfoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    m_nextPageBtn = new Button(m_paginationBar, SWT.PUSH);
    m_nextPageBtn.setText(">"); //$NON-NLS-1$
    m_nextPageBtn.setToolTipText("Next page");
    m_nextPageBtn.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        m_store.nextPage();
        renderCurrentPage();
      }
    });

    m_lastPageBtn = new Button(m_paginationBar, SWT.PUSH);
    m_lastPageBtn.setText(">>"); //$NON-NLS-1$
    m_lastPageBtn.setToolTipText("Last page");
    m_lastPageBtn.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        m_store.lastPage();
        renderCurrentPage();
      }
    });

    // Initially hidden until needed
    setPaginationVisible(false);
  }

  private void setPaginationVisible(boolean visible) {
    m_paginationBar.setVisible(visible);
    ((GridData) m_paginationBar.getLayoutData()).exclude = !visible;
    m_paginationBar.getParent().layout();
  }

  private void updatePaginationControls() {
    boolean needsPagination = m_store.getTotalFilteredCount() > m_store.getPageSize();
    setPaginationVisible(needsPagination);

    if (needsPagination) {
      int page = m_store.getCurrentPage();
      int totalPages = m_store.getTotalPages();
      int totalCount = m_store.getTotalFilteredCount();

      m_pageInfoLabel.setText("Page " + (page + 1) + " of " + totalPages //$NON-NLS-1$ //$NON-NLS-2$
          + " (" + totalCount + " results)"); //$NON-NLS-1$ //$NON-NLS-2$

      m_firstPageBtn.setEnabled(page > 0);
      m_prevPageBtn.setEnabled(page > 0);
      m_nextPageBtn.setEnabled(page < totalPages - 1);
      m_lastPageBtn.setEnabled(page < totalPages - 1);

      m_paginationBar.layout();
    }
  }

  /**
   * Clears the tree and renders only the current page slice from the store.
   */
  private void renderCurrentPage() {
    m_tree.setRedraw(false);
    resetTree();
    List<RunInfo> pageResults = m_store.getPageResults();
    for (RunInfo ri : pageResults) {
      renderSingleResult(ri, false);
    }
    expandAll();
    m_tree.setRedraw(true);
    updatePaginationControls();
  }

  /**
   * Renders a single RunInfo into the tree, creating ancestor nodes as needed.
   * This is the original tree-building logic extracted from the old updateTestResult.
   */
  private void renderSingleResult(RunInfo runInfo, boolean expand) {
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
   * This method is invoked whenever a new test result arrives from a running test.
   * The result is always added to the backing store. The tree is only updated
   * if the user is on the last page and the page still has room, so that at most
   * {@code pageSize} items exist at any time.
   */
  @Override
  public void updateTestResult(RunInfo runInfo, boolean expand) {
    boolean matchesFilter = acceptTestResult(runInfo) && matchesSearchFilter(runInfo);

    // Always update the store's filter to match current tab + search criteria
    // before adding (the store filter is set in aboutToStart / updateSearchFilter)
    m_store.addResult(runInfo);

    if (matchesFilter) {
      if (m_store.isOnLastPage() && m_store.currentPageHasRoom()) {
        // Render this single item into the tree (live update)
        renderSingleResult(runInfo, expand);
      }
      updatePaginationControls();
    }
  }

  private void focus(TreeItem treeItem) {
    // how to remove the annoying selection background?
    treeItem.getParent().setSelection(treeItem);
  }

  @Override
  public void updateTestResult(List<RunInfo> results) {
    if (results.size() > 0) {
      m_store.load(results);
      m_store.setFilter(ri -> acceptTestResult(ri) && matchesSearchFilter(ri));
      m_store.firstPage();
      renderCurrentPage();
    } else {
      postExpandAll();
    }
  }

  private void resetTree() {
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
    m_treeItemMap.clear();
    m_store.setFilter(ri -> acceptTestResult(ri) && matchesSearchFilter(ri));
    m_store.firstPage();
    renderCurrentPage();
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
    resetTree();
    m_store.clear();
    m_store.setFilter(ri -> acceptTestResult(ri) && matchesSearchFilter(ri));
    m_tree.removeAll();
    m_treeItemMap = new Hashtable<String, ITreeItem>();
    m_failureTraceComponent.clear();
    updatePaginationControls();
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
    m_stackViewIcon = TestNGPlugin.getImageDescriptor("eview16/stackframe.png")
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

}
