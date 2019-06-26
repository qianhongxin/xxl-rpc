package com.xxl.rpc.registry;


import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * service registry
 *
 *  /xxl-rpc/dev/
 *              - key01(service01)
 *                  - value01 (ip:port01)
 *                  - value02 (ip:port02)
 *
 * @author xuxueli 2018-10-17
 */
public abstract class ServiceRegistry {

    /**
     * start
     *
     * 服务开启
     */
    public abstract void start(Map<String, String> param);

    /**
     * stop
     *
     * 服务停止
     */
    public abstract void stop();


    /**
     * registry service, for mult
     *
     * 服务注册
     *
     * @param keys      service key
     * @param value     service value/ip:port
     * @return
     */
    public abstract boolean registry(Set<String> keys, String value);


    /**
     * remove service, for mult
     *
     * 服务移除
     *
     * @param keys
     * @param value
     * @return
     */
    public abstract boolean remove(Set<String> keys, String value);

    /**
     * discovery services, for mult
     *
     * 多服务发现
     *
     * @param keys
     * @return
     */
    public abstract Map<String, TreeSet<String>> discovery(Set<String> keys);

    /**
     * discovery service, for one
     *
     * 单服务发现
     *
     * @param key   service key
     * @return      service value/ip:port
     */
    public abstract TreeSet<String> discovery(String key);

}
