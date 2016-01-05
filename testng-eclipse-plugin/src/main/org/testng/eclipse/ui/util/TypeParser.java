package org.testng.eclipse.ui.util;

import org.testng.eclipse.launch.components.AnnotationVisitor;
import org.testng.eclipse.launch.components.BaseVisitor;
import org.testng.eclipse.launch.components.ITestContent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;


/**
 * This class parses an IType into an ITestContent
 * 
 * @author cbeust
 */
public class TypeParser {
  
  public static ITestContent parseType(IType type) {
      BaseVisitor result = new AnnotationVisitor();
      ICompilationUnit compilationUnit = type.getCompilationUnit();
      if (compilationUnit == null) {
        return result;
      }
      ASTParser parser = ASTParser.newParser(AST.JLS3);
      parser.setKind(ASTParser.K_COMPILATION_UNIT);
      parser.setResolveBindings(true);
      parser.setSource(compilationUnit);
      parser.setProject(type.getJavaProject());
      parser.setUnitName(compilationUnit.getPath().toString());
      CompilationUnit cu = (CompilationUnit) parser.createAST(null);
      cu.accept(result);
      return result;
  }

}
