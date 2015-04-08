package org.codemucker.jmutate.generate.bean;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codemucker.jmutate.JMutateException;
import org.codemucker.jmutate.ast.JType;
import org.codemucker.jmutate.generate.GeneratorConfig;
import org.codemucker.jmutate.generate.builder.BuilderModel;
import org.codemucker.jpattern.generate.GenerateBean;

public class BeanModel {   

	private static final Logger LOG = LogManager.getLogger(BeanModel.class);
    
	public final GenerateBeanOptions options;
    private final Map<String, BeanPropertyModel> properties = new LinkedHashMap<>();
    
    public BeanModel(JType pojoType,GenerateBean options) {
    	this.options = new GenerateBeanOptions(pojoType, options);
    }
    
    public BeanModel(JType pojoType,GeneratorConfig cfg) {
    	this.options = new GenerateBeanOptions(pojoType, cfg);
    }

    public boolean hasDirectFinalProperties(){
    	for(BeanPropertyModel p:properties.values()){
    		if(!p.isFromSuperClass() && p.isFinalField() && p.hasField()){
    			return true;
    		}
    	}
    	return false;
    }

	void addProperty(BeanPropertyModel property){
        if (hasNamedField(property.getPropertyName())) {
            throw new JMutateException("More than one property with the same name '%s' on %s", property.getPropertyName(), options.getType().getFullName());
        }
        if(LOG.isDebugEnabled()){
			LOG.debug("adding property '" + property.getPropertyName() + "', " + property);
		}
        properties.put(property.getPropertyName(), property);
    }
    
    boolean hasNamedField(String name){
        return properties.containsKey(name);
    }
    
    BeanPropertyModel getNamedField(String name){
        return properties.get(name);
    }
    
    Collection<BeanPropertyModel> getFields(){
        return properties.values();
    }

	Map<String, BeanPropertyModel> getProperties() {
		return properties;
	}
}