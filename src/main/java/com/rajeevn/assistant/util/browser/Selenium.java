package com.rajeevn.assistant.util.browser;

import com.rajeevn.assistant.KeyWord;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;

public class Selenium
{
	static
	{
		System.setProperty("webdriver.chrome.driver", "D:\\testing\\drivers\\chromedriver.exe");
	}

	private static WebDriver currentBrowser;
	private static final Map<String, WebElement> WEB_ELEMENT_MAP = new ConcurrentHashMap<>();

	private static WebDriver getCurrentBrowser()
	{
		if (currentBrowser == null || isCurrentBrowserClosed())
		{
			final WebDriver browser = new ChromeDriver();
			currentBrowser = browser;
			browser.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
			browser.get("about:blank");
			browser.manage().window().maximize();

			new Thread(() ->
			{
				boolean open = true;
				while (open)
				{
					try
					{
						browser.getTitle();
						Thread.sleep(1000);
					}
					catch (Exception e)
					{
						open = false;
					}
				}
				try
				{
					browser.quit();
				}
				catch (Exception e)
				{
				}
			}).start();
		}
		return currentBrowser;
	}

	private static boolean isCurrentBrowserClosed()
	{
		return currentBrowser.toString().contains("null");
	}

	private static void waitUtil(WebDriver driver, ExpectedCondition condition)
	{
		waitUtil(driver, condition, 30);

	}

	private static void waitUtil(WebDriver driver, ExpectedCondition condition, int timeout)
	{
		try
		{

			new WebDriverWait(driver, timeout)
					.ignoring(StaleElementReferenceException.class)
					.until(condition);
		}
		catch (Exception e)
		{
		}
	}

	public static void waitVisibilityOf(WebDriver driver, WebElement element)
	{
		waitUtil(getCurrentBrowser(), visibilityOf(element));
	}

	@KeyWord ("wait for visibility of web element ${xpath} inside ${savedWebElement}")
	public static void waitVisibilityOf(String xpath, String savedWebElement)
	{
		waitVisibilityOf(getCurrentBrowser(), getWebElement(xpath, savedWebElement));
	}

	@KeyWord ("wait for invisibility of web element ${xpath}")
	public static void waitInvisibilityOf(String xpath)
	{
		waitInvisibilityOf(xpath, null);
	}

	@KeyWord ("wait for invisibility of web element ${xpath} inside ${savedWebElement}")
	public static void waitInvisibilityOf(String xpath, String savedWebElement)
	{
		waitUtil(getCurrentBrowser(), not(visibilityOf(getWebElement(xpath, savedWebElement))));
	}

	private static WebElement getWebElement(String xpath, String savedWebElement)
	{
		if (currentBrowser == null || isCurrentBrowserClosed())
			throw new RuntimeException("Browser is not open.");
		return ofNullable(savedWebElement)
				.filter(s -> !"browser".equals(s))
				.map(WEB_ELEMENT_MAP::get)
				.map(el -> (SearchContext)el)
				.orElseGet(Selenium::getCurrentBrowser)
				.findElement(By.xpath(xpath));
	}

	private static WebElement getVisibleWebElement(String xpath, String savedWebElement)
	{
		WebElement element = getWebElement(xpath, savedWebElement);
		waitVisibilityOf(getCurrentBrowser(), element);
		return element;
	}

	@KeyWord ("close current browser")
	public static void closeCurrentBrowser()
	{
		WebDriver driver = currentBrowser;
		currentBrowser = null;
		if (driver != null)
		{
			driver.close();
			driver.quit();
		}
	}

	@KeyWord ("Open url ${url}")
	public static void openUrl(String url)
	{
		getCurrentBrowser().get(url);
	}

	@KeyWord ("Save web element ${xpath} as ${name}")
	public static void saveWebElement(String xpath, String name)
	{
		WEB_ELEMENT_MAP.put(name, getWebElement(xpath, null));
	}

	@KeyWord ("Show saved web elements")
	public static void showSavedWebElements()
	{
		WEB_ELEMENT_MAP.keySet().forEach(System.out::println);
	}

	@KeyWord ("Input ${text} into web element ${xpath}")
	public static void input(String text, String xpath)
	{
		inputIn(text, xpath, null);
	}

	@KeyWord ("Click web element ${xpath}")
	public static void click(String xpath)
	{
		clickIn(xpath, null);
	}

	@KeyWord ("Input ${text} into web element ${xpath} inside ${savedWebElement}")
	public static void inputIn(String text, String xpath, String savedWebElement)
	{
		getVisibleWebElement(xpath, savedWebElement).sendKeys(text);
	}

	@KeyWord ("hit key ${key} into web element ${xpath}")
	public static void hitKey(String key, String xpath)
	{
		hitKey(key, xpath, null);
	}

	@KeyWord ("hit key ${key} into web element ${xpath} inside ${savedWebElement}")
	public static void hitKey(String key, String xpath, String savedWebElement)
	{
		getVisibleWebElement(xpath, savedWebElement)
				.sendKeys(Keys.chord(Stream.of(key.split("[+]")).map(k ->
				{
					try
					{
						return Keys.valueOf(k);
					}
					catch (Exception e)
					{
						return k;
					}
				}).collect(joining())));
	}

	@KeyWord ("Click web element ${xpath} inside ${savedWebElement}")
	public static void clickIn(String xpath, String savedWebElement)
	{
		getVisibleWebElement(xpath, savedWebElement).click();
	}

	@KeyWord ("wait for page to contain web element ${xpath}")
	public static void waitForWebElement(String xpath)
	{
		WebElement element = getVisibleWebElement(xpath, null);
		if (element == null)
		{
			System.err.println("element not found");
		}
	}

	@KeyWord ("wait for page not to contain web element ${xpath}")
	public static void waitForNoWebElement(String xpath)
	{
		WebElement element = getWebElement(xpath, null);
		if (element != null)
		{
			System.err.println("element found");
		}
	}

	@KeyWord ("print web element ${xpath}")
	public static void print(String xpath)
	{
		System.out.println(getVisibleWebElement(xpath, null).getText());
	}
}