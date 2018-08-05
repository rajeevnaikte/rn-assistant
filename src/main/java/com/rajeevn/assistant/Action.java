package com.rajeevn.assistant;

import com.rajeevn.assistant.util.CommandCache;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.rajeevn.assistant.Constants.VALUE_PATTERN;
import static com.rajeevn.assistant.Constants.VAR_PATTERN;
import static com.rajeevn.assistant.Util.getAllMatched;
import static com.rajeevn.assistant.Util.mergeMaps;
import static com.rajeevn.assistant.Util.stringToMap;
import static com.rajeevn.assistant.Util.stringToStream;
import static com.rajeevn.assistant.util.CommandCache.cmdLogOn;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

public class Action
{
	private final String keyWord;
	private final Method method;
	private final List<String> subCmds;
	private final Map<Integer, Map<Integer, Integer>> cmdToSubCmd = new HashMap<>();
	private final Map<Integer, Map<Integer, Integer>> cmdToSubCmdVar = new HashMap<>();

	public Action(String keyWord, Method method)
	{
		requireNonNull(method);
		this.keyWord = keyWord;
		this.method = method;
		this.subCmds = null;
	}

	public Action(String keyWord, List<String> subCmds)
	{
		requireNonNull(subCmds);
		this.keyWord = keyWord;
		this.subCmds = new ArrayList<>(subCmds);
		this.method = null;

		List<String> vars = getAllMatched(VAR_PATTERN, keyWord, ArrayList.class);
		for (int i = 0; i < vars.size(); i++)
		{
			final int varIndex = i + 1;
			String var = vars.get(i);
			mergeMaps(var.startsWith(":") ? cmdToSubCmdVar : cmdToSubCmd, stringToMap(var.split("#")[1], Integer::parseInt,
					(value) -> stringToStream(value, Integer::parseInt).collect(toMap((k) -> k, (v) -> varIndex))));
		}
	}

	public String getKeyWord()
	{
		return keyWord;
	}

	private static String COMMAND_LEVEL = "";

	public void invoke(Object... params) throws Exception
	{
		if (method != null)
		{
			Object[] mParams = new Object[method.getParameterCount()];
			for (int i = 0; i < mParams.length; i++)
			{
				if (i >= params.length)
					mParams[i] = null;
				else
					mParams[i] = params[i];
			}
			method.invoke(null, mParams);
		}
		else
		{
			COMMAND_LEVEL += " ";
			for (int i = 0; i < this.subCmds.size(); i++)
			{
				String command = this.subCmds.get(i);
				command = generateCommand(cmdToSubCmdVar, VAR_PATTERN, command, i, false, params);
				command = generateCommand(cmdToSubCmd, VALUE_PATTERN, command, i, true, params);
				if (cmdLogOn)
					System.out.println(COMMAND_LEVEL + command);
				CommandCache.invoke(command, false);
			}
			COMMAND_LEVEL = COMMAND_LEVEL.substring(1);
		}
	}

	private static String generateCommand(Map<Integer, Map<Integer, Integer>> cmdToCmd, String pattern, String command, int i, boolean appendQoute, Object[] params)
			throws Exception
	{
		Matcher matcher = Pattern.compile(pattern).matcher(command);
		StringBuffer res = new StringBuffer();
		int j = 0;
		while (matcher.find())
		{
			String replacement = "";
			if (cmdToCmd.containsKey(i + 1) && cmdToCmd.get(i + 1).containsKey(j + 1))
			{
				replacement = (String)params[cmdToCmd.get(i + 1).get(j + 1) - 1];
			}
			replacement = processCmd(matcher, replacement);
			if (appendQoute && !"".equals(replacement))
				matcher.appendReplacement(res, "'" + replacement + "'");
			else if (!appendQoute)
				matcher.appendReplacement(res, replacement);

			j++;
		}
		matcher.appendTail(res);
		return res.toString();
	}

	private static String processCmd(Matcher matcher, String replacement) throws Exception
	{
		String match = matcher.group();
		if (match.startsWith("${cmd "))
		{
			String firstPart = ("".equals(replacement)) ? "" : "'" + replacement + "' ";
			CommandCache.invoke(firstPart + match.substring(6, match.length() - 1), false);
			if (CommandCache.commandResult != null)
			{
				return (String)CommandCache.commandResult;
			}
		}
		return replacement;
	}
}