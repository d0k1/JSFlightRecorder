import org.openqa.selenium.By

/**
 * Базовый скрипт для определения готовности страницы
 * Скрипту доступны след. переменные:
 *      webdriver - вебдрайвер
 *      logger - лог
 */

return webdriver.findElement(By.xpath("//body")) != null;