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
     * xml name: mandatory
     * 
     */
    private java.lang.Boolean _mandatory ;
    
    /**
     *  
     * xml name: max
     * 
     */
    private java.lang.Double _max ;
    
    /**
     *  
     * xml name: maxLength
     * 
     */
    private java.lang.Integer _maxLength ;
    
    /**
     *  
     * xml name: min
     * 
     */
    private java.lang.Double _min ;
    
    /**
     *  
     * xml name: minLength
     * 
     */
    private java.lang.Integer _minLength ;
    
    /**
     *  
     * xml name: name
     * 
     */
    private java.lang.String _name ;
    
    /**
     *  
     * xml name: pattern
     * 
     */
    private java.lang.String _pattern ;
    
    /**
     *  
     * xml name: precision
     * 
     */
    private java.lang.Integer _precision ;
    
    /**
     *  
     * xml name: scale
     * 
     */
    private java.lang.Integer _scale ;
    
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
     * xml name: mandatory
     *  
     */
    
    public java.lang.Boolean getMandatory(){
      return _mandatory;
    }

    
    public void setMandatory(java.lang.Boolean value){
        checkAllowChange();
        
        this._mandatory = value;
           
    }

    
    /**
     * 
     * xml name: max
     *  
     */
    
    public java.lang.Double getMax(){
      return _max;
    }

    
    public void setMax(java.lang.Double value){
        checkAllowChange();
        
        this._max = value;
           
    }

    
    /**
     * 
     * xml name: maxLength
     *  
     */
    
    public java.lang.Integer getMaxLength(){
      return _maxLength;
    }

    
    public void setMaxLength(java.lang.Integer value){
        checkAllowChange();
        
        this._maxLength = value;
           
    }

    
    /**
     * 
     * xml name: min
     *  
     */
    
    public java.lang.Double getMin(){
      return _min;
    }

    
    public void setMin(java.lang.Double value){
        checkAllowChange();
        
        this._min = value;
           
    }

    
    /**
     * 
     * xml name: minLength
     *  
     */
    
    public java.lang.Integer getMinLength(){
      return _minLength;
    }

    
    public void setMinLength(java.lang.Integer value){
        checkAllowChange();
        
        this._minLength = value;
           
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
     * xml name: pattern
     *  
     */
    
    public java.lang.String getPattern(){
      return _pattern;
    }

    
    public void setPattern(java.lang.String value){
        checkAllowChange();
        
        this._pattern = value;
           
    }

    
    /**
     * 
     * xml name: precision
     *  
     */
    
    public java.lang.Integer getPrecision(){
      return _precision;
    }

    
    public void setPrecision(java.lang.Integer value){
        checkAllowChange();
        
        this._precision = value;
           
    }

    
    /**
     * 
     * xml name: scale
     *  
     */
    
    public java.lang.Integer getScale(){
      return _scale;
    }

    
    public void setScale(java.lang.Integer value){
        checkAllowChange();
        
        this._scale = value;
           
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
        out.putNotNull("mandatory",this.getMandatory());
        out.putNotNull("max",this.getMax());
        out.putNotNull("maxLength",this.getMaxLength());
        out.putNotNull("min",this.getMin());
        out.putNotNull("minLength",this.getMinLength());
        out.putNotNull("name",this.getName());
        out.putNotNull("pattern",this.getPattern());
        out.putNotNull("precision",this.getPrecision());
        out.putNotNull("scale",this.getScale());
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
        instance.setMandatory(this.getMandatory());
        instance.setMax(this.getMax());
        instance.setMaxLength(this.getMaxLength());
        instance.setMin(this.getMin());
        instance.setMinLength(this.getMinLength());
        instance.setName(this.getName());
        instance.setPattern(this.getPattern());
        instance.setPrecision(this.getPrecision());
        instance.setScale(this.getScale());
        instance.setType(this.getType());
    }

    protected PropInfo newInstance(){
        return (PropInfo) ClassHelper.newInstance(getClass());
    }
}
 // resume CPD analysis - CPD-ON
