package com.github.catvod.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import com.github.catvod.Runtime;
import com.github.catvod.runtime.RuntimePlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;

public enum RuntimeModule {

    MPV("mpv", "MPV", "libplayer.so");

    public final String id;
    public final String title;
    private final String library;

    RuntimeModule(String id, String title, String library) {
        this.id = id;
        this.title = title;
        this.library = library;
    }

    public boolean isInstalled() {
        return external().info != null;
    }

    public boolean isExternal() {
        return true;
    }

    public String getPackageName() {
        return "com.fongmi.runtime." + id;
    }

    public String getCompatibilityId() {
        return "mpv-jni-ffmpeg-shared-v1";
    }

    public String getVersionName() throws IOException {
        PackageInfo info = getInstalledInfo();
        return info == null ? null : info.versionName;
    }

    public long getVersionCode() throws IOException {
        PackageInfo info = getInstalledInfo();
        return info == null ? 0 : versionCode(info);
    }

    private synchronized PackageInfo getInstalledInfo() throws IOException {
        PackageInfo info = external().info;
        if (info != null) verifyPackage(info);
        return info;
    }

    public static void refresh() {
        for (RuntimeModule module : values()) module.refreshPackage();
    }

    private synchronized void refreshPackage() {
        PackageInfo info = getExternalInfo();
        External state = external();
        PackageInfo previous = state.info;
        if (previous != null && info != null && previous.lastUpdateTime == info.lastUpdateTime && previous.applicationInfo.sourceDir.equals(info.applicationInfo.sourceDir)) return;
        state.info = info;
        state.metadata = null;
    }

    public PackageInfo validate(File file) throws IOException {
        PackageInfo info = Runtime.context().getPackageManager().getPackageArchiveInfo(file.getPath(), signatureFlags() | PackageManager.GET_SIGNATURES);
        Metadata metadata = readMetadata(file, info);
        verifyHostLibrary(metadata);
        try (ZipFile apk = new ZipFile(file)) {
            if (!getEntryClass().isEmpty()) verifyDex(apk);
            verifyPayloads(apk, metadata);
        }
        return info;
    }

    public synchronized File prepare() throws IOException {
        return new File(nativeDirectory(prepareInstalled()), library);
    }

    public synchronized File getDirectory() throws IOException {
        return directory(prepareInstalled());
    }

    private Metadata prepareInstalled() throws IOException {
        External state = external();
        Metadata metadata = state.loadedMetadata != null ? state.loadedMetadata : getMetadata();
        if (metadata == null) throw new IOException(title + " module is not installed");
        return metadata;
    }

    public String getEntryClass() {
        return "";
    }

    public synchronized <T extends RuntimePlugin> T entry(Class<T> type) {
        require();
        if (getEntryClass().isEmpty()) throw new IllegalStateException("Runtime does not provide a Java plugin");
        External state = external();
        if (state.entryFailure != null) throw state.entryFailure;
        try {
            if (state.entry == null) state.entry = createEntry(type, state);
            return type.cast(state.entry);
        } catch (ReflectiveOperationException | IOException | ClassCastException | LinkageError e) {
            state.entryFailure = new IllegalStateException("Unable to load " + title + " Java runtime; restart the app", e);
            throw state.entryFailure;
        }
    }

    private <T extends RuntimePlugin> T createEntry(Class<T> type, External state) throws ReflectiveOperationException, IOException {
        Metadata metadata = getMetadata();
        clean();
        pinAssets(metadata);
        String nativePath = nativeDirectory(metadata).getPath() + File.pathSeparator + Runtime.context().getApplicationInfo().nativeLibraryDir
                + File.pathSeparator + Runtime.context().getApplicationInfo().sourceDir + "!/lib/" + metadata.abi;
        DexClassLoader loader = new PluginLoader(this, metadata.info.applicationInfo.sourceDir, optimizedDirectory(metadata), nativePath);
        state.loadedMetadata = metadata;
        Class<?> implementation = Class.forName(getEntryClass(), true, loader);
        if (implementation.getClassLoader() != loader) throw new IOException("Runtime implementation is bundled in the host");
        return type.cast(implementation.getConstructor().newInstance());
    }

