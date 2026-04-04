package com.seago.code.po._gen;

import io.nop.commons.collections.KeyedList; //NOPMD NOSONAR - suppressed UnusedImports - Used for List Prop
import io.nop.core.lang.json.IJsonHandler;
import com.seago.code.po.PropInfo;
import io.nop.commons.util.ClassHelper;



// tell cpd to start ignoring code - CPD-OFF
/**
 * generate from /nop/schema/seago/po.xdef <p>
 * 
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable",
    "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S101","java:S1128","java:S1161"})
public abstract class _PropInfo extends io.nop.core.resource.component.AbstractComponentModel {
    
    /**
     *  
     * xml name: comment
     * 
     */
    private java.lang.String _comment ;
    
    /**
     *  
     * xml name: dict
     * 
     */
    private java.lang.String _dict ;
    
    /**
     *  
     * xml name: excelHeader
     * 
     */
    private java.lang.String _excelHeader ;
    
    /**
     *  
     * xml name: excelWidth
     * 
     */
    private java.lang.Integer _excelWidth ;
    
    /**
     *  
     * xml name: hidden
     * 
     */
    private java.lang.Boolean _hidden ;
    
    /**
     *  
     * xml name: length
     * 
     */
    private java.lang.Integer _length ;
    
    /**
     *  
     * xml name: listProvider
     * 
     */
    private java.lang.String _listProvider ;
    
    /**
     *  
     * xml name: name
     * 
     */
    private java.lang.String _name ;
    
    /**
     *  
     * xml name: type
     * 
     */
    private java.lang.String _type ;
    
    /**
     * 
     * xml name: comment
     *  
     */
    
    public java.lang.String getComment(){
      return _comment;
    }

    
    public void setComment(java.lang.String value){
        checkAllowChange();
        
        this._comment = value;
           
    }

    
    /**
     * 
     * xml name: dict
     *  
     */
    
    public java.lang.String getDict(){
      return _dict;
    }

    
    public void setDict(java.lang.String value){
        checkAllowChange();
        
        this._dict = value;
           
    }

    
    /**
     * 
     * xml name: excelHeader
     *  
     */
    
    public java.lang.String getExcelHeader(){
      return _excelHeader;
    }

    
    public void setExcelHeader(java.lang.String value){
        checkAllowChange();
        
        this._excelHeader = value;
           
    }

    
    /**
     * 
     * xml name: excelWidth
     *  
     */
    
    public java.lang.Integer getExcelWidth(){
      return _excelWidth;
    }

    
    public void setExcelWidth(java.lang.Integer value){
        checkAllowChange();
        
        this._excelWidth = value;
           
    }

    
    /**
     * 
     * xml name: hidden
     *  
     */
    
    public java.lang.Boolean getHidden(){
      return _hidden;
    }

    
    public void setHidden(java.lang.Boolean value){
        checkAllowChange();
        
        this._hidden = value;
           
    }

    
    /**
     * 
     * xml name: length
     *  
     */
    
    public java.lang.Integer getLength(){
      return _length;
    }

    
    public void setLength(java.lang.Integer value){
        checkAllowChange();
        
        this._length = value;
           
    }

    
    /**
     * 
     * xml name: listProvider
     *  
     */
    
    public java.lang.String getListProvider(){
      return _listProvider;
    }

    
    public void setListProvider(java.lang.String value){
        checkAllowChange();
        
        this._listProvider = value;
           
    }

    
    /**
     * 
     * xml name: name
     *  
     */
    
    public java.lang.String getName(){
      return _name;
    }

    
    public void setName(java.lang.String value){
        checkAllowChange();
        
        this._name = value;
           
    }

    
    /**
     * 
     * xml name: type
     *  
     */
    
    public java.lang.String getType(){
      return _type;
    }

    
    public void setType(java.lang.String value){
        checkAllowChange();
        
        this._type = value;
           
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
        
        out.putNotNull("comment",this.getComment());
        out.putNotNull("dict",this.getDict());
        out.putNotNull("excelHeader",this.getExcelHeader());
        out.putNotNull("excelWidth",this.getExcelWidth());
        out.putNotNull("hidden",this.getHidden());
        out.putNotNull("length",this.getLength());
        out.putNotNull("listProvider",this.getListProvider());
        out.putNotNull("name",this.getName());
        out.putNotNull("type",this.getType());
    }

    public PropInfo cloneInstance(){
        PropInfo instance = newInstance();
        this.copyTo(instance);
        return instance;
    }

    protected void copyTo(PropInfo instance){
        super.copyTo(instance);
        
        instance.setComment(this.getComment());
        instance.setDict(this.getDict());
        instance.setExcelHeader(this.getExcelHeader());
        instance.setExcelWidth(this.getExcelWidth());
        instance.setHidden(this.getHidden());
        instance.setLength(this.getLength());
        instance.setListProvider(this.getListProvider());
        instance.setName(this.getName());
        instance.setType(this.getType());
    }

    protected PropInfo newInstance(){
        return (PropInfo) ClassHelper.newInstance(getClass());
    }
}
 // resume CPD analysis - CPD-ON
