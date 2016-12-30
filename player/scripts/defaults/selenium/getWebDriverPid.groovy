/**
 * Базовый скрипт определения PID для webdriver
 * Скрипту доступны след. переменные:
 *      webdriver - вебдрайвер
 *      logger - логгер
 *      classloader - classloader от ScriptEngine
 *      playerContext - контекст плеера
 */

return "echo ${webdriver.binary.process.process.executeWatchdog.getPID()}".execute().text