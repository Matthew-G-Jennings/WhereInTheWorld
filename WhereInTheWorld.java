package Where;

import java.util.*;
import java.io.*;

/**
 * Class to parse and convert coordinates from stdin to standard form
 * Generates and outputs a GeoJSON file with the given coordinates
 *
 *
 * @author Matthew Jennings
 */
public class WhereInTheWorld {

    public enum Format {
        RAW_NUMBERS,
        DIRECTION,
        DEGREES,
        DETECT_FAILED,
        UNSET
    }

    public static Format format;

    public static String flat;

    public static String flong;

    public static String flabel;

    public static PrintWriter writer = null;

    public static boolean firstBody = true;

    /**
     * Main loop. Reads lines from stdin and tries to parse them as a coordinate.
     * Calls methods to generate header,body and footer of a GeoJSON file.
     *
     * @param args Not used
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input;

        try {
            writer = new PrintWriter("coords.GeoJSON", "UTF-8");
        } catch (IOException e) {

        }
        genHeader();

        while (scanner.hasNextLine()) {
            flat = "";
            flong = "";
            flabel = "";
            input = scanner.nextLine();
            format = Format.UNSET;
            // System.out.println("Input echo: " + input);
            checkFormat(stripExtra(input));
            // System.out.println(format);
            // format known, validation of each done within associated function
            boolean success = false;
            if (format == Format.RAW_NUMBERS) {
                success = parseRaw(input);
            } else if (format == Format.DEGREES) {
                success = parseDeg(input);
            } else if (format == Format.DIRECTION) {
                success = parseDir(input);
            } else {
                success = false;
            }
            if (success) {
                if (sanityCheck()) {
                    genBody();
                } else {
                    error(input + " Sanity Check failed");
                }
            }
        }

        genFooter();
    }

    /**
     * Examines the first two tokens to determine format.
     * Does not validate correctness of line.
     * Requires extranious characters to have been removed.
     *
     * @param line the line of text to check (String)
     */
    public static void checkFormat(String line) {
        Scanner lineScan = new Scanner(line);
        String token1;
        String token2;
        if (lineScan.hasNext()) {
            token1 = lineScan.next();
        } else {
            // error(line);
            return;
        }
        if (lineScan.hasNext()) {
            token2 = lineScan.next();
        } else {
            // error(line);
            return;
        }

        // if a ' or d exists in either of the first tokens it must be dms format.
        // figure out how to deal with °, it seems to make java very angry, scanner?
        for (int i = 0; i < token1.length(); i++) {
            if (token1.charAt(i) == '°' || token1.charAt(i) == 'd') {
                format = Format.DEGREES;
                return;
            }
        }
        for (int i = 0; i < token2.length(); i++) {
            if (token2.charAt(i) == '°' || token2.charAt(i) == 'd') {
                format = Format.DEGREES;
                return;
            }
        }
        // check both tokens are numbers
        // strip any positive signs first
        try {
            if (token1.charAt(0) == '+') {
                String temp = "";
                for (int i = 1; i < token1.length(); i++) {
                    temp += token1.charAt(i);
                }
                token1 = temp;
            }
            if (token2.charAt(0) == '+') {
                String temp = "";
                for (int i = 1; i < token2.length(); i++) {
                    temp += token2.charAt(i);
                }
                token1 = temp;
            }
            Double.parseDouble(token1);
            Double.parseDouble(token2);
            format = Format.RAW_NUMBERS;
            return;
        } catch (Exception e) {
            // they aren't both numbers
            // is the second token a direction
            if ((token2.charAt(0) == 'N'
                    || token2.charAt(0) == 'E'
                    || token2.charAt(0) == 'W'
                    || token2.charAt(0) == 'S')
                    && token2.length() == 1) {
                try {
                    Double.parseDouble(token1);
                    format = Format.DIRECTION;
                    return;
                } catch (Exception f) {

                }
            } else if (token1.charAt(token1.length() - 1) == 'N'
                    || token1.charAt(token1.length() - 1) == 'E'
                    || token1.charAt(token1.length() - 1) == 'W'
                    || token1.charAt(token1.length() - 1) == 'S') {
                String temp = "";
                for (int i = 0; i < token1.length() - 1; i++) {
                    temp += token1.charAt(i);
                }
                try {
                    Double.parseDouble(temp);
                    format = Format.DIRECTION;
                    return;
                } catch (Exception f) {

                }
            } else {
                format = Format.DETECT_FAILED;
                error(line);
                return;
            }

        }

    }

