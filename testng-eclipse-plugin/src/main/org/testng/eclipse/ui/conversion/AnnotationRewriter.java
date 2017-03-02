package org.testng.eclipse.ui.conversion;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.PreferenceStoreUtil.SuiteMethodTreatment;

/**
 * A rewriter that will convert the current JUnit file to TestNG
 * using JDK5 annotations
 *
 * @author C�dric Beust <cedric@beust.com>
 */
public class AnnotationRewriter implements IRewriteProvider
{
  private static final Set<String> IMPORTS_TO_REMOVE = new HashSet<String>() {{
    add("junit.framework.Assert");
    add("junit.framework.Test");
    add("junit.framework.TestCase");
    add("junit.framework.TestSuite");
    add("org.junit.After");
    add("org.junit.AfterClass");
    add("org.junit.Before");
    add("org.junit.BeforeClass");
    add("org.junit.Ignore");
    add("org.junit.Test");
    add("org.junit.runner.RunWith");
    add("org.junit.runners.Parameterized");
  }};
  private static final Set<String> STATIC_IMPORTS_TO_REMOVE = new HashSet<String>() {{
    add("org.junit.Assert");
  }};

  public ASTRewrite createRewriter(CompilationUnit astRoot, AST ast) {
    final ASTRewrite result = ASTRewrite.create(astRoot.getAST());
    JUnitVisitor visitor = new JUnitVisitor();
    astRoot.accept(visitor);


    //
    // Remove some JUnit imports.
    //
    List<ImportDeclaration> oldImports = visitor.getJUnitImports();
    for (int i = 0; i < oldImports.size(); i++) {
      Name importName = oldImports.get(i).getName();
      String fqn = importName.getFullyQualifiedName();
      if (IMPORTS_TO_REMOVE.contains(fqn)) {
        result.remove(oldImports.get(i), null);
      }
      for (String s : STATIC_IMPORTS_TO_REMOVE) {
        if (fqn.contains(s)) {
          result.remove(oldImports.get(i), null);
        }
      }
    }
    
    //
    // Add imports as needed
    //
    maybeAddImport(ast, result, astRoot, visitor.hasAsserts(), "org.testng.AssertJUnit");
    maybeAddImport(ast, result, astRoot, visitor.hasFail(), "org.testng.Assert");
    maybeAddImport(ast, result, astRoot, !visitor.getBeforeClasses().isEmpty(),
    "org.testng.annotations.BeforeClass");
    maybeAddImport(ast, result, astRoot, !visitor.getBeforeMethods().isEmpty(),
        "org.testng.annotations.BeforeMethod");
    maybeAddImport(ast, result, astRoot, visitor.hasTestMethods(), "org.testng.annotations.Test");
    maybeAddImport(ast, result, astRoot, !visitor.getAfterMethods().isEmpty(),
        "org.testng.annotations.AfterMethod");
    maybeAddImport(ast, result, astRoot, !visitor.getAfterClasses().isEmpty(),
    "org.testng.annotations.AfterClass");

    //
    // Add static imports
    //
    Set<String> staticImports = visitor.getStaticImports();
    for (String si : staticImports) {
      addImport(ast, result, astRoot, "org.testng.AssertJUnit." + si, true /* static import */);
    }

    //
    // Remove "extends TestCase"
    //
    SimpleType td = visitor.getTestCase();
    if (null != td) {
      result.remove(td, null);
    }

    //
    // Addd the annotations as needed
    //
    maybeAddAnnotations(ast, visitor, result, visitor.getTestMethods(), "Test", null, null);
    maybeAddAnnotations(ast, visitor, result, visitor.getDisabledTestMethods(), "Test", null,
        createDisabledAttribute(ast));
    maybeAddAnnotations(ast, visitor, result, visitor.getBeforeMethods(), "BeforeMethod",
        "@Before" /* annotation to remove */);
    maybeAddAnnotations(ast, visitor, result, visitor.getAfterMethods(), "AfterMethod",
        "@After" /* annotation to remove */);

    //
    // suite() method: remove, comment out or leave untouched, depending on the setting
    //
    SuiteMethodTreatment smt = TestNGPlugin.getPluginPreferenceStore().getSuiteMethodTreatement();

    MethodDeclaration suiteMethod = visitor.getSuite();
    if (smt != SuiteMethodTreatment.DONT_TOUCH && suiteMethod != null) {
      if (smt == SuiteMethodTreatment.REMOVE) {
        // Remove suite()
        result.remove(suiteMethod, null);
      } else {
        // Comment out suite()
        TypeDeclaration type = visitor.getType();
        ListRewrite lr = result.getListRewrite(type, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        lr.insertBefore(result.createStringPlaceholder("/*", ASTNode.METHOD_DECLARATION),
            suiteMethod, null);
        lr.insertAfter(result.createStringPlaceholder("*/", ASTNode.METHOD_DECLARATION),
            suiteMethod, null);
      }
    }

    //
    // Remove all the nodes that need to be removed
    //
    for (ASTNode n : visitor.getNodesToRemove()) {
      result.remove(n, null);
    }

    //
    // Replace @Ignore with @Test(enabled = false)
    //
    for (Map.Entry<MethodDeclaration, Annotation> e : visitor.getIgnoredMethods().entrySet()) {
      MethodDeclaration md = e.getKey();
      Annotation ignored = e.getValue();
      // Add the @Test(enabled = false)
      NormalAnnotation test = ast.newNormalAnnotation();
      test.setTypeName(ast.newName("Test"));
      MemberValuePair mvp = ast.newMemberValuePair();
      mvp.setName(ast.newSimpleName("enabled"));
      mvp.setValue(ast.newBooleanLiteral(false));
      test.values().add(mvp);
      result.remove(ignored, null);
      ListRewrite lr = result.getListRewrite(md, MethodDeclaration.MODIFIERS2_PROPERTY);
      lr.insertFirst(test, null);
    }

    //
    // Replace "Assert" with "AssertJUnit", unless the method is already imported statically.
    //
    Set<MethodInvocation> asserts = visitor.getAsserts();
    for (MethodInvocation m : asserts) {
      if (! staticImports.contains(m.getName().toString())) {
        Expression exp = m.getExpression();
        Name name = ast.newName("AssertJUnit");
        if (exp != null) {
          result.replace(exp, name, null);
        } else {
          result.set(m, MethodInvocation.EXPRESSION_PROPERTY, name, null);
        }
      }
    }

    //
    // Replace "fail()" with "Assert.fail()"
    //
    for (MethodInvocation fail : visitor.getFails()) {
      SimpleName exp = ast.newSimpleName("Assert");
      result.set(fail, MethodInvocation.EXPRESSION_PROPERTY, exp, null);
    }

    //
    // Replace @Test(expected) with @Test(expectedExceptions)
    // and @Test(timeout) with @Test(timeOut)
    //
    for (Map.Entry<MemberValuePair, String> pair : visitor.getTestsWithExpected().entrySet()) {
      result.replace(pair.getKey().getName(), ast.newSimpleName(pair.getValue()), null);
    }

    //
    // Remove super invocation in the constructor
    //
    SuperConstructorInvocation sci = visitor.getSuperConstructorInvocation();
    if (sci != null) {
      result.remove(sci, null);
    }

    //
    // Convert @RunWith(Parameterized.class)
    //
    SingleMemberAnnotation runWith = visitor.getRunWithParameterized();
    if (runWith != null) {
      // Remove @RunWith
      result.remove(runWith, null);

      // Add imports
      addImport(ast, result, astRoot, "org.testng.ConversionUtils.wrapDataProvider",
          true /* static import */);
      addImport(ast, result, astRoot, "org.testng.annotations.Factory", false /* not static */);

      // Add the factory method
      MethodDeclaration parameterMethod = visitor.getParametersMethod();
      ListRewrite lr = result.getListRewrite(visitor.getType(),
          TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
      MethodDeclaration md = ast.newMethodDeclaration();
      md.setName(ast.newSimpleName("factory" + capitalize(parameterMethod.getName().toString())));

      // Add the "Factory" annotation
      MarkerAnnotation factory = ast.newMarkerAnnotation();
      factory.setTypeName(ast.newName("Factory"));
      md.modifiers().add(factory);

      // Make the method public
      md.modifiers().addAll(ast.newModifiers(Modifier.PUBLIC | Modifier.STATIC));
      ArrayType returnType = ast.newArrayType(ast.newSimpleType(ast.newName("Object")));
      md.setReturnType2(returnType);

      // Create the method invocation "ConversionUtils.wrapDataProvider(Foo.class, data())"
      MethodInvocation mi = ast.newMethodInvocation();
      mi.setName(ast.newSimpleName("wrapDataProvider"));

      // Add parameters to wrapDataProvider()
      // 1) the current class
      TypeLiteral tl = ast.newTypeLiteral();
      tl.setType(ast.newSimpleType(ast.newSimpleName(visitor.getType().getName().toString())));
      mi.arguments().add(tl);

      // 2) the call to the @Parameters method
      MethodInvocation pmi = ast.newMethodInvocation();
      pmi.setName(ast.newSimpleName(parameterMethod.getName().getFullyQualifiedName()));
      mi.arguments().add(pmi);

      // Create the return statement
      ReturnStatement returnStatement = ast.newReturnStatement();
      returnStatement.setExpression(mi);

      Block block = ast.newBlock();
      block.statements().add(returnStatement);
      md.setBody(block);

      lr.insertFirst(md, null);
    }

    return result;
  }

  private String capitalize(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  private Map<String, Boolean> createDisabledAttribute(AST ast) {
    Map<String, Boolean> result = new HashMap<>();
    result.put("enabled", false);
    return result;
  }

  private void maybeAddImport(AST ast, ASTRewrite rewriter, CompilationUnit astRoot, boolean add,
      String imp) {
    if (add) {
      addImport(ast, rewriter, astRoot, imp);
    }
  }

  private void addImport(AST ast, ASTRewrite rewriter, CompilationUnit astRoot, String imp) {
    addImport(ast, rewriter, astRoot, imp, false /* non static import */);
  }

  private void addImport(AST ast, ASTRewrite rewriter, CompilationUnit astRoot, String imp,
      boolean isStatic) {
    ListRewrite lr = rewriter.getListRewrite(astRoot, CompilationUnit.IMPORTS_PROPERTY);
    ImportDeclaration id = ast.newImportDeclaration();
    id.setStatic(isStatic);
    id.setName(ast.newName(imp));
    lr.insertFirst(id, null);
  }

  /**
   * Add the given annotation if the method is non null
   */
  private void maybeAddAnnotation(AST ast, JUnitVisitor visitor, ASTRewrite rewriter,
      MethodDeclaration method, String annotation, String annotationToRemove,
      Map<String, Boolean> attributes)
  {
    if (method != null) {
      addAnnotation(ast, visitor, rewriter, method, createAnnotation(ast, annotation, attributes),
          annotationToRemove);
    }
  }

  /**
   * @return a NormalAnnotation if the annotation to create has attributes or a
   * MarkerAnnotation otherwise.
   */
  private Annotation createAnnotation(AST ast, String name, Map<String, Boolean> attributes) {
    Annotation result = null;
    NormalAnnotation normalAnnotation = null;
    if (attributes != null && attributes.size() > 0) {
      normalAnnotation = ast.newNormalAnnotation();
      result = normalAnnotation;
    } else {
      result = ast.newMarkerAnnotation();
    }
    result.setTypeName(ast.newName(name));
    if (attributes != null) {
      for (Entry<String, Boolean> a : attributes.entrySet()) {
        MemberValuePair mvp = ast.newMemberValuePair();
        mvp.setName(ast.newSimpleName(a.getKey()));
        mvp.setValue(ast.newBooleanLiteral(a.getValue()));
        normalAnnotation.values().add(mvp);
      }
    }
    return result;
  }

  /**
   * Add the given annotation if the method is non null
   */
  private void maybeAddAnnotations(AST ast, JUnitVisitor visitor, ASTRewrite rewriter,
      Collection<MethodDeclaration> methods, String annotation, String annotationToRemove) {
    maybeAddAnnotations(ast, visitor, rewriter, methods, annotation, annotationToRemove, null);
  }

  private void maybeAddAnnotations(AST ast, JUnitVisitor visitor,
      ASTRewrite rewriter, Collection<MethodDeclaration> methods, String annotation,
      String annotationToRemove, Map<String, Boolean> attributes) {
    for (MethodDeclaration method : methods) {
      maybeAddAnnotation(ast, visitor, rewriter, method, annotation, annotationToRemove,
          attributes);
    }
  }

  private void addAnnotation(AST ast, JUnitVisitor visitor, ASTRewrite rewriter,
      MethodDeclaration md, Annotation a, String annotationToRemove)
  {
    ListRewrite lr = rewriter.getListRewrite(md, MethodDeclaration.MODIFIERS2_PROPERTY);

    // Remove the annotation if applicable
    if (annotationToRemove != null) {
      List modifiers = md.modifiers();
      for (int k = 0; k < modifiers.size(); k++) {
        Object old = modifiers.get(k);
        if (old instanceof Annotation) {
          String oldAnnotation = old.toString();
          if (oldAnnotation.equals(annotationToRemove) || "@Override".equals(oldAnnotation)) {
            lr.remove((Annotation) old, null);
            break;
          }
        }
      }
    }

    // Add the annotation
    lr.insertFirst(a, null);
  }

  public String getName() {
    return "Convert to TestNG (Annotations)";
  }  
}
