package io.nop.pdf.core;

import io.nop.core.lang.eval.IEvalScope;
import io.nop.core.lang.xml.XNode;

/**
 * PDF页面事件初始化接口
 */
public interface IOpenPdfPageEventInit {
    void init(OpenPdfDocumentGenerator generator, XNode pdfConfig, IEvalScope evalScope);
}