    /**
     * Strips acceptable but extranious characters from a line
     * Replaces with spaces.
     *
     * @param line String to strip
     * @return String with extranious chars removed.
     */
    public static String stripExtra(String line) {
        String result = "";
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != ',') {
                result += line.charAt(i);
            } else {
                result += ' ';
            }
        }
        return result;
    }

    /**
     * Strips acceptable but extranious characters from a line
     * Does not replace with spaces.
     *
     * @param line String to strip
     * @return String with extranious chars removed.
     */
    public static String stripExtraClean(String line) {
        String result = "";
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != ',') {
                result += line.charAt(i);
            }
        }
        return result;
    }

    /**
     * Strips the negative signs from a line.
     *
     * @param input String to strip
     * @return String with "-"s removed.
     */
    public static String stripNeg(String line) {
        String result = "";
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != '-') {
                result += line.charAt(i);
            }
        }
        return result;
    }

    /**
     * Assumes the location is given as two numbers
     *
     * @param input String to check
     * @return true if operation succeeded without error, false otherwise
     */
    public static boolean parseRaw(String input) {
        input = cleanRaw(input);
        if (input.charAt(0) == 'X') {
            return false;
        }
        // from here on the format should be known xx.xx xx.xx label
        // no format errors should be present, just extract data and validate.
        Scanner lineScan = new Scanner(input);
        String token1 = lineScan.next();
        String token2 = lineScan.next();

        flat = String.format("%.6f", Double.parseDouble(token1));
        flong = String.format("%.6f", Double.parseDouble(token2));

        flabel = "";
        while (lineScan.hasNext()) {
            flabel += lineScan.next();
            flabel += " ";
        }

        lineScan.close();
        return true;

    }

    /**
     * Known information: the first two tokens are both valid doubles.
     *
     * Target format: xx.xx xx.xx label
     *
     * @param input String to standardise.
     * @return String in a standarised format for parsing, "X" if failure.
     */
    public static String cleanRaw(String input) {
        String result = "";
        boolean firstfound = false;
        boolean secfound = false;
        for (int i = 0; i < input.length(); i++) {
            if (!firstfound) { // still looking for first number
                if ((Character.isDigit(input.charAt(i)) || input.charAt(i) == '.') || input.charAt(i) == '-') {
                    result += input.charAt(i);
                } else if (input.charAt(i) == ' ' || input.charAt(i) == ',' || input.charAt(i) == '+') {
                    result += ' ';
                    firstfound = true;
                } else {
                    error(input);
                    return "X";
                }
            } else if (!secfound) { // still looking for second number
                if ((Character.isDigit(input.charAt(i)) || input.charAt(i) == '.' || input.charAt(i) == '-')) {
                    result += input.charAt(i);
                } else if (input.charAt(i) == ' ' || input.charAt(i) == ',') {
                    result += ' ';
                    secfound = true;
                } else {
                    error(input);
                    return "X";
                }
            } else if (firstfound && secfound) { // successfully found both numbers, dump label in
                result += input.charAt(i);
            }
        }
        // System.out.println("cleanRaw output = " + result);

        return result; // result format x.xx x.xx label, any content.

    }

    /**
     * Attempts to parse the given string as a coordinate in DMS format.
     *
     * @param input String to parse
     * @return true if successful, otherwise false.
     */
    public static boolean parseDeg(String input) {
        Scanner lineScan = new Scanner(cleanDeg(input));
        String directions = lineScan.next();
        if (directions.charAt(0) == 'X') {
            return false;
        }
        boolean firstneg = false;
        boolean secneg = false;
        double lat = 0;
        double lon = 0;
        double[] dmscoords = new double[6];

        for (int i = 0; i < 6; i++) {
            try {
                dmscoords[i] = Double.parseDouble(lineScan.next());
            } catch (Exception e) {
                dmscoords[i] = 0;
            }
        }

        if (directions.charAt(0) == 'S') {
            firstneg = true;
        }
        if (directions.charAt(1) == 'W') {
            secneg = true;
        }
        try {
            lat = dmscoords[0] + (dmscoords[1] / 60) + (dmscoords[2] / 3600);
            lon = dmscoords[3] + (dmscoords[4] / 60) + (dmscoords[5] / 3600);
        } catch (Exception e) {
            error(input + " failed conversion");
        }
        if (firstneg) {
            lat = 0 - lat;
        }
        if (secneg) {
            lon = 0 - lon;
        }

        flat = String.format("%.6f", lat);
        flong = String.format("%.6f", lon);

        while (lineScan.hasNext()) {
            flabel += lineScan.next();
            flabel += ' ';
        }

        return true;
    }

    /**
     * Attempts to clean up a given line into a standarised DMS format
     *
     * eg. NE 10 10 10 10 10 10
     *
     * @param input String to attempt to convert.
     * @return String in standarized DMS format, "X" if failure.
     */
    public static String cleanDeg(String input) {
        // the only knowns at this point is that the first or second token
        // contains either a 'd' or '°' and that this is not a RAW or DIR.
        // so therefore this is either a dms format, or invalid
        // figure out which and translate to a standarised format if valid.
        String result = "";
        String label = "";
        String temp = "";
        int deg = 0;
        int min = 0;
        int sec = 0;
        char dir1 = 'X';
        char dir2 = 'X';

        // assume it's in some sort of dms format
        for (int i = 0; i < input.length(); i++) {
            // System.out.println(deg + " " + min + " " + sec);
            if (input.charAt(i) == '°' || input.charAt(i) == 'd') {

                if (sec == 0 && deg == 1) {
                    result += " 0 ";
                    sec++;
                }
                if (min == 0 && deg == 1) {
                    result += " 0 ";
                    min++;
                }
                result += temp;
                result += ' ';
                temp = "";
                deg++;

            } else if (input.charAt(i) == 'm' || input.charAt(i) == '\'') {
                result += temp;
                result += ' ';
                temp = "";
                min++;

            } else if (input.charAt(i) == 's' || input.charAt(i) == '\"') {
                result += temp;
                result += ' ';
                temp = "";
                sec++;

            } else if (Character.isDigit(input.charAt(i)) || input.charAt(i) == ' '
                    || input.charAt(i) == '.' || input.charAt(i) == '-') {
                temp += input.charAt(i); // valid part of a number, a space, or a -
                continue;
            } else if (input.charAt(i) == 'N' || input.charAt(i) == 'S' || input.charAt(i) == 'E'
                    || input.charAt(i) == 'W') {
                if (deg == 2 && min == 1) {
                    result += " 0 ";
                    min++;
                }
                if (deg == 2 && sec == 1) {
                    result += " 0 ";
                    sec++;
                }
                if (dir1 == 'X') {
                    dir1 = input.charAt(i);
                } else if (dir2 == 'X') {
                    dir2 = input.charAt(i);
                } else {
                    error(input + " : DMS Direction issue");
                    return "X";
                }
            } else if (input.charAt(i) == ',') {
                result += ' ';
            } else if (deg + min + sec == 6 && dir2 != 'X') {
                result += ' '; // mission accomplished. both directions.
                i--;
                while (i < input.length()) {
                    label += input.charAt(i);
                    i++;
                }
                break;
            } else if (deg + min + sec == 6 && dir1 == 'X') {
                result += ' '; // mission accomplished. no directions.
                i--;
                while (i < input.length()) {
                    label += input.charAt(i);
                    i++;
                }
                break;
            } else if (i >= input.length() - 1) {
                if (deg == 2 && min == 1) {
                    result += " 0 ";
                    min++;
                }
                if (deg == 2 && sec == 1) {
                    result += " 0 ";
                    sec++;
                }
                break;
            } else {
                if (deg == 2 && min == 1) {
                    result += " 0 ";
                    min++;
                }
                if (deg == 2 && sec == 1) {
                    result += " 0 ";
                    sec++;
                }
                // System.out.println(deg + " " + min +" " + sec + " else");
            }
        }

        if (dir1 == 'E' || dir1 == 'W') {
            if (dir2 != 'N' && dir2 != 'S') {
                error(input);
                return "X";
            }
        } else if (dir1 == 'N' || dir1 == 'S') {
            if (dir2 != 'E' && dir2 != 'W') {
                error(input);
                return "X";
            }
        }

        if (result.charAt(0) == '-' && dir1 == 'X') {
            dir1 = 'S';
        } else if (result.charAt(0) == '-') {
            error(input + " negative and first direction");
        }

        for (int i = 1; i < result.length(); i++) {
            if (result.charAt(i) == '-' && dir2 == 'X') {
                dir2 = 'W';
                break;
            } else if (result.charAt(i) == '-') {
                error(input + " negative and second direction");
            }
        }

        if (dir1 == 'E' || dir1 == 'W') {
            // it's backwards
            // System.out.println("Backwards");
            Scanner lineScan = new Scanner(result);
            String first = "";
            int firstcount = 0;
            String second = "";
            while (lineScan.hasNext()) {
                if (firstcount < 3) {
                    first += lineScan.next();
                    first += ' ';
                    firstcount++;
                } else {
                    second += lineScan.next();
                    second += ' ';
                }
            }
            char dirtemp;
            dirtemp = dir1;
            dir1 = dir2;
            dir2 = dirtemp;
            result = second + first;
        }

        result = stripNeg(result);

        // System.out.println("cleanDeg output: " + dir1 + dir2 + ' ' + result + label);
        return "" + dir1 + dir2 + ' ' + result + label;

    }

    /**
     * DMS with direction handled in parseDeg, only handles raw numbers
     * with direction.
     *
     * @param input String to parse
     * @return true if successful, otherwise false.
     */
    public static boolean parseDir(String input) {
        input = cleanDir(input);
        if (input.charAt(0) == 'X') {
            return false;
        }
        // from here we are guaranteed x.x N/S x.x E/W with label optional.
        Scanner lineScan = new Scanner(input);
        String firstnum = lineScan.next();
        String northsouth = lineScan.next();
        String secondnum = lineScan.next();
        String eastwest = lineScan.next();

        if (northsouth.charAt(0) == 'S') {
            flat = String.format("%.6f", 0 - (Double.parseDouble(firstnum)));
        } else {
            flat = String.format("%.6f", Double.parseDouble(firstnum));
        }
        if (eastwest.charAt(0) == 'W') {
            flong = String.format("%.6f", 0 - (Double.parseDouble(secondnum)));
        } else {
            flong = String.format("%.6f", Double.parseDouble(secondnum));
        }

        flabel = "";
        while (lineScan.hasNext()) {
            flabel += lineScan.next();
            flabel += " ";
        }

        return true;
    }

    /**
     * Known information: Either the second token is NESW or
     * the last character of the first token is NESW.
     * If second token is NESW then the first token is a valid
     * double.
     * If the last character of the first token is NSEW, then the
     * preceeding characters of the first token form a valid double.
     *
     * Target format: xx.xx N/S, xx.xx E/W label
     *
     * @param input String to parse
     * @return String in standarized dir format, "X" if failure
     */
    public static String cleanDir(String input) {
        String result = "";
        Scanner lineScan = new Scanner(input);
        String temp = "";
        String first = "";
        String second = "";
        char dir1 = ' ';
        char dir2 = ' ';

        String token = stripExtra(lineScan.next());
        if (Character.isDigit(token.charAt(token.length() - 1))) {
            // last char is a number, directions aren't split, we must have 4 tokens.
            try {
                Double.parseDouble(token);
                first += token;
                temp = stripExtraClean(lineScan.next());
                if (temp.length() > 1 && temp.charAt(1) != ' ') {
                    error(input + " first");
                    return "X";
                } else {
                    dir1 = temp.charAt(0);
                }
                second += stripExtraClean(lineScan.next());
                Double.parseDouble(second);
                temp = stripExtra(lineScan.next());
                if (temp.length() > 1 && temp.charAt(1) != ' ') {
                    error(input + " second");
                    return "X";
                } else {
                    dir2 = temp.charAt(0);
                }
                if (!Character.isDigit(first.charAt(0)) || !Character.isDigit(second.charAt(0))) {
                    error(input);
                    return "X";
                }
                // System.out.println(first + " " + dir1 + " " + second + " " + dir2);
            } catch (Exception e) {
                error(input);
                return "X";
            }

        } else {
            // last char is not a number, directions are split, we must have 2 tokens.
            // need to split directions out.
            for (int i = 0; i < token.length() - 1; i++) {
                first += token.charAt(i);
            }
            dir1 = token.charAt(token.length() - 1);
            token = lineScan.next();
            for (int i = 0; i < token.length() - 1; i++) {
                second += token.charAt(i);
            }
            dir2 = token.charAt(token.length() - 1);
            if (!Character.isDigit(first.charAt(0)) || !Character.isDigit(second.charAt(0))) {
                error(input);
                return "X";
            }
        }

        if (dir1 == 'N' || dir1 == 'S') {
            // the first number is a N/S direction
            result += first;
            result += " ";
            result += dir1;
            result += " ";
            if (dir2 == 'E' || dir2 == 'W') {
                // the second number must be an E/W direction
                result += second;
                result += " ";
                result += dir2;
                result += " ";
            } else {
                // if it's not, this is bad input
                error(input);
                return "X";
            }
        } else if (dir2 == 'N' || dir2 == 'S') {
            // the second number is a N/S direction
            result += second;
            result += " ";
            result += dir2;
            result += " ";
            if (dir1 == 'E' || dir1 == 'W') {
                // the first number must be an E/W direction
                result += first;
                result += " ";
                result += dir1;
                result += " ";
            } else {
                // if it's not, this is bad input
                error(input);
                return "X";
            }
        }

        if (dir1 == 'E' || dir1 == 'W') {
            if (dir2 != 'N' && dir2 != 'S') {
                error(input);
                return "X";
            }
        } else if (dir1 == 'N' || dir1 == 'S') {
            if (dir2 != 'E' && dir2 != 'W') {
                error(input);
                return "X";
            }
        }

        // if we've made it this far, we must have a valid set of directions
        // dump the label back in

        while (lineScan.hasNext()) {
            result += lineScan.next();
            result += " ";
        }

        // System.out.println(result);

        return result;
    }

    /**
     * Checks that the lat and long we have extracted actually make sense as
     * coordinates.
     *
     * @return true if valid, otherwise false.
     */
    public static boolean sanityCheck() {
        if (Double.parseDouble(flat) > 90 || Double.parseDouble(flat) < -90) {
            return false;
        }
        if (Double.parseDouble(flong) > 180 || Double.parseDouble(flong) < -180) {
            return false;
        }
        return true;
    }

    /**
     * Generates the header of the GeoJSON file.
     */
    public static void genHeader() {
        writer.println("{ \"type\": \"FeatureCollection\",\"features\": [");
        writer.flush();

    }

    /**
     * Generates a body segment of the GeoJSON file from the current lat/long/label.
     */
    public static void genBody() {

        if (!firstBody) {
            writer.print(",");

        }
        firstBody = false;
        writer.println("{ \"type\": \"Feature\",\"geometry\": {\"type\": \"Point\",\"coordinates\": [" +
                flat + ", " + flong + "]},\"properties\": {\"prop0\": \"" + flabel + "\"}}");
        writer.flush();

        // System.out.println("Lat: " + flat);
        // System.out.println("Long: " + flong);
        // System.out.println("Label: " + flabel);
    }

    /**
     * Generates the footer of the GeoJSON file.
     */
    public static void genFooter() {
        writer.println("]}");
        writer.flush();
        writer.close();

    }

    /**
     * Prints a line to stderr, Bad line: "LINE"
     * 
     * @param line String which contains errors.
     */
    public static void error(String line) {
        System.err.println("Bad line:  " + line);

    }
}