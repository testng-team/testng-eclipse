package org.testng.eclipse.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.Utils.JavaElement;

/**
 * A wizard page that displays the list of public methods on the currently selected class
 * so that the user can select or deselect them before creating a new test class.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class TestNGMethodWizardPage extends WizardPage {

  private List<IMethod> m_elements = new ArrayList<>();
  private Table m_table;

  protected TestNGMethodWizardPage(List<JavaElement> elements) {
    super(ResourceUtil.getString("NewTestNGClassWizardPage.title"));
    setTitle(ResourceUtil.getString("NewTestNGClassWizardPage.title"));
    setDescription(ResourceUtil.getString("TestNGMethodWizardPage.description"));
    for (JavaElement je : elements) {
      if (je.compilationUnit != null) {
        try {
          for (IType type : je.compilationUnit.getTypes()) {
            for (IMethod method : type.getMethods()) {
              m_elements.add(method);
            }
          }
        } catch(JavaModelException ex) {
          // ignore
        }
      }
    }
    Collections.sort(m_elements, new Comparator<IMethod>() {

      public int compare(IMethod o1, IMethod o2) {
        return o1.getElementName().compareTo(o2.getElementName());
      }

    });
  }

  private String toSignature(IMethod method) {
    StringBuilder result = new StringBuilder(method.getElementName());
    String[] types = method.getParameterTypes();
    try {
      String[] names = method.getParameterNames();
      result.append("(");
      for (int i = 0; i < types.length; i++) {
        if (i > 0) result.append(", ");
        result.append(Signature.toString(types[i]) + " " + names[i]);
      }
      result.append(")");
    }
    catch(JavaModelException ex) {
      // ignore
    }

    return result.toString();
  }

  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);

    {
      GridLayout layout = new GridLayout();
      layout.numColumns = 2;
      container.setLayout(layout);
    }

    {
      m_table = new Table(container, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
      for (IMethod element : m_elements) {
        TableItem item = new TableItem(m_table, SWT.NONE);
        item.setText(toSignature(element));
        item.setData(element);
      }
      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
      gd.verticalSpan = 2;
      m_table.setLayoutData(gd);
    }

    {
      Composite cb = new Composite(container, SWT.NULL);
      GridLayout layout = new GridLayout();
//      cb.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_BLUE));
      cb.setLayout(layout);

      Button selectAll = new Button(cb, SWT.NONE);
      selectAll.setText("Select all");
      selectAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
      selectAll.addSelectionListener(new Listener(true /* select */));
  
      Button deselectAll = new Button(cb, SWT.NONE);
      deselectAll.setText("Deselect all");
      deselectAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
      deselectAll.addSelectionListener(new Listener(false /* deselect */));
    }

    setControl(container);
  }

  class Listener implements SelectionListener {
    private boolean m_select;

    public Listener(boolean select) {
      m_select = select;
    }

    public void widgetSelected(SelectionEvent e) {
      selectAll(m_select);
    }

    public void widgetDefaultSelected(SelectionEvent e) {
    }
  }

  private void selectAll(boolean select) {
    for (TableItem ti : m_table.getItems()) {
      ti.setChecked(select);
    }
  }

  public List<IMethod> getSelectedMethods() {
    List<IMethod> result = new ArrayList<>();
    for (TableItem ti : m_table.getItems()) {
      if (ti.getChecked()) {
        result.add((IMethod) ti.getData());
      }
    }

    return result;
  }
}
