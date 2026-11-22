package com.universal.erp.platform.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Har bir bo'lim nima vazifa bajarishi haqida tushuntirish: vazifasi, nima kiritiladi,
 * ma'lumot qayerda ishlatiladi. Bo'lim sahifasining tepasida va modul sahifasidagi kartalarda chiqadi.
 *
 * "Hisob-faktura" bo'limi hamma tushunadigan "Nasiya" (RU: Долги, EN: Credit sales) deb nomlangan.
 * Marshrut kaliti (finance/invoices) o'zgartirilmagan, shuning uchun havolalar va ruxsatlar buzilmaydi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/SectionGuide.java
 */
public final class SectionGuide {

    private static final Map<String, String[]> ROWS = new HashMap<>();

    private SectionGuide() {
    }

    /** [vazifasi, nima kiritiladi, qayerda ishlatiladi] — joriy tilda. */
    public static String[] of(AppSection s) {
        String[] r = ROWS.get(s.key());
        if (r == null) {
            return new String[] {s.description(), "", ""};
        }
        int i = AppTexts.index() * 3;
        return new String[] {r[i], r[i + 1], r[i + 2]};
    }

    public static String what(AppSection s) {
        return of(s)[0];
    }

    private static void g(String key,
                          String uzW, String uzE, String uzU,
                          String ruW, String ruE, String ruU,
                          String enW, String enE, String enU) {
        ROWS.put(key, new String[] {uzW, uzE, uzU, ruW, ruE, ruU, enW, enE, enU});
    }

