package com.seago.code.po._gen;

import io.nop.commons.collections.KeyedList; //NOPMD NOSONAR - suppressed UnusedImports - Used for List Prop
import io.nop.core.lang.json.IJsonHandler;
import com.seago.code.po.PoConfig;
import io.nop.commons.util.ClassHelper;



// tell cpd to start ignoring code - CPD-OFF
/**
 * generate from /nop/schema/seago/po.xdef <p>
 * 
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable",
    "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S101","java:S1128","java:S1161"})
public abstract class _PoConfig extends io.nop.core.resource.component.AbstractComponentModel {
    
    /**
     *  
     * xml name: packageName
     * 
     */
    private java.lang.String _packageName ;
    
    /**
     *  
     * xml name: pos
     * 
     */
    private java.util.List<com.seago.code.po.PoInfo> _pos = java.util.Collections.emptyList();
    
    /**
     * 
     * xml name: packageName
     *  
     */
    
    public java.lang.String getPackageName(){
      return _packageName;
    }

    
    public void setPackageName(java.lang.String value){
        checkAllowChange();
        
        this._packageName = value;
           
    }

    
    /**
     * 
     * xml name: pos
     *  
     */
    
    public java.util.List<com.seago.code.po.PoInfo> getPos(){
      return _pos;
    }

    
    public void setPos(java.util.List<com.seago.code.po.PoInfo> value){
        checkAllowChange();
        
        this._pos = value;
           
    }

    

    @Override
    public void freeze(boolean cascade){
        if(frozen()) return;
        super.freeze(cascade);

        if(cascade){ //NOPMD - suppressed EmptyControlStatement - Auto Gen Code
        
           this._pos = io.nop.api.core.util.FreezeHelper.deepFreeze(this._pos);
            
        }
    }

    @Override
    protected void outputJson(IJsonHandler out){
        super.outputJson(out);
        
        out.putNotNull("packageName",this.getPackageName());
        out.putNotNull("pos",this.getPos());
    }

    public PoConfig cloneInstance(){
        PoConfig instance = newInstance();
        this.copyTo(instance);
        return instance;
    }

    protected void copyTo(PoConfig instance){
        super.copyTo(instance);
        
        instance.setPackageName(this.getPackageName());
        instance.setPos(this.getPos());
    }

    protected PoConfig newInstance(){
        return (PoConfig) ClassHelper.newInstance(getClass());
    }
}
 // resume CPD analysis - CPD-ON
