package com.test.myspring.controller;

import com.test.myspring.annonation.Autowired1;
import com.test.myspring.annonation.Controller1;
import com.test.myspring.annonation.RequestMapping1;
import com.test.myspring.annonation.RequestParam1;
import com.test.myspring.service.TestService;

import java.lang.reflect.Method;

/**
 * @author wangsheng
 * @version V1.0
 * @ClassName: TestController
 * @Description: TODO
 * @Date 2021/4/14 15:02
 */
@Controller1
@RequestMapping1("/test")
public class TestController {

    @Autowired1
    private TestService testService;

    @RequestMapping1("/operate")
    public String operate(@RequestParam1("name") String name){
        return testService.operate(name);
    }

    public String test(String name, String address){
        return name+ " "+address;
    }


    public static void main(String[] args) throws Exception{
        Method method = TestController.class.getDeclaredMethod("test", String.class, String.class);
        Object ret = method.invoke(TestController.class.newInstance(), new String[]{"hello world", "hubeisheng"});
        System.out.println();





    }
}
