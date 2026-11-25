package com.kemrp.helper

import android.graphics.Color

data class PhraseItem(
    val label: String,
    val hotkey: String,
    var text: String
)

data class FactionTheme(
    val bg: Int,
    val panel: Int,
    val row: Int,
    val entry: Int,
    val border: Int,
    val btn: Int
)

data class FactionDef(
    val id: String,
    val name: String,
    val sectionKeys: List<String>,
    val rankLabel: String,
    val nameLabel: String,
    val defaultRank: String,
    val defaultName: String,
    val megaphoneKey: String?,
    val themeKey: String
)

object AppData {
    const val APP_VERSION = "1.6"
    val GOLD: Int = Color.parseColor("#F2B705")

    val THEMES: Map<String, FactionTheme> = mapOf(
        "blue" to FactionTheme(
            bg = Color.parseColor("#0F172A"),
            panel = Color.parseColor("#1E293B"),
            row = Color.parseColor("#334155"),
            entry = Color.parseColor("#0F172A"),
            border = Color.parseColor("#3B82F6"),
            btn = Color.parseColor("#2563EB")
        ),
        "black" to FactionTheme(
            bg = Color.parseColor("#09090B"),
            panel = Color.parseColor("#18181B"),
            row = Color.parseColor("#27272A"),
            entry = Color.parseColor("#09090B"),
            border = Color.parseColor("#52525B"),
            btn = Color.parseColor("#3F3F46")
        ),
        "red" to FactionTheme(
            bg = Color.parseColor("#1E1014"),
            panel = Color.parseColor("#2A141A"),
            row = Color.parseColor("#4C1D2C"),
            entry = Color.parseColor("#1E1014"),
            border = Color.parseColor("#E11D48"),
            btn = Color.parseColor("#BE123C")
        ),
        "bordeaux" to FactionTheme(
            bg = Color.parseColor("#1C101B"),
            panel = Color.parseColor("#2D162A"),
            row = Color.parseColor("#502449"),
            entry = Color.parseColor("#1C101B"),
            border = Color.parseColor("#D946EF"),
            btn = Color.parseColor("#C026D3")
        ),
        "green" to FactionTheme(
            bg = Color.parseColor("#061F16"),
            panel = Color.parseColor("#064E3B"),
            row = Color.parseColor("#047857"),
            entry = Color.parseColor("#061F16"),
            border = Color.parseColor("#10B981"),
            btn = Color.parseColor("#059669")
        ),
        "teal" to FactionTheme(
            bg = Color.parseColor("#082F49"),
            panel = Color.parseColor("#0C4A6E"),
            row = Color.parseColor("#0369A1"),
            entry = Color.parseColor("#082F49"),
            border = Color.parseColor("#0EA5E9"),
            btn = Color.parseColor("#0284C7")
        )
    )

    val SECTION_TITLES: Map<String, String> = mapOf(
        "dps_proc" to "Процедуры",
        "dps_move" to "Движение",
        "dps_docs" to "Документы / ТС",
        "dps_park" to "Стоянка / ДТП",
        "dps_uk" to "УК РФ",
        "dps_arrest" to "Задержание",
        "pps_proc" to "Процедуры",
        "pps_koap1" to "КоАП: Порядок",
        "pps_koap2" to "КоАП: Прочее",
        "pps_uk1" to "УК РФ I",
        "pps_uk2" to "УК РФ II",
        "pps_detain" to "Задержание",
        "sk_proc" to "Процедуры",
        "sk_act" to "Следств. действия",
        "sk_rights" to "Права и меры",
        "sk_uk" to "УК РФ",
        "opg" to "Отыгровки ОПГ",
        "fsb" to "Процедуры ФСБ",
        "usb_mvd_proc" to "Процедуры",
        "usb_mvd_arrest" to "Задержание",
        "usb_mvd_uk" to "УК РФ",
        "usb_mvd_disc" to "Дисциплинарные",
        "usb_fsb_proc" to "Процедуры",
        "usb_fsb_arrest" to "Задержание",
        "usb_fsb_uk" to "УК РФ",
        "usb_fsb_disc" to "Дисциплинарные"
    )

