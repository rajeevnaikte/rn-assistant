package com.rajeevn.assistant;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.rajeevn.assistant.Util.mergeMaps;
import static com.rajeevn.assistant.Util.stringToMap;
import static com.rajeevn.assistant.Util.stringToStream;
import static java.util.stream.Collectors.toMap;

public class GitOperationsTest
{
	@Test
	public void testStringToMap()
	{
		Map<Integer, Map<Integer, Integer>> map = new HashMap<>();
		Map<Integer, Map<Integer, Integer>> map1 = stringToMap("1:3,4;3:1;", Integer::parseInt,
				(value) -> stringToStream(value, Integer::parseInt).collect(toMap((k) -> k, (v) -> 1)));
		Map<Integer, Map<Integer, Integer>> map2 = stringToMap("2:1,4;3:3;", Integer::parseInt,
				(value) -> stringToStream(value, Integer::parseInt).collect(toMap((k) -> k, (v) -> 1)));

		mergeMaps(map, map1);
		mergeMaps(map, map2);

		System.out.println(map);
	}
}