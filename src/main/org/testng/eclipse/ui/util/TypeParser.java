package org.testng.eclipse.ui.util;

import org.testng.eclipse.launch.components.AnnotationVisitor;
import org.testng.eclipse.launch.components.BaseVisitor;
import org.testng.eclipse.launch.components.ITestContent;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
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
    ASTParser parser = ASTParser.newParser(AST.JLS3); 
    try {
      parser.setSource(type.getSource().toCharArray());
      CompilationUnit cu = (CompilationUnit) parser.createAST(null);
//      ppp("===== VISITING " + type.getFullyQualifiedName());
      cu.accept(result);
//      ppp("===== DONE VISITING " + type.getFullyQualifiedName());
    }
    catch (JavaModelException e) {
      e.printStackTrace();
    }    
    
    return result;
  }
  
  public static void ppp(String s) {
    System.out.println("[TypeParser] " + s);
  }
}
