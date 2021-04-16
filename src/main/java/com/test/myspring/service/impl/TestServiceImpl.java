package com.test.myspring.service.impl;

import com.test.myspring.annonation.Service1;
import com.test.myspring.service.TestService;

/**
 * @author wangsheng
 * @version V1.0
 * @ClassName: TestServiceImpl
 * @Description: TODO
 * @Date 2021/4/14 15:01
 */
@Service1
public class TestServiceImpl implements TestService {
    public String operate(String name) {
        return name + "hello world";
    }
}
