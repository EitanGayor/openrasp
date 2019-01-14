/*
 * Copyright 2017-2018 Baidu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.openrasp.hook.server.weblogic;

import com.baidu.openrasp.HookHandler;
import com.baidu.openrasp.hook.server.ServerXssHook;
import com.baidu.openrasp.plugin.checker.CheckParameter;
import com.baidu.openrasp.tool.Reflection;
import com.baidu.openrasp.tool.annotation.HookAnnotation;
import com.baidu.openrasp.tool.hook.ServerXss;
import com.baidu.openrasp.tool.model.ApplicationModel;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * @description: weblogic的xss检测hook点
 * @author: anyang
 * @create: 2018/09/05 15:06
 */
@HookAnnotation
public class WeblogicXssHook extends ServerXssHook {
    @Override
    public boolean isClassMatched(String className) {
        return "weblogic/servlet/internal/CharsetChunkOutput".equals(className);
    }

    @Override
    protected void hookMethod(CtClass ctClass) throws IOException, CannotCompileException, NotFoundException {
        String src = getInvokeStaticSrc(WeblogicXssHook.class, "getWeblogicOutputBuffer", "$0", Object.class);
        insertBefore(ctClass, "flush", "()V", src);
    }

    public static void getWeblogicOutputBuffer(Object object){
        try {
            ByteBuffer byteBuffer = (ByteBuffer) Reflection.getField(object,"buf");
            String content = new String(byteBuffer.array());
            if (HookHandler.requestCache.get() != null) {
                HashMap<String, Object> params = ServerXss.generateXssParameters(content);
                HookHandler.doCheck(CheckParameter.Type.XSS, params);
            }
        } catch (Exception e) {
            HookHandler.LOGGER.warn(ApplicationModel.getServerName() + " xss detectde failed: ", e);
        }
    }
}