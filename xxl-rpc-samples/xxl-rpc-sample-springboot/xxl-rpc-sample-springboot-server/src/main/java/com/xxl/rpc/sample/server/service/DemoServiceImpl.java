package com.xxl.rpc.sample.server.service;

import com.xxl.rpc.sample.api.DemoService;
import com.xxl.rpc.sample.api.dto.UserDTO;
import com.xxl.rpc.core.remoting.provider.annotation.XxlRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;

/**
 * @author xuxueli
 */
@XxlRpcService
@Service
public class DemoServiceImpl implements DemoService {
	private static Logger logger = LoggerFactory.getLogger(DemoServiceImpl.class);

	@Override
	public UserDTO sayHi(String name) {

		String word = MessageFormat.format("Hi {0}, from {1} as {2}",
				name, DemoServiceImpl.class.getName(), String.valueOf(System.currentTimeMillis()));

		if ("error".equalsIgnoreCase(name)) throw new RuntimeException("test exception.");

		UserDTO userDTO = new UserDTO(name, word);
		logger.info(userDTO.toString());

		return userDTO;
	}

}
