package org.testng.eclipse.util.param;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.google.common.base.Strings;

/**
 * Visitor for TestNG methods.
 */
public class TestNGMethodParameterVisitor extends ASTVisitor {
  private static final String ANNOTATION_PACKAGE = "org.testng.annotations.";
  private static final String PARAMETER_ANNOTATION = "Parameters";
  private static final String PARAMETER_ANNOTATION_FQN =  ANNOTATION_PACKAGE + PARAMETER_ANNOTATION;
  private static final String CONFIGURATION_ANNOTATION = "Configuration";
  private static final String CONFIGURATION_ANNOTATION_FQN = ANNOTATION_PACKAGE + CONFIGURATION_ANNOTATION;
  private static final String TEST_ANNOTATION = "Test";
  private static final String TEST_ANNOTATION_FQN = ANNOTATION_PACKAGE + TEST_ANNOTATION;
  private static final String DEFAULT_PARAM_VALUE = "param-val-not-found";

  private Map<MethodDeclaration, List<String>> m_parameters = new HashMap<>();
  private IType m_typeFilter;
  private IMethod m_methodFilter;

  public TestNGMethodParameterVisitor() {
  }

  public TestNGMethodParameterVisitor(IMethod methodOnly) {
    m_methodFilter= methodOnly;
  }

  public TestNGMethodParameterVisitor(IType typeOnly) {
    m_typeFilter= typeOnly;
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    if(null != m_typeFilter) {
      return node.getName().getIdentifier().equals(m_typeFilter.getElementName());
    }
    else {
      return true;
    }
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    if(null != m_methodFilter) {
      return node.getName().getIdentifier().equals(m_methodFilter.getElementName())
          && node.parameters().size() == m_methodFilter.getNumberOfParameters(); 
    }
    else {
      return true;
    }
  }

  /**
   * No MarkerAnnotated method can have parameters. Ignore.
   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MarkerAnnotation)
   */
  @Override
  public boolean visit(MarkerAnnotation annotation) {
    return false;
  }

  @Override
  public boolean visit(NormalAnnotation annotation) {
    if(!isKnownAnnotation(annotation.getTypeName().getFullyQualifiedName())) {
      return false;
    }

    List<?> values= annotation.values();

    if(null != values && !values.isEmpty()) {
      for(int i= 0; i < values.size(); i++) {
        MemberValuePair pair= (MemberValuePair) values.get(i);
        if("parameters".equals(pair.getName().toString())) {
          Expression paramAttr= pair.getValue();
          if(paramAttr instanceof ArrayInitializer) {
            record((MethodDeclaration) annotation.getParent(), (ArrayInitializer) paramAttr);
          } else if (paramAttr instanceof StringLiteral) {
            record((MethodDeclaration) annotation.getParent(), (StringLiteral) paramAttr);
          }
        }
      }
    }

    return false;
  }

  /**
   * Only Parameters annotation can be used to describe the parameters.
   * 
   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SingleMemberAnnotation)
   */
  @Override
  public boolean visit(SingleMemberAnnotation annotation) {
    if(isParameterAnnotation(annotation.getTypeName().getFullyQualifiedName())) {
      Expression paramValues= annotation.getValue();
      if (paramValues instanceof ArrayInitializer) {
        record((MethodDeclaration) annotation.getParent(), (ArrayInitializer) paramValues);
      } else if (paramValues instanceof StringLiteral) {
        record((MethodDeclaration) annotation.getParent(), (StringLiteral) paramValues);
      }
    }

    return false;
  }

  public boolean hasParameters() {
    return !m_parameters.isEmpty();
  }

  private void record(MethodDeclaration method, List<String> paramNames) {
    m_parameters.put(method,  paramNames);
  }

  protected void record(MethodDeclaration method, StringLiteral value){
    if (! Strings.isNullOrEmpty(value.getLiteralValue())) {
      List<String> paramNames = new ArrayList<String>();
      paramNames.add(value.getLiteralValue());
      record(method, paramNames);
    }
  }

  /**
   * 
   * @param method the known TestNG annotation with required parameters
   * @param values array initializer containing StringLiterals for the parameter names
   */
  protected void record(MethodDeclaration method, ArrayInitializer values) {
    List<?> literals = values.expressions();
    List<String> paramNames = new ArrayList<String>(literals.size());
    for(int i= 0; i < literals.size(); i++) {
      StringLiteral str = (StringLiteral) literals.get(i);
      paramNames.add(str.getLiteralValue());
    }

    record(method, paramNames);
  }

  protected boolean isParameterAnnotation(String annotationType) {
    return PARAMETER_ANNOTATION.equals(annotationType) || PARAMETER_ANNOTATION_FQN.equals(annotationType);
  }

  protected boolean isKnownAnnotation(String annotationType) {
    return CONFIGURATION_ANNOTATION.equals(annotationType)
           || CONFIGURATION_ANNOTATION_FQN.equals(annotationType)
           || TEST_ANNOTATION.equals(annotationType)
           || TEST_ANNOTATION_FQN.equals(annotationType);
  }

  /**
   * @return the non-null parameter name/value map
   */
  public Map<String, String> getParametersMap() {
    Map<String, String> parameterMap = new HashMap<>();
    for (Entry<MethodDeclaration, List<String>> paramEntry : m_parameters.entrySet()) {
      // get all '@Optional' value, as the default value for the parameters
      List<String> optionals = new ArrayList<>();
      for (Object p : paramEntry.getKey().parameters()) {
        SingleVariableDeclaration paramDecl = (SingleVariableDeclaration) p;
        for (Object pm : paramDecl.modifiers()) {
          if (pm instanceof SingleMemberAnnotation) {
            SingleMemberAnnotation paramAnn = (SingleMemberAnnotation) pm;
            if ("Optional".equals(paramAnn.getTypeName().toString())) {
              optionals.add(paramAnn.getValue().toString());
            }
          }
        };
      };

      List<String> paramNames = paramEntry.getValue();
      for(int i = 0; i < paramNames.size(); i++) {
        String val = DEFAULT_PARAM_VALUE;
        // the user is responsible for making sure the order of the @Optional parameters follow the order in @Parameters
        if (i < optionals.size() && optionals.get(i) != null) {
          val = optionals.get(i);
        }

        String paramName = paramNames.get(i);

        if (parameterMap.containsKey(paramName) && !DEFAULT_PARAM_VALUE.equals(parameterMap.get(paramName))) {
          continue;
        }

        parameterMap.put(paramName, val);
      }
    }

    return parameterMap;
  }
}
