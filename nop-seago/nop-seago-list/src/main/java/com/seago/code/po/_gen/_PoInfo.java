package com.seago.code.po._gen;

import io.nop.commons.collections.KeyedList; //NOPMD NOSONAR - suppressed UnusedImports - Used for List Prop
import io.nop.core.lang.json.IJsonHandler;
import com.seago.code.po.PoInfo;
import io.nop.commons.util.ClassHelper;



// tell cpd to start ignoring code - CPD-OFF
/**
 * generate from /nop/schema/seago/po.xdef <p>
 * 
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable",
    "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S101","java:S1128","java:S1161"})
public abstract class _PoInfo extends io.nop.core.resource.component.AbstractComponentModel {
    
    /**
     *  
     * xml name: abstract
     * 
     */
    private boolean _abstract  = false;
    
    /**
     *  
     * xml name: attr
     * 
     */
    private java.lang.String _attr ;
    
    /**
     *  
     * xml name: attrView
     * 
     */
    private java.lang.String _attrView ;
    
    /**
     *  
     * xml name: branch
     * 
     */
    private java.lang.String _branch ;
    
    /**
     *  
     * xml name: comment
     * 
     */
    private java.lang.String _comment ;
    
    /**
     *  
     * xml name: extends
     * 
     */
    private java.lang.String _extends ;
    
    /**
     *  
     * xml name: indexes
     * 
     */
    private java.lang.String _indexes ;
    
    /**
     *  
     * xml name: iteration
     * 
     */
    private java.lang.String _iteration ;
    
    /**
     *  
     * xml name: master
     * 
     */
    private java.lang.String _master ;
    
    /**
     *  
     * xml name: name
     * 
     */
    private java.lang.String _name ;
    
    /**
     *  
     * xml name: props
     * 
     */
    private java.util.List<com.seago.code.po.PropInfo> _props = java.util.Collections.emptyList();
    
    /**
     *  
     * xml name: tableName
     * 
     */
    private java.lang.String _tableName ;
    
    /**
     * 
     * xml name: abstract
     *  
     */
    
    public boolean isAbstract(){
      return _abstract;
    }

    
    public void setAbstract(boolean value){
        checkAllowChange();
        
        this._abstract = value;
           
    }

    
    /**
     * 
     * xml name: attr
     *  
     */
    
    public java.lang.String getAttr(){
      return _attr;
    }

    
    public void setAttr(java.lang.String value){
        checkAllowChange();
        
        this._attr = value;
           
    }

    
    /**
     * 
     * xml name: attrView
     *  
     */
    
    public java.lang.String getAttrView(){
      return _attrView;
    }

    
    public void setAttrView(java.lang.String value){
        checkAllowChange();
        
        this._attrView = value;
           
    }

    
    /**
     * 
     * xml name: branch
     *  
     */
    
    public java.lang.String getBranch(){
      return _branch;
    }

    
    public void setBranch(java.lang.String value){
        checkAllowChange();
        
        this._branch = value;
           
    }

    
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
     * xml name: extends
     *  
     */
    
    public java.lang.String getExtends(){
      return _extends;
    }

    
    public void setExtends(java.lang.String value){
        checkAllowChange();
        
        this._extends = value;
           
    }

    
    /**
     * 
     * xml name: indexes
     *  
     */
    
    public java.lang.String getIndexes(){
      return _indexes;
    }

    
    public void setIndexes(java.lang.String value){
        checkAllowChange();
        
        this._indexes = value;
           
    }

    
    /**
     * 
     * xml name: iteration
     *  
     */
    
    public java.lang.String getIteration(){
      return _iteration;
    }

    
    public void setIteration(java.lang.String value){
        checkAllowChange();
        
        this._iteration = value;
           
    }

    
    /**
     * 
     * xml name: master
     *  
     */
    
    public java.lang.String getMaster(){
      return _master;
    }

    
    public void setMaster(java.lang.String value){
        checkAllowChange();
        
        this._master = value;
           
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
     * xml name: props
     *  
     */
    
    public java.util.List<com.seago.code.po.PropInfo> getProps(){
      return _props;
    }

    
    public void setProps(java.util.List<com.seago.code.po.PropInfo> value){
        checkAllowChange();
        
        this._props = value;
           
    }

    
    /**
     * 
     * xml name: tableName
     *  
     */
    
    public java.lang.String getTableName(){
      return _tableName;
    }

    
    public void setTableName(java.lang.String value){
        checkAllowChange();
        
        this._tableName = value;
           
    }

    

    @Override
    public void freeze(boolean cascade){
        if(frozen()) return;
        super.freeze(cascade);

        if(cascade){ //NOPMD - suppressed EmptyControlStatement - Auto Gen Code
        
           this._props = io.nop.api.core.util.FreezeHelper.deepFreeze(this._props);
            
        }
    }

    @Override
    protected void outputJson(IJsonHandler out){
        super.outputJson(out);
        
        out.putNotNull("abstract",this.isAbstract());
        out.putNotNull("attr",this.getAttr());
        out.putNotNull("attrView",this.getAttrView());
        out.putNotNull("branch",this.getBranch());
        out.putNotNull("comment",this.getComment());
        out.putNotNull("extends",this.getExtends());
        out.putNotNull("indexes",this.getIndexes());
        out.putNotNull("iteration",this.getIteration());
        out.putNotNull("master",this.getMaster());
        out.putNotNull("name",this.getName());
        out.putNotNull("props",this.getProps());
        out.putNotNull("tableName",this.getTableName());
    }

    public PoInfo cloneInstance(){
        PoInfo instance = newInstance();
        this.copyTo(instance);
        return instance;
    }

    protected void copyTo(PoInfo instance){
        super.copyTo(instance);
        
        instance.setAbstract(this.isAbstract());
        instance.setAttr(this.getAttr());
        instance.setAttrView(this.getAttrView());
        instance.setBranch(this.getBranch());
        instance.setComment(this.getComment());
        instance.setExtends(this.getExtends());
        instance.setIndexes(this.getIndexes());
        instance.setIteration(this.getIteration());
        instance.setMaster(this.getMaster());
        instance.setName(this.getName());
        instance.setProps(this.getProps());
        instance.setTableName(this.getTableName());
    }

    protected PoInfo newInstance(){
        return (PoInfo) ClassHelper.newInstance(getClass());
    }
}
 // resume CPD analysis - CPD-ON
