
package com.universal.erp.platform.ui;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class LanAddress {

    public record Candidate(String name, String ip) {
    }

    private LanAddress() {
    }

    public static List<Candidate> list() {
        List<Candidate> out = new ArrayList<>();

        try {
            for (NetworkInterface ni :
                    Collections.list(NetworkInterface.getNetworkInterfaces())) {

                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }

                for (InetAddress address :
                        Collections.list(ni.getInetAddresses())) {

                    if (address instanceof Inet4Address
                            && address.isSiteLocalAddress()) {

                        out.add(new Candidate(
                                ni.getDisplayName(),
                                address.getHostAddress()
                        ));
                    }
                }
            }
        } catch (SocketException ignored) {
            // Tarmoq interfeyslarini olishda xato bo'lsa,
            // bo'sh ro'yxat qaytariladi.
        }

        out.sort(Comparator.comparingInt(LanAddress::score));

        return out;
    }

    private static int score(Candidate candidate) {
        String name = candidate.name().toLowerCase(Locale.ROOT);

        if (name.contains("virtual")
                || name.contains("vmware")
                || name.contains("vbox")
                || name.contains("hyper-v")
                || name.contains("vethernet")
                || name.contains("wsl")
                || name.contains("docker")
                || name.contains("tap")
                || name.contains("vpn")
                || name.contains("loopback")) {
            return 9;
        }

        if (name.contains("wi-fi")
                || name.contains("wifi")
                || name.contains("wireless")
                || name.contains("wlan")) {
            return 0;
        }

        return 5;
    }
}
