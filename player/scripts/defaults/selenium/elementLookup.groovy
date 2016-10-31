import com.focusit.jsflight.player.constants.EventType
import org.openqa.selenium.*
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.WebDriverWait

import javax.annotation.Nullable
import java.util.stream.Collectors

import static com.focusit.jsflight.player.webdriver.SeleniumDriver.NO_OP_ELEMENT
/**
 * Базовый скрипт поиска элементов
 * Скрипту доступны след. переменные:
 *      target - целевой XPath из события
 *      webdriver - вебдрайвер
 *      event - само событие
 *      logger - лог
 */

List<String> elements = Arrays.stream(target.split("//\\*"))
        .filter { element -> !element.isEmpty() }
        .map { element -> "//*" + element }
        .collect(Collectors.toList());

def waitForElement(String xpath) {

    WebDriverWait driverWait = new WebDriverWait(webdriver, 10l, 50l);
    try {
        return driverWait.until(new ExpectedCondition<WebElement>() {
            @Override
            WebElement apply(@Nullable WebDriver driver) {
                try {
                    WebElement element = driver.findElement(By.xpath(xpath))
                    if (element.isDisplayed()) {
                        return element
                    }
                } catch (StaleElementReferenceException ignored) {
                    return null;
                }
            }
        });
    } catch (TimeoutException e) {
        logger.warn(String.format("Element '%s' wasn't found. Returning NO_OP_ELEMENT", xpath), e);
        return NO_OP_ELEMENT;
    }
}

/**
 *  Если идет работа со скроллом, и скролл не в выпадающих списках, скрипт должен вернуть элемент html
 */
if (event.type == EventType.MOUSE_WHEEL) {
    if (!target.contains('popup')) {
        return webdriver.findElement(By.xpath('//html'));
    }
}

def applicableForSearch = [EventType.CLICK, EventType.MOUSE_DOWN, EventType.KEY_PRESS,
                           EventType.KEY_DOWN, EventType.KEY_UP, EventType.MOUSE_WHEEL];

if (event.type in applicableForSearch) {
    swapToValidTreeXp(elements);
    logger.info("Looking for ${target}");
    def targetElement = waitForElement(target);
    if (targetElement == NO_OP_ELEMENT) {
        logger.info("Not found: ${target}");
        logger.info("Checking each step in xpath");
        //Brute search failed
        List<String> validElements = new ArrayList<>();
        for (String element : elements) {
            validElements << element;
            def tmpXpath = String.join("", validElements);
            try {
                logger.info("Checking step ${tmpXpath}");
                webdriver.findElement(By.xpath(tmpXpath));
            }
            catch (NoSuchElementException ignored) {
                //currently handles problems with id, that has time-based uuid
                logger.info("Element ${element} in ${tmpXpath} doesn't exist");
                validElements.pop();

                String newElement = element.replaceAll("(@[^=]*)=['|\"]([^'\"]*)['|\"]", "contains(\$1, '\$2')");
                validElements << newElement;
                tmpXpath = String.join("", validElements);
                //Should be ok by now
                try {
                    logger.info("Another checking step ${tmpXpath}");
                    webdriver.findElement(By.xpath(tmpXpath))
                }
                catch (NoSuchElementException _) {
                    logger.warn("Element ${element} doesn't point to any DOM-element. Removing from xpath ${tmpXpath}");
                    validElements.pop();
                }
            }
        }
        //After search we wait until element by new xpath becomes availiable
        def newXpath = String.join("", validElements)
        logger.info("New xpath for target element: {}", newXpath);
        targetElement = waitForElement(newXpath)
    }
    return targetElement;
}

return NO_OP_ELEMENT;
