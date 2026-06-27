package com.supcaf.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatUtils {

    private static final DecimalFormat DF_AMOUNT;
    private static final DecimalFormat DF_QTY;
    private static final SimpleDateFormat SDF_DB   = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat SDF_DISP = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.getDefault());
        sym.setDecimalSeparator(',');
        sym.setGroupingSeparator(' ');
        DF_AMOUNT = new DecimalFormat("#,##0.00", sym);
        DF_QTY    = new DecimalFormat("#,##0.###", sym);
    }

    /** Formate un montant ex: 12 500,00 */
    public static String montant(double val) {
        return DF_AMOUNT.format(val);
    }

    /** Formate une quantité (jusqu'à 3 décimales) */
    public static String quantite(double val) {
        return DF_QTY.format(val);
    }

    /** Date du jour au format BD (yyyy-MM-dd) */
    public static String dateAujourdhui() {
        return SDF_DB.format(new Date());
    }

    /** Convertit date BD → affichage */
    public static String dateAffichage(String dateBd) {
        try {
            Date d = SDF_DB.parse(dateBd);
            return d != null ? SDF_DISP.format(d) : dateBd;
        } catch (Exception e) {
            return dateBd != null ? dateBd : "";
        }
    }

    /** Convertit date affichage → BD */
    public static String dateBd(String dateAff) {
        try {
            Date d = SDF_DISP.parse(dateAff);
            return d != null ? SDF_DB.format(d) : dateAff;
        } catch (Exception e) {
            return dateAff != null ? dateAff : "";
        }
    }

    /** Parse double depuis saisie (gère virgule et point) */
    public static double parseDouble(String s) {
        if (s == null || s.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(s.trim().replace(',', '.').replace(" ", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /** Parse long depuis saisie */
    public static long parseLong(String s) {
        if (s == null || s.trim().isEmpty()) return 0L;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
