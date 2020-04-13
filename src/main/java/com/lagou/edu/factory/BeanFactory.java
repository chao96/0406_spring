package com.lagou.edu.factory;

import com.lagou.edu.annotation.Autowired;
import com.lagou.edu.annotation.Bean;
import com.lagou.edu.annotation.Repository;
import com.lagou.edu.annotation.Service;
import com.lagou.edu.utils.PackageScaner;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xuchao
 *
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */
    private static Map<String,Object> map = new HashMap<>();  // 存储对象


    static {
        try {
            List<Class<?>> classes = PackageScaner.getClzFromPkg("com.lagou.edu");
            for (Class<?> aClass : classes) {
                if (aClass.isAnnotation()) continue;

                String beanId = null;
                if (aClass.isAnnotationPresent(Service.class)) {
                    Service serviceAnnotation = aClass.getAnnotation(Service.class);
                    beanId = serviceAnnotation.value();
                }
                if (aClass.isAnnotationPresent(Repository.class)) {
                    Repository repositoryAnnotation = aClass.getAnnotation(Repository.class);
                    beanId = repositoryAnnotation.value();
                }
                if (aClass.isAnnotationPresent(Bean.class)) {
                    Bean beanAnnotation = aClass.getAnnotation(Bean.class);
                    beanId = beanAnnotation.value();
                }
                if (beanId != null) {
                    // 默认用类名的小写值代替
                    if (beanId.equals("")){
                        beanId = aClass.getSimpleName().toLowerCase();
                    }

                    Object obj = aClass.newInstance();

                    map.put(beanId, obj);
                }
            }

            // 实现成员变量的依赖注入
            dependencyInjection();

            // 实现bean的增强，添加事务控制
            transactionEnhance();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 实现成员变量的依赖注入
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private static void dependencyInjection() throws InvocationTargetException, IllegalAccessException {

        for (Map.Entry<String, Object> entry : map.entrySet()) {

            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (declaredField.isAnnotationPresent(Autowired.class)) {
                    // 按id注入
                    Autowired autowiredAnnotation = declaredField.getAnnotation(Autowired.class);
                    String name = declaredField.getName();

                    // 方法一：直接给字段赋值（可以省略set方法）
                    declaredField.setAccessible(true);
                    declaredField.set(entry.getValue(), map.get(autowiredAnnotation.id()));

                    // 方法二：通过set方法给字段赋值
                    /*
                    Method[] methods = entry.getValue().getClass().getMethods();
                    for (int j = 0; j < methods.length; j++) {
                        Method method = methods[j];
                        if (method.getName().equalsIgnoreCase("set" + name)) {
                            method.invoke(entry.getValue(), map.get(autowiredAnnotation.id()));
                        }
                    }
                    */
                }
            }
        }

    }

    /**
     * 实现bean的增强，添加事务控制
     * @throws Exception
     */
    private static void transactionEnhance() throws Exception{
        ProxyFactory proxyFactory = ProxyFactory.getInstance();

        for (Map.Entry<String, Object> entry : map.entrySet()) {

            //是否实现了接口
            Class<?>[] interfaces = entry.getValue().getClass().getInterfaces();
            Object proxyObj ;
            if (interfaces != null && interfaces.length > 0) {
                proxyObj = proxyFactory.getJdkProxy(entry.getValue());
                map.put(entry.getKey(), proxyObj);
            } else {
                proxyObj = proxyFactory.getCglibProxy(entry.getValue());
            }
            map.put(entry.getKey(), proxyObj);
        }
    }

    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static  Object getBean(String id) {
        return map.get(id);
    }

}
