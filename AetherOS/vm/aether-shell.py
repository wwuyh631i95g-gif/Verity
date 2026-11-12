#!/usr/bin/python3
# Aether OS guest phone shell. Runs inside the ARM64 Linux VM on Xvnc.
import ast
import datetime
import os
import operator
import subprocess
import threading

import gi
gi.require_version("Gtk", "3.0")
from gi.repository import Gtk, Gdk, GLib

APP_DIR = os.path.expanduser("~/.config/aether")
os.makedirs(APP_DIR, exist_ok=True)
NOTES_FILE = os.path.join(APP_DIR, "notes.txt")

CSS = b"""
* { font-family: 'DejaVu Sans', sans-serif; }
window { background: #10172f; color: #f5f7ff; }
.aether-root { background: linear-gradient(155deg, #10172f 0%, #302d62 52%, #075765 100%); }
.topbar { padding: 12px 18px 8px 18px; color: #cbd8ff; }
.brand { color: #ffffff; font-weight: 800; letter-spacing: 2px; }
.clock { color: #ffffff; font-size: 48px; font-weight: 300; }
.date { color: #ced8ff; font-size: 15px; }
.hero { background: rgba(255,255,255,0.11); border: 1px solid rgba(255,255,255,0.18); border-radius: 26px; padding: 18px; }
.hero-title { color: #ffffff; font-size: 20px; font-weight: 700; }
.muted { color: #c4cbe8; font-size: 12px; }
.section { color: #eff2ff; font-size: 16px; font-weight: 700; padding: 12px 3px 7px 3px; }
.tile { background: rgba(255,255,255,0.12); border: 1px solid rgba(255,255,255,0.16); border-radius: 22px; padding: 12px 5px; color: white; }
.tile:hover { background: rgba(255,255,255,0.22); }
.tile-icon { font-size: 30px; color: #ffffff; }
.tile-title { font-size: 12px; font-weight: 700; color: #ffffff; }
.action { background: #dce8ff; color: #14203d; border-radius: 17px; padding: 12px 16px; font-weight: 700; }
.action:hover { background: #ffffff; }
.back { background: rgba(255,255,255,0.15); color: white; border-radius: 16px; padding: 9px 14px; }
.entry, textview { background: rgba(255,255,255,0.94); color: #17213d; border-radius: 12px; padding: 10px; }
.page-title { font-size: 25px; font-weight: 800; color: white; padding: 4px 0 8px 0; }
.list-card { background: rgba(255,255,255,0.11); border: 1px solid rgba(255,255,255,0.14); border-radius: 18px; padding: 12px; }
"""
OPS = {
    ast.Add: operator.add, ast.Sub: operator.sub, ast.Mult: operator.mul,
    ast.Div: operator.truediv, ast.Pow: operator.pow, ast.USub: operator.neg,
    ast.UAdd: operator.pos, ast.Mod: operator.mod,
}

def safe_calculate(expression):
    tree = ast.parse(expression, mode="eval")
    def walk(node):
        if isinstance(node, ast.Expression): return walk(node.body)
        if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)): return node.value
        if isinstance(node, ast.BinOp) and type(node.op) in OPS:
            a, b = walk(node.left), walk(node.right)
            if isinstance(node.op, ast.Pow) and abs(b) > 8:
                raise ValueError("Слишком большая степень")
            return OPS[type(node.op)](a, b)
        if isinstance(node, ast.UnaryOp) and type(node.op) in OPS:
            return OPS[type(node.op)](walk(node.operand))
        raise ValueError("Допустимы только числа и знаки + − × ÷ ^")
    value = walk(tree)
    if abs(value) > 1e100:
        raise ValueError("Слишком большой результат")
    return value