    val FACTIONS: List<FactionDef> = listOf(
        FactionDef("dps", "ДПС", listOf("dps_proc", "dps_move", "dps_docs", "dps_park", "dps_uk", "dps_arrest"), "Звание:", "Фамилия:", "Лейтенант", "Иванов", "megaphone_dps", "blue"),
        FactionDef("pps", "ППС", listOf("pps_proc", "pps_koap1", "pps_koap2", "pps_uk1", "pps_uk2", "pps_detain"), "Звание:", "Фамилия:", "Сержант", "Петров", "megaphone_pps", "blue"),
        FactionDef("opg", "ОПГ", listOf("opg"), "Роль:", "Прозвище:", "Бригадир", "Сава", null, "red"),
        FactionDef("fsb", "ФСБ", listOf("fsb"), "Звание:", "Фамилия:", "Майор", "Смирнов", null, "black"),
        FactionDef("sk", "СК", listOf("sk_proc", "sk_act", "sk_rights", "sk_uk"), "Звание:", "Фамилия:", "Следователь", "Соколов", null, "bordeaux"),
        FactionDef("usbm", "УСБ МВД", listOf("usb_mvd_proc", "usb_mvd_arrest", "usb_mvd_uk", "usb_mvd_disc"), "Звание:", "Фамилия:", "Капитан", "Громов", null, "green"),
        FactionDef("usbf", "УСБ ФСБ", listOf("usb_fsb_proc", "usb_fsb_arrest", "usb_fsb_uk", "usb_fsb_disc"), "Звание:", "Фамилия:", "Майор", "Волков", null, "teal")
    )

    fun factionById(id: String): FactionDef = FACTIONS.firstOrNull { it.id == id } ?: FACTIONS.first()
    fun themeFor(fid: String): FactionTheme = THEMES[factionById(fid).themeKey] ?: THEMES.getValue("blue")

    val ARTICLES: LinkedHashMap<String, List<Pair<String, String>>> = linkedMapOf(
        "КоАП (Люди)" to listOf(
            "Ст. 20.1" to "Мелкое хулиганство (мат, оскорбительное приставание, порча имущества).",
            "Ст. 20.20" to "Распитие алкогольной продукции в запрещенных (общественных) местах.",
            "Ст. 20.21" to "Появление в общественных местах в состоянии опьянения, оскорбляющем человеческое достоинство.",
            "Ст. 19.3" to "Неповиновение законному распоряжению или требованию сотрудника полиции/ФСБ.",
            "Ст. 19.15" to "Проживание по месту жительства без документа, удостоверяющего личность (без паспорта).",
            "Ст. 7.27" to "Мелкое хищение чужого имущества путем кражи или мошенничества (до 2500 руб.).",
            "Ст. 6.8" to "Незаконный оборот наркотических средств (без цели сбыта, малые объемы)."
        ),
        "КоАП (Авто)" to listOf(
            "Ст. 12.1" to "Управление ТС, не зарегистрированным в установленном порядке.",
            "Ст. 12.2" to "Управление ТС без номеров, а также с видоизмененными или скрытыми номерами.",
            "Ст. 12.3" to "Управление ТС водителем, не имеющим при себе документов (права, СТС).",
            "Ст. 12.5" to "Управление ТС при наличии неисправностей. Тонировка не по ГОСТу (ч. 3.1).",
            "Ст. 12.7" to "Управление ТС водителем, не имеющим права управления ТС (без прав вообще или лишенным).",
            "Ст. 12.8" to "Управление ТС водителем, находящимся в состоянии опьянения.",
            "Ст. 12.15" to "Выезд на полосу встречного движения.",
            "Ст. 12.19" to "Нарушение правил остановки или стоянки ТС.",
            "Ст. 12.25" to "Невыполнение требования о предоставлении ТС или об остановке ТС.",
            "Ст. 12.26" to "Невыполнение водителем требования о прохождении медицинского освидетельствования."
        ),
        "УК РФ (Люди)" to listOf(
            "Ст. 105" to "Убийство, то есть умышленное причинение смерти другому человеку.",
            "Ст. 111" to "Умышленное причинение тяжкого вреда здоровью.",
            "Ст. 112" to "Умышленное причинение средней тяжести вреда здоровью.",
            "Ст. 115" to "Умышленное причинение легкого вреда здоровью.",
            "Ст. 158" to "Кража, то есть тайное хищение чужого имущества.",
            "Ст. 161" to "Грабеж, то есть открытое хищение чужого имущества.",
            "Ст. 162" to "Разбой (нападение в целях хищения чужого имущества с применением насилия).",
            "Ст. 222" to "Незаконные приобретение, передача, сбыт, хранение, перевозка или ношение оружия.",
            "Ст. 228" to "Незаконные приобретение, хранение, перевозка наркотических средств в значительном размере.",
            "Ст. 318" to "Применение насилия в отношении представителя власти.",
            "Ст. 319" to "Публичное оскорбление представителя власти при исполнении."
        ),
        "УК РФ (Авто)" to listOf(
            "Ст. 166" to "Неправомерное завладение автомобилем без цели хищения (Угон).",
            "Ст. 264" to "Нарушение ПДД, повлекшее по неосторожности тяжкий вред здоровью или смерть человека.",
            "Ст. 264.1" to "Управление ТС в состоянии опьянения лицом, уже подвергнутым адм. наказанию за пьяную езду.",
            "Ст. 327" to "Подделка, изготовление или оборот поддельных документов (поддельные права или номера ТС)."
        )
    )
}
