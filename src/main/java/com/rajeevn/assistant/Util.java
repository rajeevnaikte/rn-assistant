package com.rajeevn.assistant;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public final class Util
{
	private static final Map<String, Pattern> patternCache = new HashMap<>();

	private Util()
	{
	}

	public static Pattern getPattern(String regex)
	{
		Pattern pattern = patternCache.get(regex);
		if (pattern == null)
		{
			pattern = Pattern.compile(regex);
			patternCache.put(regex, pattern);
		}
		return pattern;
	}

	public static <T> void forEachRemaining(Enumeration<T> e, Consumer<T> c)
	{
		while (e.hasMoreElements())
			c.accept(e.nextElement());
	}

	public static <T> Optional<T> findMatching(Enumeration<T> e, Function<T, Boolean> f)
	{
		while (e.hasMoreElements())
		{
			T el = e.nextElement();
			if (f.apply(el))
				return Optional.of(el);
		}
		return Optional.empty();
	}

	public static class ExecThread
	{
		private Thread thread;
		private boolean stopped = false;

		public ExecThread(Function<ExecThread, Runnable> function)
		{
			thread = new Thread(function.apply(this));
		}

		public void start()
		{
			thread.start();
		}

		public void stop()
		{
			stopped = true;
			thread.interrupt();
		}

		public Thread getThread()
		{
			return thread;
		}

		public boolean isStopped()
		{
			return stopped;
		}
	}

	private static final Function<OutputStream, ExecThread> CONSOLE_IN = (out) -> new ExecThread((t) -> () -> {
		try (OutputStreamWriter writer = new OutputStreamWriter(out))
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while (!t.isStopped())
			{
				if (!br.ready())
					continue;
				writer.write(br.readLine());
				writer.flush();
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	});

	private static final Function<InputStream, Thread> CONSOLE_OUT = (in) ->
			new Thread(() -> {
				try (InputStreamReader isr = new InputStreamReader(in))
				{
					char[] buff = new char[1024];
					while ((isr.read(buff, 0, buff.length)) != -1)
					{
						of(buff).map(String::new).map(String::trim)
								.filter(s -> !s.isEmpty())
								.ifPresent(System.out::println);
						buff = new char[1024];
					}
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			});

	public static void execCommand(String command) throws Exception
	{
		Process process = null;
		ExecThread out = null;
		Thread err = null;
		Thread in = null;
		try
		{
			process = Runtime.getRuntime().exec(command);
			out = CONSOLE_IN.apply(process.getOutputStream());
			err = CONSOLE_OUT.apply(process.getErrorStream());
			in = CONSOLE_OUT.apply(process.getInputStream());
			in.start();
			err.start();
			out.start();
			process.waitFor();
		}
		finally
		{
			ofNullable(out).ifPresent(ExecThread::stop);
			ofNullable(in).ifPresent(Thread::interrupt);
			ofNullable(err).ifPresent(Thread::interrupt);
			ofNullable(process).ifPresent(Process::destroy);
			System.out.println("\nComplete\n");
		}
	}

	public static String stripExt(String fileName)
	{
		return fileName.substring(0, fileName.lastIndexOf('.'));
	}

	public static String zipEntryToPackage(ZipEntry entry)
	{
		return getPattern("[\\\\/]").matcher(entry.getName()).replaceAll(".");
	}

	public static <T extends Collection> T getAllMatched(String pattern, String text, Class<T> collection)
	{
		try
		{
			T inst = collection.newInstance();
			Matcher matcher = Pattern.compile(pattern).matcher(text);
			while (matcher.find())
				inst.add(matcher.group(1));
			return inst;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public static <T> Stream<T> stringToStream(final String text)
	{
		return stringToStream(text, null);
	}

	public static <T> Stream<T> stringToStream(final String text, final Function<String, T> parseItem)
	{
		final String[] items = text.split(",");
		return Stream.of(items).map(item -> parseItem == null ? (T)item : parseItem.apply(item));
	}

	public static <K, V> Map<K, V> stringToMap(String text, Function<String, K> parseKey, Function<String, V> parseValue)
	{
		String[] items = text.split(";");
		Map<K, V> map = new HashMap<>(items.length);
		for (String item : items)
		{
			String[] kv = item.split(":");
			K key = (parseKey == null) ? (K)kv[0] : parseKey.apply(kv[0]);
			V value = parseValue == null ? (V)kv[1] : parseValue.apply(kv[1]);
			map.put(key, value);
		}
		return map;
	}

	public static <K, V> void mergeMaps(final Map<K, V> map1, final Map<K, V> map2)
	{
		map2.forEach((k, v) ->
		{
			V val = map1.get(k);
			if (val instanceof Map)
			{
				mergeMaps((Map)val, (Map)v);
			}
			else
			{
				map1.put(k, v);
			}
		});
	}

	public static ClassLoader getClassLoader(final String path)
	{
		URL[] jars;
		File root = new File(path);
		if (root.isDirectory())
		{
			jars = Stream.of(root.listFiles()).map(file ->
			{
				try
				{
					return new URL("file:///" + file.getCanonicalPath());
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}).toArray(URL[]::new);
		}
		else
		{
			try
			{
				jars = new URL[] {new URL("file:///" + root.getCanonicalPath())};
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		return new URLClassLoader(jars);
	}

	public static String getException(Exception e)
	{
		return ofNullable(e).map(Exception::getCause).map(Throwable::toString).orElseGet(e::getMessage);
	}
}