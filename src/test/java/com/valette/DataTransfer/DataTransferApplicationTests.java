package com.valette.DataTransfer;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DataTransferApplicationTests {
	@Autowired
	private StringEncryptor stringEncryptor;

	@Value("${jasypt.test-str}")
	private String value;

	@Test
	void contextLoads() {
	}

	@Test
	void encryptStr(){
		System.out.println(stringEncryptor.encrypt(value));
	}

}