    static {
        // ---------------------------------------------------------------- Moliya
        g("finance/invoices",
                "Nasiya: mijozlar tovarni nasiyaga olganda kim sizga qancha qarz ekanini va qachongacha to'lashini yuritadi.",
                "Nasiya raqami, mijoz, summa, to'lash muddati va holat (Qoralama → Yuborildi → To'landi yoki Muddati o'tgan).",
                "Summalar Moliya → Hisobotlar va BI'dagi nasiyaga berilgan summa hamda qarzdorlik hisobiga kiradi.",
                "Долги: когда клиент берёт товар в долг (насия), здесь видно, кто сколько должен и до какого срока.",
                "Номер, клиент, сумма, срок оплаты и статус (Черновик → Отправлен → Оплачен или Просрочен).",
                "Суммы попадают в расчёт выданного в долг и задолженности в Финансы → Отчёты и в BI.",
                "Credit sales: when a customer takes goods on credit, this shows who owes how much and by when.",
                "Number, customer, amount, due date and status (Draft → Sent → Paid or Overdue).",
                "Amounts feed the credit sales and receivables figures in Finance → Reports and BI.");
        g("finance/payments",
                "Kelgan to'lovlarni qayd etadi va qaysi nasiyaga (qarzga) tegishli ekanini ko'rsatadi.",
                "Sana, to'lovchi, summa, to'lov usuli (naqd, karta, o'tkazma) va nasiya raqami.",
                "Foyda va daromad hisobotlari shu summalardan hisoblanadi.",
                "Фиксирует полученные платежи и к какому долгу они относятся.",
                "Дата, плательщик, сумма, способ (наличные, карта, перевод) и номер долга.",
                "Отчёты о прибыли и доходах считаются по этим суммам.",
                "Records payments received and which credit sale they belong to.",
                "Date, payer, amount, method (cash, card, transfer) and credit sale number.",
                "Profit and income reports are calculated from these amounts.");
        g("finance/expenses",
                "Kompaniya chiqimlarini toifalar bo'yicha yuritadi: ijara, ish haqi, soliq va boshqalar.",
                "Sana, toifa, summa va qisqa tavsif.",
                "Hisobotlarda foydadan ayriladi; toifalar bo'yicha grafik chiqadi.",
                "Ведёт расходы компании по категориям: аренда, зарплата, налоги и другое.",
                "Дата, категория, сумма и краткое описание.",
                "Вычитаются из прибыли в отчётах; по категориям строится график.",
                "Tracks company spending by category: rent, salaries, taxes and more.",
                "Date, category, amount and a short description.",
                "Subtracted from profit in reports; shown as a chart by category.");
        g("finance/accounts",
                "Kassa va bank hisobraqamlaridagi pul qoldig'ini bir joyda ko'rsatadi.",
                "Nomi, turi (kassa yoki bank), hisobraqam, joriy qoldiq va valyuta.",
                "Mablag' qayerda turganini bilish uchun. Hozircha hisobotlarga avtomatik qo'shilmaydi.",
                "Показывает остатки денег в кассе и на банковских счетах в одном месте.",
                "Название, тип (касса или банк), номер счёта, текущий остаток и валюта.",
                "Чтобы видеть, где лежат деньги. Пока не включается в отчёты автоматически.",
                "Shows cash and bank balances in one place.",
                "Name, type (cash or bank), account number, current balance and currency.",
                "To see where the money sits. Not included in reports automatically yet.");
        g("finance/reports",
                "Foyda, zarar va qarzdorlikni bitta sahifada ko'rsatadi.",
                "Hech narsa kiritilmaydi: hisobot avtomatik yig'iladi.",
                "Manba: Nasiya, To'lovlar va Xarajatlar bo'limlari. Ular to'ldirilgani sari hisobot boyiydi.",
                "Показывает прибыль, убытки и задолженность на одной странице.",
                "Ничего вводить не нужно: отчёт собирается автоматически.",
                "Источник: разделы Долги, Платежи и Расходы. Чем больше в них данных, тем полнее отчёт.",
                "Shows profit, loss and receivables on one page.",
                "Nothing to enter: the report is built automatically.",
                "Source: Credit sales, Payments and Expenses. The more you record there, the fuller the report.");

        // ---------------------------------------------------------------- CRM
        g("crm/customers",
                "Mijozlar bazasi: kim bilan ishlaysiz va ular bilan qanday bog'lanasiz.",
                "Ism yoki kompaniya, telefon, email, holat (Potensial, Faol, Nofaol) va izoh.",
                "Bitimlar va nasiyada mijoz nomini shu yerdagidek yozing. BI'da mijozlar soni chiqadi.",
                "База клиентов: с кем вы работаете и как с ними связаться.",
                "Имя или компания, телефон, email, статус (Потенциальный, Активный, Неактивный) и примечание.",
                "Указывайте имя клиента в сделках и долгах так же, как здесь. В BI выводится число клиентов.",
                "Your customer base: who you work with and how to reach them.",
                "Name or company, phone, email, status (Lead, Active, Inactive) and a note.",
                "Use the same customer name in deals and credit sales. BI shows the customer count.");
        g("crm/deals",
                "Savdo voronkasi: har bir potensial sotuv va u hozir qaysi bosqichda ekani.",
                "Bitim nomi, mijoz, summa, bosqich (Yangi → Muzokara → Taklif → Yutildi yoki Yo'qotildi) va yopilish sanasi.",
                "Savdo tahlili va Boshqaruv panelidagi bitimlar soni, summasi va yutish ulushi shu yerdan olinadi.",
                "Воронка продаж: каждая потенциальная сделка и её текущий этап.",
                "Название, клиент, сумма, этап (Новая → Переговоры → Предложение → Выиграна или Проиграна) и дата закрытия.",
                "Число и сумма сделок и доля выигранных в Анализе продаж и на Панели управления берутся отсюда.",
                "Sales pipeline: every potential sale and the stage it is in.",
                "Title, customer, amount, stage (New → Negotiation → Proposal → Won or Lost) and close date.",
                "Deal count, value and win rate in Sales analytics and the Dashboard come from here.");
        g("crm/tasks",
                "Jamoa uchun qo'ng'iroq, uchrashuv va eslatma vazifalari ro'yxati.",
                "Vazifa, muddat, muhimlik darajasi, holat va mas'ul xodim.",
                "Hozircha alohida yuritiladi: muddati o'tgan va yuqori muhimlikdagilarni kunlik ko'rib chiqish uchun.",
                "Список задач для команды: звонки, встречи и напоминания.",
                "Задача, срок, приоритет, статус и ответственный.",
                "Пока ведётся отдельно: для ежедневного просмотра просроченных и важных задач.",
                "A task list for the team: calls, meetings and reminders.",
                "Task, due date, priority, status and assignee.",
                "Kept separately for now: review overdue and high-priority tasks daily.");
        g("crm/price-lists",
                "Mahsulot va xizmatlarning sotuv narxlari va chegirmalari.",
                "Mahsulot yoki xizmat nomi, narx, birlik, chegirma foizi va amal qilish muddati.",
                "Nasiya va bitim summasini belgilashda narxni shu yerdan ko'rib olasiz (hozircha qo'lda).",
                "Продажные цены и скидки на товары и услуги.",
                "Название товара или услуги, цена, единица, процент скидки и срок действия.",
                "Цены берутся отсюда при оформлении долгов и сделок (пока вручную).",
                "Selling prices and discounts for products and services.",
                "Product or service name, price, unit, discount percent and validity date.",
                "Look prices up here when entering credit sales and deals (manual for now).");

        // ---------------------------------------------------------------- Ombor
        g("inventory/products",
                "Mahsulotlar katalogi: nimani sotasiz yoki saqlaysiz.",
                "Kod (SKU), nomi, o'lchov birligi, narx va toifa.",
                "Qoldiq, Kirim va Chiqimda mahsulot nomini shu yerdagidek yozing. BI'da mahsulotlar soni chiqadi.",
                "Каталог товаров: что вы продаёте или храните.",
                "Код (SKU), название, единица измерения, цена и категория.",
                "В Остатках, Приходе и Расходе указывайте название так же, как здесь. В BI выводится число товаров.",
                "Product catalog: what you sell or keep in stock.",
                "SKU, name, unit of measure, price and category.",
                "Use the same product name in Stock, Receipts and Issues. BI shows the product count.");
        g("inventory/stock",
                "Omborlardagi joriy qoldiq: qaysi mahsulotdan qancha bor.",
                "Mahsulot, ombor, miqdor va minimal qoldiq (shundan kamayganda to'ldirish kerak).",
                "Kirim va Chiqim bilan solishtirib turing. Hozircha qoldiq qo'lda yangilanadi.",
                "Текущие остатки на складах: сколько каких товаров есть.",
                "Товар, склад, количество и минимальный остаток (ниже него нужно пополнять).",
                "Сверяйте с Приходом и Расходом. Пока остаток обновляется вручную.",
                "Current stock levels per warehouse: how much of each product you have.",
                "Product, warehouse, quantity and minimum stock (restock below it).",
                "Compare with Receipts and Issues. Stock is updated manually for now.");
        g("inventory/receipts",
                "Yetkazib beruvchidan kelgan tovarlarni qayd etadi.",
                "Sana, yetkazib beruvchi, mahsulot, miqdor va narx.",
                "Kirim qoldiqni oshiradi: shu miqdorni Qoldiq bo'limiga ham qo'shing (hozircha qo'lda).",
                "Фиксирует товары, поступившие от поставщиков.",
                "Дата, поставщик, товар, количество и цена.",
                "Приход увеличивает остаток: добавьте это количество и в раздел Остатки (пока вручную).",
                "Records goods received from suppliers.",
                "Date, supplier, product, quantity and price.",
                "Receipts increase stock: add the same quantity in Stock too (manual for now).");
        g("inventory/issues",
                "Ombordan chiqqan tovarlar: sotuv, ichki sarf yoki hisobdan chiqarish.",
                "Sana, mijoz, mahsulot, miqdor va sabab.",
                "Chiqim qoldiqni kamaytiradi: shu miqdorni Qoldiq bo'limidan ayiring (hozircha qo'lda).",
                "Товары, ушедшие со склада: продажа, внутренний расход или списание.",
                "Дата, клиент, товар, количество и причина.",
                "Расход уменьшает остаток: вычтите это количество в разделе Остатки (пока вручную).",
                "Goods leaving the warehouse: sales, internal use or write-offs.",
                "Date, customer, product, quantity and reason.",
                "Issues reduce stock: subtract the same quantity in Stock (manual for now).");
        g("inventory/stocktake",
                "Haqiqiy sanalgan qoldiqni hisobdagi bilan solishtiradi.",
                "Sana, ombor, mahsulot, hisobdagi va haqiqiy miqdor, izoh.",
                "Farq (kamomad yoki ortiqcha) aniqlanadi, so'ng Qoldiq bo'limi to'g'rilanadi.",
                "Сверяет фактически пересчитанные остатки с учётными.",
                "Дата, склад, товар, количество по учёту и фактическое, примечание.",
                "Выявляется разница (недостача или излишек), после чего исправляется раздел Остатки.",
                "Compares counted stock with recorded stock.",
                "Date, warehouse, product, recorded and actual quantity, note.",
                "Shows the gap (shortage or surplus); then correct the Stock section.");

        // ---------------------------------------------------------------- Kadrlar
        g("hr/employees",
                "Xodimlar ro'yxati: kim, qaysi lavozimda va qancha oylik oladi.",
                "F.I.Sh., lavozim, telefon, ishga olingan sana, oylik va holat.",
                "Davomat, Ish haqi va Ta'tillarda xodim ismini shu yerdagidek yozing. BI'da xodimlar soni chiqadi.",
                "Список сотрудников: кто, в какой должности и с каким окладом.",
                "Ф.И.О., должность, телефон, дата приёма, оклад и статус.",
                "В Табеле, Зарплате и Отпусках указывайте имя так же, как здесь. В BI выводится число сотрудников.",
                "Employee list: who works where and what they earn.",
                "Full name, position, phone, hire date, salary and status.",
                "Use the same name in Attendance, Payroll and Leaves. BI shows the headcount.");
        g("hr/attendance",
                "Xodimlarning ishga kelish va ketish vaqtini yuritadi.",
                "Xodim, sana, kelish va ketish vaqti, holat (Keldi, Kechikdi, Kelmadi).",
                "Oy oxirida ish haqini hisoblashda asos bo'ladi.",
                "Ведёт время прихода и ухода сотрудников.",
                "Сотрудник, дата, время прихода и ухода, статус (Пришёл, Опоздал, Отсутствует).",
                "Служит основой для расчёта зарплаты в конце месяца.",
                "Tracks when employees arrive and leave.",
                "Employee, date, check-in and check-out times, status (Present, Late, Absent).",
                "The basis for payroll at the end of the month.");
        g("hr/payroll",
                "Oylik ish haqini hisoblaydi: asosiy summa, mukofot va ushlanmalar.",
                "Xodim, oy, asosiy summa, mukofot, ushlanma va holat.",
                "Hisobotlarga avtomatik qo'shilmaydi: to'langan oylikni Xarajatlar bo'limiga \"Ish haqi\" toifasida kiriting.",
                "Рассчитывает зарплату за месяц: оклад, премии и удержания.",
                "Сотрудник, месяц, оклад, премия, удержания и статус.",
                "В отчёты автоматически не попадает: выплаченную зарплату вносите в Расходы с категорией «Зарплата».",
                "Calculates monthly pay: base, bonuses and deductions.",
                "Employee, month, base, bonus, deductions and status.",
                "Not added to reports automatically: record paid salaries in Expenses under \"Salary\".");
        g("hr/leaves",
                "Ta'til va kasallik ruxsatnomalarini yuritadi.",
                "Xodim, turi, boshlanish va tugash sanasi, holat (Kutilmoqda, Tasdiqlandi, Rad etildi).",
                "Davomat va ish haqini rejalashtirishda hisobga olinadi.",
                "Ведёт отпуска и больничные.",
                "Сотрудник, тип, даты начала и конца, статус (Ожидает, Одобрено, Отклонено).",
                "Учитывается при планировании табеля и зарплаты.",
                "Tracks vacations and sick leave.",
                "Employee, type, start and end dates, status (Pending, Approved, Rejected).",
                "Taken into account when planning attendance and payroll.");

        // ---------------------------------------------------------------- BI
        g("bi/dashboard",
                "Asosiy ko'rsatkichlar bir sahifada: mijozlar, ochiq bitimlar, mahsulotlar, xodimlar va foyda.",
                "Hech narsa kiritilmaydi.",
                "Manba: CRM, Ombor, Kadrlar va Moliya bo'limlari.",
                "Ключевые показатели на одной странице: клиенты, открытые сделки, товары, сотрудники и прибыль.",
                "Ничего вводить не нужно.",
                "Источник: разделы CRM, Склад, Кадры и Финансы.",
                "Key metrics on one page: customers, open deals, products, employees and profit.",
                "Nothing to enter.",
                "Source: CRM, Inventory, HR and Finance sections.");
        g("bi/sales",
                "Savdo natijalari: bitimlar summasi, yutilganlari va yutish ulushi, mijozlar kesimida.",
                "Hech narsa kiritilmaydi.",
                "Manba: CRM → Bitimlar. Bitimlarni to'g'ri bosqichga o'tkazib borsangiz, tahlil aniq bo'ladi.",
                "Результаты продаж: сумма сделок, выигранные и доля выигранных, в разрезе клиентов.",
                "Ничего вводить не нужно.",
                "Источник: CRM → Сделки. Чем точнее этапы сделок, тем точнее анализ.",
                "Sales results: deal value, won deals and win rate, by customer.",
                "Nothing to enter.",
                "Source: CRM → Deals. Keep deal stages up to date for an accurate analysis.");
        g("bi/finance",
                "Daromad, xarajat, foyda va rentabellikni tahlil qiladi.",
                "Hech narsa kiritilmaydi.",
                "Manba: Moliya → To'lovlar va Xarajatlar.",
                "Анализ дохода, расходов, прибыли и рентабельности.",
                "Ничего вводить не нужно.",
                "Источник: Финансы → Платежи и Расходы.",
                "Analyses income, expenses, profit and margin.",
                "Nothing to enter.",
                "Source: Finance → Payments and Expenses.");
    }
}