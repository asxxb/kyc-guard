package io.github.kycguard.generator.service;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class RandomDataPool {

    public static final List<String> FIRST_NAMES = List.of(
            "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
            "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
            "Thomas", "Sarah", "Charles", "Karen", "Christopher", "Nancy", "Daniel", "Lisa",
            "Matthew", "Betty", "Anthony", "Margaret", "Mark", "Sandra", "Donald", "Ashley",
            "Alexander", "Dmitri", "Sergei", "Vladimir", "Mikhail", "Nikolai", "Alexei", "Boris",
            "Mohammed", "Ahmed", "Ali", "Hassan", "Hussein", "Omar", "Tariq", "Mustafa",
            "Wei", "Li", "Zhang", "Liu", "Chen", "Yang", "Huang", "Zhao"
    );

    public static final List<String> LAST_NAMES = List.of(
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
            "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White",
            "Ivanov", "Smirnov", "Kuznetsov", "Popov", "Vasiliev", "Petrov", "Sokolov", "Mikhailov",
            "Al-Mansoor", "Al-Hashimi", "Al-Fassi", "Khoury", "Haddad", "Nasser", "Sarkis", "Darwish",
            "Wang", "Li", "Zhang", "Liu", "Chen", "Yang", "Huang", "Wu"
    );

    public static final List<String> COMPANY_PREFIXES = List.of(
            "Apex", "Global", "Summit", "Vanguard", "Pinnacle", "Horizon", "Orion", "Alpha",
            "Titan", "Meridian", "Omega", "Crest", "Vertex", "Atlas", "Pacific", "Atlantic"
    );

    public static final List<String> COMPANY_SUFFIXES = List.of(
            "Holdings Ltd", "Trading Corp", "Investments Inc", "Consulting LLC", "Capital AG",
            "International S.A.", "Logistics Group", "Technologies Corp", "Enterprises Ltd", "Partners LLP"
    );

    public static final List<String> COUNTRIES = List.of(
            "US", "GB", "DE", "FR", "IT", "ES", "CH", "NL", "AE", "SG", "JP", "CN", "RU", "IR", "SY", "KP"
    );

    public static final List<String> CITIES = List.of(
            "New York", "London", "Frankfurt", "Paris", "Zurich", "Singapore", "Dubai", "Tokyo",
            "Milan", "Amsterdam", "Berlin", "Geneva", "Vienna", "Hong Kong", "Toronto", "Sydney"
    );

    public static final List<String> STREETS = List.of(
            "Main Street", "Broadway", "Wall Street", "Oxford Street", "Regent Street", "Bahnhofstrasse",
            "Champs-Elysees", "Kurfuerstendamm", "Shenton Way", "Sheikh Zayed Road", "Ginza Ave"
    );

    public static final List<String> SOURCES = List.of(
            "OFAC-SDN", "EU-FSF", "UN-SANCTIONS", "UK-HMT", "PEP-GLOBAL"
    );

    public static final Map<String, String> TRANSLITERATIONS = Map.ofEntries(
            Map.entry("Mohammed", "Muhammad"),
            Map.entry("Muhammad", "Mohamed"),
            Map.entry("Alexander", "Aleksandr"),
            Map.entry("Aleksandr", "Alexander"),
            Map.entry("Dmitri", "Dmitry"),
            Map.entry("Sergei", "Sergey"),
            Map.entry("Stephen", "Steven"),
            Map.entry("Smith", "Smyth"),
            Map.entry("Johnson", "Jonson"),
            Map.entry("Mikhail", "Michael")
    );

    private RandomDataPool() {
    }

    public static String createFuzzyVariant(String name, Random random) {
        String[] parts = name.split("\\s+");
        int strategy = random.nextInt(4);

        if (strategy == 0 && parts.length >= 2) {
            // Swap first and last name
            return parts[parts.length - 1] + " " + parts[0];
        } else if (strategy == 1) {
            // Transliterate a known word if present
            for (int i = 0; i < parts.length; i++) {
                if (TRANSLITERATIONS.containsKey(parts[i])) {
                    parts[i] = TRANSLITERATIONS.get(parts[i]);
                    return String.join(" ", parts);
                }
            }
        } else if (strategy == 2 && name.length() > 4) {
            // Introduce a 1-character typo (swap two adjacent letters)
            int idx = random.nextInt(name.length() - 2) + 1;
            char[] chars = name.toCharArray();
            char temp = chars[idx];
            chars[idx] = chars[idx + 1];
            chars[idx + 1] = temp;
            return new String(chars);
        }

        // Default variant: add common honorific prefix
        return "Mr. " + name;
    }
}