class AetherShell:
    def __init__(self):
        self.window = Gtk.Window(title="Aether OS")
        self.window.set_decorated(False)
        self.window.fullscreen()
        self.window.set_default_size(720, 1600)
        self.root = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=0)
        self.root.get_style_context().add_class("aether-root")
        self.window.add(self.root)
        provider = Gtk.CssProvider()
        provider.load_from_data(CSS)
        screen = Gdk.Screen.get_default()
        Gtk.StyleContext.add_provider_for_screen(screen, provider, Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION)
        self.time_label = None
        self.window.connect("destroy", Gtk.main_quit)
        self.show_splash()
        self.window.show_all()
        GLib.timeout_add_seconds(1, self.tick)
        GLib.timeout_add(1500, self.show_home)

    def clear(self):
        for child in self.root.get_children():
            self.root.remove(child)

    def button(self, label, callback, css="action"):
        b = Gtk.Button(label=label)
        b.get_style_context().add_class(css)
        b.connect("clicked", lambda *_: callback())
        return b

    def label(self, text, css=None, size=None):
        w = Gtk.Label(label=text)
        w.set_xalign(0)
        if css:
            w.get_style_context().add_class(css)
        if size:
            w.set_markup("<span size='%d'>%s</span>" % (size * 1000, GLib.markup_escape_text(text)))
        return w

    def statusbar(self):
        row = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=8)
        row.get_style_context().add_class("topbar")
        brand = Gtk.Label(label="◈  AETHER OS")
        brand.set_xalign(0)
        brand.get_style_context().add_class("brand")
        self.time_label = Gtk.Label(label="")
        self.time_label.set_xalign(1)
        row.pack_start(brand, True, True, 0)
        row.pack_end(self.time_label, False, False, 0)
        self.tick()
        return row

    def tick(self):
        if self.time_label:
            self.time_label.set_text(datetime.datetime.now().strftime("%H:%M   •   %d.%m"))
        return True

    def show_splash(self):
        self.clear()
        box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=16)
        box.set_halign(Gtk.Align.CENTER)
        box.set_valign(Gtk.Align.CENTER)
        logo = Gtk.Label(label="◈")
        logo.set_markup("<span size='60000' weight='bold' foreground='#ffffff'>◈</span>")
        title = Gtk.Label(label="AETHER OS")
        title.set_markup("<span size='26000' weight='bold' foreground='#ffffff' letter_spacing='1800'>AETHER OS</span>")
        sub = Gtk.Label(label="Загрузка виртуальной системы")
        sub.get_style_context().add_class("muted")
        spinner = Gtk.Spinner()
        spinner.start()
        box.pack_start(logo, False, False, 0)
        box.pack_start(title, False, False, 0)
        box.pack_start(sub, False, False, 0)
        box.pack_start(spinner, False, False, 10)
        self.root.pack_start(box, True, True, 0)

    def frame(self, title):
        self.clear()
        self.root.pack_start(self.statusbar(), False, False, 0)
        title_row = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=12)
        back = self.button("‹", self.show_home, "back")
        back.set_size_request(46, 44)
        title_label = Gtk.Label(label=title)
        title_label.set_xalign(0)
        title_label.get_style_context().add_class("page-title")
        title_row.pack_start(back, False, False, 0)
        title_row.pack_start(title_label, True, True, 0)
        title_row.set_margin_start(16)
        title_row.set_margin_end(16)
        self.root.pack_start(title_row, False, False, 5)
        body = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=12)
        body.set_margin_start(16)
        body.set_margin_end(16)
        body.set_margin_top(8)
        body.set_margin_bottom(18)
        scroller = Gtk.ScrolledWindow()
        scroller.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        scroller.add(body)
        self.root.pack_start(scroller, True, True, 0)
        self.window.show_all()
        return body

    def tile(self, icon, name, detail, action):
        b = Gtk.Button()
        b.get_style_context().add_class("tile")
        box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=6)
        box.set_halign(Gtk.Align.CENTER)
        ico = Gtk.Label(label=icon)
        ico.get_style_context().add_class("tile-icon")
        txt = Gtk.Label(label=name)
        txt.get_style_context().add_class("tile-title")
        desc = Gtk.Label(label=detail)
        desc.get_style_context().add_class("muted")
        desc.set_line_wrap(True)
        desc.set_justify(Gtk.Justification.CENTER)
        box.pack_start(ico, False, False, 0)
        box.pack_start(txt, False, False, 0)
        box.pack_start(desc, False, False, 0)
        b.add(box)
        b.connect("clicked", lambda *_: action())
        b.set_size_request(0, 118)
        return b

    def show_home(self):
        self.clear()
        self.root.pack_start(self.statusbar(), False, False, 0)
        outer = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=12)
        outer.set_margin_start(18)
        outer.set_margin_end(18)
        outer.set_margin_top(8)
        outer.set_margin_bottom(12)

        now = datetime.datetime.now()
        clock = Gtk.Label(label=now.strftime("%H:%M"))
        clock.get_style_context().add_class("clock")
        clock.set_halign(Gtk.Align.START)
        date = Gtk.Label(label=now.strftime("%A, %d %B %Y"))
        date.get_style_context().add_class("date")
        date.set_halign(Gtk.Align.START)
        outer.pack_start(clock, False, False, 0)
        outer.pack_start(date, False, False, 0)

        hero = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)
        hero.get_style_context().add_class("hero")
        htitle = Gtk.Label(label="Твой виртуальный телефон")
        htitle.set_halign(Gtk.Align.START)
        htitle.get_style_context().add_class("hero-title")
        hsub = Gtk.Label(label="Aether OS работает в отдельной ARM64 Linux-системе.")
        hsub.set_halign(Gtk.Align.START)
        hsub.set_line_wrap(True)
        hsub.get_style_context().add_class("muted")
        details = Gtk.Label(label="Собственное ядро • виртуальный диск • сохранение данных")
        details.set_halign(Gtk.Align.START)
        details.get_style_context().add_class("muted")
        hero.pack_start(htitle, False, False, 0)
        hero.pack_start(hsub, False, False, 0)
        hero.pack_start(details, False, False, 0)
        outer.pack_start(hero, False, False, 4)

        section = Gtk.Label(label="ПРИЛОЖЕНИЯ")
        section.set_halign(Gtk.Align.START)
        section.get_style_context().add_class("section")
        outer.pack_start(section, False, False, 0)

        grid = Gtk.Grid()
        grid.set_column_spacing(10)
        grid.set_row_spacing(10)
        items = [
            ("🗂", "Файлы", "Папки и документы", self.show_files),
            ("⌨", "Терминал", "Команды Linux", self.open_terminal),
            ("🌐", "Интернет", "Текстовый браузер", self.open_browser),
            ("📝", "Заметки", "Локальные записи", self.show_notes),
            ("🧮", "Калькулятор", "Вычисления", self.show_calculator),
            ("🛍", "Магазин", "Программы Linux", self.show_store),
            ("⚙", "Настройки", "Параметры системы", self.show_settings),
            ("▤", "Состояние", "Информация о VM", self.show_system),
            ("◷", "Дата", "Календарь и время", self.show_calendar),
        ]
        for i, (icon, name, desc, action) in enumerate(items):
            grid.attach(self.tile(icon, name, desc, action), i % 3, i // 3, 1, 1)
        outer.pack_start(grid, True, True, 0)

        dock = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=8)
        dock.get_style_context().add_class("hero")
        for icon, name, action in [
            ("⌂", "Домой", self.show_home),
            ("⌨", "Терминал", self.open_terminal),
            ("📝", "Заметки", self.show_notes),
            ("⏻", "Питание", self.show_power),
        ]:
            b = Gtk.Button(label=icon + "\n" + name)
            b.get_style_context().add_class("tile")
            b.connect("clicked", lambda _, fn=action: fn())
            dock.pack_start(b, True, True, 0)
        outer.pack_end(dock, False, False, 0)

        scroller = Gtk.ScrolledWindow()
        scroller.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        scroller.add_with_viewport(outer)
        self.root.pack_start(scroller, True, True, 0)
        self.window.show_all()

    def open_terminal(self):
        try:
            subprocess.Popen(["xterm", "-fa", "DejaVu Sans Mono", "-fs", "12", "-fullscreen"])
        except Exception as e:
            self.message("Терминал", str(e))

    def open_browser(self):
        try:
            subprocess.Popen(["xterm", "-fa", "DejaVu Sans Mono", "-fs", "12", "-e", "links", "https://alpinelinux.org"])
        except Exception as e:
            self.message("Интернет", "Браузер не удалось запустить: " + str(e))

    def show_files(self):
        dialog = Gtk.FileChooserDialog(
            title="Файлы Aether", parent=self.window,
            action=Gtk.FileChooserAction.OPEN,
            buttons=("Отмена", Gtk.ResponseType.CANCEL, "Открыть", Gtk.ResponseType.ACCEPT),
        )
        dialog.set_current_folder(os.path.expanduser("~"))
        dialog.run()
        dialog.destroy()

    def show_notes(self):
        body = self.frame("Заметки")
        view = Gtk.TextView()
        view.set_wrap_mode(Gtk.WrapMode.WORD_CHAR)
        view.get_style_context().add_class("entry")
        try:
            with open(NOTES_FILE, "r", encoding="utf-8") as f:
                view.get_buffer().set_text(f.read())
        except FileNotFoundError:
            view.get_buffer().set_text("")
        scroller = Gtk.ScrolledWindow()
        scroller.set_size_request(-1, 430)
        scroller.add(view)
        body.pack_start(self.label("Здесь хранятся заметки внутри виртуального диска."), False, False, 0)
        body.pack_start(scroller, True, True, 0)
        def save():
            start, end = view.get_buffer().get_bounds()
            with open(NOTES_FILE, "w", encoding="utf-8") as f:
                f.write(view.get_buffer().get_text(start, end, True))
            self.message("Заметки", "Запись сохранена.")
        body.pack_start(self.button("Сохранить", save), False, False, 0)
        self.window.show_all()

    def show_calculator(self):
        body = self.frame("Калькулятор")
        entry = Gtk.Entry()
        entry.set_placeholder_text("Например: (125 + 75) / 4")
        entry.get_style_context().add_class("entry")
        result = Gtk.Label(label="Введите выражение")
        result.get_style_context().add_class("hero-title")
        def calculate():
            try:
                result.set_text(str(safe_calculate(entry.get_text())))
            except Exception as e:
                result.set_text("Ошибка: " + str(e))
        body.pack_start(entry, False, False, 0)
        body.pack_start(self.button("Вычислить", calculate), False, False, 0)
        body.pack_start(result, False, False, 0)
        self.window.show_all()

    def show_store(self):
        body = self.frame("Магазин Aether")
        body.pack_start(self.label("Каталог программ для гостевой Linux-системы. Установка сохраняется на виртуальном диске."), False, False, 0)
        packages = [
            ("htop", "Монитор процессов и нагрузки"),
            ("nano", "Простой текстовый редактор"),
            ("mc", "Двухпанельный файловый менеджер"),
            ("vim", "Расширенный редактор текста"),
            ("bash-completion", "Автодополнение команд"),
            ("links", "Текстовый интернет-браузер"),
        ]
        for package, desc in packages:
            row = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=10)
            card = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=3)
            card.get_style_context().add_class("list-card")
            title = Gtk.Label(label=package)
            title.set_halign(Gtk.Align.START)
            title.get_style_context().add_class("hero-title")
            sub = Gtk.Label(label=desc)
            sub.set_halign(Gtk.Align.START)
            sub.get_style_context().add_class("muted")
            card.pack_start(title, False, False, 0)
            card.pack_start(sub, False, False, 0)
            install = self.button("Установить", lambda p=package: self.install_package(p))
            row.pack_start(card, True, True, 0)
            row.pack_end(install, False, False, 0)
            body.pack_start(row, False, False, 0)
        self.window.show_all()

    def install_package(self, package):
        allowed = {"htop", "nano", "mc", "vim", "bash-completion", "links"}
        if package not in allowed:
            return
        try:
            subprocess.Popen(["xterm", "-fa", "DejaVu Sans Mono", "-fs", "12", "-e", "apk", "add", package])
        except Exception as e:
            self.message("Магазин", str(e))

    def show_settings(self):
        body = self.frame("Настройки")
        body.pack_start(self.label("Aether OS — гостевая Linux-система ARM64"), False, False, 0)
        body.pack_start(self.label("Все данные сохраняются на виртуальном диске."), False, False, 0)
        body.pack_start(self.label("Экран: портретный режим 720 × 1280"), False, False, 0)
        body.pack_start(self.button("О системе", self.show_system), False, False, 0)
        body.pack_start(self.button("Перезапустить систему", lambda: self.confirm_power("reboot")), False, False, 0)
        body.pack_start(self.button("Выключить систему", lambda: self.confirm_power("poweroff")), False, False, 0)
        self.window.show_all()

    def show_system(self):
        body = self.frame("Состояние системы")
        for title, cmd in [
            ("Ядро", ["uname", "-r"]),
            ("Архитектура", ["uname", "-m"]),
            ("Память", ["sh", "-c", "free -h | head -2"]),
            ("Диск", ["sh", "-c", "df -h / | tail -1"]),
            ("Сеть", ["sh", "-c", "ip -brief addr 2>/dev/null | head -4"]),
        ]:
            try:
                value = subprocess.check_output(cmd, text=True, stderr=subprocess.DEVNULL).strip()
            except Exception:
                value = "Недоступно"
            card = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=4)
            card.get_style_context().add_class("list-card")
            t = Gtk.Label(label=title)
            t.set_halign(Gtk.Align.START)
            t.get_style_context().add_class("hero-title")
            v = Gtk.Label(label=value)
            v.set_halign(Gtk.Align.START)
            v.set_selectable(True)
            v.set_line_wrap(True)
            v.get_style_context().add_class("muted")
            card.pack_start(t, False, False, 0)
            card.pack_start(v, False, False, 0)
            body.pack_start(card, False, False, 0)
        self.window.show_all()

    def show_calendar(self):
        body = self.frame("Дата и время")
        now = datetime.datetime.now()
        body.pack_start(self.label(now.strftime("%A, %d %B %Y"), "hero-title"), False, False, 0)
        body.pack_start(self.label(now.strftime("Локальное время виртуальной системы: %H:%M:%S")), False, False, 0)
        body.pack_start(self.label("Время гостевой системы синхронизируется при запуске виртуальной машины."), False, False, 0)
        self.window.show_all()

    def show_power(self):
        body = self.frame("Питание")
        body.pack_start(self.label("Управление только гостевой системой. Android на телефоне продолжит работать."), False, False, 0)
        body.pack_start(self.button("Перезагрузить Aether OS", lambda: self.confirm_power("reboot")), False, False, 0)
        body.pack_start(self.button("Выключить Aether OS", lambda: self.confirm_power("poweroff")), False, False, 0)
        self.window.show_all()

    def confirm_power(self, command):
        title = "Перезагрузить систему?" if command == "reboot" else "Выключить систему?"
        dialog = Gtk.MessageDialog(parent=self.window, flags=0, message_type=Gtk.MessageType.QUESTION,
                                   buttons=Gtk.ButtonsType.CANCEL, text=title)
        dialog.add_button("Продолжить", Gtk.ResponseType.OK)
        response = dialog.run()
        dialog.destroy()
        if response == Gtk.ResponseType.OK:
            subprocess.Popen([command])

    def message(self, title, text):
        dialog = Gtk.MessageDialog(parent=self.window, flags=0, message_type=Gtk.MessageType.INFO,
                                   buttons=Gtk.ButtonsType.OK, text=title)
        dialog.format_secondary_text(text)
        dialog.run()
        dialog.destroy()

if __name__ == "__main__":
    AetherShell()
    Gtk.main()
