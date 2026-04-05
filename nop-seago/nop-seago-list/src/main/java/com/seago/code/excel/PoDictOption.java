package com.seago.code.excel;

import java.util.List;

/**
 * 字典项模型
 */
public class PoDictOption {
    private String value;
    private String label;

    public PoDictOption() {}

    public PoDictOption(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
