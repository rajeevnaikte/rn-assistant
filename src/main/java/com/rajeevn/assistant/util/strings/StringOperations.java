package com.rajeevn.assistant.util.strings;

import com.rajeevn.assistant.KeyWord;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.rajeevn.assistant.util.CommandCache.commandResult;

public class StringOperations
{
	@KeyWord ("${text} in this extract text with match ${regex}")
	public static void extractText(String text, String regex)
	{
		Matcher matcher = Pattern.compile(regex).matcher(text);
		if (matcher.find())
		{
			commandResult = matcher.group(1);
		}
		else
		{
			commandResult = "";
		}
	}

	private static final String KEY = "Q!w2e3r4t5y6u7i8";
	private static final String ALGORITHM = "AES";

	@KeyWord ("${text} encrypt text")
	public static void encryptText(String text) throws Exception
	{
		SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);

		commandResult = new String(Base64.getEncoder().encode(cipher.doFinal(text.getBytes())));
		System.out.println(commandResult);
	}

	@KeyWord ("${text} decrypt text")
	public static void decryptText(String cipherText) throws Exception
	{
		SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, secretKey);

		commandResult = new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)));
	}
}