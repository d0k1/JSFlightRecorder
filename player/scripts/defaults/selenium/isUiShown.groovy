/**
 * Базовый скрипт для определения готовности страницы для дальнейшей работы
 * Скрипту доступны след. переменные:
 *      webdriver - вебдрайвер
 *      logger - логгер
 *      classloader - classloader от ScriptEngine
 *      playerContext - контекст плеера
 */

import org.openqa.selenium.By

return webdriver.findElement(By.xpath("//body")) != null;