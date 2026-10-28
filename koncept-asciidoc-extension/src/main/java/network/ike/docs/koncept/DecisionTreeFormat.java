/*
 * Copyright © 2026 IKE Network (support@ike.network)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.ike.docs.koncept;

import network.ike.docs.koncept.DecisionTreeSource.Interval;
import network.ike.docs.koncept.DecisionTreeSource.Node;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;

/** How the decision-tree processor writes numbers and names: the regulatory topics' house style. */
final class DecisionTreeFormat {

    private DecisionTreeFormat() {
    }

    static String label(Node node) {
        return node.label();
    }

    /**
     * A money value as the topics write it: to the whole unit with thousands separators, computed
     * from the unrounded value so a last digit may differ from a hand sum; to the cent only below
     * one unit.
     */
    static String money(double value) {
        if (Math.abs(value) < 1 && value != 0) {
            return String.format(Locale.ROOT, "%,.2f", value);
        }
        return String.format(Locale.ROOT, "%,d", Math.round(value));
    }

    /** A plain number: whole when whole, else to three significant digits beyond the integer part. */
    static String plain(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.format(Locale.ROOT, "%,d", (long) value);
        }
        BigDecimal decimal = new BigDecimal(value).round(new MathContext(6, RoundingMode.HALF_UP)).stripTrailingZeros();
        return decimal.toPlainString();
    }

    /** An interval of plain numbers: one number when the bounds meet. */
    static String plain(Interval interval) {
        if (interval.isPoint()) {
            return plain(interval.lower());
        }
        return (interval.lowerOpen() ? "(" : "[") + plain(interval.lower()) + ", " + plain(interval.upper())
                + (interval.upperOpen() ? ")" : "]");
    }

    /** An interval of money, with a symbol before each bound. */
    static String money(Interval interval, String symbol) {
        if (interval.isPoint()) {
            return symbol + money(interval.lower());
        }
        return (interval.lowerOpen() ? "(" : "[") + symbol + money(interval.lower()) + ", " + symbol
                + money(interval.upper()) + (interval.upperOpen() ? ")" : "]");
    }

    /** A share as a percentage to three significant digits: 21.4%, 8.41%, 0.254%. */
    static String percent(double share) {
        BigDecimal percent = new BigDecimal(share * 100).round(new MathContext(3, RoundingMode.HALF_UP)).stripTrailingZeros();
        return percent.toPlainString() + "%";
    }

    static String percent(Interval share) {
        if (share.isPoint()) {
            return percent(share.lower());
        }
        return (share.lowerOpen() ? "(" : "[") + percent(share.lower()) + ", " + percent(share.upper())
                + (share.upperOpen() ? ")" : "]");
    }

    /** Text safe inside a Graphviz HTML-like label. */
    static String html(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Text safe inside an AsciiDoc table cell: a bar would split the cell. */
    static String cell(String text) {
        return text == null ? "" : text.replace("|", "\\|");
    }
}
