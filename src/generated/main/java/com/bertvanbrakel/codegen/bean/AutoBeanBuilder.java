package com.bertvanbrakel.codegen.bean;
import org.codemucker.jpattern.Generated;
import org.codemucker.jpattern.Pattern;
import org.codemucker.jpattern.PatternType;
@Generated
/** generated by org.codemucker.jmutate.bean.BeanBuilderWriter */
public class AutoBeanBuilder{
@Generated
private double primitiveDouble;
@Generated
private java.lang.Integer integer;
@Generated
private boolean primitiveBoolean;
@Generated
private java.util.Date date;
@Generated
private java.lang.Double _double;
@Generated
private java.lang.Long _long;
@Generated
private java.lang.Float _float;
@Generated
private long primitiveLong;
@Generated
private java.lang.Character character;
@Generated
private java.lang.Short _short;
@Generated
private int primitiveInt;
@Generated
private byte primitiveByte;
@Generated
private java.lang.Byte _byte;
@Generated
private java.lang.String string;
@Generated
private short primitiveShort;
@Generated
private java.lang.Boolean _boolean;
@Generated
private char primitiveChar;
@Generated
private float primitiveFloat;
@Generated
public AutoBeanBuilder primitiveDouble(double primitiveDouble){ this.primitiveDouble = primitiveDouble; return this;}
@Generated
public AutoBeanBuilder integer(java.lang.Integer integer){ this.integer = integer; return this;}
@Generated
public AutoBeanBuilder primitiveBoolean(boolean primitiveBoolean){ this.primitiveBoolean = primitiveBoolean; return this;}
@Generated
public AutoBeanBuilder date(java.util.Date date){ this.date = date; return this;}
@Generated
public AutoBeanBuilder Double(java.lang.Double _double){ this._double = _double; return this;}
@Generated
public AutoBeanBuilder Long(java.lang.Long _long){ this._long = _long; return this;}
@Generated
public AutoBeanBuilder Float(java.lang.Float _float){ this._float = _float; return this;}
@Generated
public AutoBeanBuilder primitiveLong(long primitiveLong){ this.primitiveLong = primitiveLong; return this;}
@Generated
public AutoBeanBuilder character(java.lang.Character character){ this.character = character; return this;}
@Generated
public AutoBeanBuilder Short(java.lang.Short _short){ this._short = _short; return this;}
@Generated
public AutoBeanBuilder primitiveInt(int primitiveInt){ this.primitiveInt = primitiveInt; return this;}
@Generated
public AutoBeanBuilder primitiveByte(byte primitiveByte){ this.primitiveByte = primitiveByte; return this;}
@Generated
public AutoBeanBuilder bite(java.lang.Byte _byte){ this._byte = _byte; return this;}
@Generated
public AutoBeanBuilder string(java.lang.String string){ this.string = string; return this;}
@Generated
public AutoBeanBuilder primitiveShort(short primitiveShort){ this.primitiveShort = primitiveShort; return this;}
@Generated
public AutoBeanBuilder Boolean(java.lang.Boolean _boolean){ this._boolean = _boolean; return this;}
@Generated
public AutoBeanBuilder primitiveChar(char primitiveChar){ this.primitiveChar = primitiveChar; return this;}
@Generated
public AutoBeanBuilder primitiveFloat(float primitiveFloat){ this.primitiveFloat = primitiveFloat; return this;}
@Generated
@Pattern(type=PatternType.BuilderCreate)
public com.bertvanbrakel.codegen.bean.AutoBean create(){ 
   com.bertvanbrakel.codegen.bean.AutoBean bean = new com.bertvanbrakel.codegen.bean.AutoBean();
   bean.setPrimitiveDouble(this.primitiveDouble);
   bean.setInteger(this.integer);
   bean.setPrimitiveBoolean(this.primitiveBoolean);
   bean.setDate(this.date);
   bean.setDouble(this._double);
   bean.setLong(this._long);
   bean.setFloat(this._float);
   bean.setPrimitiveLong(this.primitiveLong);
   bean.setCharacter(this.character);
   bean.setShort(this._short);
   bean.setPrimitiveInt(this.primitiveInt);
   bean.setPrimitiveByte(this.primitiveByte);
   bean.setByte(this._byte);
   bean.setString(this.string);
   bean.setPrimitiveShort(this.primitiveShort);
   bean.setBoolean(this._boolean);
   bean.setPrimitiveChar(this.primitiveChar);
   bean.setPrimitiveFloat(this.primitiveFloat);
   return bean;
}
@Generated
public boolean equals(Object obj){
if( this == obj ) return true;
if( obj == null ) return false;
if( getClass() != obj.getClass() ) return false;
AutoBeanBuilder other = (AutoBeanBuilder)obj;
if (java.lang.Double.doubleToLongBits(this.primitiveDouble) != java.lang.Double.doubleToLongBits(other.primitiveDouble)) return false;
if(this.integer == null){
   if( other.integer != null ){ return false; }
} else if ( !this.integer.equals(other.integer ) ){ return false; }
if(primitiveBoolean != other.primitiveBoolean){ return false; }
if(this.date == null){
   if( other.date != null ){ return false; }
} else if ( !this.date.equals(other.date ) ){ return false; }
if(this._double == null){
   if( other._double != null ){ return false; }
} else if ( !this._double.equals(other._double ) ){ return false; }
if(this._long == null){
   if( other._long != null ){ return false; }
} else if ( !this._long.equals(other._long ) ){ return false; }
if(this._float == null){
   if( other._float != null ){ return false; }
} else if ( !this._float.equals(other._float ) ){ return false; }
if(primitiveLong != other.primitiveLong){ return false; }
if(this.character == null){
   if( other.character != null ){ return false; }
} else if ( !this.character.equals(other.character ) ){ return false; }
if(this._short == null){
   if( other._short != null ){ return false; }
} else if ( !this._short.equals(other._short ) ){ return false; }
if(primitiveInt != other.primitiveInt){ return false; }
if(primitiveByte != other.primitiveByte){ return false; }
if(this._byte == null){
   if( other._byte != null ){ return false; }
} else if ( !this._byte.equals(other._byte ) ){ return false; }
if(this.string == null){
   if( other.string != null ){ return false; }
} else if ( !this.string.equals(other.string ) ){ return false; }
if(primitiveShort != other.primitiveShort){ return false; }
if(this._boolean == null){
   if( other._boolean != null ){ return false; }
} else if ( !this._boolean.equals(other._boolean ) ){ return false; }
if(primitiveChar != other.primitiveChar){ return false; }
if (java.lang.Float.floatToIntBits(this.primitiveFloat) != java.lang.Float.floatToIntBits(other.primitiveFloat)) return false;
  return true;
}
@Generated
public AutoBeanBuilder clone(){
AutoBeanBuilder clone = new AutoBeanBuilder();
clone.primitiveDouble = this.primitiveDouble;
clone.integer = this.integer;
clone.primitiveBoolean = this.primitiveBoolean;
clone.date = this.date;
clone._double = this._double;
clone._long = this._long;
clone._float = this._float;
clone.primitiveLong = this.primitiveLong;
clone.character = this.character;
clone._short = this._short;
clone.primitiveInt = this.primitiveInt;
clone.primitiveByte = this.primitiveByte;
clone._byte = this._byte;
clone.string = this.string;
clone.primitiveShort = this.primitiveShort;
clone._boolean = this._boolean;
clone.primitiveChar = this.primitiveChar;
clone.primitiveFloat = this.primitiveFloat;
return clone;
}
}
