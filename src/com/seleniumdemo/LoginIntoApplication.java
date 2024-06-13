package com.seleniumdemo;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

public class LoginIntoApplication {

	public static void main(String[] args) {
        // Set the path to the ChromeDriver executable
        System.setProperty("webdriver.chrome.driver", "lib\\chromedriver.exe");

        // Create ChromeOptions for headless mode
        ChromeOptions options = new ChromeOptions();
        // gg
        //options.addArguments("--headless");
        
//        options.addArguments("--disable-dev-shm-usage");
//        options.addArguments("--no-sandbox");
//        options.addArguments("--disable-web-security");
//        options.addArguments("--disable-features=NetworkService");
//        options.addArguments("--disable-features=VizDisplayCompositor");
        
        //options.setExperimentalOption("useAutomationExtension", false);
        //options.addArguments("--disable-extensions");
        //options.setCapability("goog:loggingPrefs", "{browser: 'ALL', driver: 'ALL', performance: 'ALL'}");


        // Create an instance of ChromeDriver with options
        WebDriver driver = new ChromeDriver(options);

        // Maximize the browser window
        driver.manage().window().maximize();

        // Navigate to Google
        driver.get("https://www.google.com");

        // Perform any additional actions here

        // Close the browser
        driver.quit();
        System.out.println("Browser has been closed");
    }

}
