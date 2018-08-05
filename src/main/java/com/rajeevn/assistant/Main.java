package com.rajeevn.assistant;

import com.rajeevn.assistant.util.CommandCache;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.rajeevn.assistant.util.CommandCache.addKeyWord;
import static com.rajeevn.assistant.util.CommandCache.loadFromFile;
import static java.util.Optional.ofNullable;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		load();
		loadFromFile();
		System.out.println("\nHello Rajeev!\nHow may i help you today?\n");

		while (true)
		{
			Scanner scanner = new Scanner(System.in);
			String command = scanner.nextLine();
			if (command == null || "".equals(command))
			{
				continue;
			}
			if ("bye".equalsIgnoreCase(command))
			{
				scanner.close();
				System.out.println("Happy to serve you.\nBye.");
				System.exit(0);
			}
			CommandCache.invoke(command, true);
		}
	}

	private static void load() throws Exception
	{
		final String packageName = "com.rajeevn.assistant.util.";
		File thisJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		if (thisJar.isDirectory())
		{
			Enumeration<URL> resources = Main.class.getClassLoader().getResources(packageName.replaceAll("[.]", "/"));
			Util.forEachRemaining(resources, url -> findClasses(new File(url.getPath()), "com.rajeevn.assistant."));
		}
		else
		{
			Enumeration<? extends ZipEntry> resources = new ZipFile(thisJar).entries();
			Util.forEachRemaining(resources, entry -> processClassFile(entry.getName()));
		}
	}

	private static void findClasses(File file, String packageName)
	{
		if (file.isDirectory())
		{
			Stream.of(file.listFiles()).forEach(subFile -> findClasses(subFile, packageName + file.getName() + "."));
		}
		else
		{
			processClassFile(packageName + file.getName());
		}
	}

	private static void processClassFile(String classFullName)
	{
		classFullName = classFullName.replaceAll("[\\\\/]", ".");
		if (classFullName.startsWith("com.rajeevn.assistant.util.") && classFullName.endsWith(".class"))
		{
			try
			{
				Class clazz = Class.forName(Util.stripExt(classFullName));
				Stream.of(clazz.getDeclaredMethods()).forEach(Main::register);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	private static void register(final Method method)
	{
		ofNullable(method)
				.map(m -> m.getAnnotation(KeyWord.class))
				.ifPresent(kw -> addKeyWord(kw.value(), method));
	}
}




