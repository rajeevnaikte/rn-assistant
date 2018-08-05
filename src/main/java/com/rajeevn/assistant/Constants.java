package com.rajeevn.assistant;

public interface Constants
{
	String COMMANDS_FILE = "D:\\AIAssistant\\CombinedCommands.txt";
	String COMMAND_START = "***";
	String COMMAND_END = "***";

	String NEW_COMMAND_ADDED = "New command ''{0}'' is added.";

	String CREATING_FILE = "Creating file {0}...";

	String NOT_VALID_NUMBER = "{0} is not a valid number.";

	String VAR_PATTERN = "[$][{]([^}]*)[}]";
	String VALUE_PATTERN = "[']([^']*)[']";

	String STRING_TO_LIST_SPLIT_REGEX = "[,;]";
	String STRING_TO_KEY_VAL_REGEX = ":";
}