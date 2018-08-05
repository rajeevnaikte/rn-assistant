package com.rajeevn.assistant.util;

import com.rajeevn.assistant.Action;
import com.rajeevn.assistant.KeyWord;
import com.rajeevn.assistant.Util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.rajeevn.assistant.Constants.COMMANDS_FILE;
import static com.rajeevn.assistant.Constants.COMMAND_END;
import static com.rajeevn.assistant.Constants.COMMAND_START;
import static com.rajeevn.assistant.Constants.NEW_COMMAND_ADDED;
import static com.rajeevn.assistant.Constants.NOT_VALID_NUMBER;
import static com.rajeevn.assistant.Constants.VALUE_PATTERN;
import static com.rajeevn.assistant.Constants.VAR_PATTERN;
import static com.rajeevn.assistant.Util.getAllMatched;
import static com.rajeevn.assistant.Util.getException;
import static java.lang.Integer.parseInt;
import static java.text.MessageFormat.format;
import static java.util.Comparator.comparing;

public class CommandCache
{
	public static Object commandResult;

	public static final Map<String, Action> keywordMethodMap = new ConcurrentHashMap<>();

	public static final List<String> commands = new ArrayList<>();

	public static boolean cmdLogOn = true;

	private static String buildKey(String keyWord)
	{
		return keyWord.replaceAll(VAR_PATTERN, "").toLowerCase();
	}

	private static Action getAction(String command)
	{
		return keywordMethodMap.get(command.replaceAll(VALUE_PATTERN, "").toLowerCase());
	}

	public static void addKeyWord(String keyWord, Method method)
	{
		keywordMethodMap.put(buildKey(keyWord), new Action(keyWord, method));
	}

	public static void addKeyWord(String keyWord, List<String> childCommands)
	{
		keywordMethodMap.put(buildKey(keyWord), new Action(keyWord, childCommands));
	}

	@KeyWord ("command log ${val}")
	public static void setCmdLogOn(String val)
	{
		cmdLogOn = Boolean.parseBoolean(val);
	}

	@KeyWord ("Show all commands")
	public static void showKeyWords()
	{
		keywordMethodMap.entrySet().stream()
				.sorted(comparing(Map.Entry::getKey))
				.forEach(entry -> System.out.println(entry.getValue().getKeyWord()));
	}

	@KeyWord ("Clear recent commands from cache")
	public static void clearCommands()
	{
		commands.clear();
	}

	@KeyWord ("Save last ${x} commands as ${newCommand}")
	public static void saveCommands(String x, String newCommand)
	{
		try
		{
			int count = parseInt(x);
			if (count > commands.size())
				throw new IndexOutOfBoundsException();
			if (count == 0)
			{
				System.out.println("Nothing to save.");
				return;
			}
			List<String> commandList = commands.subList(commands.size() - count, commands.size());
			saveCommandsToFile(commandList, newCommand);
			addKeyWord(newCommand, commandList);
			System.out.println(format(NEW_COMMAND_ADDED, newCommand));
		}
		catch (NumberFormatException e)
		{
			System.err.println(format(NOT_VALID_NUMBER, x));
		}
		catch (IndexOutOfBoundsException e)
		{
			System.err.println(format("Only {0} commands are in cache!", commands.size()));
		}
	}

	private static void saveCommandsToFile(List<String> commandList, String newCommand)
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(COMMANDS_FILE), true)))
		{
			writer.write(COMMAND_START + newCommand);
			writer.newLine();
			for (String command : commandList)
			{
				writer.write(command);
				writer.newLine();
			}
			writer.write(COMMAND_END);
			writer.newLine();
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}

	@KeyWord ("Reload commands from file")
	public static void loadFromFile()
	{
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(COMMANDS_FILE))))
		{
			boolean started = false;
			String line;
			final List<String> currentCmds = new ArrayList<>();
			final List<String> currentSubCmds = new ArrayList<>();
			while ((line = reader.readLine()) != null)
			{
				if (started)
				{
					if (COMMAND_END.equals(line))
					{
						started = false;
						currentCmds.forEach(currentCmd -> addKeyWord(currentCmd, currentSubCmds));
						currentCmds.clear();
						currentSubCmds.clear();
					}
					else if (line.startsWith(COMMAND_START))
					{
						currentCmds.add(line.substring(3));
					}
					else
					{
						currentSubCmds.add(line);
					}
				}
				if (line.startsWith(COMMAND_START))
				{
					started = true;
					currentCmds.add(line.substring(3));
				}
			}
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}

	public static void invoke(final String command, boolean isNotSubCmd)
	{
		if (command.startsWith("cmd.exe /c "))
		{
			try
			{
				Util.execCommand(command);
				commands.add(command);
			}
			catch (Exception e)
			{
				if (isNotSubCmd)
					System.err.println(getException(e));
				else
					throw new RuntimeException(e);
			}
			return;
		}

		List<String> paramList = getAllMatched(VALUE_PATTERN, command, ArrayList.class);
		Action action = getAction(command);
		if (action != null)
		{
			try
			{
				if (isNotSubCmd && cmdLogOn)
				{
					System.out.println("\nRunning...\n");
				}
				action.invoke(paramList.toArray());
				if (isNotSubCmd)
				{
					commands.add(command);
				}
			}
			catch (Exception e)
			{
				System.err.println(getException(e));
				if (!isNotSubCmd)
					throw new RuntimeException();
			}
			finally
			{
				if (isNotSubCmd && cmdLogOn)
					System.out.println("\ncomplete.\n");
			}
		}
		else
		{
			System.err.println("Command not found.");
		}
	}

	@KeyWord ("sleep for ${sec} second")
	public static void sleep(String sec) throws InterruptedException
	{
		Thread.sleep((long)(Double.parseDouble(sec) * 1000));
	}
}
