//package com.login;

import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import junit.framework.Assert;

public class LoginTest {

	@Test
	public void testLogin() {

		WebDriverManager.chromedriver().setup();
		ChromeOptions options = new ChromeOptions();

        // REQUIRED for GitHub Actions
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);

		driver.get("https://www.google.com");
		
		
		Assert.assertEquals(false, false);
		
		
		
		driver.quit();

	}

}
