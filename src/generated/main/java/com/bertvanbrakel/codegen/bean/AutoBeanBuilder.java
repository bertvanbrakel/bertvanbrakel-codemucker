package com.bertvanbrakel.codegen.bean;
import org.codemucker.jpattern.IsGenerated;
import org.codemucker.jpattern.Pattern;
import org.codemucker.jpattern.PatternType;
@IsGenerated
/** generated by org.codemucker.jmutate.bean.BeanBuilderWriter */
public class AutoBeanBuilder{
@IsGenerated
private java.util.Date date;
@IsGenerated
private short primitiveShort;
@IsGenerated
private java.lang.String string;
@IsGenerated
private java.lang.Byte _byte;
@IsGenerated
private java.lang.Double _double;
@IsGenerated
private char primitiveChar;
@IsGenerated
private java.lang.Integer integer;
@IsGenerated
private java.lang.Float _float;
@IsGenerated
private java.lang.Long _long;
@IsGenerated
private java.lang.Character character;
@IsGenerated
private java.lang.Boolean _boolean;
@IsGenerated
private int primitiveInt;
@IsGenerated
private float primitiveFloat;
@IsGenerated
private java.lang.Short _short;
@IsGenerated
private boolean primitiveBoolean;
@IsGenerated
private byte primitiveByte;
@IsGenerated
private long primitiveLong;
@IsGenerated
private double primitiveDouble;
@IsGenerated
public AutoBeanBuilder date(java.util.Date date){ this.date = date; return this;}
@IsGenerated
public AutoBeanBuilder primitiveShort(short primitiveShort){ this.primitiveShort = primitiveShort; return this;}
@IsGenerated
public AutoBeanBuilder string(java.lang.String string){ this.string = string; return this;}
@IsGenerated
public AutoBeanBuilder bite(java.lang.Byte _byte){ this._byte = _byte; return this;}
@IsGenerated
public AutoBeanBuilder Double(java.lang.Double _double){ this._double = _double; return this;}
@IsGenerated
public AutoBeanBuilder primitiveChar(char primitiveChar){ this.primitiveChar = primitiveChar; return this;}
@IsGenerated
public AutoBeanBuilder integer(java.lang.Integer integer){ this.integer = integer; return this;}
@IsGenerated
public AutoBeanBuilder Float(java.lang.Float _float){ this._float = _float; return this;}
@IsGenerated
public AutoBeanBuilder Long(java.lang.Long _long){ this._long = _long; return this;}
@IsGenerated
public AutoBeanBuilder character(java.lang.Character character){ this.character = character; return this;}
@IsGenerated
public AutoBeanBuilder Boolean(java.lang.Boolean _boolean){ this._boolean = _boolean; return this;}
@IsGenerated
public AutoBeanBuilder primitiveInt(int primitiveInt){ this.primitiveInt = primitiveInt; return this;}
@IsGenerated
public AutoBeanBuilder primitiveFloat(float primitiveFloat){ this.primitiveFloat = primitiveFloat; return this;}
@IsGenerated
public AutoBeanBuilder Short(java.lang.Short _short){ this._short = _short; return this;}
@IsGenerated
public AutoBeanBuilder primitiveBoolean(boolean primitiveBoolean){ this.primitiveBoolean = primitiveBoolean; return this;}
@IsGenerated
public AutoBeanBuilder primitiveByte(byte primitiveByte){ this.primitiveByte = primitiveByte; return this;}
@IsGenerated
public AutoBeanBuilder primitiveLong(long primitiveLong){ this.primitiveLong = primitiveLong; return this;}
@IsGenerated
public AutoBeanBuilder primitiveDouble(double primitiveDouble){ this.primitiveDouble = primitiveDouble; return this;}
@IsGenerated
@Pattern(type=PatternType.BuilderCreate)
public com.bertvanbrakel.codegen.bean.AutoBean create(){ 
   com.bertvanbrakel.codegen.bean.AutoBean bean = new com.bertvanbrakel.codegen.bean.AutoBean();
   bean.setDate(this.date);
   bean.setPrimitiveShort(this.primitiveShort);
   bean.setString(this.string);
   bean.setByte(this._byte);
   bean.setDouble(this._double);
   bean.setPrimitiveChar(this.primitiveChar);
   bean.setInteger(this.integer);
   bean.setFloat(this._float);
   bean.setLong(this._long);
   bean.setCharacter(this.character);
   bean.setBoolean(this._boolean);
   bean.setPrimitiveInt(this.primitiveInt);
   bean.setPrimitiveFloat(this.primitiveFloat);
   bean.setShort(this._short);
   bean.setPrimitiveBoolean(this.primitiveBoolean);
   bean.setPrimitiveByte(this.primitiveByte);
   bean.setPrimitiveLong(this.primitiveLong);
   bean.setPrimitiveDouble(this.primitiveDouble);
   return bean;
}
@IsGenerated
public boolean equals(Object obj){
if( this == obj ) return true;
if( obj == null ) return false;
if( getClass() != obj.getClass() ) return false;
AutoBeanBuilder other = (AutoBeanBuilder)obj;
if(this.date == null){
   if( other.date != null ){ return false; }
} else if ( !this.date.equals(other.date ) ){ return false; }
if(primitiveShort != other.primitiveShort){ return false; }
if(this.string == null){
   if( other.string != null ){ return false; }
} else if ( !this.string.equals(other.string ) ){ return false; }
if(this._byte == null){
   if( other._byte != null ){ return false; }
} else if ( !this._byte.equals(other._byte ) ){ return false; }
if(this._double == null){
   if( other._double != null ){ return false; }
} else if ( !this._double.equals(other._double ) ){ return false; }
if(primitiveChar != other.primitiveChar){ return false; }
if(this.integer == null){
   if( other.integer != null ){ return false; }
} else if ( !this.integer.equals(other.integer ) ){ return false; }
if(this._float == null){
   if( other._float != null ){ return false; }
} else if ( !this._float.equals(other._float ) ){ return false; }
if(this._long == null){
   if( other._long != null ){ return false; }
} else if ( !this._long.equals(other._long ) ){ return false; }
if(this.character == null){
   if( other.character != null ){ return false; }
} else if ( !this.character.equals(other.character ) ){ return false; }
if(this._boolean == null){
   if( other._boolean != null ){ return false; }
} else if ( !this._boolean.equals(other._boolean ) ){ return false; }
if(primitiveInt != other.primitiveInt){ return false; }
if (java.lang.Float.floatToIntBits(this.primitiveFloat) != java.lang.Float.floatToIntBits(other.primitiveFloat)) return false;
if(this._short == null){
   if( other._short != null ){ return false; }
} else if ( !this._short.equals(other._short ) ){ return false; }
if(primitiveBoolean != other.primitiveBoolean){ return false; }
if(primitiveByte != other.primitiveByte){ return false; }
if(primitiveLong != other.primitiveLong){ return false; }
if (java.lang.Double.doubleToLongBits(this.primitiveDouble) != java.lang.Double.doubleToLongBits(other.primitiveDouble)) return false;
  return true;
}
@IsGenerated
public AutoBeanBuilder clone(){
AutoBeanBuilder clone = new AutoBeanBuilder();
clone.date = this.date;
clone.primitiveShort = this.primitiveShort;
clone.string = this.string;
clone._byte = this._byte;
clone._double = this._double;
clone.primitiveChar = this.primitiveChar;
clone.integer = this.integer;
clone._float = this._float;
clone._long = this._long;
clone.character = this.character;
clone._boolean = this._boolean;
clone.primitiveInt = this.primitiveInt;
clone.primitiveFloat = this.primitiveFloat;
clone._short = this._short;
clone.primitiveBoolean = this.primitiveBoolean;
clone.primitiveByte = this.primitiveByte;
clone.primitiveLong = this.primitiveLong;
clone.primitiveDouble = this.primitiveDouble;
return clone;
}
}
