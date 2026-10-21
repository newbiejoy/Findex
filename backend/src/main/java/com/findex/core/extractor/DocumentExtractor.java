package com.findex.core.extractor;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DocumentExtractor {
    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024;

    private static final Set<String> SUPPORTED_TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
        "txt", "md", "markdown", "java", "py", "js", "ts", "jsx", "tsx", "html",
        "htm", "css", "scss", "json", "xml", "csv", "sql", "properties", "yaml",
        "yml", "c", "cpp", "h", "hpp", "rs", "go", "sh", "bat", "cmd", "ps1", "log"
    ));

    public boolean isSupported(Path path) {
        if (!Files.isRegularFile(path)) return false;
        String ext = getExtension(path);
        return SUPPORTED_TEXT_EXTENSIONS.contains(ext) || "docx".equals(ext) || "pdf".equals(ext);
    }

    public String extractText(Path path) throws IOException {
        long size = Files.size(path);
        if (size > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds 15MB safety limit (" + (size / (1024 * 1024)) + "MB)");
        }

        String ext = getExtension(path);

        if ("pdf".equals(ext)) {
            return extractFromPdf(path);
        } else if ("docx".equals(ext)) {
            return extractFromDocx(path);
        } else {
            return extractFromText(path);
        }
    }

    private String extractFromText(Path path) throws IOException {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {

            return Files.readString(path, StandardCharsets.ISO_8859_1);
        }
    }

    private String extractFromPdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            if (document.isEncrypted()) {
                throw new IOException("Encrypted PDF files are not supported");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractFromDocx(Path path) throws IOException {
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            ZipEntry entry = zipFile.getEntry("word/document.xml");
            if (entry == null) {
                throw new IOException("Invalid DOCX file: missing word/document.xml");
            }

            try (InputStream is = zipFile.getInputStream(entry)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                org.w3c.dom.Document xmlDoc = builder.parse(is);

                org.w3c.dom.NodeList textNodes = xmlDoc.getElementsByTagName("w:t");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < textNodes.getLength(); i++) {
                    sb.append(textNodes.item(i).getTextContent()).append(" ");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            throw new IOException("Failed to extract DOCX text: " + e.getMessage(), e);
        }
    }

    public static String getExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
}
