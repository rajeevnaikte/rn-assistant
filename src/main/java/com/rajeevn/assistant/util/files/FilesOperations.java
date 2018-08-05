package com.rajeevn.assistant.util.files;

import com.rajeevn.assistant.KeyWord;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilesOperations
{
	@KeyWord ("replace ${text} with ${replacement} in file ${filePath}")
	public static void replace(String text, String replacement, String filePath) throws IOException
	{
		Path path = Paths.get(filePath);
		Charset charset = StandardCharsets.UTF_8;

		String content = new String(Files.readAllBytes(path), charset);
		content = content.replaceAll(text, replacement);
		Files.write(path, content.getBytes(charset));
	}
}