package org.testng.eclipse.util.signature;

import java.util.List;

import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WildcardType;


public class TypeSignature {
  private TypeSignature() {
  }
  
  public static String getSignature(Type type) {
    return dispatch(type);
  } 
  
  private static String getPrimitiveSignature(PrimitiveType pt) {
    String type = pt.toString();
    if("byte".equals(type)) {
      return "B";
    } else if("short".equals(type)) {
      return "S";
    } else if("char".equals(type)) {
      return "C";
    } else if("int".equals(type)) {
      return "I";
    } else if("long".equals(type)) {
      return "J";
    } else if("float".equals(type)) {
      return "F";
    } else if("double".equals(type)) {
      return "D";
    } else if("boolean".equals(type)) {
      return "Z";
    } else if("void".equals(type)) {
      return "V";
    }
    
    return "";
  }
  
  private static String getArraySignature(ArrayType type) {
    int dimensions = type.getDimensions();
    StringBuffer buf = new StringBuffer();
    for(int i = 0; i < dimensions; i++) {
      buf.append("[");
    }
    
    buf.append(dispatch(type.getComponentType()));
    
    return buf.toString();
  }
  
  private static String getTypeSignature(Type type) {
    return "Q" + type.toString() + ";";
  }
  
  private static String getWildcardSignature(WildcardType wtype) {
    if(wtype.isUpperBound()) {
      return "+" + dispatch(wtype.getBound());
    }
    else {
      return "-" + dispatch(wtype.getBound());
    }
  }

  /**
   * Make sure that this signature matches the string returned by IMethod#getSignature
   * or bugs like this one will happen:
   * http://groups.google.com/group/testng-users/browse_thread/thread/50db1c6deb2ff48e/60b80ac1999394f2?hl=en&lnk=gst&q=lisak#60b80ac1999394f2
   */
  private static String getParameterizedSignature(ParameterizedType ptype) {
    StringBuffer buf = new StringBuffer();
    String sig = dispatch(ptype.getType());
    if(sig.length() > 0) {
      buf.append(sig.substring(0, sig.length() - 1));
    }
    else {
      buf.append(sig);
    }
    
    buf.append("<");
    List types = ptype.typeArguments();
    for(int i = 0; i < types.size(); i++) {
      buf.append(dispatch((Type) types.get(i)));
    }
    
    buf.append(">;");
    
    return buf.toString();
  }
  
  private static String dispatch(Type type) {
    if(null == type) {
      return "";
    }
    if(type.isArrayType()) {
      return getArraySignature((ArrayType) type);
    } else if(type.isPrimitiveType()) {
      return getPrimitiveSignature((PrimitiveType) type); 
    } else if(type.isSimpleType()) {
      return getTypeSignature((SimpleType) type);
    } else if(type.isQualifiedType()) {
      return getTypeSignature((QualifiedType) type);
    } else if(type.isParameterizedType()) {
      return getParameterizedSignature((ParameterizedType) type);
    } else if(type.isWildcardType()) {
      return getWildcardSignature((WildcardType) type);
    }

    return "";
  }
}
