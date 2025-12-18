"""
Telegram бот для отправки сообщений в Claude Desktop
Запуск: python claude_telegram_bot.py
"""

import pyautogui
import pyperclip
import pygetwindow as gw
import time
import logging
from telegram import Update
from telegram.ext import Application, CommandHandler, MessageHandler, filters, ContextTypes

# Логирование
logging.basicConfig(
    format='%(asctime)s - %(levelname)s - %(message)s',
    level=logging.INFO
)
logger = logging.getLogger(__name__)

# === НАСТРОЙКИ ===
BOT_TOKEN = "8212197837:AAE3fpoG9XFmGzBi07MP6XsBWK55yrG-VYw"
ALLOWED_USER_ID = 8080436435
CLAUDE_WINDOW_TITLE = "Claude"

# === ФУНКЦИИ ===

def find_claude_window():
    try:
        windows = gw.getWindowsWithTitle(CLAUDE_WINDOW_TITLE)
        if windows:
            return windows[0]
    except Exception as e:
        logger.error(f"Ошибка поиска окна: {e}")
    return None

def send_to_claude(message: str) -> bool:
    try:
        window = find_claude_window()
        if not window:
            return False
        
        try:
            if window.isMinimized:
                window.restore()
            window.activate()
        except Exception:
            pass
        
        time.sleep(0.7)
        pyperclip.copy(message)
        time.sleep(0.2)
        pyautogui.hotkey('ctrl', 'v')
        time.sleep(0.4)
        pyautogui.press('enter')
        return True
    except Exception as e:
        logger.error(f"Ошибка отправки: {e}")
        return False

# === ОБРАБОТЧИКИ ===

async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if update.effective_user.id != ALLOWED_USER_ID:
        await update.message.reply_text("⛔ Доступ запрещён")
        return
    await update.message.reply_text(
        "🤖 Бот для управления Claude Desktop\n\n"
        "Просто отправь сообщение — оно будет передано в Claude.\n\n"
        "/status — проверить подключение"
    )

async def status(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if update.effective_user.id != ALLOWED_USER_ID:
        return
    window = find_claude_window()
    if window:
        await update.message.reply_text(f"✅ Окно найдено: {window.title}")
    else:
        await update.message.reply_text("❌ Окно Claude не найдено")

async def handle_message(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if update.effective_user.id != ALLOWED_USER_ID:
        return
    
    await update.message.reply_text("📤 Отправляю...")
    success = send_to_claude(update.message.text)
    
    if success:
        await update.message.reply_text("✅ Отправлено!")
    else:
        await update.message.reply_text("❌ Окно Claude не найдено")

async def error_handler(update, context):
    logger.error(f"Ошибка: {context.error}")

# === ЗАПУСК ===

def main():
    print("🚀 Запуск бота...")
    
    window = find_claude_window()
    if window:
        print(f"✅ Окно найдено: {window.title}")
    else:
        print("⚠️ Окно Claude не найдено")
    
    app = (
        Application.builder()
        .token(BOT_TOKEN)
        .connect_timeout(30.0)
        .read_timeout(30.0)
        .write_timeout(30.0)
        .build()
    )
    
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("status", status))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_message))
    app.add_error_handler(error_handler)
    
    print("✅ Бот запущен! Ctrl+C для остановки")
    app.run_polling(drop_pending_updates=True)

if __name__ == "__main__":
    main()
