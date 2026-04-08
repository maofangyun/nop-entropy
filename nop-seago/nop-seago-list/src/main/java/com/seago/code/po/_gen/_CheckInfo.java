package com.seago.code.po._gen;

import io.nop.commons.collections.KeyedList; //NOPMD NOSONAR - suppressed UnusedImports - Used for List Prop
import io.nop.core.lang.json.IJsonHandler;
import com.seago.code.po.CheckInfo;
import io.nop.commons.util.ClassHelper;



// tell cpd to start ignoring code - CPD-OFF
/**
 * generate from /nop/schema/seago/po.xdef <p>
 * 
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable",
    "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S101","java:S1128","java:S1161"})
public abstract class _CheckInfo extends io.nop.core.resource.component.AbstractComponentModel {
    
    /**
     *  
     * xml name: message
     * 
     */
    private java.lang.String _message ;
    
    /**
     *  
     * xml name: 
     * 
     */
    private java.lang.String _tagName ;
    
    /**
     * 
     * xml name: message
     *  
     */
    
    public java.lang.String getMessage(){
      return _message;
    }

    
    public void setMessage(java.lang.String value){
        checkAllowChange();
        
        this._message = value;
           
    }

    
    /**
     * 
     * xml name: 
     *  
     */
    
    public java.lang.String getTagName(){
      return _tagName;
    }

    
    public void setTagName(java.lang.String value){
        checkAllowChange();
        
        this._tagName = value;
           
    }

    

    @Override
    public void freeze(boolean cascade){
        if(frozen()) return;
        super.freeze(cascade);

        if(cascade){ //NOPMD - suppressed EmptyControlStatement - Auto Gen Code
        
        }
    }

    @Override
    protected void outputJson(IJsonHandler out){
        super.outputJson(out);
        
        out.putNotNull("message",this.getMessage());
        out.putNotNull("tagName",this.getTagName());
    }

    public CheckInfo cloneInstance(){
        CheckInfo instance = newInstance();
        this.copyTo(instance);
        return instance;
    }

    protected void copyTo(CheckInfo instance){
        super.copyTo(instance);
        
        instance.setMessage(this.getMessage());
        instance.setTagName(this.getTagName());
    }

    protected CheckInfo newInstance(){
        return (CheckInfo) ClassHelper.newInstance(getClass());
    }
}
 // resume CPD analysis - CPD-ON