    public synchronized String readAsset(String path) {
        try {
            require();
            External state = external();
            Metadata metadata = state.loadedMetadata == null ? getMetadata() : state.loadedMetadata;
            return readAsset(state, metadata, path);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + title + " runtime asset", e);
        }
    }

    private String readAsset(External state, Metadata metadata, String path) throws IOException {
        String asset = "assets/" + path.replace("assets://", "");
        if (metadata.files.stream().noneMatch(payload -> payload.path.equals(asset))) throw new IOException("Runtime asset is not declared");
        ZipFile apk = state.assets == null ? new ZipFile(metadata.info.applicationInfo.sourceDir) : state.assets;
        try (InputStream input = apk.getInputStream(getAssetEntry(apk, asset))) {
            return Path.readOrThrow(input);
        } finally {
            if (apk != state.assets) apk.close();
        }
    }

    private String optimizedDirectory(Metadata metadata) throws IOException {
        if (Build.VERSION.SDK_INT >= 26) return null;
        File directory = new File(Runtime.context().getCodeCacheDir(), "runtimes/" + id + "/" + metadata.abi + "/" + metadata.digest);
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) throw new IOException("Unable to create runtime DEX cache");
        return directory.getPath();
    }

    private File directory(Metadata metadata) throws IOException {
        return nativeDirectory(metadata);
    }

    private File nativeDirectory(Metadata metadata) {
        return new File(metadata.info.applicationInfo.sourceDir + "!/lib/" + metadata.abi);
    }

    private void pinAssets(Metadata metadata) throws IOException {
        External state = external();
        if (state.assets != null || metadata.files.stream().noneMatch(payload -> payload.path.startsWith("assets/"))) return;
        state.assets = new ZipFile(metadata.info.applicationInfo.sourceDir);
        state.loadedMetadata = metadata;
    }

    private static ZipEntry getAssetEntry(ZipFile apk, String path) throws IOException {
        ZipEntry entry = apk.getEntry(path);
        if (entry == null) throw new IOException("Runtime asset is missing: " + path);
        return entry;
    }

    public synchronized void load() {
        if (!getEntryClass().isEmpty()) throw new IllegalStateException("Java runtime must load its own native libraries");
        load(System::load);
    }

    public synchronized void load(Consumer<String> loader) {
        require();
        External state = external();
        if (state.loadFailure != null) throw state.loadFailure;
        if (state.complete) return;
        try {
            Metadata metadata = state.loadedMetadata == null ? getMetadata() : state.loadedMetadata;
            verifyHostLibrary(metadata);
            pinAssets(metadata);
            loadLibraries(loader, state, metadata);
            state.complete = true;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + title, e);
        } catch (LinkageError e) {
            state.loadFailure = e;
            throw e;
        }
    }

    private void loadLibraries(Consumer<String> loader, External state, Metadata metadata) {
        File directory = nativeDirectory(metadata);
        for (String name : libraries()) loadLibrary(loader, state, metadata, directory, name);
    }

    private void loadLibrary(Consumer<String> loader, External state, Metadata metadata, File directory, String name) {
        File file = new File(directory, name);
        loader.accept(file.getPath());
        state.loadedMetadata = metadata;
        if (name.equals(library)) state.loaded = file;
    }

    public synchronized boolean isRestartRequired() throws IOException {
        External state = external();
        if (state.loadFailure != null || state.entryFailure != null) return true;
        if (state.loaded == null && state.loadedMetadata == null) return false;
        Metadata metadata = getMetadata();
        if (metadata == null || state.loadedMetadata != null && !state.loadedMetadata.digest.equals(metadata.digest)) return true;
        File loaded = state.loadedMetadata == null ? state.loaded.getParentFile() : nativeDirectory(state.loadedMetadata);
        return !loaded.equals(nativeDirectory(metadata));
    }

    public synchronized File getLoadedFile() {
        return external().loaded;
    }

    public synchronized void clean() throws IOException {
        External state = external();
        if (state.loaded != null || state.loadedMetadata != null) return;
        getMetadata();
        File[] abis = new File(Runtime.context().getNoBackupFilesDir(), "runtimes/" + id).listFiles();
        if (abis == null) return;
        for (File abi : abis) {
            File[] generations = abi.listFiles();
            if (generations == null) continue;
            for (File generation : generations) delete(generation);
        }
    }

    private synchronized Metadata getMetadata() throws IOException {
        External state = external();
        if (state.info == null) return null;
        if (state.metadata == null) state.metadata = readMetadata(new File(state.info.applicationInfo.sourceDir), state.info);
        return state.metadata;
    }

    private void verifyPayloads(ZipFile apk, Metadata metadata) throws IOException {
        for (Payload payload : metadata.files) {
            ZipEntry entry = apk.getEntry(payload.path);
            if (payload.path.startsWith("lib/") && entry != null && entry.getMethod() != ZipEntry.STORED) throw new IOException(title + " libraries must be stored uncompressed");
            verifyPayload(apk, metadata.abi, payload);
        }
    }

    private Metadata readMetadata(File file, PackageInfo info) throws IOException {
        verifyPackage(info);
        try (ZipFile apk = new ZipFile(file)) {
            ZipEntry entry = apk.getEntry("assets/runtime.json");
            if (entry == null || entry.getSize() <= 0 || entry.getSize() > 64 * 1024) throw new IOException("Invalid runtime metadata");
            JSONObject json;
            byte[] bytes;
            try (DataInputStream input = new DataInputStream(apk.getInputStream(entry))) {
                bytes = readMetadataBytes(input, entry.getSize());
                json = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            }
            return parseMetadata(apk, info, json, bytes);
        } catch (JSONException e) {
            throw new IOException("Invalid runtime metadata", e);
        }
    }

    private byte[] readMetadataBytes(DataInputStream input, long size) throws IOException {
        byte[] bytes = new byte[(int) size];
        input.readFully(bytes);
        return bytes;
    }

    private Metadata parseMetadata(ZipFile apk, PackageInfo info, JSONObject json, byte[] bytes) throws IOException, JSONException {
        String abi = PackageAbi.get();
        verifyMetadata(json, info, abi);
        List<Payload> files = readPayloads(json, abi);
        String hostCppSha256 = readHostCppSha256(json.getJSONObject("hostLibraries"));
        String digest = hash(new ByteArrayInputStream(bytes));
        return new Metadata(info, abi, digest, List.copyOf(files), hostCppSha256);
    }

    private void verifyMetadata(JSONObject json, PackageInfo info, String abi) throws IOException, JSONException {
        if (!id.equals(json.getString("module")) || !getPackageName().equals(json.getString("packageName")) || versionCode(info) != json.getLong("versionCode") || !json.getString("versionName").equals(info.versionName) || !getCompatibilityId().equals(json.getString("compatibilityId"))) throw new IOException("Incompatible runtime metadata");
        if (!abi.equals(json.getString("abi"))) throw new IOException("Incompatible runtime ABI");
        if (!("lib/" + abi + "/" + library).equals(json.getString("library"))) throw new IOException("Incompatible runtime ABI");
    }

    private void verifyPackage(PackageInfo info) throws IOException {
        if (info == null || info.applicationInfo == null || versionCode(info) <= 0 || info.versionName == null || info.versionName.isEmpty() || !getPackageName().equals(info.packageName) || info.splitNames != null && info.splitNames.length != 0) throw new IOException("Invalid runtime package");
        try {
            PackageInfo host = Runtime.context().getPackageManager().getPackageInfo(Runtime.context().getPackageName(), signatureFlags());
            Signature[] expected = signatures(host);
            Signature[] actual = signatures(info);
            if (expected == null || actual == null || expected.length == 0 || expected.length != actual.length || !Arrays.asList(expected).containsAll(Arrays.asList(actual))) throw new IOException("Invalid runtime signer");
        } catch (PackageManager.NameNotFoundException e) {
            throw new IOException("Unable to verify runtime signer", e);
        }
    }

    private static void verifyDex(ZipFile apk) throws IOException {
        var files = new HashSet<String>();
        for (var entries = apk.entries(); entries.hasMoreElements();) verifyDexEntry(apk, entries.nextElement(), files);
        if (!files.contains("classes.dex")) throw new IOException("Java runtime DEX is missing");
    }

    private static void verifyDexEntry(ZipFile apk, ZipEntry entry, HashSet<String> files) throws IOException {
        String name = entry.getName();
        if (!name.endsWith(".dex")) return;
        if (!name.matches("classes(?:[2-9]|1[0-6])?\\.dex") || !files.add(name) || entry.getSize() < 112 || entry.getSize() > 64 * 1024 * 1024) throw new IOException("Invalid runtime DEX");
        try (DataInputStream input = new DataInputStream(apk.getInputStream(entry))) {
            if (input.readInt() != 0x6465780a) throw new IOException("Invalid runtime DEX header");
        }
    }

    private static String readHostCppSha256(JSONObject libraries) throws IOException, JSONException {
        if (libraries.length() != 1 || !libraries.has("libc++_shared.so")) throw new IOException("Invalid MPV host libraries");
        String expected = libraries.getString("libc++_shared.so");
        if (!expected.matches("[0-9a-f]{64}")) throw new IOException("Invalid MPV host library digest");
        return expected;
    }

    private static void verifyHostLibrary(Metadata metadata) throws IOException {
        if (metadata.hostCppSha256 == null) return;
        try (ZipFile host = new ZipFile(Runtime.context().getApplicationInfo().sourceDir)) {
            ZipEntry entry = host.getEntry("lib/" + metadata.abi + "/libc++_shared.so");
            if (entry == null) throw new IOException("MPV requires host library libc++_shared.so");
            try (InputStream input = host.getInputStream(entry)) {
                if (!metadata.hostCppSha256.equals(hash(input))) throw new IOException("Incompatible MPV host library libc++_shared.so");
            }
        }
    }

    private List<Payload> readPayloads(JSONObject json, String abi) throws IOException, JSONException {
        var files = new ArrayList<Payload>();
        Payload primary = new Payload(json.getString("library"), json.getString("sha256"), json.getLong("size"));
        JSONArray entries = json.optJSONArray("files");
        if (entries == null) throw new IOException("Runtime payload list is missing");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject item = entries.getJSONObject(i);
            files.add(new Payload(item.getString("path"), item.getString("sha256"), item.getLong("size")));
        }
        if (files.isEmpty() || files.size() > 256 || !files.contains(primary)) throw new IOException("Invalid runtime payload list");
        HashSet<String> paths = verifyPayloadMetadata(files, abi);
        verifyRequiredPayloads(paths, abi);
        return files;
    }

    private HashSet<String> verifyPayloadMetadata(List<Payload> files, String abi) throws IOException {
        var paths = new HashSet<String>();
        for (Payload payload : files) {
            if (!paths.add(payload.path) || !payload.sha256.matches("[0-9a-f]{64}") || payload.size <= 0 || payload.size > 128 * 1024 * 1024) throw new IOException("Invalid runtime payload metadata");
            verifyPayloadPath(payload, abi);
        }
        return paths;
    }

    private void verifyPayloadPath(Payload payload, String abi) throws IOException {
        if (payload.path.startsWith("lib/")) {
            String prefix = "lib/" + abi + "/";
            if (!payload.path.startsWith(prefix) || !Arrays.asList(libraries()).contains(payload.path.substring(prefix.length()))) throw new IOException("Invalid runtime library path");
        } else {
            throw new IOException("Invalid runtime asset path");
        }
    }

    private void verifyRequiredPayloads(HashSet<String> paths, String abi) throws IOException {
        for (String name : libraries()) if (!paths.contains("lib/" + abi + "/" + name)) throw new IOException("Runtime library is missing");
    }

    private static final class PluginLoader extends DexClassLoader {

        private final RuntimeModule module;

        PluginLoader(RuntimeModule module, String apk, String directory, String nativePath) {
            super(apk, directory, nativePath, RuntimeModule.class.getClassLoader());
            this.module = module;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            boolean owned = name.startsWith("j$.") || name.startsWith("com.android.tools.r8.");
            if (!owned) return super.loadClass(name, resolve);
            synchronized (this) {
                Class<?> type = findLoadedClass(name);
                if (type == null) type = findClass(name);
                if (resolve) resolveClass(type);
                return type;
            }
        }
    }

    private static void verifyPayload(ZipFile apk, String abi, Payload payload) throws IOException {
        ZipEntry entry = apk.getEntry(payload.path);
        if (entry == null || entry.getSize() != payload.size) throw new IOException("Invalid runtime payload size");
        if (payload.path.endsWith(".so")) {
            try (DataInputStream input = new DataInputStream(apk.getInputStream(entry))) {
                verifyElf(input, abi);
            }
        }
        try (InputStream input = apk.getInputStream(entry)) {
            if (!payload.sha256.equals(hash(input))) throw new IOException("Invalid runtime payload digest");
        }
    }

    private static void verifyElf(InputStream input, String abi) throws IOException {
        byte[] header = new byte[20];
        new DataInputStream(input).readFully(header);
        verifyElf(header, abi);
    }

    private static void verifyElf(byte[] header, String abi) throws IOException {
        int machine = "arm64-v8a".equals(abi) ? 183 : 40;
        if (header[0] != 127 || header[1] != 'E' || header[2] != 'L' || header[3] != 'F' || header[4] != (machine == 183 ? 2 : 1) || header[5] != 1 || (header[18] & 255) != machine || header[19] != 0) throw new IOException("Invalid runtime ELF ABI");
    }

    private static String hash(InputStream input) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateDigest(digest, input);
            return hex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("Unable to hash runtime library", e);
        }
    }

    private static void updateDigest(MessageDigest digest, InputStream input) throws IOException {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) value.append(Character.forDigit((b & 255) >>> 4, 16)).append(Character.forDigit(b & 15, 16));
        return value.toString();
    }

    private static int signatureFlags() {
        return Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
    }

    private static Signature[] signatures(PackageInfo info) {
        return Build.VERSION.SDK_INT >= 28 ? info.signingInfo == null ? null : info.signingInfo.getApkContentsSigners() : info.signatures;
    }

    private static long versionCode(PackageInfo info) {
        return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
    }

    private PackageInfo getExternalInfo() {
        try {
            return Runtime.context().getPackageManager().getPackageInfo(getPackageName(), signatureFlags());
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private String[] libraries() {
        return new String[]{"libmpv.so", "libplayer.so"};
    }

    private static void delete(File file) throws IOException {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        if (!file.delete() && file.exists()) throw new IOException("Unable to remove old runtime file");
    }

    private record Payload(String path, String sha256, long size) {
    }

    private record Metadata(PackageInfo info, String abi, String digest, List<Payload> files, String hostCppSha256) {
    }

    private static class External {
        RuntimePlugin entry;
        RuntimeException entryFailure;
        volatile PackageInfo info;
        Metadata metadata;
        Metadata loadedMetadata;
        ZipFile assets;
        File loaded;
        LinkageError loadFailure;
        boolean complete;
    }

    private static class Packages {
        static final External[] states = getStates();

        private static External[] getStates() {
            External[] states = new External[values().length];
            for (RuntimeModule module : values()) {
                External state = new External();
                state.info = module.getExternalInfo();
                states[module.ordinal()] = state;
            }
            return states;
        }
    }

    private External external() {
        return Packages.states[ordinal()];
    }

    public synchronized void require() {
        if (!isInstalled()) throw new MissingException(this);
        try {
            getMetadata();
        } catch (IOException e) {
            throw new IllegalStateException("Invalid " + title + " module", e);
        }
    }

    public static class MissingException extends IllegalStateException {

        public final RuntimeModule module;

        public MissingException(RuntimeModule module) {
            super(module.title + " module is not installed");
            this.module = module;
        }

        public static MissingException find(Throwable error) {
            for (Throwable current = error; current != null; current = current.getCause()) if (current instanceof MissingException missing) return missing;
            return null;
        }
    }

}
