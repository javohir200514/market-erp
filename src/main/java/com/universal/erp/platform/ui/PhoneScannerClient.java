package com.universal.erp.platform.ui;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;



import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Telefonni tarmoqdan o'zi topadi (hotspot), unga ulanadi, shtrix-kodlarni qabul qiladi
 * va mahsulot so'rovlariga javob beradi. Hech qanday sozlama kerak emas.
 */
@Component
public class PhoneScannerClient {

    private static final int PORT = 8765;
    private static final String HELLO = "UZMARK-PHONE";
    private static final Path STORE = Path.of(System.getProperty("user.home"), ".uzmark-phone");

    private final PhoneProductService products;
    private final ObjectMapper mapper = new ObjectMapper();

    private final CopyOnWriteArrayList<Consumer<String>> codeListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Boolean>> statusListeners = new CopyOnWriteArrayList<>();

    private volatile String lastHost = "";
    private volatile String company = "";
    private volatile String phoneHost = "";
    private volatile boolean connected;
    private volatile boolean running;
    private volatile Thread worker;
    private volatile Socket socket;

    public PhoneScannerClient(PhoneProductService products) {
        this.products = products;
        loadStore();
    }

    @PostConstruct
    void init() {
        running = true;
        Thread t = new Thread(this::loop, "pos-phone-client");
        t.setDaemon(true);
        worker = t;
        t.start();
    }

    @PreDestroy
    void shutdown() {
        running = false;
        closeQuietly(socket);
        Thread t = worker;
        if (t != null) {
            t.interrupt();
        }
    }

    // ---------------------------------------------------------------- API

    public synchronized void setCompany(String c) {
        if (c != null && !c.isBlank() && !c.equals(company)) {
            company = c;
            saveStore();
        }
    }

    /** Hozirgi ulanishni uzib, telefonni qaytadan qidirtiradi. */
    public void reconnect() {
        closeQuietly(socket);
    }

    public boolean isConnected() { return connected; }
    public String phoneHost() { return phoneHost; }

    public Runnable addCodeListener(Consumer<String> l) {
        codeListeners.add(l);
        return () -> codeListeners.remove(l);
    }

    public Runnable addStatusListener(Consumer<Boolean> l) {
        statusListeners.add(l);
        return () -> statusListeners.remove(l);
    }

    // ---------------------------------------------------------------- saqlash

