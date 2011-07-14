/**
 * Copyright (c)2010-2011 Enterprise Website Content Management System(EWCMS), All rights reserved.
 * EWCMS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * http://www.ewcms.com
 */

package com.ewcms.publication.freemarker.directive.article;

import java.io.IOException;
import java.util.Map;

import com.ewcms.publication.freemarker.FreemarkerUtil;
import com.ewcms.publication.freemarker.directive.ArticleDirective;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * 文章属性标签
 *
 * @deprecated
 * @author wangwei
 */
public abstract class ArticlePropertyDirective extends ArticleDirective {
    
    
    @SuppressWarnings("rawtypes")
    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        
        FreemarkerUtil.setVariable(env, nameParam, getPropertyName());
        super.execute(env, params, loopVars, body);
        FreemarkerUtil.removeVariable(env, nameParam);
    }
    
    /**
     * 显示文章的属性
     * 
     * @return
     */
    protected abstract String getPropertyName();
}