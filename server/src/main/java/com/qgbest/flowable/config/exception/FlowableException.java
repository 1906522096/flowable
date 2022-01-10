package com.qgbest.flowable.config.exception;


import com.qgbest.tools.microservice.exception.BaseSysException;

/**异常定义
 * @author hjt
 * @date 2019/11/14
 * @description 异常定义
 */
public class FlowableException extends BaseSysException {
    private String message = "参数不能为空！";

    public FlowableException(String msg) {
        this.addDebugInfo("info", msg+this.message);
        this.message = msg + this.message;
    }

    @Override
    public String getFriendlyMessage() {
       return this.message;
    }
}