    private void loadStore() {
        try {
            if (Files.exists(STORE)) {
                List<String> lines = Files.readAllLines(STORE, StandardCharsets.UTF_8);
                if (lines.size() > 0) {
                    lastHost = lines.get(0).trim();
                }
                if (lines.size() > 1) {
                    company = lines.get(1).trim();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private synchronized void saveStore() {
        try {
            Files.writeString(STORE, lastHost + "\n" + company + "\n", StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    // ---------------------------------------------------------------- asosiy sikl

    private void loop() {
        while (running) {
            Socket s = null;
            try {
                s = discover();
                if (s == null) {
                    sleep(1500);
                    continue;
                }
                socket = s;
                s.setTcpNoDelay(true);
                s.setKeepAlive(true);
                s.setSoTimeout(5000);
                phoneHost = s.getInetAddress().getHostAddress();
                if (!phoneHost.equals(lastHost)) {
                    lastHost = phoneHost;
                    saveStore();
                }
                setConnected(true);
                readLoop(s);
            } catch (Exception ex) {
                System.err.println("[phone] uzildi: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            } finally {
                socket = null;
                setConnected(false);
                closeQuietly(s);
            }
            sleep(1000);
        }
    }

    private void readLoop(Socket s) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        OutputStream out = s.getOutputStream();
        long lastRx = System.currentTimeMillis();
        while (running && !s.isClosed()) {
            try {
                String line = in.readLine();
                if (line == null) {
                    return;
                }
                lastRx = System.currentTimeMillis();
                String text = line.trim();
                if (text.isEmpty() || "PONG".equals(text) || "PING".equals(text)) {
                    continue;
                }
                if (text.startsWith("{")) {
                    String reply = handleRequest(text);
                    out.write((reply + "\n").getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    continue;
                }
                for (Consumer<String> l : codeListeners) {
                    try {
                        l.accept(text);
                    } catch (RuntimeException ignored) {
                    }
                }
            } catch (SocketTimeoutException e) {
                if (System.currentTimeMillis() - lastRx > 15_000) {
                    return;
                }
                out.write("PING\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }
    }

    private String handleRequest(String line) {
        String rid = "";
        Map<String, Object> res = new LinkedHashMap<>();
        try {
            JsonNode req = mapper.readTree(line);
            rid = req.path("rid").asText("");
            if (company.isEmpty()) {
                throw new IllegalStateException("Kompaniya noma'lum. Kompyuterda ERP dagi POS sahifasini bir marta oching.");
            }
            res.putAll(products.handle(company, req));
            res.put("ok", true);
        } catch (Exception ex) {
            res.clear();
            res.put("ok", false);
            res.put("error", ex.getMessage() == null ? "Xatolik" : ex.getMessage());
        }
        res.put("rid", rid);
        try {
            return mapper.writeValueAsString(res);
        } catch (Exception e) {
            return "{\"rid\":\"" + rid + "\",\"ok\":false,\"error\":\"Javobni tayyorlab bo'lmadi\"}";
        }
    }

    // ---------------------------------------------------------------- telefonni topish

    /** Avval oxirgi telefon, keyin butun tarmoq. Telefon bo'lsa, ulangan Socket qaytadi. */
    private Socket discover() {
        String last = lastHost;
        if (!last.isEmpty()) {
            Socket s = probe(last, 1500);
            if (s != null) {
                return s;
            }
        }
        List<String> targets = scanTargets();
        if (targets.isEmpty()) {
            return null;
        }
        AtomicReference<Socket> winner = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(targets.size());
        ExecutorService pool = Executors.newFixedThreadPool(48, r -> {
            Thread t = new Thread(r, "pos-phone-scan");
            t.setDaemon(true);
            return t;
        });
        try {
            for (String ip : targets) {
                pool.execute(() -> {
                    try {
                        if (winner.get() != null) {
                            return;
                        }
                        Socket s = probe(ip, 700);
                        if (s != null && !winner.compareAndSet(null, s)) {
                            closeQuietly(s);
                        }
                    } finally {
                        done.countDown();
                    }
                });
            }
            while (running && winner.get() == null && done.getCount() > 0) {
                done.await(200, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
        }
        return winner.get();
    }

    /** Ulanadi va telefon salomini (UZMARK-PHONE) tekshiradi. Telefon emas bo'lsa null. */
    private static Socket probe(String host, int timeoutMs) {
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, PORT), timeoutMs);
            s.setSoTimeout(1500);
            InputStream in = s.getInputStream();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            int b;
            while ((b = in.read()) != -1 && b != '\n' && buf.size() < 64) {
                buf.write(b);
            }
            if (HELLO.equals(buf.toString(StandardCharsets.UTF_8).trim())) {
                return s;
            }
        } catch (Exception ignored) {
        }
        closeQuietly(s);
        return null;
    }

    /** Haqiqiy tarmoqlardagi (virtual/VPN emas) hamma manzillar, o'zimiznikidan tashqari. */
    private static List<String> scanTargets() {
        Set<String> res = new LinkedHashSet<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual() || ni.isPointToPoint() || isVirtualName(ni)) {
                    continue;
                }
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress a = ia.getAddress();
                    if (!(a instanceof Inet4Address) || a.isLinkLocalAddress()) {
                        continue;
                    }
                    byte[] ipb = a.getAddress();
                    int ip = ((ipb[0] & 255) << 24) | ((ipb[1] & 255) << 16) | ((ipb[2] & 255) << 8) | (ipb[3] & 255);
                    int prefix = Math.max(ia.getNetworkPrefixLength(), 24);
                    int mask = prefix >= 32 ? -1 : (-1 << (32 - prefix));
                    int net = ip & mask;
                    int count = ~mask;               // xostlar soni + 1
                    for (int i = 1; i < count; i++) {
                        int h = net | i;
                        if (h == ip) {
                            continue;
                        }
                        res.add(((h >>> 24) & 255) + "." + ((h >>> 16) & 255) + "." + ((h >>> 8) & 255) + "." + (h & 255));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>(res);
    }

    private static boolean isVirtualName(NetworkInterface ni) {
        String n = ((ni.getDisplayName() == null ? "" : ni.getDisplayName()) + " " + ni.getName()).toLowerCase();
        String[] bad = {"virtual", "vethernet", "hyper-v", "vmware", "vbox", "wsl", "tap", "ovpn",
                "tunnel", "pseudo", "bluetooth", "vpn", "docker", "loopback"};
        for (String b : bad) {
            if (n.contains(b)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- yordamchi

    private void setConnected(boolean v) {
        if (connected == v) {
            return;
        }
        connected = v;
        for (Consumer<Boolean> l : statusListeners) {
            try {
                l.accept(v);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static void closeQuietly(Socket s) {
        if (s != null) {
            try {
                s.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}