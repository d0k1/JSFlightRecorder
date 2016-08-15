import org.apache.commons.lang3.StringUtils
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.WebElement

import java.util.stream.Collectors

/**
 * Базовый скрипт поиска элементов
 * Скрипту доступны след. переменные:
 * 		target - целевой XPath из события
 * 		webdriver - вебдрайвер
 * 		event - само событие
 * 		logger - лог
 */

java.lang.Thread.currentThread().setContextClassLoader(classloader);

if (target.isEmpty()) {
    return webdriver.findElement(By.xpath('/html'));
}

List<String> elements = Arrays.stream(target.split("//\\*"))
        .filter { element -> !element.isEmpty() }
        .map { element -> "//*" + element }
        .collect(Collectors.toList());

/**
 *  Если идет работа со скроллом, и скролл не в выпадающих списках, скрипт должен вернуть элемент html
 */
if (event.type.equals('mousewheel')) {
    if (!target.contains('popup')) {
        return webdriver.findElement(By.xpath('/html'));
    }
    JavascriptExecutor js = (JavascriptExecutor) webdriver;
    try {
        return webdriver.findElement(By.xpath("//*[@class='formtree']"));
    } catch (Exception e) {
        logger.info("This is not a popup tree select")
    }
    StringBuilder xp = new StringBuilder("//*[@class='popupContent']");
    WebElement element = webdriver.findElement(By.xpath(xp.toString()));
    while (!Boolean.valueOf(js.executeScript('return arguments[0].childElementCount > 20', element))) {
        logger.info(xp.toString())
        element = webdriver.findElement(By.xpath(xp.toString()));
        if (Boolean.valueOf(js.executeScript("var curr = arguments[0].scrollTop; arguments[0].scrollTop = curr + 1;" +
                "var eq =  arguments[0].scrollTop == curr +1; arguments[0].scrollTop = curr; return eq", element))) {
            return element;
        }
        xp.append("/div")
    }
    return null;
}

def applicableForSearch = ['click', 'mousedown', 'keypress', 'keydown']

if (event.type in applicableForSearch) {
    List<String> presentElements = new ArrayList<>();
    WebElement targetElement = null;
    for (Iterator<String> iterator = elements.iterator(); iterator.hasNext();) {
        String element = iterator.next();
        presentElements.add(element);
        try {
            targetElement = webdriver.findElement(By.xpath(String.join("", presentElements)));
        }
        catch (NoSuchElementException e) {
            //currently handles problems with id, that has time-based uuid
            presentElements.remove(presentElements.size() - 1);
            String newElement = "//*[contains(@id,'" + StringUtils.substringBetween(element, "@id='", ".") + "')]";
            presentElements.add(newElement);

            //Should be ok by now
            try {
                targetElement = webdriver.findElement(By.xpath(String.join("", presentElements)));
            }
            catch (NoSuchElementException e1) {
                logger.warn("Failed to rearrange element {}. Removing from xpath", element);
                //if not, remove this element and continue search
                presentElements.remove(presentElements.size() - 1);
            }
        }
    }
    logger.info("new xpath for target element {}", String.join("", presentElements));


    return targetElement;
}