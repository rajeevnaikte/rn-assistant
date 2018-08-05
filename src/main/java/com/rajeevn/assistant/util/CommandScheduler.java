package com.rajeevn.assistant.util;

import com.rajeevn.assistant.KeyWord;

import java.util.Timer;

public class CommandScheduler
{
	private static final Timer TIMER = new Timer();

	@KeyWord ("schedule the command ${interval}")
	public static void schedule(String interval)
	{

	}
}