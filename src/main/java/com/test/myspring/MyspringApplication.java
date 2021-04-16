package com.test.myspring;


import com.test.myspring.servlet.DisPatcherServlet;
import com.test.myspring.servlet.SecondServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootApplication
//启动器启动时，扫描本目录以及子目录带有的webservlet注解的
@ServletComponentScan
public class MyspringApplication {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(MyspringApplication.class, args);
    }

    @Bean  //一定要加，不然这个方法不会运行
    public ServletRegistrationBean getServletRegistrationBean() {  //一定要返回ServletRegistrationBean
        ServletRegistrationBean bean = new ServletRegistrationBean(new DisPatcherServlet());     //放入自己的Servlet对象实例
        bean.addUrlMappings("/*");  //访问路径值
        bean.setLoadOnStartup(1);

        Map<String, String> initParameters = new HashMap<String, String>();
        initParameters.put("contextConfigLocation", "application.properties");
        bean.setInitParameters(initParameters);
        return bean;
    }
}

