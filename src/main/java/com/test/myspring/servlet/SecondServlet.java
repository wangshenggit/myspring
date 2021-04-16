package com.test.myspring.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author wangsheng
 * @version V1.0
 * @ClassName: SecondServlet
 * @Description: TODO
 * @Date 2021/4/14 15:59
 */
//这里不需要添加webServlet注解
public class SecondServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().append("SecondServlet");
    }
}

