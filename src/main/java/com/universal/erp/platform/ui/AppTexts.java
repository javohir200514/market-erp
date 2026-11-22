package com.universal.erp.platform.ui;

import com.vaadin.flow.server.VaadinSession;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public final class AppTexts {

    private static final String ATTR = "uzmark.lang";
    private static final List<String> CODES = List.of("uz", "ru", "en");
    private static final Map<String, String[]> ROWS = new HashMap<>();

    private AppTexts() {
    }

    // ------------------------------------------------------------------ til

    public static String lang() {
        VaadinSession s = VaadinSession.getCurrent();
        Object v = s == null ? null : s.getAttribute(ATTR);
        return (v instanceof String l && CODES.contains(l)) ? l : "uz";
    }

    public static void setLang(String code) {
        VaadinSession s = VaadinSession.getCurrent();
        if (s != null && CODES.contains(code)) {
            s.setAttribute(ATTR, code);
        }
    }

    /** Joriy til indeksi: uz=0, ru=1, en=2. */
    public static int index() {
        int i = CODES.indexOf(lang());
        return i < 0 ? 0 : i;
    }

    public static String fullName(String code) {
        return switch (code) {
            case "ru" -> "Русский";
            case "en" -> "English";
            default -> "O'zbekcha";
        };
    }

    public static List<String> codes() {
        return CODES;
    }

    // ------------------------------------------------------------------ matn olish

    public static String t(String key) {
        return get(key, lang());
    }

    /** String.format bilan: AppTexts.f("wz.selected", 3). */
    public static String f(String key, Object... args) {
        return String.format(Locale.ROOT, t(key), args);
    }

    public static String get(String key, String lang) {
        String[] r = ROWS.get(key);
        if (r == null) {
            return key;
        }
        int i = CODES.indexOf(lang);
        return r[i < 0 ? 0 : i];
    }

    private static void row(String key, String uz, String ru, String en) {
        ROWS.put(key, new String[] {uz, ru, en});
    }

    static {
        // ---------------- ilova qobig'i
        row("shell.dashboard", "Bosh sahifa", "Главная", "Dashboard");
        row("shell.modules", "Modullar", "Модули", "Modules");
        row("shell.search", "Qidirish...", "Поиск...", "Search...");
        row("shell.company", "Kompaniya", "Компания", "Company");
        row("shell.menu", "Menyu", "Меню", "Menu");
        row("shell.lang", "Til", "Язык", "Language");
        row("shell.profile", "Profil", "Профиль", "Profile");
        row("shell.logout", "Chiqish", "Выйти", "Sign out");
        row("shell.user", "Foydalanuvchi", "Пользователь", "User");
        row("shell.welcome", "Xush kelibsiz", "Добро пожаловать", "Welcome");
        row("shell.welcome.sub", "Kerakli modulni tanlang va ishni boshlang.",
                "Выберите модуль и начните работу.", "Pick a module to get started.");
        row("shell.open", "Ochish", "Открыть", "Open");
        row("shell.sections", "Bo'limlar", "Разделы", "Sections");
        row("shell.sec.new", "Yangi qo'shish", "Добавить", "Add new");
        row("shell.overview", "Umumiy ko'rinish", "Обзор", "Overview");
        row("rec.add", "Yangi yozuv", "Новая запись", "New record");
        row("rec.edit", "Tahrirlash", "Редактировать", "Edit");
        row("rec.delete", "O'chirish", "Удалить", "Delete");
        row("rec.save", "Saqlash", "Сохранить", "Save");
        row("rec.cancel", "Bekor qilish", "Отмена", "Cancel");
        row("rec.search", "Qidirish...", "Поиск...", "Search...");
        row("rec.required", "To'ldirish shart", "Обязательное поле", "Required");
        row("rec.saved", "Saqlandi", "Сохранено", "Saved");
        row("rec.deleted", "O'chirildi", "Удалено", "Deleted");
        row("rec.empty.t", "Hali ma'lumot yo'q", "Данных пока нет", "No data yet");
        row("rec.empty.d", "Birinchi yozuvni qo'shing, u shu yerda ko'rinadi.",
                "Добавьте первую запись — она появится здесь.", "Add your first record and it will show up here.");
        row("rec.confirm.t", "Yozuvni o'chiramizmi?", "Удалить запись?", "Delete this record?");
        row("rec.confirm.d", "Bu amalni qaytarib bo'lmaydi.", "Это действие нельзя отменить.",
                "This can't be undone.");
        row("rec.count", "Yozuvlar: %d", "Записей: %d", "Records: %d");
        row("rec.total", "Jami: %s", "Итого: %s", "Total: %s");
        row("rec.cur", "so'm", "сум", "UZS");
        row("rec.nomatch", "Hech narsa topilmadi", "Ничего не найдено", "Nothing found");
        row("rec.err.save", "Saqlab bo'lmadi. Qayta urinib ko'ring.", "Не удалось сохранить. Попробуйте снова.",
                "Couldn't save. Please try again.");

        row("rep.nodata", "Hozircha ma'lumot yo'q", "Данных пока нет", "No data yet");
        row("rep.hint", "Hisobot boshqa bo'limlardagi yozuvlardan avtomatik yig'iladi.",
                "Отчёт собирается автоматически из записей других разделов.",
                "This report is built automatically from records in other sections.");
        row("rep.billed", "Yozilgan hisob-fakturalar", "Выставлено счетов", "Invoiced");
        row("rep.income", "Qabul qilingan to'lovlar", "Полученные платежи", "Payments received");
        row("rep.expenses", "Xarajatlar", "Расходы", "Expenses");
        row("rep.profit", "Foyda", "Прибыль", "Profit");
        row("rep.receivable", "Qarzdorlik", "Задолженность", "Receivables");
        row("rep.margin", "Rentabellik", "Рентабельность", "Margin");
        row("rep.customers", "Mijozlar", "Клиенты", "Customers");
        row("rep.opendeals", "Ochiq bitimlar", "Открытые сделки", "Open deals");
        row("rep.products", "Mahsulotlar", "Товары", "Products");
        row("rep.employees", "Xodimlar", "Сотрудники", "Employees");
        row("rep.dealsum", "Bitimlar summasi", "Сумма сделок", "Deals value");
        row("rep.won", "Yutilgan bitimlar", "Выигранные сделки", "Won deals");
        row("rep.winrate", "Yutish ulushi", "Доля выигранных", "Win rate");
        row("rep.by.expense", "Xarajatlar toifalar bo'yicha", "Расходы по категориям", "Expenses by category");
        row("rep.by.invoice", "Hisob-fakturalar holati bo'yicha", "Счета по статусам", "Invoices by status");
        row("rep.by.stage", "Bitimlar bosqichlar bo'yicha", "Сделки по этапам", "Deals by stage");
        row("rep.by.customer", "Mijozlar bo'yicha bitimlar", "Сделки по клиентам", "Deals by customer");
        row("rep.by.method", "To'lovlar usullar bo'yicha", "Платежи по способам", "Payments by method");
        row("wz.err.fullname", "Ism va familiyani kiriting (masalan: Dilshod Karimov)",
                "Введите имя и фамилию (например: Dilshod Karimov)", "Enter first and last name (e.g. Dilshod Karimov)");
        row("wz.err.cooldown", "Kodni qayta yuborish %d soniyadan keyin mumkin",
                "Повторно отправить код можно через %d сек.", "You can resend the code in %d seconds");
        row("wz.err.limit", "Juda ko'p urinish. Keyinroq qayta urinib ko'ring.",
                "Слишком много попыток. Повторите позже.", "Too many attempts. Please try again later.");
        row("wz.err.taken", "Bu email bilan hisob allaqachon mavjud",
                "Аккаунт с таким email уже существует", "An account with this email already exists");
        row("wz.s2.hint", "Kod kelmadimi? Spam papkasini ham tekshirib ko'ring.",
                "Код не пришёл? Проверьте также папку «Спам».", "Didn't get it? Check your spam folder too.");
        row("wz.done.slug", "Kompaniya yaratildi! Kirish uchun kompaniya nomi: %s",
                "Компания создана! Название компании для входа: %s",
                "Company created! Company name for sign-in: %s");
        row("guide.title", "Bu bo'lim nima uchun?", "Для чего этот раздел?", "What is this section for?");
        row("guide.what", "Vazifasi", "Назначение", "Purpose");
        row("guide.enter", "Nima kiritiladi", "Что вводить", "What to enter");
        row("guide.use", "Qayerda ishlatiladi", "Где используется", "Where it is used");
        row("set.menu", "Modullarni boshqarish", "Управление модулями", "Manage modules");
        row("set.title", "Modullarni boshqarish", "Управление модулями", "Manage modules");
        row("set.sub", "Faqat belgilangan modullar chap menyuda ko'rinadi. Istalgan vaqt o'zgartirish mumkin.",
                "В меню слева отображаются только отмеченные модули. Менять можно в любой момент.",
                "Only checked modules appear in the left menu. You can change this anytime.");
        row("set.save", "Saqlash", "Сохранить", "Save");
        row("set.saved", "Modullar yangilandi", "Модули обновлены", "Modules updated");
        row("set.min", "Kamida bitta modul tanlang", "Выберите хотя бы один модуль", "Pick at least one module");
        row("u.menu", "Xodimlar va rollar", "Сотрудники и роли", "Staff & roles");
        row("u.title", "Xodimlar va rollar", "Сотрудники и роли", "Staff & roles");
        row("u.sub", "Xodim qo'shing, unga rol bering. Kirish nomi va parolni tizim o'zi yaratadi.",
                "Добавьте сотрудника и назначьте роль. Имя пользователя и пароль система создаст сама.",
                "Add a team member and assign a role. The username and password are generated for you.");
        row("u.roles", "Rollar", "Роли", "Roles");
        row("u.add", "Yangi xodim", "Новый сотрудник", "New member");
        row("u.fullname", "Ism familiya", "Имя и фамилия", "Full name");
        row("u.role", "Rol", "Роль", "Role");
        row("u.email", "Email (ixtiyoriy)", "Email (необязательно)", "Email (optional)");
        row("u.email.hint", "Bo'sh qoldirsangiz, kirish nomi ism-familiyadan o'zi yaratiladi.",
                "Если оставить пустым, имя пользователя создастся из имени и фамилии.",
                "If left empty, the username is generated from the name.");
        row("u.create", "Yaratish", "Создать", "Create");
        row("u.cred.title", "Kirish ma'lumotlari tayyor", "Данные для входа готовы", "Sign-in details are ready");
        row("u.reset.title", "Yangi parol yaratildi", "Создан новый пароль", "New password created");
        row("u.cred.note", "Parol faqat hozir ko'rsatiladi, keyin ko'rib bo'lmaydi. Xodimga xavfsiz yo'l bilan yetkazing: u kompaniya nomi, foydalanuvchi nomi va parol bilan kiradi.",
                "Пароль показывается только сейчас и позже недоступен. Передайте его сотруднику безопасным способом: вход по названию компании, имени пользователя и паролю.",
                "The password is shown only now and can't be viewed later. Share it securely: the member signs in with company name, username and password.");
        row("u.company", "Kompaniya", "Компания", "Company");
        row("u.username", "Foydalanuvchi nomi", "Имя пользователя", "Username");
        row("u.password", "Parol", "Пароль", "Password");
        row("u.copy", "Nusxalash", "Копировать", "Copy");
        row("u.copied", "Nusxalandi", "Скопировано", "Copied");
        row("u.close", "Yopish", "Закрыть", "Close");
        row("u.col.name", "Xodim", "Сотрудник", "Member");
        row("u.col.login", "Kirish nomi", "Логин", "Username");
        row("u.col.role", "Rol", "Роль", "Role");
        row("u.col.status", "Holat", "Статус", "Status");
        row("u.col.last", "Oxirgi kirish", "Последний вход", "Last sign-in");
        row("u.active", "Faol", "Активен", "Active");
        row("u.blocked", "Bloklangan", "Заблокирован", "Blocked");
        row("u.never", "Hali kirmagan", "Ещё не входил", "Never");
        row("u.you", "Siz", "Вы", "You");
        row("u.changerole", "Rol", "Роль", "Role");
        row("u.reset", "Yangi parol", "Новый пароль", "New password");
        row("u.block", "Bloklash", "Заблокировать", "Block");
        row("u.unblock", "Faollashtirish", "Разблокировать", "Unblock");
        row("u.err.name", "Ism va familiyani yozing", "Укажите имя и фамилию", "Enter first and last name");
        row("u.err.taken", "Bu email band", "Этот email уже занят", "This email is taken");
        row("u.err.fail", "Bajarib bo'lmadi. Qayta urinib ko'ring.", "Не удалось выполнить. Попробуйте снова.",
                "Couldn't complete. Please try again.");
        row("shell.wip.t", "Bu bo'lim tayyorlanmoqda", "Раздел в разработке", "This section is being built");
        row("shell.wip.d", "Modul tez orada shu yerda ochiladi.", "Модуль скоро появится здесь.",
                "The module will appear here soon.");

        // ---------------- modullar
        row("mod.finance.t", "Moliya", "Финансы", "Finance");
        row("mod.finance.d", "Hisob-kitob, to'lov va hisobotlar", "Учёт, платежи и отчёты",
                "Accounting, payments and reports");
        row("mod.finance.f", "Hisob-fakturalar · To'lovlar · Balans", "Счета · Платежи · Баланс",
                "Invoices · Payments · Balance");

        row("mod.crm.t", "CRM", "CRM", "CRM");
        row("mod.crm.d", "Mijozlar, bitimlar va savdo voronkasi", "Клиенты, сделки и воронка продаж",
                "Customers, deals and sales pipeline");
        row("mod.crm.f", "Mijozlar bazasi · Bitimlar · Eslatmalar", "База клиентов · Сделки · Напоминания",
                "Customer base · Deals · Reminders");

        row("mod.inventory.t", "Ombor", "Склад", "Inventory");
        row("mod.inventory.d", "Mahsulot qoldig'i, kirim va chiqim", "Остатки, приход и расход товаров",
                "Stock levels, receipts and issues");
        row("mod.inventory.f", "Qoldiq · Kirim-chiqim · Inventarizatsiya", "Остатки · Приход-расход · Инвентаризация",
                "Stock · In/Out · Stocktake");

        row("mod.pos.t", "Kassa (POS)", "Касса (POS)", "POS");
        row("mod.pos.d", "Skaner orqali tez savdo, kassa va chek sozlamalari",
                "Быстрая продажа через сканер, настройки кассы и чека",
                "Fast scanner sales, register and receipt settings");
        row("mod.pos.f", "Scanner savdolari · Sozlamalar", "Продажи по сканеру · Настройки",
                "Scanner sales · Settings");

        row("mod.hr.t", "Kadrlar", "Кадры", "HR");
        row("mod.hr.d", "Xodimlar, ish haqi va ta'tillar", "Сотрудники, зарплата и отпуска",
                "Staff, payroll and leave");
        row("mod.hr.f", "Xodimlar · Ish haqi · Davomat", "Сотрудники · Зарплата · Табель",
                "Employees · Payroll · Attendance");

        row("mod.bi.t", "BI", "BI", "BI");
        row("mod.bi.d", "Grafiklar va tahliliy hisobotlar", "Графики и аналитические отчёты",
                "Charts and analytical reports");
        row("mod.bi.f", "Dashboardlar · Tahlil · Eksport", "Дашборды · Аналитика · Экспорт",
                "Dashboards · Analytics · Export");

        // ---------------- ro'yxatdan o'tish (wizard)
        row("wz.signin.q", "Hisobingiz bormi?", "Уже есть аккаунт?", "Already have an account?");
        row("wz.signin", "Kirish", "Войти", "Sign in");
        row("wz.step1", "Kompaniya", "Компания", "Company");
        row("wz.step2", "Tasdiqlash", "Подтверждение", "Verification");
        row("wz.step3", "Modullar", "Модули", "Modules");
        row("wz.stepB", "Do'kon turi", "Тип магазина", "Store type");
        row("wz.sb.title", "Qanday do'kon yuritasiz?", "Какой у вас магазин?", "What kind of store do you run?");
        row("wz.sb.sub", "Bir nechtasini tanlash mumkin. Mahsulot maydonlari tanlovingizga moslanadi.",
                "Можно выбрать несколько. Поля товара подстроятся под ваш выбор.",
                "You can pick several. Product fields adapt to your choice.");
        row("wz.sb.err", "Kamida bitta turni tanlang", "Выберите хотя бы один тип", "Pick at least one type");
        row("wz.back", "Orqaga", "Назад", "Back");
        row("wz.s1.title", "Kompaniyangizni ro'yxatdan o'tkazing", "Зарегистрируйте компанию",
                "Register your company");
        row("wz.s1.sub", "Ma'lumotlarni kiriting, keyingi qadamda emailingizni tasdiqlaymiz.",
                "Введите данные, на следующем шаге подтвердим email.",
                "Enter your details; we'll verify your email next.");
        row("wz.sec.company", "Kompaniya ma'lumotlari", "Данные компании", "Company details");
        row("wz.sec.admin", "Administrator", "Администратор", "Administrator");
        row("wz.company", "Kompaniya nomi", "Название компании", "Company name");
        row("wz.taxid", "STIR (ixtiyoriy)", "ИНН (необязательно)", "Tax ID (optional)");
        row("wz.fullname", "Ism familiya", "Имя и фамилия", "Full name");
        row("wz.email", "Email", "Email", "Email");
        row("wz.phone", "Telefon (ixtiyoriy)", "Телефон (необязательно)", "Phone (optional)");
        row("wz.password", "Parol", "Пароль", "Password");
        row("wz.password2", "Parolni takrorlang", "Повторите пароль", "Repeat password");
        row("wz.pwhint", "Kamida 8 ta belgi", "Минимум 8 символов", "At least 8 characters");
        row("wz.terms", "Foydalanish shartlari va maxfiylik siyosatiga roziman",
                "Согласен с условиями использования и политикой конфиденциальности",
                "I agree to the terms of use and privacy policy");
        row("wz.next", "Davom etish", "Продолжить", "Continue");

        row("wz.err.required", "To'ldirish shart", "Обязательное поле", "Required");
        row("wz.err.email", "Email noto'g'ri", "Некорректный email", "Invalid email");
        row("wz.err.taxid", "STIR 9 ta raqam bo'lishi kerak", "ИНН — 9 цифр", "Tax ID must be 9 digits");
        row("wz.err.pwmatch", "Parollar mos emas", "Пароли не совпадают", "Passwords don't match");
        row("wz.err.terms", "Davom etish uchun rozilik bering", "Для продолжения необходимо согласие",
                "Please accept to continue");
        row("wz.err.send", "Kod yuborib bo'lmadi. Keyinroq urinib ko'ring.",
                "Не удалось отправить код. Попробуйте позже.", "Couldn't send the code. Try again later.");

        row("wz.s2.title", "Emailni tasdiqlang", "Подтвердите email", "Verify your email");
        row("wz.s2.sub", "6 xonali kod %s manziliga yuborildi.", "6-значный код отправлен на %s.",
                "A 6-digit code was sent to %s.");
        row("wz.code", "Tasdiqlash kodi", "Код подтверждения", "Verification code");
        row("wz.verify", "Tasdiqlash", "Подтвердить", "Verify");
        row("wz.err.code", "Kod noto'g'ri yoki muddati o'tgan", "Неверный код или срок истёк",
                "Wrong or expired code");
        row("wz.resend", "Kodni qayta yuborish", "Отправить код ещё раз", "Resend code");
        row("wz.resent", "Kod qayta yuborildi", "Код отправлен повторно", "Code sent again");
        row("wz.changeemail", "Emailni o'zgartirish", "Изменить email", "Change email");

        row("wz.s3.title", "Kerakli modullarni tanlang", "Выберите нужные модули", "Choose your modules");
        row("wz.s3.sub", "Faqat tanlangan modullar yoqiladi. Keyin istalgan vaqt qo'shish yoki o'chirish mumkin.",
                "Включатся только выбранные модули. Позже их можно добавить или отключить.",
                "Only the modules you pick are enabled. You can add or remove them anytime.");
        row("wz.selectall", "Hammasini tanlash", "Выбрать все", "Select all");
        row("wz.clear", "Tanlovni bekor qilish", "Снять выбор", "Clear all");
        row("wz.selected", "Tanlandi: %d", "Выбрано: %d", "Selected: %d");
        row("wz.verified", "Email tasdiqlandi", "Email подтверждён", "Email verified");
        row("wz.finish", "Ro'yxatdan o'tishni yakunlash", "Завершить регистрацию", "Finish registration");
        row("wz.err.modules", "Kamida bitta modul tanlang", "Выберите хотя бы один модуль",
                "Pick at least one module");
        row("wz.err.finish", "Ro'yxatdan o'tkazib bo'lmadi. Qayta urinib ko'ring.",
                "Не удалось завершить регистрацию. Попробуйте снова.",
                "Couldn't complete registration. Please try again.");
        row("wz.done", "Kompaniya yaratildi! Endi tizimga kiring.", "Компания создана! Теперь войдите.",
                "Company created! Please sign in.");
    }
}