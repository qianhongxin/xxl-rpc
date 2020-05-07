package com.xxl.rpc.core.remoting.invoker.annotation;

import com.xxl.rpc.core.remoting.invoker.call.CallType;
import com.xxl.rpc.core.remoting.invoker.route.LoadBalance;
import com.xxl.rpc.core.remoting.net.Client;
import com.xxl.rpc.core.remoting.net.impl.netty.client.NettyClient;
import com.xxl.rpc.core.serialize.Serializer;
import com.xxl.rpc.core.serialize.impl.HessianSerializer;

import java.lang.annotation.*;

/**
 * rpc service annotation, skeleton of stub ("@Inherited" allow service use "Transactional")
 *
 * @author 2015-10-29 19:44:33
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface XxlRpcReference {

    Class<? extends Client> client() default NettyClient.class;
    Class<? extends Serializer> serializer() default HessianSerializer.class;
    CallType callType() default CallType.SYNC;
    LoadBalance loadBalance() default LoadBalance.ROUND;

    //Class<?> iface;
    String version() default "";

    long timeout() default 1000;

    String address() default "";
    String accessToken() default "";

    //XxlRpcInvokeCallback invokeCallback() ;

}
