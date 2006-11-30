package org.testng.eclipse.launch.components;


import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.testng.eclipse.ui.util.Utils;

/**
 * An AST visitor used to parse all the JavaDoc annotations in a file
 *
 * @author cbeust
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class JavaDocVisitor extends BaseVisitor {
  private static final Pattern GROUPS_PATTERN = Pattern.compile(".*\\s*groups\\s*=\\s*\"([^\"]*)\"\\s*.*");
  
  public JavaDocVisitor() {
    super(true); // visit JavaDoc tags
  }

  /**
   * If @testng.test found on Type level than all methods are considered valid TestNGMethods.
   * 
   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodDeclaration)
   */
  public boolean visit(MethodDeclaration node) {
    if(m_typeIsTest) {
      addTestMethod(node, JDK14_ANNOTATION);
      return false; // no need to continue;
    }
    
    return true;
  }

  /**
   * Records if a Type or Method is javadoc annotated with @testng.test.
   * 
   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.Javadoc)
   */
  public boolean visit(Javadoc node) {
    ASTNode parent = node.getParent();
    
    if(ASTNode.TYPE_DECLARATION != parent.getNodeType()
        && ASTNode.METHOD_DECLARATION != parent.getNodeType()) {
      return false;
    }
    
    // visit tag elements inside doc comments only if requested
    List   tags = node.tags();
    for(Iterator it = tags.iterator(); it.hasNext();) {
      TagElement te = (TagElement) it.next();
      String     tagName = te.getTagName();
      if(Utils.isTestNGTag(tagName)) {
        if(Utils.TEST_ANNOTATION.equals(tagName)) {
          m_annotationType = JDK14_ANNOTATION;
          if(parent instanceof MethodDeclaration) {
            addTestMethod((MethodDeclaration) parent, JDK14_ANNOTATION);
          } 
          else if(parent instanceof TypeDeclaration) {
            m_typeIsTest = true;
          }
          
          List fragments = te.fragments();
          for(int i = 0; i < fragments.size(); i++) {
            if(fragments.get(i) instanceof TextElement) {
              TextElement txte = (TextElement) fragments.get(i);
              Matcher matcher = GROUPS_PATTERN.matcher(txte.getText());
              if(matcher.matches()) {
                addGroup(matcher.group(1));
              }
            }
          }
        }
        else if (Utils.FACTORY_ANNOTATION.equals(tagName)) { // BUGFIX: TESTNG-18
          if(parent instanceof MethodDeclaration) {
            m_annotationType = JDK14_ANNOTATION;
            addFactoryMethod((MethodDeclaration) parent, JDK14_ANNOTATION);
          } 
        }
      }
    }

    return false; // no need to continue under Javadoc node
  }

  public static void ppp(String s) {
    System.out.println("[JavaDocVisitor] " + s);
  }
}
