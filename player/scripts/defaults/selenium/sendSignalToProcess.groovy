/**
 * Базовый скрипт для посыла сигналов процессам
 * Скрипту доступны след. переменные:
 *      signal - номер/имя сгнала
 *      pid - PID
 *      logger - логгер
 *      classloader - classloader от ScriptEngine
 *      playerContext - контекст плеера
 */

"kill ${signal} ${pid}".execute()