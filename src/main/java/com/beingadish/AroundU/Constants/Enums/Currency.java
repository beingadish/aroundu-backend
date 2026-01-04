package com.beingadish.AroundU.Constants.Enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Currency {
    USD("$"),           // US Dollar
    EUR("€"),           // Euro
    GBP("£"),           // British Pound
    JPY("¥"),           // Japanese Yen
    CNY("¥"),           // Chinese Yuan
    INR("₹"),           // Indian Rupee
    AUD("A$"),          // Australian Dollar
    CAD("C$"),          // Canadian Dollar
    CHF("CHF"),         // Swiss Franc
    AED("AED"),         // UAE Dirham
    RUB("₽"),           // Russian Ruble
    KRW("₩"),           // South Korean Won
    BRL("R$"),          // Brazilian Real
    ZAR("R"),           // South African Rand
    HKD("HK$"),         // Hong Kong Dollar
    SGD("S$"),          // Singapore Dollar
    SEK("kr"),          // Swedish Krona
    NOK("kr"),          // Norwegian Krone
    NZD("NZ$"),         // New Zealand Dollar
    MXN("$"),           // Mexican Peso
    SAR("SAR"),         // Saudi Riyal
    TRY("₺"),           // Turkish Lira
    THB("฿"),           // Thai Baht
    MYR("RM"),          // Malaysian Ringgit
    PHP("₱"),           // Philippine Peso
    IDR("Rp"),          // Indonesian Rupiah
    PLN("zł"),          // Polish Zloty
    DKK("kr"),          // Danish Krone
    HUF("Ft"),          // Hungarian Forint
    CZK("Kč"),          // Czech Koruna
    ILS("₪"),           // Israeli Shekel
    CLP("$"),           // Chilean Peso
    VND("₫"),           // Vietnamese Dong
    PKR("Rs");          // Pakistani Rupee

    // Getter for symbol
    private final String symbol;

    // Override toString() for display
    @Override
    public String toString() {
        return name() + " (" + symbol + ")";
    }
}
