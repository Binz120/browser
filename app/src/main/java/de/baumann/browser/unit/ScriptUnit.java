package de.baumann.browser.unit;

import static de.baumann.browser.database.UserScript.DOC_END;
import static de.baumann.browser.database.UserScript.DOC_START;
import static de.baumann.browser.database.UserScript.META_END;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.baumann.browser.database.UserScript;
import de.baumann.browser.database.UserScriptsHelper;
import de.baumann.browser.view.NinjaToast;

public class ScriptUnit {
    private static List<UserScript> scriptsDocStart;
    private static List<UserScript> scriptsDocEnd;

    public static void initScripts(Context context){
        UserScriptsHelper userScriptsHelper = new UserScriptsHelper(context);
        scriptsDocStart = userScriptsHelper.getActiveScriptsByType(DOC_START);
        scriptsDocEnd = userScriptsHelper.getActiveScriptsByType(DOC_END);

        for (UserScript script:scriptsDocStart) {
            Scanner scanner = new Scanner(script.getScript());
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("@match")) script.getMatchPatterns().add(getRegex(line.split("@match")[1].trim()));
                if (line.contains(META_END)) break;
            }
            scanner.close();
        }

        for (UserScript script:scriptsDocEnd) {
            Scanner scanner = new Scanner(script.getScript());
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("@match")) script.getMatchPatterns().add(getRegex(line.split("@match")[1].trim()));
                if (line.contains(META_END)) break;
            }
            scanner.close();
        }
    }

    @NonNull
    private static String getRegex(String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*://", "(http|https)://")
                .replace("*", ".*")
                .replace("?", "\\?")
                .replace("/", "\\/");

        // path must contain at least a forward slash. The slash by itself matches any path, as if it were followed by a wildcard (/*).
        // If the pattern ends with /, add an asterisk after it
        if (regex.endsWith("/")) {
            regex += ".*";
        }
        return regex;
    }

    private static boolean matchesPattern(Context context, String url, String pattern) {
        // Use regular expressions to match the URL against the pattern

        String urlFormatPattern = "^(http|https)://[a-z0-9.-]+/.*$";
        if (!url.toLowerCase().matches(urlFormatPattern)) return false;  //not a valid URL

        try {
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(url);
            return matcher.matches();
        } catch (Exception e){
            NinjaToast.show(context,e.toString());
            return false;}

    }

    public static List<UserScript> findScriptsToExecute(Context context, String url, String type) {
        List<UserScript> matchedScripts = new ArrayList<>();

        for (UserScript userScript: type.equals(DOC_START) ? scriptsDocStart : scriptsDocEnd){
            List<String> matchPatterns = userScript.getMatchPatterns();
            boolean isMatch = false;
            for (String pattern : matchPatterns) {
                if (matchesPattern(context, url, pattern)) {
                    isMatch = true;
                    break; // Exit the loop if a match is found
                }
            }
            if (isMatch) matchedScripts.add(userScript);
        }
        return matchedScripts;
    }
}