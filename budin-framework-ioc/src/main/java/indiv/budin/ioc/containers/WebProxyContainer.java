package indiv.budin.ioc.containers;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import indiv.budin.ioc.annotations.*;
import indiv.budin.ioc.constants.WebMessage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebProxyContainer extends HttpServlet {
    private AopProxyContainer aopProxyContainer;
    private AnnotationContainer annotationContainer;

    private Map<String, Method> methodHandler;
    private Map<String, Object> urlController;

    private String containerName;


    public WebProxyContainer() {
        aopProxyContainer = AopProxyContainer.getInstance();
        annotationContainer = (AnnotationContainer) AnnotationContainer.getInstance();
        methodHandler = new ConcurrentHashMap<>();
        urlController = new ConcurrentHashMap<>();
    }

    public static WebProxyContainer create() {
        return new WebProxyContainer();
    }

    public WebProxyContainer build() {
        init();
        return this;
    }

    public Object getBean(String s) {
        if (aopProxyContainer.containsBean(s)) return aopProxyContainer.getBean(s);
        if (annotationContainer.containsBean(s)) return annotationContainer.getBean(s);
        return null;
    }

    public void init() {
        Set<Class<?>> classesByAnnotation = annotationContainer.getClassesByAnnotation(IocController.class);
        //映射url-method
        for (Class<?> clazz : classesByAnnotation) {
            for (Method method : clazz.getDeclaredMethods()) {
                method.setAccessible(true);
                if (!method.isAnnotationPresent(IocRequestMapping.class)) continue;
                IocRequestMapping annotation = method.getAnnotation(IocRequestMapping.class);
                String url = annotation.url();
                Object obj = getBean(clazz.getName());
                methodHandler.put(url, method);
                urlController.put(url, obj);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setCharacterEncoding("UTF-8");
            Object res = doDispatcher(req, resp);
            if (res == null) return;
            resp.getWriter().write(res.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Object getRequestBody(HttpServletRequest req, Method method) {
        try (BufferedReader reader = req.getReader()) {
            StringBuffer stringBuffer = new StringBuffer();// 接收用户端传来的JSON字符串（body体里的数据）
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuffer.append(line);
            }
            JSONObject jsonObject = JSON.parseObject(stringBuffer.toString());
            Object obj=null;
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (Class<?> parameter : parameterTypes) {
                if (parameter.isAnnotationPresent(IocRequestBody.class)) {
                    obj = jsonToObject(parameter, jsonObject);
                    break;
                }
            }
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Object jsonToObject(Class<?> parameter, JSONObject jsonObject) {
        try {
            Object obj = Class.forName(parameter.getName()).newInstance();
            Field[] declaredFields = parameter.getDeclaredFields();
            for (Field field : declaredFields) {
                JSONObject o = null;
                if (jsonObject.containsKey(field.getName())) {
                    o = (JSONObject) jsonObject.get(field.getName());
                }
                field.set(obj, jsonToObject(field.getClass(), o));
            }
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Object doDispatcher(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        if (!methodHandler.containsKey(requestURI)) {
            response.setStatus(404);
            response.getWriter().write(WebMessage.URL_NOT_FOUND);
            return null;
        }
        Method method = methodHandler.get(requestURI);
        Object requestBody = getRequestBody(request, method);
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Map<String, String[]> parameterMap = request.getParameterMap();
        List<Object> parameterValues = new ArrayList<>();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                Class<?> aClass = parameterAnnotations[i][j].annotationType();
                if (aClass.equals(IocRequestParam.class)) {
                    IocRequestParam iocRequestParam = (IocRequestParam) parameterAnnotations[i][j];
                    if (!parameterMap.containsKey(iocRequestParam.value())) {
                        throw new RuntimeException();
                    }
                    parameterValues.add(parameterMap.get(iocRequestParam.value())[0]);
                    break;
                }
                if (aClass.isAnnotationPresent(IocRequestBody.class)){
                    parameterValues.add(requestBody);
                }
            }
        }
        Object[] objects = parameterValues.toArray();
        Object obj = urlController.get(requestURI);
        try {
            Object result = method.invoke(obj, objects);
            if (method.isAnnotationPresent(IocResponseBody.class)) {
                response.setContentType("application/json");
                return JSON.toJSON(result);
            }
            return result;
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }
}
