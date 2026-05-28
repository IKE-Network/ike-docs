package network.ike.docs.ingest;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

/// Download and extract `.tar.gz` (TGZ) tarballs from HTTP/HTTPS sources.
///
/// Used by corpus ingesters to fetch upstream content packaged as
/// tarballs — most commonly FHIR NPM packages from
/// `packages.fhir.org`, but any TGZ source works.
///
/// Both phases — download and extract — defend against the standard
/// hazards: the downloader honors redirects and applies generous-but-
/// finite timeouts; the extractor refuses entries that would write
/// outside the target directory ("zip slip").
public final class TarballDownloader {

    private final HttpClient httpClient;

    /// Construct a downloader with the default HTTP client (follows
    /// redirects, 30-second connect timeout).
    public TarballDownloader() {
        this(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(30))
                .build());
    }

    /// Construct a downloader with a caller-supplied HTTP client.
    ///
    /// @param httpClient the client to use; must not be null
    public TarballDownloader(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /// Fetch the URL into a temp file. The caller owns the returned
    /// path and is responsible for deleting it when done.
    ///
    /// @param url the source URL
    /// @param requestTimeout per-request timeout (e.g. 2 minutes for large packages)
    /// @return path to the downloaded tarball in the system temp directory
    /// @throws IOException on HTTP-level or I/O failure
    /// @throws InterruptedException if the calling thread is interrupted
    public Path download(URI url, Duration requestTimeout) throws IOException, InterruptedException {
        Path target = Files.createTempFile("ike-ingest-tarball-", ".tgz");
        HttpRequest req = HttpRequest.newBuilder(url)
                .timeout(requestTimeout)
                .GET()
                .build();
        HttpResponse<Path> resp = httpClient.send(req,
                HttpResponse.BodyHandlers.ofFile(target,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE));
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + resp.statusCode() + " fetching " + url);
        }
        return target;
    }

    /// Fetch the URL into a temp file using a default 2-minute timeout.
    ///
    /// @see #download(URI, Duration)
    public Path download(URI url) throws IOException, InterruptedException {
        return download(url, Duration.ofMinutes(2));
    }

    /// Extract a TGZ tarball into a fresh temp directory. The caller
    /// owns the returned directory and is responsible for deleting it
    /// when done.
    ///
    /// Defends against path traversal — any entry whose normalized
    /// target would escape the temp directory causes the extraction
    /// to fail with an IOException.
    ///
    /// @param tarball path to the .tar.gz file
    /// @return path to the extraction root (a fresh temp directory)
    /// @throws IOException on I/O failure or detected path traversal
    public Path extract(Path tarball) throws IOException {
        Path tempDir = Files.createTempDirectory("ike-ingest-extracted-");
        try (InputStream fin = Files.newInputStream(tarball);
             BufferedInputStream bin = new BufferedInputStream(fin);
             GzipCompressorInputStream gz = new GzipCompressorInputStream(bin);
             TarArchiveInputStream tar = new TarArchiveInputStream(gz)) {
            TarArchiveEntry e;
            while ((e = tar.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                Path out = tempDir.resolve(e.getName()).normalize();
                if (!out.startsWith(tempDir)) {
                    throw new IOException("Tar entry escapes target dir: " + e.getName());
                }
                Files.createDirectories(out.getParent());
                Files.copy(tar, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return tempDir;
    }

    /// Convenience: download then extract in one call.
    ///
    /// @param url the source URL
    /// @return path to the extraction root
    /// @throws IOException on HTTP or I/O failure
    /// @throws InterruptedException if the calling thread is interrupted
    public Path downloadAndExtract(URI url) throws IOException, InterruptedException {
        Path tarball = download(url);
        try {
            return extract(tarball);
        } finally {
            Files.deleteIfExists(tarball);
        }
    }
}
