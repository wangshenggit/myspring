package com.test.myspring.servlet;

import com.test.myspring.annonation.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wangsheng
 * @version V1.0
 * @ClassName: DisPatcherServlet
 * @Description: TODO
 * @Date 2021/4/14 15:11
 */
public class DisPatcherServlet extends HttpServlet {

    Properties properties = new Properties();
    List<String> classPaths = new ArrayList<>();
    ConcurrentHashMap<String, Object> ioc = new ConcurrentHashMap<>();
    List<Handler> handleMapping = new ArrayList<>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatcher(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatcher(req, resp);
    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //设置请求返回格式
        resp.setContentType("text/html;charset=UTF-8");

        Handler handler = getHandler(req);
        if (handler == null) {
            resp.getWriter().write("404 NOT FOUND");
            return;
        }

        Method method = handler.getMethod();
        //参数类型数组
        Class<?>[] parameterTypes = method.getParameterTypes();
        //参数值数组
        Object[] paramValues = new Object[parameterTypes.length];

        Map<String, String[]> params = req.getParameterMap();
        //参数位置映射
        Map<String, Integer> paramIndexMapping = handler.getParamIndexMapping();

        for (Map.Entry<String, String[]> param : params.entrySet()) {
            String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "")
                    .replaceAll("\\s", ",");

            //判断当前方法参数索引的位置，是否包含参数名字
            if (!paramIndexMapping.containsKey(param.getKey())) {
                continue;
            }

            Integer index = paramIndexMapping.get(param.getKey());
            //转换参数类型
            paramValues[index] = convert(parameterTypes[index], value);
        }

        //传统请求
        if (paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
            int reqIndex = paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
        }
        if (paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
            int respIndex = paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = resp;
        }

