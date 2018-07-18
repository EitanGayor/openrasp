package com.baidu.openrasp.hook.server.catalina;

import com.baidu.openrasp.HookHandler;
import com.baidu.openrasp.config.Config;
import com.baidu.openrasp.hook.AbstractClassHook;
import com.baidu.openrasp.plugin.checker.CheckParameter;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 　　* @Description: catalina 获取输出buffer的hook点
 * 　　* @author anyang
 * 　　* @date 2018/6/11 14:53
 *
 */
public class CatalinaOutputBufferFlushHook extends AbstractClassHook {

    @Override
    public String getType() {
        return "xss";
    }

    /**
     * (none-javadoc)
     *
     * @see com.baidu.openrasp.hook.AbstractClassHook#isClassMatched(String)
     */
    @Override
    public boolean isClassMatched(String className) {
        return "org/apache/tomcat/util/buf/CharChunk".equals(className);
    }

    @Override
    protected void hookMethod(CtClass ctClass) throws NotFoundException, CannotCompileException {
        String src = getInvokeStaticSrc(CatalinaOutputBufferFlushHook.class, "getOutputBuffer", "buff,start,end", char[].class, int.class, int.class);
        insertBefore(ctClass, "flushBuffer", "()V", src);
    }


    public static void getOutputBuffer(char[] buffer, int start, int end) {

        boolean isEnableXssHook=HookHandler.isEnableXssHook();
        if (isEnableXssHook){
            HookHandler.disableXssHook();
            int parameterLength = Integer.valueOf(Config.getConfig().getXssParameterLength());
            String regex = Config.getConfig().getXssRegex();
            try {
                char[]temp=new char[end-start+1];
                System.arraycopy(buffer,start,temp,0,end-start+1);
                byte[]bytes=new String(temp).getBytes();
                String content=new String(bytes);
                if (!content.contains("X-Protected-By")) {
                    HashMap<String, Object> params = null;
                    Map<String, String[]> parameterMap = HookHandler.requestCache.get().getParameterMap();
                    int exceedLengthCount = 0;
                    List<String> paramList = new ArrayList<String>();
                    for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {

                        for (String value : entry.getValue()) {
                            Pattern pattern = Pattern.compile(regex);
                            Matcher matcher = pattern.matcher(value);
                            boolean isMatch = matcher.find();
                            if (value.length() >= parameterLength && isMatch) {
                                exceedLengthCount++;
                                paramList.add(value);
                            }
                        }
                    }
                    params = new HashMap<String, Object>(3);
                    params.put("exceed_count", exceedLengthCount);
                    params.put("html_body", content);
                    params.put("param_list", paramList);
                    HookHandler.doCheck(CheckParameter.Type.XSS, params);

                }

            } catch (Exception e) {

                e.printStackTrace();
            }

        }

    }
}
