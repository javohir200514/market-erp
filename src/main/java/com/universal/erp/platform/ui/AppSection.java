package com.universal.erp.platform.ui;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Modulning ichki bo'limi.
 *
 * Yangi bo'lim qo'shish — ALL ro'yxatiga bitta qator qo'shish.
 * Manzil: /app/{modul}/{bo'lim}
 *
 * Bog'lash: maydon turi "ref", oxirgi qism — maqsad bo'lim kaliti.
 *   "*customer|ref|Mijoz|Клиент|Customer|crm/customers"
 */
public final class AppSection {

    public static final List<AppSection> ALL = new ArrayList<>();

    static {
        // ---------------- MOLIYA ----------------

        add(AppModule.FINANCE, "invoices",
                "Nasiya", "Счета", "Invoices",
                "Mijozlarga yozilgan nasiyalar va ularning to'lov holati",
                "Счета, выставленные клиентам, и статус их оплаты",
                "Invoices issued to customers and their payment status")
                .fields(
                        "*number|text|Raqam|Номер|Number",
                        "*customer|ref|Mijoz|Клиент|Customer|crm/customers",
                        "date|date|Sana|Дата|Date",
                        "due|date|To'lov muddati|Срок оплаты|Due date",
                        "*amount|money|Summa|Сумма|Amount",
                        "status|select|Holat|Статус|Status|draft:Qoralama:Черновик:Draft,sent:Yuborildi:Отправлен:Sent,paid:To'landi:Оплачен:Paid,overdue:Muddati o'tgan:Просрочен:Overdue"
                ).sum("amount");

        add(AppModule.FINANCE, "payments",
                "To'lovlar", "Платежи", "Payments",
                "Kelgan va chiqqan to'lovlar, qarzdorlik",
                "Входящие и исходящие платежи, задолженность",
                "Incoming and outgoing payments, receivables")
                .fields(
                        "date|date|Sana|Дата|Date",
                        "*payer|ref|To'lovchi|Плательщик|Payer|crm/customers",
                        "*amount|money|Summa|Сумма|Amount",
                        "method|select|Usul|Способ|Method|cash:Naqd:Наличные:Cash,card:Karta:Карта:Card,transfer:O'tkazma:Перевод:Transfer",
                        "invoice|ref|Hisob-faktura|Счёт|Invoice|finance/invoices",
                        "account|ref|Kassa / bank|Касса / банк|Cash / bank account|finance/accounts",
                        "note|long|Izoh|Примечание|Note"
                ).sum("amount");

        add(AppModule.FINANCE, "expenses",
                "Xarajatlar", "Расходы", "Expenses",
                "Kompaniya xarajatlari toifalar bo'yicha",
                "Расходы компании по категориям",
                "Company expenses by category")
                .fields(
                        "date|date|Sana|Дата|Date",
                        "*category|select|Toifa|Категория|Category|rent:Ijara:Аренда:Rent,salary:Ish haqi:Зарплата:Salary,tax:Soliq:Налоги:Tax,transport:Transport:Транспорт:Transport,marketing:Marketing:Маркетинг:Marketing,other:Boshqa:Другое:Other",
                        "*amount|money|Summa|Сумма|Amount",
                        "account|ref|Kassa / bank|Касса / банк|Cash / bank account|finance/accounts",
                        "description|text|Tavsif|Описание|Description"
                ).sum("amount");

        add(AppModule.FINANCE, "accounts",
                "Kassa va bank", "Касса и банк", "Cash & bank",
                "Hisobraqamlar va kassa qoldig'i",
                "Расчётные счета и остаток кассы",
                "Bank accounts and cash balances")
                .fields(
                        "*name|text|Nomi|Название|Name",
                        "kind|select|Turi|Тип|Type|cash:Kassa:Касса:Cash,bank:Bank:Банк:Bank",
                        "number|text|Hisobraqam|Номер счёта|Account no.",
                        "*balance|money|Qoldiq|Остаток|Balance",
                        "currency|select|Valyuta|Валюта|Currency|UZS:UZS:UZS:UZS,USD:USD:USD:USD"
                );

        add(AppModule.FINANCE, "reports",
                "Hisobotlar", "Отчёты", "Reports",
                "Foyda, zarar va pul oqimi hisobotlari",
                "Отчёты о прибыли, убытках и движении денег",
                "Profit, loss and cash-flow reports")
                .report();

        // ---------------- CRM ----------------

        add(AppModule.CRM, "customers",
                "Mijozlar", "Клиенты", "Customers",
                "Mijozlar bazasi va aloqa tarixi",
                "База клиентов и история контактов",
                "Customer database and contact history")
                .fields(
                        "*name|text|Ism / kompaniya|Имя / компания|Name / company",
                        "phone|text|Telefon|Телефон|Phone",
                        "email|text|Email|Email|Email",
                        "status|select|Holat|Статус|Status|lead:Potensial:Потенциальный:Lead,active:Faol:Активный:Active,inactive:Nofaol:Неактивный:Inactive",
                        "note|long|Izoh|Примечание|Note"
                );

        add(AppModule.CRM, "deals",
                "Bitimlar", "Сделки", "Deals",
                "Savdo voronkasi va bitimlar bosqichlari",
                "Воронка продаж и этапы сделок",
                "Sales pipeline and deal stages")
                .fields(
                        "*title|text|Bitim nomi|Название сделки|Deal title",
                        "customer|ref|Mijoz|Клиент|Customer|crm/customers",
                        "*amount|money|Summa|Сумма|Amount",
                        "stage|select|Bosqich|Этап|Stage|new:Yangi:Новая:New,negotiation:Muzokara:Переговоры:Negotiation,proposal:Taklif:Предложение:Proposal,won:Yutildi:Выиграна:Won,lost:Yo'qotildi:Проиграна:Lost",
                        "close|date|Yopilish sanasi|Дата закрытия|Close date"
                ).sum("amount");

        add(AppModule.CRM, "tasks",
                "Vazifalar", "Задачи", "Tasks",
                "Qo'ng'iroq, uchrashuv va eslatmalar",
                "Звонки, встречи и напоминания",
                "Calls, meetings and reminders")
                .fields(
                        "*title|text|Vazifa|Задача|Task",
                        "due|date|Muddat|Срок|Due",
                        "priority|select|Muhimlik|Приоритет|Priority|low:Past:Низкий:Low,medium:O'rta:Средний:Medium,high:Yuqori:Высокий:High",
                        "status|select|Holat|Статус|Status|todo:Rejada:В плане:To do,doing:Jarayonda:В работе:In progress,done:Bajarildi:Готово:Done",
                        "assignee|text|Mas'ul|Ответственный|Assignee"
                );

        add(AppModule.CRM, "price-lists",
                "Narxnomalar", "Прайс-листы", "Price lists",
                "Mahsulot va xizmat narxlari, chegirmalar",
                "Цены на товары и услуги, скидки",
                "Product and service prices, discounts")
                .fields(
                        "*product|text|Mahsulot / xizmat|Товар / услуга|Product / service",
                        "*price|money|Narx|Цена|Price",
                        "unit|text|Birlik|Единица|Unit",
                        "discount|number|Chegirma, %|Скидка, %|Discount, %",
                        "valid|date|Amal qilish muddati|Действует до|Valid until"
                );

        // ---------------- OMBOR ----------------

        add(AppModule.INVENTORY, "products",
                "Mahsulotlar", "Товары", "Products",
                "Mahsulotlar ro'yxati, kodlar va o'lchov birliklari",
                "Список товаров, коды и единицы измерения",
                "Product list, codes and units of measure")
                .fields(
                        "sku|text|Ichki kod|Внутренний код|Internal code",
                        "barcode|text|Shtrix-kod|Штрихкод|Barcode",
                        "*name|text|Nomi|Название|Name",
                        "category|select|Toifa|Категория|Category|food:Oziq-ovqat:Продукты:Food,drinks:Ichimliklar:Напитки:Drinks,household:Xo'jalik mollari:Хозтовары:Household,electronics:Elektronika:Электроника:Electronics,clothes:Kiyim:Одежда:Clothes,other:Boshqa:Другое:Other",
                        "unit|select|Birlik|Единица|Unit|pcs:dona:шт:pcs,kg:kg:кг:kg,l:litr:л:l,m:metr:м:m,box:quti:коробка:box",
                        "cost|money|Tannarxi|Себестоимость|Cost price",
                        "*price|money|Sotish narxi|Цена продажи|Sale price",
                        "image|text|Rasm|Фото|Image"
                );

        add(AppModule.INVENTORY, "stock",
                "Qoldiq", "Остатки", "Stock",
                "Omborlar bo'yicha joriy qoldiq",
                "Текущие остатки по складам",
                "Current stock by warehouse")
                .fields(
                        "*product|text|Mahsulot|Товар|Product",
                        "warehouse|text|Ombor|Склад|Warehouse",
                        "*quantity|number|Miqdor|Количество|Quantity",
                        "min|number|Minimal qoldiq|Мин. остаток|Min. stock"
                );

        add(AppModule.INVENTORY, "receipts",
                "Kirim", "Приход", "Receipts",
                "Yetkazib beruvchidan kelgan tovarlar",
                "Товары, поступившие от поставщиков",
                "Goods received from suppliers")
                .fields(
                        "date|date|Sana|Дата|Date",
                        "supplier|text|Yetkazib beruvchi|Поставщик|Supplier",
                        "*product|text|Mahsulot|Товар|Product",
                        "*quantity|number|Miqdor|Количество|Quantity",
                        "price|money|Narx|Цена|Price"
                );

        add(AppModule.INVENTORY, "issues",
                "Chiqim", "Расход", "Issues",
                "Sotuv va ichki sarf bo'yicha chiqim",
                "Списание по продажам и внутреннему расходу",
                "Outgoing goods for sales and internal use")
                .fields(
                        "date|date|Sana|Дата|Date",
                        "customer|text|Mijoz|Клиент|Customer",
                        "*product|text|Mahsulot|Товар|Product",
                        "*quantity|number|Miqdor|Количество|Quantity",
                        "reason|select|Sabab|Причина|Reason|sale:Sotuv:Продажа:Sale,internal:Ichki sarf:Внутренний расход:Internal use,writeoff:Hisobdan chiqarish:Списание:Write-off"
                );

        add(AppModule.INVENTORY, "stocktake",
                "Inventarizatsiya", "Инвентаризация", "Stocktake",
                "Haqiqiy va hisobdagi qoldiqni solishtirish",
                "Сверка фактических и учётных остатков",
                "Compare actual and recorded stock")
                .fields(
                        "date|date|Sana|Дата|Date",
                        "warehouse|text|Ombor|Склад|Warehouse",
                        "*product|text|Mahsulot|Товар|Product",
                        "recorded|number|Hisobda|По учёту|Recorded",
                        "actual|number|Haqiqatda|Фактически|Actual",
                        "note|long|Izoh|Примечание|Note"
                );

        // ---------------- POS ----------------

        add(AppModule.POS, "terminal",
                "Kassa", "Касса", "Terminal",
                "Shtrix-kodni skanerlab savdo qilish",
                "Продажа со сканированием штрихкода",
                "Sell by scanning barcodes");

        add(AppModule.POS, "sales",
                "Scanner savdolari", "Продажи по сканеру", "Scanner sales",
                "Shtrix-kod skaneri orqali kassada yopilgan savdolar",
                "Продажи, закрытые на кассе через сканер штрихкодов",
                "Sales closed at the register via barcode scanner")
                .fields(
                        "*number|text|Raqam|Номер|Number",
                        "date|date|Sana|Дата|Date",
                        "time|text|Vaqt|Время|Time",
                        "method|select|To'lov usuli|Способ оплаты|Payment method|cash:Naqd:Наличные:Cash,card:Karta:Карта:Card",
                        "*amount|money|Summa|Сумма|Amount",
                        "received|money|Qabul qilindi|Принято|Received",
                        "change|money|Qaytim|Сдача|Change",
                        "cashier|text|Kassir|Кассир|Cashier",
                        "items|long|Mahsulotlar|Товары|Items"
                ).sum("amount");

        add(AppModule.POS, "settings",
                "Kassa sozlamalari", "Настройки кассы", "Register settings",
                "Kassa, skaner va global katalog sozlamalari",
                "Настройки кассы, сканера и глобального каталога",
                "Register, scanner and global catalog settings");

        // ---------------- KADRLAR ----------------

        add(AppModule.HR, "employees",
                "Xodimlar", "Сотрудники", "Employees",
                "Xodimlar ro'yxati, lavozim va shartnomalar",
                "Список сотрудников, должности и договоры",
                "Employee list, positions and contracts")
                .fields(
                        "*name|text|F.I.Sh.|Ф.И.О.|Full name",
                        "position|text|Lavozim|Должность|Position",
                        "phone|text|Telefon|Телефон|Phone",
                        "hired|date|Ishga olingan sana|Дата приёма|Hire date",
                        "salary|money|Oylik|Оклад|Salary",
                        "status|select|Holat|Статус|Status|active:Ishlayapti:Работает:Active,vacation:Ta'tilda:В отпуске:On leave,fired:Bo'shagan:Уволен:Left"
                );

        add(AppModule.HR, "attendance",
                "Davomat", "Табель", "Attendance",
                "Ish vaqti va kelib-ketish hisobi",
                "Учёт рабочего времени и посещаемости",
                "Working hours and attendance")
                .fields(
                        "*employee|text|Xodim|Сотрудник|Employee",
                        "date|date|Sana|Дата|Date",
                        "in|text|Kelish|Приход|Check-in",
                        "out|text|Ketish|Уход|Check-out",
                        "status|select|Holat|Статус|Status|present:Keldi:Пришёл:Present,late:Kechikdi:Опоздал:Late,absent:Kelmadi:Отсутствует:Absent"
                );

        add(AppModule.HR, "payroll",
                "Ish haqi", "Зарплата", "Payroll",
                "Oylik hisoblash, avans va ushlanmalar",
                "Расчёт зарплаты, авансы и удержания",
                "Salary calculation, advances and deductions")
                .fields(
                        "*employee|text|Xodim|Сотрудник|Employee",
                        "month|text|Oy (2026-11)|Месяц (2026-11)|Month (2026-11)",
                        "*base|money|Asosiy|Оклад|Base",
                        "bonus|money|Mukofot|Премия|Bonus",
                        "deductions|money|Ushlanma|Удержания|Deductions",
                        "status|select|Holat|Статус|Status|draft:Hisoblangan:Начислено:Calculated,paid:To'langan:Выплачено:Paid"
                );

        add(AppModule.HR, "leaves",
                "Ta'tillar", "Отпуска", "Leaves",
                "Ta'til va kasallik ruxsatnomalari",
                "Отпуска и больничные",
                "Vacations and sick leave")
                .fields(
                        "*employee|text|Xodim|Сотрудник|Employee",
                        "type|select|Turi|Тип|Type|vacation:Ta'til:Отпуск:Vacation,sick:Kasallik:Больничный:Sick leave,unpaid:Haq to'lanmaydigan:Без содержания:Unpaid",
                        "from|date|Boshlanishi|Начало|From",
                        "to|date|Tugashi|Конец|To",
                        "status|select|Holat|Статус|Status|pending:Kutilmoqda:Ожидает:Pending,approved:Tasdiqlandi:Одобрено:Approved,rejected:Rad etildi:Отклонено:Rejected"
                );

        // ---------------- BI ----------------

        add(AppModule.BI, "dashboard",
                "Boshqaruv paneli", "Панель управления", "Dashboard",
                "Asosiy ko'rsatkichlar bir sahifada",
                "Ключевые показатели на одной странице",
                "Key metrics on one page")
                .report();

        add(AppModule.BI, "sales",
                "Savdo tahlili", "Анализ продаж", "Sales analytics",
                "Savdo dinamikasi, mijoz va mahsulot kesimida",
                "Динамика продаж по клиентам и товарам",
                "Sales trends by customer and product")
                .report();

        add(AppModule.BI, "finance",
                "Moliyaviy tahlil", "Финансовый анализ", "Financial analytics",
                "Foyda, xarajat va rentabellik tahlili",
                "Анализ прибыли, расходов и рентабельности",
                "Profit, cost and margin analysis")
                .report();

        // Barcha bo'limlar qo'shilgandan keyin REF maqsadlarini tekshiramiz
        validateRefs();
    }

    private final AppModule module;
    private final String slug;
    private final String[] title;
    private final String[] desc;

    private final List<FieldDef> fields = new ArrayList<>();

    private String sumKey;
    private boolean report;

    private AppSection(
            AppModule module,
            String slug,
            String[] title,
            String[] desc
    ) {
        this.module = module;
        this.slug = slug;
        this.title = title;
        this.desc = desc;
    }

    private static AppSection add(
            AppModule module,
            String slug,
            String titleUz,
            String titleRu,
            String titleEn,
            String descUz,
            String descRu,
            String descEn
    ) {
        AppSection section = new AppSection(
                module,
                slug,
                new String[]{titleUz, titleRu, titleEn},
                new String[]{descUz, descRu, descEn}
        );

        ALL.add(section);
        return section;
    }

    private AppSection fields(String... specs) {
        for (String spec : specs) {
            fields.add(FieldDef.of(spec));
        }
        return this;
    }

    private AppSection sum(String key) {
        this.sumKey = key;
        return this;
    }

    private AppSection report() {
        this.report = true;
        return this;
    }

    /**
     * Har bir REF maydonining maqsad bo'limi mavjudligini tekshiradi.
     * Xato bo'lsa ilova ishga tushishda darrov yiqiladi (noto'g'ri kalit keyin sezilmay qolmasligi uchun).
     */
    private static void validateRefs() {
        for (AppSection section : ALL) {
            for (FieldDef f : section.fields) {
                if (!f.isRef()) {
                    continue;
                }
                boolean exists = ALL.stream().anyMatch(s -> s.key().equals(f.target()));
                if (!exists) {
                    throw new IllegalStateException(
                            "REF maqsadi topilmadi: " + section.key() + "." + f.key()
                                    + " -> " + f.target());
                }
            }
        }
    }

    public AppModule module() {
        return module;
    }

    public String slug() {
        return slug;
    }

    /**
     * Bazadagi bo'lim kaliti: finance/invoices
     */
    public String key() {
        return module.key() + "/" + slug;
    }

    /**
     * To'liq marshrut: app/finance/invoices
     */
    public String route() {
        return "app/" + module.key() + "/" + slug;
    }

    public String title() {
        return title[AppTexts.index()];
    }

    public String description() {
        return desc[AppTexts.index()];
    }

    public List<FieldDef> fields() {
        return fields;
    }

    /**
     * Yo'nalishga qarab kengaytirilgan maydonlar. Hozircha faqat Ombor → Mahsulotlar.
     * Qo'shimcha atributlar mavjud maydon kalitlari bilan to'qnashmaydi.
     */
    public List<FieldDef> fields(Set<BusinessType> types) {
        if (types == null || types.isEmpty() || !"inventory/products".equals(key())) {
            return fields;
        }
        List<FieldDef> all = new ArrayList<>(fields);
        for (String spec : AttributeCatalog.specsFor(types)) {
            FieldDef extra = FieldDef.of(spec);
            if (field(extra.key()) == null) {
                all.add(extra);
            }
        }
        return all;
    }

    /** Boshqa bo'limlarga bog'langan (REF) maydonlar. */
    public List<FieldDef> refFields() {
        return fields.stream()
                .filter(FieldDef::isRef)
                .toList();
    }

    public FieldDef field(String key) {
        return fields.stream()
                .filter(f -> f.key().equals(key))
                .findFirst()
                .orElse(null);
    }

    public String sumKey() {
        return sumKey;
    }

    public boolean isReport() {
        return report;
    }

    public static List<AppSection> of(AppModule module) {
        return ALL.stream()
                .filter(section -> section.module == module)
                .toList();
    }

    /** Bo'limni to'liq kalit bo'yicha topadi: "crm/customers". */
    public static Optional<AppSection> findByKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return ALL.stream()
                .filter(section -> section.key().equalsIgnoreCase(key.trim()))
                .findFirst();
    }

    public static Optional<AppSection> find(
            AppModule module,
            String slug
    ) {
        if (module == null || slug == null || slug.isBlank()) {
            return Optional.empty();
        }

        String normalizedSlug = slug.trim();

        return ALL.stream()
                .filter(section ->
                        section.module == module
                                && section.slug.equalsIgnoreCase(normalizedSlug))
                .findFirst();
    }

    /**
     * Bo'limni topadi.
     * Topilmasa, qaysi kalit yo'qligini aniq ko'rsatadi.
     */
    public static AppSection get(
            AppModule module,
            String slug
    ) {
        return find(module, slug)
                .orElseThrow(() -> new IllegalArgumentException(
                        "AppSection topilmadi. module="
                                + module
                                + ", slug="
                                + slug
                                + ". Mavjud bo'limlar: "
                                + ALL.stream()
                                .map(AppSection::key)
                                .toList()
                ));
    }
}