        try{
            Object result  = method.invoke(handler.getInstance(), paramValues);
            if (result == null || result instanceof Void) {
                return;
            }
            resp.getWriter().write(result.toString());
        }catch (Exception e){

        }
    }

    /**
     * url传输过来的参数都是String类型的，由于HTTP基于字符串协议
     * 只需要把String转换为任意类型
     *
     * @param parameterType
     * @param value
     * @return
     */
    private Object convert(Class<?> parameterType, String value) {
        if (Integer.class == parameterType) {
            return Integer.valueOf(value);
        }
        return value;
    }


    @Override
    public void init(ServletConfig config) throws ServletException {
        //加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //扫描相关的类
        doScanner(properties.getProperty("scanPackage"));
        //初始化加载的类，并放入IOC容器
        doInstance();
        //完成依赖注入
        doAutoWired();
        //初始化handlerMapping
        doInitHandleMapping();


    }

    //加载配置文件
    private void doLoadConfig(String contextConfigLocation) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        String scanPackage = properties.getProperty("scanPackage");
        System.out.println(scanPackage);
    }

    //扫描相关的类
    private void doScanner(String scanPackage) {

        URL url = this.getClass().getResource("/" + scanPackage.replaceAll("\\.", "/"));

        File file = new File(url.getFile());

        for (File f : file.listFiles()) {
            //如果是目录，则继续扫描
            if (f.isDirectory()) {
                doScanner(scanPackage + "." + f.getName());
            } else {
                //如果不是目录，且不是class文件，则跳过
                if (!f.getName().endsWith(".class")) {
                    continue;
                }
                String classPath = scanPackage + "." + f.getName().replace(".class", "");

                classPaths.add(classPath);

            }
        }


    }


    private void doInstance() {
        if (CollectionUtils.isEmpty(classPaths)) {
            return;
        }

        try {
            for (String className : classPaths) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(Controller1.class)) {
                    Object instance = clazz.newInstance();
                    String instanceName = clazz.getSimpleName();
                    ioc.put(instanceName, instance);
                } else if (clazz.isAnnotationPresent(Service1.class)) {
                    Object instance = clazz.newInstance();
                    Class[] classes = clazz.getInterfaces();
                    String beanName = "";
                    if (classes.length == 0) {
                        beanName = clazz.getSimpleName();
                    } else {
                        beanName = classes[0].getSimpleName();
                    }
                    ioc.put(beanName, instance);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doAutoWired() {
        if (CollectionUtils.isEmpty(ioc)) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {

                //查看每个实体类的参数是否有被autowired修饰，如果没有就返回，有的话就开始注入容器实体类参数
                if (!field.isAnnotationPresent(Autowired1.class)) {
                    continue;
                }
                String beanName = field.getType().getSimpleName();
                field.setAccessible(true);
                try {
                    //有authowired的实体，参数注入
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doInitHandleMapping() {
        if (CollectionUtils.isEmpty(ioc)) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(Controller1.class)) {
                continue;
            }

            //一个controller的基本路径
            String baseUrl = "";
            if (clazz.isAnnotationPresent(RequestMapping1.class)) {
                RequestMapping1 requestMapping1 = clazz.getAnnotation(RequestMapping1.class);
                baseUrl = requestMapping1.value()[0];
            }

            Method[] methods = clazz.getDeclaredMethods();
            //获取每个方法的路径，方法和路径的映射
            for (Method method : methods) {
                if (!method.isAnnotationPresent(RequestMapping1.class)) {
                    continue;
                }

                RequestMapping1 methodRequestMapping1 = method.getAnnotation(RequestMapping1.class);

                String regex = baseUrl + methodRequestMapping1.value()[0];
                Pattern pattern = Pattern.compile(regex);

                Handler handler = new Handler(entry.getValue(), method, pattern);
                handleMapping.add(handler);
                System.out.println("mapping :" + regex + "," + method);
            }
        }
    }

    /**
     * 将类名首字母小写
     * 这里比较的是ASCII码
     * (char)小写字母 - (char)大写字母 = 32
     *
     * @param simpleName
     */
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * 记录controller中路径和方法的对应关系
     */
    static class Handler {
        //实例
        Object instance;
        //方法
        Method method;
        Pattern pattern;

        //参数顺序
        Map<String, Integer> paramIndexMapping;


        public Handler(Object instance, Method method, Pattern pattern) {
            this.instance = instance;
            this.method = method;
            this.pattern = pattern;
            paramIndexMapping = new HashMap<>();
            putParamIndexMapping(method);
        }


        public void putParamIndexMapping(Method method) {
            int index = 0;

            Class<?>[] parameterTypes = method.getParameterTypes();

            for (Class<?> parameterType : parameterTypes) {
                //提取request\response对象
                if (parameterType == HttpServletRequest.class || parameterType == HttpServletResponse.class) {
                    paramIndexMapping.put(parameterType.getName(), index++);
                }
            }


            Annotation[][] parameterAnnotations = method.getParameterAnnotations();

            for (Annotation[] annotations : parameterAnnotations) {
                for (Annotation annotation : annotations) {
                    if (annotation instanceof RequestParam1) {
                        String paramName = ((RequestParam1) annotation).value();
                        if (!StringUtils.isEmpty(paramName.trim())) {
                            paramIndexMapping.put(paramName, index++);
                        }
                    }
                }
            }
        }


        public Object getInstance() {
            return instance;
        }

        public void setInstance(Object instance) {
            this.instance = instance;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public void setPattern(Pattern pattern) {
            this.pattern = pattern;
        }

        public Map<String, Integer> getParamIndexMapping() {
            return paramIndexMapping;
        }

        public void setParamIndexMapping(Map<String, Integer> paramIndexMapping) {
            this.paramIndexMapping = paramIndexMapping;
        }
    }


    /**
     * 通过请求获取相应的实例和实例方法
     *
     * @param request
     * @return
     */
    private Handler getHandler(HttpServletRequest request) {
        if (CollectionUtils.isEmpty(handleMapping)) {
            return null;
        }
        //请求路径
        String requestUrl = request.getRequestURI();
        //上下文路径
        String contextPath = request.getContextPath();

        requestUrl = requestUrl.replace(contextPath, "").replaceAll("/+", "/");

        for (Handler handler : handleMapping) {
            try {
                Matcher matcher = handler.getPattern().matcher(requestUrl);
                if (!matcher.matches()) {
                    continue;
                }
                return handler;
            } catch (Exception e) {
                //匹配失败，异常直接上抛
                throw e;
            }
        }
        return null;
    }
}
